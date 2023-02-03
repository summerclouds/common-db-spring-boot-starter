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
package org.summerclouds.common.db.sql;

import java.io.IOException;
import java.sql.Connection;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.RC;
import org.summerclouds.common.core.parser.Parser;
import org.summerclouds.common.db.sql.parser.SimpleQueryCompiler;

/**
 * This class is a proxy for DbProvider and it is the public interface for it. It will automatic
 * free the provider in the pool if the connection is no more needed.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class JdbcConnection extends InternalDbConnection {

    /** Constant <code>LANGUAGE_SQL="sql"</code> */
    public static final String LANGUAGE_SQL = "sql";

    private boolean used = false;
    private Connection connection;
    private DbProvider provider;
    private boolean closed;

    private int id = System.identityHashCode(this);

    /** {@inheritDoc} */
    @Override
    public void commit() throws Exception {
        log().t("commit", id, poolId);
        if (closed) throw new MException(RC.INTERNAL_ERROR, "Connection not valid", poolId);
        if (!connection.getAutoCommit()) connection.commit();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReadOnly() throws Exception {
        if (closed) throw new MException(RC.INTERNAL_ERROR, "Connection not valid", poolId);
        return connection.isReadOnly();
    }

    /** {@inheritDoc} */
    @Override
    public void rollback() throws Exception {
        log().t("rollback", id, poolId);
        if (closed) throw new IOException("Connection not valid");
        connection.rollback();
    }

    /**
     * Constructor for JdbcConnection.
     *
     * @param provider a {@link de.mhus.lib.sql.DbProvider} object.
     * @param con a {@link java.sql.Connection} object.
     */
    public JdbcConnection(DbProvider provider, Connection con) {
        this.provider = provider;
        this.connection = con;
    }

    /** {@inheritDoc} */
    @Override
    public DbStatement getStatement(String name) throws MException {
        synchronized (this) {
            if (closed) throw new MException(RC.STATUS.INTERNAL_ERROR, "Connection not valid");

            String[] query = provider.getQuery(name);
            if (query == null) return null;
            return new JdbcStatement(this, query[1], query[0]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public DbStatement createStatement(String sql, String language) throws MException {
        synchronized (this) {
            if (closed) throw new MException(RC.STATUS.INTERNAL_ERROR, "Connection not valid");

            return new JdbcStatement(this, sql, language);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClosed() {
        synchronized (this) {
            return closed;
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUsed() {
        synchronized (this) {
            return used;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setUsed(boolean used) {
        log().t("used", id, poolId, used);
        super.setUsed(used);
        synchronized (this) {
            this.used = used;
            if (!used) // for security reasons - remove old garbage in the session
            try {
                    if (connection != null) connection.rollback();
                } catch (Exception e) {
                    log().d(e);
                    close();
                }
        }
    }

    /**
     * Returns the JDBC Connection - if possible.
     *
     * @return JDBC Connection or null
     */
    public Connection getConnection() {
        return connection;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        log().t("close", id, poolId);
        synchronized (this) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    connection = null;
                }
            } catch (Exception e) {
                log().d("close failed", this, e);
                connection = null;
            }
            closed = true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Parser createQueryCompiler(String language) throws MException {
        if (pool != null) return pool.getDialect().getQueryParser(language);
        return new SimpleQueryCompiler();
    }

    /** {@inheritDoc} */
    @Override
    public DbConnection instance() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public DbStatement createStatement(DbPrepared dbPrepared) {
        return new JdbcStatement(this, dbPrepared);
    }

    /**
     * setTimeoutUnused.
     *
     * @param timeoutUnused a long.
     */
    @Override
    public void setTimeoutUnused(long timeoutUnused) {
        this.timeoutUnused = timeoutUnused;
    }

    /**
     * setTimeoutLifetime.
     *
     * @param timeoutLifetime a long.
     */
    @Override
    public void setTimeoutLifetime(long timeoutLifetime) {
        this.timeoutLifetime = timeoutLifetime;
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultLanguage() {
        return LANGUAGE_SQL;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getLanguages() {
        return new String[] {LANGUAGE_SQL};
    }

    /** {@inheritDoc} */
    @Override
    public DbStatement createStatement(String sql) throws MException {
        return createStatement(sql, provider.getDialect().detectLanguage(sql));
    }

    @Override
    public int getInstanceId() {
        return id;
    }
}
