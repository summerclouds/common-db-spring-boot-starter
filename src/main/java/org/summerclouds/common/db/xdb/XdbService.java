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
package org.summerclouds.common.db.xdb;

import java.util.List;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.lang.Adaptable;
import org.summerclouds.common.core.pojo.PojoModelFactory;
import org.summerclouds.common.db.DbCollection;
import org.summerclouds.common.db.Persistable;
import org.summerclouds.common.db.QueryParser;
import org.summerclouds.common.db.query.AQuery;

public interface XdbService extends Adaptable {

    boolean isConnected();

    List<String> getTypeNames();

    <T> XdbType<T> getType(String name) throws NotFoundException;

    String getSchemaName();

    //	String getDataSourceName();

    void updateSchema(boolean cleanup) throws MException;

    void connect() throws Exception;

    default <T> T getObjectByQualification(AQuery<T> query) throws MException {
        @SuppressWarnings("unchecked")
        XdbType<T> type = (XdbType<T>) getType(query.getType());
        return type.getObjectByQualification(query);
    }

    default <T> DbCollection<T> getByQualification(AQuery<T> query) throws MException {
        @SuppressWarnings("unchecked")
        XdbType<T> type = (XdbType<T>) getType(query.getType());
        return type.getByQualification(query);
    }

    default <T extends Object> DbCollection<T> getAll(Class<T> type) throws MException {
        XdbType<T> xType = (XdbType<T>) getType(type);
        return xType.getAll();
    }

    default <T> long count(AQuery<T> query) throws MException {
        @SuppressWarnings("unchecked")
        XdbType<T> type = (XdbType<T>) getType(query.getType());
        return type.count(query);
    }

    <T> XdbType<T> getType(Class<T> type) throws NotFoundException;

    <T extends Object> T inject(T object);

    <T> T getObject(Class<T> clazz, Object... keys) throws MException;

    PojoModelFactory getPojoModelFactory();

    String getDataSourceName();

    void delete(Object object) throws MException;

    void save(Object object) throws MException;

    QueryParser createParser();

	void initialize(List<Class<?>> value);
}
