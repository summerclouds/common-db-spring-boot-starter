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

import org.summerclouds.common.core.cfg.CfgBoolean;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.parser.Parser;
import org.summerclouds.common.core.tool.MSystem;

/**
 * The class capsulate the real connection to bring it back into the pool if the connection in no
 * more needed - closed or cleanup by the gc.
 *
 * @author mikehummel
 */
public class DbConnectionProxy extends MLog implements DbConnection {

    private static final CfgBoolean CFG_TRACE_CALLER =
            new CfgBoolean(DbConnection.class, "traceCallers", false);

    private DbConnection instance;
    private int id = System.identityHashCode(this);
    //	private StackTraceElement[] createStackTrace;
    private DbPool pool;

    public DbConnectionProxy(DbPool pool, DbConnection instance) {
        if (CFG_TRACE_CALLER.value()) {
            this.pool = pool;
            pool.getStackTraces().put(MSystem.getObjectId(this), new ConnectionTrace(this));
            //			instance.setUsedTrace(createStackTrace);
        }
        this.instance = instance;
        log().t("created", id, instance.getInstanceId());
    }

    @Override
    public void commit() throws Exception {

        instance.commit();
    }

    @Override
    public boolean isReadOnly() throws Exception {
        return instance.isReadOnly();
    }

    @Override
    public void rollback() throws Exception {
        instance.rollback();
    }

    @Override
    public DbStatement getStatement(String name) throws MException {
        return instance.getStatement(name);
    }

    @Override
    public boolean isClosed() {
        if (instance == null) return true;
        return instance.isClosed();
    }

    @Override
    public boolean isUsed() {
        return instance.isUsed();
    }

    @Override
    public void setUsed(boolean used) {
        if (instance == null) return;
        instance.setUsed(used);
        if (!used) instance = null; // invalidate this proxy
    }

    @Override
    public void close() {
        if (instance == null) return;
        log().t("close", id, instance.getInstanceId());
        setUsed(false); // close of the proxy will free the connection
        if (CFG_TRACE_CALLER.value()) pool.getStackTraces().remove(MSystem.getObjectId(this));
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        log().t("finalized", id, instance.getInstanceId());
        if (instance != null) {
            log().i("final closed", id, instance.getInstanceId());
            ConnectionTrace trace = pool.getStackTraces().get(MSystem.getObjectId(this));
            if (trace != null) trace.log(log());
            setUsed(false);
        }
        if (CFG_TRACE_CALLER.value()) pool.getStackTraces().remove(MSystem.getObjectId(this));
        super.finalize();
    }

    @Override
    public DbStatement createStatement(String sql, String language) throws MException {
        return instance.createStatement(sql, language);
    }

    @Override
    public int getInstanceId() {
        return id;
    }

    @Override
    public Parser createQueryCompiler(String language) throws MException {
        return instance.createQueryCompiler(language);
    }

    @Override
    public DbConnection instance() {
        return instance;
    }

    @Override
    public DbStatement createStatement(DbPrepared dbPrepared) {
        return instance.createStatement(dbPrepared);
    }

    @Override
    public String getDefaultLanguage() {
        return instance.getDefaultLanguage();
    }

    @Override
    public String[] getLanguages() {
        return instance.getLanguages();
    }

    @Override
    public DbStatement createStatement(String sql) throws MException {
        return instance.createStatement(sql);
    }
}
