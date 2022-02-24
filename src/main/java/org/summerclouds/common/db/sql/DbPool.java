/**
 * Copyright (C) 2020 Mike Hummel (mh@mhus.de)
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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.cfg.CfgBoolean;
import org.summerclouds.common.core.cfg.CfgTimeInterval;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.node.MNode;
import org.summerclouds.common.core.tool.MSpring;
import org.summerclouds.common.core.tool.MSystem;
import org.summerclouds.common.core.util.Activator;
import org.summerclouds.common.db.annotations.DbTransactionable;

import de.mhus.lib.annotations.jmx.JmxManaged;
import de.mhus.lib.core.MActivator;
import de.mhus.lib.core.MApi;
import de.mhus.lib.core.MHousekeeper;
import de.mhus.lib.core.MHousekeeperTask;
import de.mhus.lib.core.service.UniqueId;

/**
 * The pool handles a bundle of connections. The connections should have the same credentials (url,
 * user access). Unused or closed connections will be freed after a pending period.
 *
 * @author mikehummel
 */
public abstract class DbPool extends MLog implements DbTransactionable {

    // Trace parameters
    private Map<String, ConnectionTrace> stackTraces = new HashMap<>();
    private long lastStackTracePrint = 0;
    private CfgBoolean traceCaller =
            new CfgBoolean(DbConnection.class, "traceCallers", false);
    protected CfgBoolean tracePoolSize = new CfgBoolean(DbConnection.class, "tracePoolSize", false);
    private CfgTimeInterval traceWait =
            new CfgTimeInterval(DbConnection.class, "traceCallersWait", "10m");
    private CfgBoolean autoCleanup = new CfgBoolean(DbConnection.class, "autoCleanup", true);
    private CfgBoolean autoCleanupUnused =
            new CfgBoolean(DbConnection.class, "autoCleanupUnused", true);

    private DbProvider provider;
    private String name;
    private INode config;

    /**
     * Create a new pool from central configuration. It's used the MApi configuration with the key
     * of this class.
     *
     * @throws Exception
     */
    public DbPool() throws Exception {
        this(null, null);
    }

    /**
     * Create a new pool from a configuration.
     *
     * @param config Config element or null. null will use the central MApi configuration.
     * @param activator Activator or null. null will use the central MApi Activator.
     * @throws Exception
     */
    public DbPool(INode config, Activator activator) throws Exception {

        this.config = config;

        if (this.config == null) doCreateConfig();
        if (activator == null) activator = MSpring.getDefaultActivator();

        DbProvider provider =
                (DbProvider)
                        activator.createObject(
                                this.config.getExtracted(
                                        "provider", JdbcProvider.class.getCanonicalName()));
        provider.doInitialize(this.config, activator);

        this.provider = provider;

        init();
    }

    /**
     * Create a pool with the DbProvider.
     *
     * @param provider
     */
    public DbPool(DbProvider provider) {
        doCreateConfig();
        setProvider(provider);

        init();
    }

    protected synchronized void init() {
        if (housekeeperTask != null) return;

        housekeeperTask =
                new MHousekeeperTask(name) {

                    @Override
                    public void doit() throws Exception {
                        if (!isClosed() && autoCleanup.value()) {
                            log().t(DbPool.this.getName(), "autoCleanup connections");
                            cleanup(autoCleanupUnused.value());
                        }
                        if (isClosed()) cancel();
                    }
                };
        MHousekeeper housekeeper = M.l(MHousekeeper.class);
        if (housekeeper != null) {
            housekeeper.register(housekeeperTask, getConfig().getLong("autoCleanupSleep", 300000));
        } else {
            log().w("Housekeeper not found - autoCleanup disabled");
        }
    }

    protected INode getConfig() {
        return config;
    }

    protected String getName() {
        return name;
    }

    protected void doCreateConfig() {
        try {
            config = MApi.get().getCfgManager().getCfg(this, null);
        } catch (Throwable t) {
        }
        if (config == null) config = new MNode();
    }

    /**
     * Set a DbProvider for this pool.
     *
     * @param provider
     */
    protected void setProvider(DbProvider provider) {
        this.provider = provider;
        name = provider.getName();
        if (name == null) name = "pool";
        name = name + MSystem.getObjectId(this);
    }

    /**
     * Returns the DbProvider, it implements the database behavior and creates new connections.
     *
     * @return x
     */
    public DbProvider getProvider() {
        return provider;
    }

    /**
     * Returns the database dialect object. (Delegated to DbProvider).
     *
     * @return x
     */
    public Dialect getDialect() {
        return provider.getDialect();
    }

    /**
     * Look into the pool for an unused DbProvider. If no one find, create one.
     *
     * @return x
     * @throws Exception
     */
    public abstract DbConnection getConnection() throws Exception;

    /**
     * Current pool size.
     *
     * @return x Current pool size, also pending closed connections.
     */
    public abstract int getSize();

    public abstract int getUsedSize();

    /**
     * Cleanup the connection pool. Unused or closed connections will be removed. TODO new strategy
     * to remove unused connections - not prompt, need a timeout time or minimum pool size.
     *
     * @param unusedAlso
     */
    public abstract void cleanup(boolean unusedAlso);

    /** Close the pool and all connections. */
    public abstract void close();

    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        close();
        housekeeperTask = null;
        super.finalize();
    }

    public DbPrepared getStatement(String name) throws MException {
        String[] query = provider.getQuery(name);
        return new DbPrepared(this, query[1], query[0]);
    }

    /**
     * Create a prepared statement using the default language.
     *
     * @param sql
     * @return x
     * @throws MException
     */
    public DbPrepared createStatement(String sql) throws MException {
        return createStatement(sql, null);
    }

    /**
     * Create a new prepared statement for further use.
     *
     * @param sql
     * @param language
     * @return x
     * @throws MException
     */
    public DbPrepared createStatement(String sql, String language) throws MException {
        return new DbPrepared(this, sql, language);
    }

    public String getPoolId() {
        return name;
    }

    public abstract String dumpUsage(boolean used);

    public abstract boolean isClosed();

    public Map<String, ConnectionTrace> getStackTraces() {
        return stackTraces;
    }

    public void printStackTrace() {
        if (traceCaller.value()
                && lastStackTracePrint + traceWait.interval() < System.currentTimeMillis()) {
            lastStackTracePrint = System.currentTimeMillis();
            LinkedList<ConnectionTrace> list =
                    new LinkedList<ConnectionTrace>(getStackTraces().values());
            Collections.sort(list);
            log().f("Connection Usage", list.size());
            for (ConnectionTrace trace : list) {
                trace.log(log());
            }
        }
    }

    @Override
    public DbConnection createTransactionalConnection() {
        try {
            return getConnection();
        } catch (Exception e) {
            return null;
        }
    }
}
