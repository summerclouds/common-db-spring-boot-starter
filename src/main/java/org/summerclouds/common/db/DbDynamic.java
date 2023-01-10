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
package org.summerclouds.common.db;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.node.INode;

/**
 * Implement this interface for a persistent class to manage the columns by you own.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public interface DbDynamic {

    /**
     * This is only called at manager initialization time and it return the field definitions for
     * this class. If you want to change the field definition recreate the DbManager (it's planed to
     * create the possibility to recreate the definition of the manager in lifetime.
     *
     * @return an array of {@link de.mhus.lib.adb.DbDynamic.Field} objects.
     */
    Field[] getFieldDefinitions();

    /**
     * setValue.
     *
     * @param dynamicField a {@link de.mhus.lib.adb.DbDynamic.Field} object.
     * @param value a {@link java.lang.Object} object.
     */
    void setValue(Field dynamicField, Object value);

    /**
     * getValue.
     *
     * @param dynamicField a {@link de.mhus.lib.adb.DbDynamic.Field} object.
     * @return a {@link java.lang.Object} object.
     */
    Object getValue(Field dynamicField);

    public interface Field {

        String getName();

        boolean isPrimaryKey();

        Class<?> getReturnType();

        INode getAttributes();

        String[] getIndexes() throws MException;

        boolean isPersistent();

        boolean isReadOnly();
    }
}
