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
package org.summerclouds.common.db.util;

import org.summerclouds.common.db.DbComfortableObject;
import org.summerclouds.common.db.annotations.DbPersistent;
import org.summerclouds.common.db.annotations.DbPrimaryKey;

/**
 * Property class.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class Property extends DbComfortableObject {

    @DbPrimaryKey private String key;
    @DbPersistent private String value;

    /**
     * Getter for the field <code>key</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getKey() {
        return key;
    }
    /**
     * Setter for the field <code>key</code>.
     *
     * @param key a {@link java.lang.String} object.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Getter for the field <code>value</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getValue() {
        return value;
    }
    /**
     * Setter for the field <code>value</code>.
     *
     * @param value a {@link java.lang.String} object.
     */
    public void setValue(String value) {
        this.value = value;
    }
}
