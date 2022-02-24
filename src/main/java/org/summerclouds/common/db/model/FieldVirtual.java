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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.pojo.PojoAttribute;
import org.summerclouds.common.db.sql.DbResult;

public class FieldVirtual extends Field {

    @SuppressWarnings("unchecked")
    public FieldVirtual(
            Table table,
            boolean isPrimary,
            PojoAttribute<?> attribute,
            INode attr2,
            String[] features)
            throws MException {
        this.attribute = (PojoAttribute<Object>) attribute;
        this.nameOrg = attribute.getName();
        this.table = table;
        this.manager = table.manager;
        this.name = nameOrg.toLowerCase();
        this.createName = name.toLowerCase();
        this.methodName = name;
        this.isPrimary = isPrimary;
        this.attr = attr2;
        init(features);
    }

    //	public FieldVirtual(Table table, de.mhus.lib.adb.DbDynamic.Field f) {
    //		methodName = f.getName();
    //		this.table = table;
    //		this.nameOrg = methodName;
    //		this.name = methodName.toLowerCase();
    //		this.createName = methodName.toLowerCase();
    //		this.isPrimary = f.isPrimaryKey();
    //		this.ret = f.getReturnType();
    //		this.attr = f.getAttributes();
    //		this.dynamicField = f;
    //	}

    @Override
    public void prepareCreate(Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {}

    @Override
    public Object getFromTarget(Object obj) throws Exception {
        return get(obj);
    }

    @Override
    public void setToTarget(DbResult res, Object obj) throws Exception {}

    @Override
    public boolean changed(DbResult res, Object obj) throws Exception {
        return false;
    }

    @Override
    public void fillNameMapping(HashMap<String, Object> nameMapping) {}

    @Override
    public boolean isPersistent() {
        return false;
    }
}
