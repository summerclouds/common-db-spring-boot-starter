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

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.db.DbManager;
import org.summerclouds.common.db.sql.DbConnection;
import org.summerclouds.common.db.sql.DbResult;

public abstract class Feature extends MLog {

    protected DbManager manager;
    protected Table table;

    public void init(DbManager manager, Table table) throws MException {
        this.manager = manager;
        this.table = table;
        doInit();
    }

    protected void doInit() throws MException {}

    public void preCreateObject(DbConnection con, Object object) throws Exception {}

    public void preSaveObject(DbConnection con, Object object) throws Exception {}

    public void preGetObject(DbConnection con, DbResult ret) throws Exception {}

    public void postGetObject(DbConnection con, Object obj) throws Exception {}

    public void preFillObject(Object obj, DbConnection con, DbResult res) throws Exception {}

    public void deleteObject(DbConnection con, Object object) throws Exception {}

    public Object getValue(Object obj, Field field, Object val) throws Exception {
        return val;
    }

    public Object setValue(Object obj, Field field, Object value) throws Exception {
        return value;
    }

    public void postFillObject(Object obj, DbConnection con) throws Exception {}

    public void postCreateObject(DbConnection con, Object object) throws Exception {}

    public void postSaveObject(DbConnection con, Object object) throws Exception {}
}
