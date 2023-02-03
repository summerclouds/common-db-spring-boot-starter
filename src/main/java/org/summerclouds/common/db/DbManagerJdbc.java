/**
 * Copyright (C) 2022 Mike Hummel (mh@mhus.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.summerclouds.common.db;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.summerclouds.common.core.activator.Activator;
import org.summerclouds.common.core.cfg.CfgBoolean;
import org.summerclouds.common.core.concurrent.Lock;
import org.summerclouds.common.core.concurrent.ThreadLock;
import org.summerclouds.common.core.error.AccessDeniedException;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.RC;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MDate;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.tool.MTracing;
import org.summerclouds.common.core.tracing.IScope;
import org.summerclouds.common.core.util.FallbackMap;
import org.summerclouds.common.db.model.Field;
import org.summerclouds.common.db.model.MutableDbSchema;
import org.summerclouds.common.db.model.Table;
import org.summerclouds.common.db.query.AQuery;
import org.summerclouds.common.db.sql.DbConnection;
import org.summerclouds.common.db.sql.DbPool;
import org.summerclouds.common.db.sql.DbResult;
import org.summerclouds.common.db.sql.DbStatement;
import org.summerclouds.common.db.sql.MetadataBundle;
import org.summerclouds.common.db.sql.SqlDialectCreateContext;
import org.summerclouds.common.db.util.DbProperties;
import org.summerclouds.common.db.util.ParserJdbcDebug;
import org.summerclouds.common.db.util.Property;

/**
 * The implementation hold the table definitions and handle all operations on the objects. It's the
 * central point of access.
 */
public class DbManagerJdbc extends DbManager implements DbObjectHandler {

    private static CfgBoolean CFG_DEBUG_PARSER =
            new CfgBoolean(DbManagerJdbc.class, "debugParser", false);
    public static final String DATABASE_VERSION = "db.version";
    public static final String DATABASE_CREATED = "db.created";
    public static final String DATABASE_MANAGER_VERSION = "db.manager.version";

    public static final String MANAGER_VERSION = "1.0";

    private static final long MAX_LOCK = 1000 * 60 * 10;

    private DbSchema schema;
    private DbPool pool;
    private DbPool poolRo;
    private HashMap<String, Table> cIndex = new HashMap<String, Table>();
    private HashMap<String, Object> nameMapping;
    private Map<String, Object> nameMappingRO;

    private DbProperties schemaPersistence;
    private MetadataBundle caoBundle;
    private Activator activator;
    private Lock reloadLock = new ThreadLock("reload");
    private String dataSourceName;

    public DbManagerJdbc(String dataSourceName, DbPool pool, DbPool poolRo, DbSchema schema)
            throws Exception {
        this(dataSourceName, pool, poolRo, schema, false);
    }

    /**
     * Create a new manager instance, a db connection and a database schema is needed.
     *
     * @param dataSourceName
     * @param pool
     * @param poolRo
     * @param schema
     * @param cleanup
     * @throws MException
     */
    public DbManagerJdbc(
            String dataSourceName, DbPool pool, DbPool poolRo, DbSchema schema, boolean cleanup)
            throws MException {
        this.dataSourceName = dataSourceName;
        this.pool = pool;
        this.poolRo = poolRo == null ? pool : poolRo;
        this.schema = schema;
        this.activator = pool.getProvider().getActivator();
        initDatabase(cleanup);
    }

    public DbManagerJdbc(DbPool pool, DbSchema schema, Activator activator) throws Exception {
        this(pool, schema, activator, false);
    }

    public DbManagerJdbc(DbPool pool, DbSchema schema, Activator activator, boolean cleanup)
            throws Exception {
        this.pool = pool;
        this.schema = schema;
        this.activator = activator == null ? pool.getProvider().getActivator() : activator;
        initDatabase(cleanup);
    }

    @Override
    public <T> T getObjectByQualification(AQuery<T> qualification) throws MException {
        return getByQualification(qualification).getNextAndClose();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DbCollection<T> getByQualification(
            Class<T> clazz, String qualification, Map<String, Object> attributes)
            throws MException {
        return (DbCollection<T>)
                getByQualification(null, (Object) clazz, null, qualification, attributes);
    }

    @Override
    public <T> DbCollection<T> getByQualification(
            T object, String qualification, Map<String, Object> attributes) throws MException {
        return getByQualification(null, object, null, qualification, attributes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DbCollection<T> getByQualification(AQuery<T> qualification) throws MException {
        qualification.doFinal();
        return (DbCollection<T>)
                getByQualification(
                        null,
                        qualification.getType(),
                        null,
                        toQualification(qualification),
                        qualification.getAttributes());
    }

    @Override
    public <T> String toQualification(AQuery<T> qualification) {
        StringBuilder buffer = new StringBuilder();
        qualification.setContext(new SqlDialectCreateContext(this, buffer));
        getPool().getDialect().createQuery(qualification, qualification);
        return buffer.toString();
    }

    /**
     * Get an collection of objects by it's qualification. The qualification is the WHERE part of a
     * query. e.g. "$db.table.name$ like 'Joe %'"
     *
     * @param <T> Type of the object
     * @param con The connection or null
     * @param object a representation of the object (empty object) or a object to recycle in the
     *     collection.
     * @param registryName The name of the registry (if not default) or null
     * @param qualification The WHERE string
     * @param attributes attributes or null if not needed
     * @return A collection with the results
     * @throws MException
     */
    @Override
    public <T> DbCollection<T> getByQualification(
            DbConnection con,
            T object,
            String registryName,
            String qualification,
            Map<String, Object> attributes)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        Class<?> clazz = schema.findClassForObject(object, this);
        String s = createSqlSelect(clazz, "*", qualification);
        log().t("getByQualification", registryName == null ? clazz : registryName, s, attributes);
        return executeQuery(con, object, registryName, s, attributes);
    }

    @Override
    public String createSqlSelect(Class<?> clazz, String columns, String qualification) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append(columns)
                .append(" FROM $db.")
                .append(getMappingName(clazz))
                .append("$ ");
        if (MString.isSet(qualification)) {
            String low =
                    qualification
                            .trim()
                            .substring(0, Math.min(qualification.length(), 6))
                            .toLowerCase();
            if (low.startsWith("order ") || low.startsWith("limit ")) sql.append(qualification);
            else sql.append("WHERE ").append(qualification);
        }
        String s = sql.toString();
        return s;
    }

    @Override
    public <T> long getCountAll(Class<T> clazz) throws MException {
        return getCountByQualification(null, (Object) clazz, null, "", null);
    }

    @Override
    public <T> long getCountByQualification(
            Class<T> clazz, String qualification, Map<String, Object> attributes)
            throws MException {
        return getCountByQualification(null, (Object) clazz, null, qualification, attributes);
    }

    @Override
    public <T> long getCountByQualification(
            T object, String qualification, Map<String, Object> attributes) throws MException {
        return getCountByQualification(null, object, null, qualification, attributes);
    }

    @Override
    public <T> long getCountByQualification(AQuery<T> qualification) throws MException {
        qualification.doFinal();
        return getCountByQualification(
                null,
                qualification.getType(),
                null,
                toQualification(qualification),
                qualification.getAttributes());
    }

    /**
     * Returns the count of all found objects for the qualification. It's faster than loading all
     * data from the database with getByQualification.
     *
     * @param con
     * @param object
     * @param registryName
     * @param qualification
     * @param attributes
     * @return x
     * @throws MException
     */
    @Override
    public <T> long getCountByQualification(
            DbConnection con,
            T object,
            String registryName,
            String qualification,
            Map<String, Object> attributes)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        Class<?> clazz = schema.findClassForObject(object, this);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT count(*) AS count FROM $db.").append(getMappingName(clazz)).append("$ ");
        if (MString.isSet(qualification)) {
            String low = qualification.trim().toLowerCase();
            if (low.startsWith("order ") || low.startsWith("limit ")) sql.append(qualification);
            else sql.append("WHERE ").append(qualification);
        }

        return executeCountQuery(con, "count", sql.toString(), attributes);
    }

    @Override
    public <T> long getMax(Class<T> clazz, String field) throws MException {
        return getMaxByQualification(null, (Object) clazz, null, field, "", null);
    }

    @Override
    public <T> long getMaxByQualification(
            Class<T> clazz, String field, String qualification, Map<String, Object> attributes)
            throws MException {
        return getMaxByQualification(null, (Object) clazz, null, field, qualification, attributes);
    }

    @Override
    public <T> long getMaxByQualification(
            T object, String field, String qualification, Map<String, Object> attributes)
            throws MException {
        return getMaxByQualification(null, object, null, field, qualification, attributes);
    }

    @Override
    public <T> long getMaxByQualification(String field, AQuery<T> qualification) throws MException {
        qualification.doFinal();
        return getMaxByQualification(
                null,
                qualification.getType(),
                null,
                field,
                toQualification(qualification),
                qualification.getAttributes());
    }

    @Override
    public <T> long getMaxByQualification(
            DbConnection con,
            T object,
            String registryName,
            String field,
            String qualification,
            Map<String, Object> attributes)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        Class<?> clazz = schema.findClassForObject(object, this);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT max($db.")
                .append(getMappingName(clazz))
                .append(".")
                .append(field)
                .append("$) AS max FROM $db.")
                .append(getMappingName(clazz))
                .append("$ ");
        if (MString.isSet(qualification)) {
            String low = qualification.trim().toLowerCase();
            if (low.startsWith("order ") || low.startsWith("limit ")) sql.append(qualification);
            else sql.append("WHERE ").append(qualification);
        }

        return executeCountQuery(con, "max", sql.toString(), attributes);
    }

    @Override
    public <T, R> List<R> getAttributeByQualification(
            Class<? extends T> clazz,
            String attribute,
            String qualification,
            Map<String, Object> attributes)
            throws MException {
        return getAttributeByQualification(null, clazz, null, attribute, qualification, attributes);
    }

    @Override
    public <T, R> List<R> getAttributeByQualification(
            String attribute, AQuery<? extends T> qualification) throws MException {
        qualification.doFinal();
        return getAttributeByQualification(
                null,
                (Class<? extends T>) qualification.getType(),
                null,
                attribute,
                toQualification(qualification),
                qualification.getAttributes());
    }

    @Override
    public <T, R> List<R> getAttributeByQualification(
            DbConnection con,
            Class<? extends T> clazz,
            String registryName,
            String attribute,
            String qualification,
            Map<String, Object> attributes)
            throws MException {

        Class<? extends Object> clazz2 = schema.findClassForObject(clazz, this);
        getSchema().authorizeReadAttributes(con, this, clazz, clazz2, registryName, attribute);

        reloadLock.waitWithException(MAX_LOCK);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT $db.")
                .append(getMappingName(clazz2))
                .append(".")
                .append(attribute)
                .append("$ AS value FROM $db.")
                .append(getMappingName(clazz2))
                .append("$ ");
        if (MString.isSet(qualification)) {
            String low = qualification.trim().toLowerCase();
            if (low.startsWith("order ") || low.startsWith("limit ")) sql.append(qualification);
            else sql.append("WHERE ").append(qualification);
        }

        return executeAttributeQuery(con, "value", sql.toString(), attributes);
    }

    @Override
    public <T> DbCollection<T> executeQuery(T clazz, String query, Map<String, Object> attributes)
            throws MException {

        return executeQuery(null, clazz, null, query, attributes);
    }

    /**
     * Returns an collection.
     *
     * @param <T>
     * @param con DbConnection or null
     * @param clazz Empty Object class
     * @param registryName registry name or null
     * @param query The query, remember to return all attributes
     * @param attributes attributes for the query or null
     * @return a collection with the results
     * @throws MException
     */
    @Override
    public <T> DbCollection<T> executeQuery(
            DbConnection con,
            T clazz,
            String registryName,
            String query,
            Map<String, Object> attributes)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        try (IScope scope =
                MTracing.enter(
                        "executeQuery " + query,
                        "class",
                        clazz,
                        "registryName",
                        registryName,
                        "query",
                        query,
                        "attributes",
                        attributes)) {
            log().t("query", clazz, registryName, query, attributes);
            Map<String, Object> map = null;

            DbConnection myCon = null;
            if (con == null) {
                try {
                    myCon = schema.getConnection(poolRo);
                } catch (Exception t) {
                    throw new MException(RC.STATUS.ERROR, con, query, attributes, t);
                }
                con = myCon;
            }
            if (attributes == null) map = nameMappingRO;
            else map = new FallbackMap<String, Object>(attributes, nameMappingRO, true);
            try {
                DbStatement sth = con.createStatement(query);
                DbResult res = sth.executeQuery(map);
                return new DbCollectionImpl<T>(this, con, myCon != null, registryName, clazz, res);
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, con, query, attributes, t);
            }
        }
    }

    /**
     * Returns a long value out of a query.
     *
     * @param con
     * @param attributeName
     * @param query
     * @param attributes
     * @return x
     * @throws MException
     */
    @Override
    public <T> long executeCountQuery(
            DbConnection con, String attributeName, String query, Map<String, Object> attributes)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);
        try (IScope scope =
                MTracing.enter(
                        "executeCountQuery " + query,
                        "attributeName",
                        attributeName,
                        "query",
                        query,
                        "attributes",
                        attributes)) {
            log().t("count", attributeName, query, attributes);
            Map<String, Object> map = null;

            DbConnection myCon = null;
            if (con == null) {
                try {
                    myCon = schema.getConnection(poolRo);
                } catch (Exception t) {
                    throw new MException(RC.STATUS.ERROR, con, query, attributes, t);
                }
                con = myCon;
            }
            if (attributes == null) map = nameMappingRO;
            else map = new FallbackMap<String, Object>(attributes, nameMappingRO, true);
            DbStatement sth = null;
            DbResult res = null;
            try {
                sth = con.createStatement(query);
                res = sth.executeQuery(map);
                long count = -1;
                while (res.next()) count = res.getLong(attributeName);
                return count;

            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, con, query, attributes, t);
            } finally {
                try {
                    if (res != null) res.close();
                    if (sth != null) sth.close();
                    if (myCon != null) schema.closeConnection(pool, myCon);
                } catch (Exception t) {
                    log().w(query, attributeName, t);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> executeAttributeQuery(
            DbConnection con, String alias, String query, Map<String, Object> attributes)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);
        try (IScope scope =
                MTracing.enter(
                        "executeAttributeQuery" + query,
                        "alias",
                        alias,
                        "query",
                        query,
                        "attributes",
                        attributes)) {
            log().t("query", alias, query, attributes);
            Map<String, Object> map = null;

            DbConnection myCon = null;
            if (con == null) {
                try {
                    myCon = schema.getConnection(poolRo);
                } catch (Exception t) {
                    throw new MException(RC.STATUS.ERROR, con, query, attributes, t);
                }
                con = myCon;
            }
            if (attributes == null) map = nameMappingRO;
            else map = new FallbackMap<String, Object>(attributes, nameMappingRO, true);
            try {
                DbStatement sth = con.createStatement(query);
                DbResult res = sth.executeQuery(map);
                LinkedList<T> out = new LinkedList<>();
                while (res.next()) {
                    out.add((T) res.getObject(alias));
                }
                try {
                    res.close();
                    sth.close();
                } catch (Exception t) {
                    log().w(query, alias, t);
                }
                return out;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, con, query, attributes, t);
            } finally {
                try {
                    if (myCon != null) myCon.close();
                } catch (Exception t) {
                    log().w(query, alias, t);
                }
            }
        }
    }

    /**
     * Returns the persistent schema properties if supported.
     *
     * @return The properties or null
     */
    @Override
    public DbProperties getSchemaProperties() {
        reloadLock.waitWithException(MAX_LOCK);
        return schemaPersistence;
    }

    @Override
    public Object getObject(String registryName, Object... keys) throws MException {
        return getObject(null, registryName, keys);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObject(Class<T> clazz, Object... keys) throws MException {
        return (T) getObject(null, getRegistryName(clazz), keys);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObject(DbConnection con, Class<T> clazz, Object... keys) throws MException {
        return (T) getObject(con, getRegistryName(clazz), keys);
    }

    /**
     * Return an object from the database defined by the primary key - like a load operation. If
     * more the one attributes is needed, the order is alphabetic by the attribute name.
     *
     * @param con A connection to use or null
     * @param registryName The registry name
     * @param keys The primary key values
     * @return x
     * @throws MException
     */
    @Override
    public Object getObject(DbConnection con, String registryName, Object... keys)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        //		registryName = registryName.toLowerCase();

        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(poolRo);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        log().t("get", registryName, keys);
        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            Object out = c.getObject(con, keys);
            schema.doPostLoad(c, out, con, this);
            return out;
        } catch (AccessDeniedException e) {
            return null;
        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {

            try {
                if (myCon != null) {
                    try {
                        schema.commitConnection(pool, myCon);
                    } catch (Exception t) {
                        throw new MException(RC.STATUS.ERROR, t);
                    }
                    schema.closeConnection(pool, myCon);
                }
            } catch (Exception t) {
                log().w(t);
            }
        }
    }

    //

    @Override
    public boolean existsObject(String registryName, Object... keys) throws MException {
        return existsObject(null, registryName, keys);
    }

    @Override
    public <T> boolean existsObject(Class<T> clazz, Object... keys) throws MException {
        return existsObject(null, getRegistryName(clazz), keys);
    }

    @Override
    public <T> boolean existsObject(DbConnection con, Class<T> clazz, Object... keys)
            throws MException {
        return existsObject(con, getRegistryName(clazz), keys);
    }

    /**
     * Return an object from the database defined by the primary key - like a load operation. If
     * more the one attributes is needed, the order is alphabetic by the attribute name.
     *
     * @param con A connection to use or null
     * @param registryName The registry name
     * @param keys The primary key values
     * @return x
     * @throws MException
     */
    @Override
    public boolean existsObject(DbConnection con, String registryName, Object... keys)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);
        log().t("existsObject", registryName, keys);
        //		registryName = registryName.toLowerCase();

        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(poolRo);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            boolean ret = c.existsObject(con, keys);
            return ret;
        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {
            try {
                if (myCon != null) {
                    try {
                        schema.closeConnection(pool, myCon);
                    } catch (Exception t) {
                    }
                }
            } catch (Exception t) {
                log().w(t);
            }
        }
    }

    /**
     * Fill an object with values.
     *
     * @param registryName The registry name or null
     * @param object The object to fill
     * @param con The connection - not null
     * @param res The databse resultset with the data
     * @throws MException
     */
    @Override
    void fillObject(String registryName, Object object, DbConnection con, DbResult res)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        if (registryName == null) {
            Class<?> clazz = getSchema().findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.STATUS.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName(),
                        registryName);
            registryName = getRegistryName(clazz);
        }

        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            if (object == null) {

                object = schema.createObject(c.getClazz(), registryName, res, this, true);
            }
            c.fillObject(object, con, res);

            schema.doPostLoad(c, object, con, this);

        } catch (AccessDeniedException ade) {
            throw ade;
        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        }
    }

    @Override
    public void reloadObject(String registryName, Object object) throws MException {
        reloadObject(null, registryName, object);
    }

    /**
     * Reload the object data from database.
     *
     * @param con The connection to use or null
     * @param registryName The registry name or null
     * @param object The object to fill
     * @throws MException
     */
    @Override
    public void reloadObject(DbConnection con, String registryName, Object object)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(pool);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        if (registryName == null) {
            Class<?> clazz = schema.findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.STATUS.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName(),
                        registryName);
            registryName = getRegistryName(clazz);
        }

        log().t("reload", registryName, object);
        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        LinkedList<Object> keys = new LinkedList<Object>();
        try {
            for (Field f : c.getPrimaryKeys()) {
                keys.add(f.getFromTarget(object));
            }

            if (c.fillObject(con, object, keys.toArray()) == null)
                throw new MException(RC.NOT_FOUND, "object not found");

            schema.doPostLoad(c, object, con, this);

        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {
            try {
                if (myCon != null) {
                    try {
                        schema.commitConnection(pool, myCon);
                    } catch (Exception t) {
                        throw new MException(RC.STATUS.ERROR, t);
                    }
                    schema.closeConnection(pool, myCon);
                }
            } catch (Exception t) {
                log().w(t);
            }
        }
    }

    @Override
    public boolean objectChanged(Object object) throws MException {
        return objectChanged(null, null, object);
    }

    @Override
    public boolean objectChanged(String registryName, Object object) throws MException {
        return objectChanged(null, registryName, object);
    }

    /**
     * Check the object date against the data in the database. If the data differs it will return
     * true.
     *
     * @param con
     * @param registryName
     * @param object
     * @return x
     * @throws MException
     */
    @Override
    public boolean objectChanged(DbConnection con, String registryName, Object object)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(poolRo);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        if (registryName == null) {
            Class<?> clazz = schema.findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName(),
                        registryName);
            registryName = getRegistryName(clazz);
        }

        log().t("changed", registryName, object);
        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        boolean ret = false;
        LinkedList<Object> keys = new LinkedList<Object>();
        try {
            for (Field f : c.getPrimaryKeys()) {
                keys.add(f.getFromTarget(object));
            }

            ret = c.objectChanged(con, object, keys.toArray());

            // schema.doPostLoad(c,object,con,this);

        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {
            try {
                if (myCon != null) {
                    try {
                        schema.commitConnection(pool, myCon);
                    } catch (Exception t) {
                        throw new MException(RC.STATUS.ERROR, t);
                    }
                    schema.closeConnection(pool, myCon);
                }
            } catch (Exception t) {
                log().w(t);
            }
        }

        log().t("changed", registryName, object, ret);
        return ret;
    }

    /**
     * Fill a object with the values of an entry. This can cause big problems and exceptions if the
     * kind of the object is not correct. This is used to recycle objects. It's not save in every
     * case.
     *
     * @param con The connection to use or null
     * @param registryName The rgistryName or null
     * @param object The object to fill
     * @param keys The primary keys
     * @throws MException
     */
    @Override
    public void fillObject(DbConnection con, String registryName, Object object, Object... keys)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(pool);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        if (registryName == null) {
            Class<?> clazz = schema.findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName(),
                        registryName);
            registryName = getRegistryName(clazz);
        }

        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            c.fillObject(con, object, keys);

            schema.doPostLoad(c, object, con, this);

        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        }

        if (myCon != null) {
            try {
                schema.commitConnection(pool, myCon);
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
            schema.closeConnection(pool, myCon);
        }
    }

    @Override
    public void createObject(Object object) throws MException {
        createObject(null, null, object);
    }

    @Override
    public void createObject(String registryName, Object object) throws MException {
        createObject(null, registryName, object);
    }

    @Override
    public void createObject(DbConnection con, Object object) throws MException {
        createObject(con, null, object);
    }

    /**
     * Create a object in the database.
     *
     * @param con The connection to use or null.
     * @param registryName The registryName or null if it is a simple object reference
     * @param object The object to create.
     * @throws MException
     */
    @Override
    public void createObject(DbConnection con, String registryName, Object object)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);
        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(pool);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        if (registryName == null) {
            Class<?> clazz = schema.findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName(),
                        registryName);
            registryName = getRegistryName(clazz);
        }
        log().t("create", registryName, object);
        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            // prepare object
            c.prepareCreate(object);
            schema.doPreCreate(c, object, con, this);

            // save object

            c.createObject(con, object);

            schema.doPostCreate(c, object, con, this);

        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {
            try {
                if (myCon != null) {
                    try {
                        schema.commitConnection(pool, myCon);
                    } catch (Exception t) {
                        throw new MException(RC.STATUS.ERROR, t);
                    }
                    schema.closeConnection(pool, myCon);
                }
            } catch (Exception t) {
                log().w(t);
            }
        }
    }

    @Override
    public void saveObject(Object object) throws MException {
        saveObject(null, null, object);
    }

    @Override
    public void saveObject(String registryName, Object object) throws MException {
        saveObject(null, registryName, object);
    }

    @Override
    public void saveObject(DbConnection con, Object object) throws MException {
        saveObject(con, null, object);
    }

    /**
     * Update the data of the object in the database. This will not create an object.
     *
     * @param con The connection to use or null
     * @param registryName The registryName or null
     * @param object The object to save
     * @throws MException
     */
    @Override
    public void saveObject(DbConnection con, String registryName, Object object) throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(pool);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        if (registryName == null) {
            Class<?> clazz = schema.findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName());
            registryName = getRegistryName(clazz);
        }
        log().t("save", registryName, object);
        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            // prepare object
            schema.doPreSave(c, object, con, this);

            // save object
            c.saveObject(con, object);
        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {
            try {
                if (myCon != null) {
                    try {
                        schema.commitConnection(pool, myCon);
                    } catch (Exception t) {
                        throw new MException(RC.STATUS.ERROR, t);
                    }
                    schema.closeConnection(pool, myCon);
                }
            } catch (Exception t) {
                log().w(t);
            }
        }
    }

    @Override
    public void saveObjectForce(Object object, boolean raw) throws MException {
        saveObjectForce(null, null, object, raw);
    }

    @Override
    public void saveObjectForce(String registryName, Object object, boolean raw) throws MException {
        saveObjectForce(null, registryName, object, raw);
    }

    @Override
    public void saveObjectForce(DbConnection con, Object object, boolean raw) throws MException {
        saveObjectForce(con, null, object, raw);
    }

    /**
     * Update the data of the object in the database. This will not create an object. This update
     * will also update read-only fields.
     *
     * @param con The connection to use or null
     * @param registryName The registryName or null
     * @param object The object to save
     * @param raw If true no events like preSave are called
     * @throws MException
     */
    @Override
    public void saveObjectForce(DbConnection con, String registryName, Object object, boolean raw)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(pool);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        if (registryName == null) {
            Class<?> clazz = schema.findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName());
            registryName = getRegistryName(clazz);
        }
        log().t("save force", registryName, object);
        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            // prepare object
            if (!raw) schema.doPreSave(c, object, con, this);

            // save object
            c.saveObjectForce(con, object, raw);
        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {
            try {
                if (myCon != null) {
                    try {
                        schema.commitConnection(pool, myCon);
                    } catch (Exception t) {
                        throw new MException(RC.STATUS.ERROR, t);
                    }
                    schema.closeConnection(pool, myCon);
                }
            } catch (Exception t) {
                log().w(t);
            }
        }
    }

    @Override
    public void updateAttributes(Object object, boolean raw, String... attributeNames)
            throws MException {
        updateAttributes(null, null, object, raw, attributeNames);
    }

    @Override
    public void updateAttributes(
            String registryName, Object object, boolean raw, String... attributeNames)
            throws MException {
        updateAttributes(null, registryName, object, raw, attributeNames);
    }

    @Override
    public void updateAttributes(
            DbConnection con, Object object, boolean raw, String... attributeNames)
            throws MException {
        updateAttributes(con, null, object, raw, attributeNames);
    }

    @Override
    public void updateAttributes(
            DbConnection con,
            String registryName,
            Object object,
            boolean raw,
            String... attributeNames)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);

        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(pool);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        if (registryName == null) {
            Class<?> clazz = schema.findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName());
            registryName = getRegistryName(clazz);
        }
        log().t("save force", registryName, object);
        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            // prepare object
            if (!raw) schema.doPreSave(c, object, con, this);

            // save object
            c.updateAttributes(con, object, raw, attributeNames);
        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {
            try {
                if (myCon != null) {
                    try {
                        schema.commitConnection(pool, myCon);
                    } catch (Exception t) {
                        throw new MException(RC.STATUS.ERROR, t);
                    }
                    schema.closeConnection(pool, myCon);
                }
            } catch (Exception t) {
                log().w(t);
            }
        }
    }

    @Override
    public void deleteObject(Object object) throws MException {
        deleteObject(null, null, object);
    }

    @Override
    public void deleteObject(String registryName, Object object) throws MException {
        deleteObject(null, registryName, object);
    }

    @Override
    public void deleteObject(DbConnection con, Object object) throws MException {
        deleteObject(con, null, object);
    }

    /**
     * Delete an object in the database.
     *
     * @param con The connection to use or null
     * @param registryName The registry name to use or null
     * @param object The object to delete from database
     * @throws MException
     */
    @Override
    public void deleteObject(DbConnection con, String registryName, Object object)
            throws MException {
        reloadLock.waitWithException(MAX_LOCK);
        DbConnection myCon = null;
        if (con == null) {
            try {
                myCon = schema.getConnection(pool);
                con = myCon;
            } catch (Exception t) {
                throw new MException(RC.STATUS.ERROR, t);
            }
        }

        if (registryName == null) {
            Class<?> clazz = schema.findClassForObject(object, this);
            if (clazz == null)
                throw new MException(
                        RC.ERROR,
                        "class definition not found for object",
                        object.getClass().getCanonicalName());
            registryName = getRegistryName(clazz);
        }
        log().t("delete", registryName, object);
        Table c = cIndex.get(registryName);
        if (c == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);

        try {
            // prepare object
            schema.doPreDelete(c, object, con, this);

            // save object
            c.deleteObject(con, object);

            schema.doPostDelete(c, object, con, this);

        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, registryName, t);
        } finally {

            try {
                if (myCon != null) {
                    try {
                        schema.commitConnection(pool, myCon);
                    } catch (Exception t) {
                        throw new MException(RC.STATUS.ERROR, t);
                    }
                    schema.closeConnection(pool, myCon);
                }
            } catch (Exception t) {
                log().w(t);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return nameMapping != null;
    }

    @Override
    public void connect() throws MException {
        log().i("connect");
        synchronized (this) {
            initDatabase(false);
        }
    }

    @Override
    public void disconnect() {
        log().i("disconnect");
        synchronized (this) {
            if (nameMapping == null) return;
            cIndex.clear();

            nameMapping = null;
            nameMappingRO = null;
            caoBundle = null;
            schemaPersistence = null;
        }
    }

    @Override
    public void reconnect() throws MException {
        try {
            reloadLock.lockWithException(MAX_LOCK);
            disconnect();
            connect();
        } finally {
            reloadLock.unlockHard();
        }
    }

    /**
     * Initialize the database schema.
     *
     * @throws Exception
     */
    protected void initDatabase(boolean cleanup) throws MException {

        if (nameMapping != null) return;

        try (IScope scope =
                MTracing.enter("initDatabase", "cleanup", cleanup, "dataSource", dataSourceName)) {
            schema.resetObjectTypes();
            pool.cleanup(true);
            Class<? extends Object>[] types = schema.getObjectTypes();
            DbConnection con = pool.getConnection();
            if (con == null) return;

            cIndex.clear();
            nameMapping = new HashMap<String, Object>();
            nameMappingRO = Collections.unmodifiableMap(nameMapping);
            caoBundle = new MetadataBundle();

            // schema info
            if (schema.hasPersistentInfo()) {
                addClass(
                        schema.getSchemaName(),
                        getRegistryName(schema.getClass()),
                        Property.class,
                        con,
                        cleanup);
                schemaPersistence = new DbProperties(this, getRegistryName(schema.getClass()));
            }

            // classes
            for (Class<? extends Object> clazz : types) {
                addClass(null, getRegistryName(clazz), clazz, con, cleanup);
            }
            con.commit();

            // fill name mapping
            for (Table c : cIndex.values()) {
                c.fillNameMapping(nameMapping);
            }

            schema.doFillNameMapping(nameMapping);

            // validate and migrate database version
            if (schemaPersistence != null) {
                String dbVersion = schemaPersistence.get(DATABASE_VERSION);
                if (dbVersion == null) {
                    // init persistence
                    schemaPersistence.set(DATABASE_VERSION, "0");
                    schemaPersistence.set(DATABASE_CREATED, MDate.toIso8601(new Date()));
                    schemaPersistence.set(DATABASE_MANAGER_VERSION, MANAGER_VERSION);
                    schema.doInitProperties(this);

                    dbVersion = schemaPersistence.get(DATABASE_VERSION);
                }

                // migrate to current version
                long dbVersionLong = MCast.tolong(dbVersion, 0);
                schema.doMigrate(this, dbVersionLong);
            }

            con.close();
        } catch (MException t) {
            throw t;
        } catch (Exception t) {
            throw new MException(RC.STATUS.ERROR, t);
        }
    }

    protected void addClass(
            String tableName,
            String registryName,
            Class<? extends Object> clazz,
            DbConnection con,
            boolean cleanup)
            throws Exception {
        Table c = schema.createTable(this, clazz, registryName, tableName);
        c.initDatabase(con, cleanup);
        //		c.registryName = registryName;
        //		parseClass(c,tableName);
        //		c.createTable(con);
        //		c.postInit();
        cIndex.put(registryName, c);
    }

    @Override
    public DbSchema getSchema() {
        return schema;
    }

    @Override
    public DbPool getPool() {
        return pool;
    }

    @Override
    public DbPool getPoolRo() {
        return poolRo;
    }

    @Override
    public Activator getActivator() {
        return activator;
    }

    @Override
    public Map<String, Object> getNameMapping() {
        reloadLock.waitWithException(MAX_LOCK);
        return nameMappingRO;
    }

    @Override
    public MetadataBundle getCaoMetadata() {
        reloadLock.waitWithException(MAX_LOCK);
        return caoBundle;
    }

    @Override
    public String[] getRegistryNames() {
        reloadLock.waitWithException(MAX_LOCK);
        return cIndex.keySet().toArray(new String[0]);
    }

    @Override
    public Table getTable(String registryName) {
        reloadLock.waitWithException(MAX_LOCK);
        return cIndex.get(registryName);
    }

    @Override
    public Object createSchemaObject(String registryName) throws Exception {
        reloadLock.waitWithException(MAX_LOCK);
        Table table = cIndex.get(registryName);
        if (table == null)
            throw new MException(RC.ERROR, "class definition not found in schema", registryName);
        return schema.createObject(table.getClazz(), table.getRegistryName(), null, this, false);
    }

    @Override
    public String getRegistryName(Object object) {
        reloadLock.waitWithException(MAX_LOCK);
        if (object instanceof Class<?>) {
            return ((Class<?>) object).getCanonicalName();
        }
        Class<? extends Object> clazz = schema.findClassForObject(object, this);
        if (clazz == null) return null;
        return clazz.getCanonicalName();
    }

    @Override
    public String getMappingName(Class<?> clazz) {
        reloadLock.waitWithException(MAX_LOCK);
        return clazz.getSimpleName().toLowerCase();
    }

    @Override
    public <T extends Object> T inject(T object) {
        reloadLock.waitWithException(MAX_LOCK);
        schema.injectObject(object, this, getTable(getRegistryName(object)));
        return object;
    }

    @Override
    public <T extends Object> DbCollection<T> getAll(Class<T> clazz) throws MException {
        return getByQualification(clazz, "", null);
    }

    @Override
    public String getDataSourceName() {
        return dataSourceName;
    }

    @Override
    public QueryParser createParser() {
        ParserJdbc ret = CFG_DEBUG_PARSER.value() ? new ParserJdbcDebug() : new ParserJdbc();
        ret.init(this);
        return ret;
    }

    @Override
    public void initialize(List<Class<?>> value) {
        ((MutableDbSchema) schema).getRegistry().clear();
        ((MutableDbSchema) schema).getRegistry().addAll(value);
    }
}
