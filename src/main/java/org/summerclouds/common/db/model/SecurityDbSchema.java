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

import java.util.HashMap;

import org.summerclouds.common.core.error.AccessDeniedException;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.node.MNode;
import org.summerclouds.common.core.tool.MPeriod;
import org.summerclouds.common.core.tool.MSecurity;
import org.summerclouds.common.db.DbManager;
import org.summerclouds.common.db.DbPermissionManager;
import org.summerclouds.common.db.DbSchema;
import org.summerclouds.common.db.sql.DbConnection;
import org.summerclouds.common.db.transaction.MemoryLockStrategy;

public abstract class SecurityDbSchema extends DbSchema implements DbPermissionManager {

    private INode config;
    private boolean trace;

    public SecurityDbSchema(INode config) {
        setConfig(config);
        if (trace) log().i("start");
        lockStrategy = new MemoryLockStrategy();
        ((MemoryLockStrategy) lockStrategy)
                .setMaxLockAge(
                        getConfig().getLong("maxLockAge", MPeriod.MINUTE_IN_MILLISECONDS * 5));
    }

    @Override
    public void authorizeSaveForceAllowed(DbConnection con, Table table, Object object, boolean raw)
            throws AccessDeniedException {
        if (!MSecurity.hasPermission(Table.class, "saveforce", table.getRegistryName()))
            throw new AccessDeniedException(
                    "save forced not allowed", MSecurity.getCurrent(), table.getName());
    }

    @Override
    public void authorizeUpdateAttributes(
            DbConnection con, Table table, Object object, boolean raw, String... attributeNames)
            throws AccessDeniedException {
        if (MSecurity.hasPermission(Table.class, "updateattributes", table.getRegistryName()))
            return;
        for (String attr : attributeNames)
            if (!MSecurity.hasPermission(
                    Table.class, "updateattributes", table.getRegistryName() + "_" + attr))
                throw new AccessDeniedException(
                        "update attributes not allowed",
                        MSecurity.getCurrent(),
                        table.getName(),
                        attr);
    }

    @Override
    public void authorizeReadAttributes(
            DbConnection con,
            DbManager dbManagerJdbc,
            Class<?> clazz,
            Class<?> clazz2,
            String registryName,
            String attribute) {

        if (registryName == null) {
            if (clazz2 != null) registryName = dbManagerJdbc.getRegistryName(clazz2);
            else if (clazz != null) registryName = dbManagerJdbc.getRegistryName(clazz);
        }

        if (!MSecurity.hasPermission(Table.class, "readattributes", registryName + "_" + attribute)
                && !MSecurity.hasPermission(Table.class, "readattributes", registryName))
            throw new AccessDeniedException(
                    "read single attribute not allowerd",
                    MSecurity.getCurrent(),
                    registryName,
                    attribute);
    }

    @Override
    public void internalCreateObject(
            DbConnection con, String name, Object object, HashMap<String, Object> attributes) {
        super.internalCreateObject(con, name, object, attributes);
        if (trace) log().i("create", name, attributes, object);
    }

    @Override
    public void internalSaveObject(
            DbConnection con, String name, Object object, HashMap<String, Object> attributes) {
        super.internalSaveObject(con, name, object, attributes);
        if (trace) log().i("modify", name, attributes, object);
    }

    @Override
    public void internalDeleteObject(
            DbConnection con, String name, Object object, HashMap<String, Object> attributes) {
        super.internalDeleteObject(con, name, object, attributes);
        if (trace) log().i("delete", name, attributes, object);
    }

    @Override
    public synchronized DbPermissionManager getAccessManager(Table c) {
        return this;
    }

    protected INode getConfig() {
        return config;
    }

    protected void setConfig(INode config) {
        if (config == null) config = new MNode();
        trace = config.getBoolean("trace", false);
        this.config = config;
    }

    @Override
    public boolean hasPermission(
            DbManager manager, Table c, DbConnection con, Object object, ACCESS right) {
        return MSecurity.hasPermission(DbSchema.class, right.name(), c.getName());
    }
}
