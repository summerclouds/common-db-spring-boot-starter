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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.pojo.PojoAttribute;
import org.summerclouds.common.db.DbDynamic;
import org.summerclouds.common.db.annotations.DbIndex;

/**
 * TableDynamic class.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class TableDynamic extends Table {

    /**
     * {@inheritDoc}
     *
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     */
    @Override
    protected void parseFields()
            throws InstantiationException, IllegalAccessException, MException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
                    SecurityException {
        DbDynamic.Field[] fa =
                ((DbDynamic) clazz.getDeclaredConstructor().newInstance()).getFieldDefinitions();
        for (DbDynamic.Field f : fa) {

            PojoAttribute<?> attr = new DynamicAttribute(f);
            Field field =
                    manager.getSchema()
                            .createField(
                                    manager,
                                    this,
                                    f.isPrimaryKey(),
                                    f.isReadOnly(),
                                    !f.isPersistent(),
                                    attr,
                                    f.getAttributes(),
                                    f,
                                    null);

            if (field != null) addField(field);

            // indexes
            String[] indexes = f.getIndexes();
            if (indexes != null) {
                addToIndex(indexes, DbIndex.TYPE.AUTO, "", field);
            }
        }
    }

    private class DynamicAttribute implements PojoAttribute<Object> {

        private org.summerclouds.common.db.DbDynamic.Field f;

        public DynamicAttribute(org.summerclouds.common.db.DbDynamic.Field f) {
            this.f = f;
        }

        @Override
        public Object get(Object pojo) throws IOException {
            return null;
        }

        @Override
        public void set(Object pojo, Object value, boolean force) throws IOException {}

        @Override
        public Class<?> getType() {
            return f.getReturnType();
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public boolean canWrite() {
            return true;
        }

        @Override
        public Class<Object> getManagedClass() {
            return null;
        }

        @Override
        public String getName() {
            return f.getName();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<? extends A> annotationClass) {
            return null;
        }
    }
}
