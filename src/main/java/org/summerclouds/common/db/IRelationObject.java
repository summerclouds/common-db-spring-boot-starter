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

import org.summerclouds.common.db.model.FieldRelation;
import org.summerclouds.common.db.sql.DbConnection;

/**
 * IRelationObject interface.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public interface IRelationObject {

    /**
     * prepareCreate.
     *
     * @throws java.lang.Exception if any.
     */
    void prepareCreate() throws Exception;

    /**
     * created.
     *
     * @param con a {@link de.mhus.lib.sql.DbConnection} object.
     * @throws java.lang.Exception if any.
     */
    void created(DbConnection con) throws Exception;

    /**
     * saved.
     *
     * @param con a {@link de.mhus.lib.sql.DbConnection} object.
     * @throws java.lang.Exception if any.
     */
    void saved(DbConnection con) throws Exception;

    /**
     * setManager.
     *
     * @param fieldRelation a {@link de.mhus.lib.adb.model.FieldRelation} object.
     * @param obj a {@link java.lang.Object} object.
     */
    void setManager(FieldRelation fieldRelation, Object obj);

    /**
     * isChanged.
     *
     * @return a boolean.
     */
    boolean isChanged();

    /**
     * loaded.
     *
     * @param con a {@link de.mhus.lib.sql.DbConnection} object.
     */
    void loaded(DbConnection con);

    /**
     * prepareSave.
     *
     * @param con a {@link de.mhus.lib.sql.DbConnection} object.
     * @throws java.lang.Exception if any.
     */
    void prepareSave(DbConnection con) throws Exception;
}
