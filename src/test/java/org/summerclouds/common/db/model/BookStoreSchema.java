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

import java.util.HashMap;
import java.util.List;

import org.summerclouds.common.core.error.AccessDeniedException;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.db.DbPermissionManager;
import org.summerclouds.common.db.DbManager;
import org.summerclouds.common.db.DbManagerJdbc;
import org.summerclouds.common.db.DbSchema;
import org.summerclouds.common.db.sql.DbConnection;

public class BookStoreSchema extends DbSchema {

    public boolean switchStore = false;

    @Override
    public void doFillNameMapping(HashMap<String, Object> nameMapping) {
        super.doFillNameMapping(nameMapping);
        System.out.println(nameMapping);
    }

    @Override
    public void doMigrate(DbManager dbManager, long currentVersion) throws MException {
        if (currentVersion == 0) {
            Person p = new Person();
            p.setName("Hausmeister Krause");
            dbManager.createObject(p);
            dbManager.getSchemaProperties().set(DbManagerJdbc.DATABASE_VERSION, "1");
        }
    }

    @Override
    public DbPermissionManager getAccessManager(Table c) {
        if (c.getClazz() == Finances.class) {
            return new DbPermissionManager() {

                @Override
                public boolean hasPermission(
                        DbManager manager,
                        Table c,
                        DbConnection con,
                        Object object,
                        DbPermissionManager.ACCESS right)
                        throws AccessDeniedException {

                    Finances f = (Finances) object;

                    String conf = f.getConfidential();
                    if (conf != null) {
                        if (right == DbPermissionManager.ACCESS.UPDATE && conf.indexOf("write") >= 0)
                            throw new AccessDeniedException("access denied");

                        if (right == DbPermissionManager.ACCESS.DELETE && conf.indexOf("remove") >= 0)
                            throw new AccessDeniedException("access denied");

                        if (right == DbPermissionManager.ACCESS.READ && conf.indexOf("read") >= 0)
                            throw new AccessDeniedException("access denied");
                    }
                    // set new acl if needed
                    if (f.getNewConfidential() != null) f.setConfidential(f.getNewConfidential());
                    
                    return true;
                }
            };
        }
        return null;
    }

    @Override
    public void findObjectTypes(List<Class<? extends Object>> list) {
        list.add(Book.class);
        list.add(Person.class);
        list.add(Person2.class);
        list.add(Finances.class);
        list.add(Regal.class);

        if (switchStore) list.add(org.summerclouds.common.db.model2.Store.class);
        else list.add(Store.class);
    }

    @Override
    public void authorizeReadAttributes(
            DbConnection con,
            DbManager dbManagerJdbc,
            Class<?> clazz,
            Class<?> clazz2,
            String registryName,
            String attribute) {}
}
