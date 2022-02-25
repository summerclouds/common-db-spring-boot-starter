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
package org.summerclouds.common.db.model;

import org.summerclouds.common.core.error.AccessDeniedException;
import org.summerclouds.common.core.tool.MSecurity;
import org.summerclouds.common.db.DbPermissionManager;
import org.summerclouds.common.db.sql.DbConnection;

/**
 * FeatureAccessManager class.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class FeatureAccessManager extends Feature {

    public DbPermissionManager accessManager;

    /** {@inheritDoc} */
    @Override
    protected void doInit() {
        accessManager = manager.getSchema().getAccessManager(table);
    }

    /** {@inheritDoc} */
    @Override
    public void postFillObject(Object obj, DbConnection con) throws Exception {
        if (accessManager != null && !accessManager.hasPermission(manager, table, con, obj, DbPermissionManager.ACCESS.READ))
        	throw new AccessDeniedException("read/fill denied",MSecurity.getCurrent(), table.getName(), obj.getClass().getCanonicalName());
    }

    /** {@inheritDoc} */
    @Override
    public void preCreateObject(DbConnection con, Object object) throws Exception {
        if (accessManager != null && !accessManager.hasPermission(manager, table, con, object, DbPermissionManager.ACCESS.CREATE))
        	throw new AccessDeniedException("create denied",MSecurity.getCurrent(), table.getName(), object.getClass().getCanonicalName());
    }

    /** {@inheritDoc} */
    @Override
    public void preSaveObject(DbConnection con, Object object) throws Exception {
        if (accessManager != null && !accessManager.hasPermission(manager, table, con, object, DbPermissionManager.ACCESS.UPDATE))
        	throw new AccessDeniedException("update/save denied",MSecurity.getCurrent(), table.getName(), object.getClass().getCanonicalName());
    }

    /** {@inheritDoc} */
    @Override
    public void deleteObject(DbConnection con, Object object) throws Exception {
        if (accessManager != null && !accessManager.hasPermission(manager, table, con, object, DbPermissionManager.ACCESS.DELETE))
        	throw new AccessDeniedException("delete denied",MSecurity.getCurrent(), table.getName(), object.getClass().getCanonicalName());
    }

    /** {@inheritDoc} */
    @Override
    public void postGetObject(DbConnection con, Object obj) throws Exception {
        if (accessManager != null && !accessManager.hasPermission(manager, table, con, obj, DbPermissionManager.ACCESS.READ))
            	throw new AccessDeniedException("read denied",MSecurity.getCurrent(), table.getName(), obj.getClass().getCanonicalName());
    }
}
