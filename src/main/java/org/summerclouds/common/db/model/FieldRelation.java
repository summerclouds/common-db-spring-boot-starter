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
package org.summerclouds.common.db.model;

import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.pojo.PojoAttribute;
import org.summerclouds.common.db.DbManager;
import org.summerclouds.common.db.IRelationObject;
import org.summerclouds.common.db.annotations.DbRelation;
import org.summerclouds.common.db.sql.DbConnection;

/**
 * FieldRelation class.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class FieldRelation extends MLog {

    private DbManager manager;
    private DbRelation config;
    private Table table;
    private PojoAttribute<Object> attribute;

    /**
     * Constructor for FieldRelation.
     *
     * @param manager a {@link de.mhus.lib.adb.DbManager} object.
     * @param table a {@link de.mhus.lib.adb.model.Table} object.
     * @param attribute a {@link de.mhus.lib.core.pojo.PojoAttribute} object.
     * @param config a {@link de.mhus.lib.annotations.adb.DbRelation} object.
     */
    @SuppressWarnings("unchecked")
    public FieldRelation(
            DbManager manager, Table table, PojoAttribute<?> attribute, DbRelation config) {
        this.attribute = (PojoAttribute<Object>) attribute;
        this.manager = manager;
        this.config = config;
        this.table = table;
    }

    /**
     * getName.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return attribute.getName();
    }

    /**
     * getRelationObject.
     *
     * @param obj a {@link java.lang.Object} object.
     * @return a {@link de.mhus.lib.adb.IRelationObject} object.
     */
    public IRelationObject getRelationObject(Object obj) {
        try {
            IRelationObject rel = (IRelationObject) attribute.get(obj);
            if (rel == null && attribute.canWrite()) {
                rel = (IRelationObject) attribute.getType().getDeclaredConstructor().newInstance();
                attribute.set(obj, rel, false);
            }
            if (rel != null) rel.setManager(this, obj);
            return rel;
        } catch (Exception e) {
            log().t(getName(), obj, e);
        }
        return null;
    }

    /**
     * prepareCreate.
     *
     * @param object a {@link java.lang.Object} object.
     * @throws java.lang.Exception if any.
     */
    public void prepareCreate(Object object) throws Exception {
        IRelationObject rel = getRelationObject(object);
        if (rel != null) rel.prepareCreate();
    }

    /**
     * created.
     *
     * @param con a {@link de.mhus.lib.sql.DbConnection} object.
     * @param object a {@link java.lang.Object} object.
     * @throws java.lang.Exception if any.
     */
    public void created(DbConnection con, Object object) throws Exception {
        IRelationObject rel = getRelationObject(object);
        if (rel != null) rel.created(con);
    }

    /**
     * loaded.
     *
     * @param con a {@link de.mhus.lib.sql.DbConnection} object.
     * @param object a {@link java.lang.Object} object.
     * @throws java.lang.Exception if any.
     */
    public void loaded(DbConnection con, Object object) throws Exception {
        IRelationObject rel = getRelationObject(object);
        if (rel != null) rel.loaded(con);
    }

    /**
     * saved.
     *
     * @param con a {@link de.mhus.lib.sql.DbConnection} object.
     * @param object a {@link java.lang.Object} object.
     * @throws java.lang.Exception if any.
     */
    public void saved(DbConnection con, Object object) throws Exception {
        IRelationObject rel = getRelationObject(object);
        if (rel != null) rel.saved(con);
    }

    /**
     * Getter for the field <code>table</code>.
     *
     * @return a {@link de.mhus.lib.adb.model.Table} object.
     */
    public Table getTable() {
        return table;
    }

    /**
     * Getter for the field <code>manager</code>.
     *
     * @return a {@link de.mhus.lib.adb.DbManager} object.
     */
    public DbManager getManager() {
        return manager;
    }

    /**
     * Getter for the field <code>config</code>.
     *
     * @return a {@link de.mhus.lib.annotations.adb.DbRelation} object.
     */
    public DbRelation getConfig() {
        return config;
    }

    /**
     * isChanged.
     *
     * @param object a {@link java.lang.Object} object.
     * @return a boolean.
     */
    public boolean isChanged(Object object) {
        IRelationObject rel = getRelationObject(object);
        if (rel != null) return rel.isChanged();
        return false;
    }

    /**
     * prepareSave.
     *
     * @param con a {@link de.mhus.lib.sql.DbConnection} object.
     * @param object a {@link java.lang.Object} object.
     * @throws java.lang.Exception if any.
     */
    public void prepareSave(DbConnection con, Object object) throws Exception {
        IRelationObject rel = getRelationObject(object);
        if (rel != null) rel.prepareSave(con);
    }

    /**
     * inject.
     *
     * @param object a {@link java.lang.Object} object.
     */
    public void inject(Object object) {
        IRelationObject rel = getRelationObject(object);
        if (rel != null) rel.setManager(this, object); // again
    }
}
