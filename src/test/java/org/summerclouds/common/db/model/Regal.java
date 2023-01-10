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
import java.util.UUID;

import org.summerclouds.common.db.DbComfortableObject;
import org.summerclouds.common.db.DbDynamic;
import org.summerclouds.common.db.util.DynamicField;

public class Regal extends DbComfortableObject implements DbDynamic {

    private HashMap<String, Object> values = new HashMap<String, Object>();

    private static Field[] fields =
            new Field[] {
                new DynamicField(
                        "id", UUID.class, DynamicField.PRIMARY_KEY, "true", "auto_id", "true"),
                new DynamicField("store", UUID.class),
                new DynamicField("name", String.class),
                new DynamicField("room", int.class),
                new DynamicField("position", int.class)
            };

    @Override
    public Field[] getFieldDefinitions() {
        return fields;
    }

    @Override
    public void setValue(Field dynamicField, Object value) {
        setValue(dynamicField.getName(), value);
    }

    @Override
    public Object getValue(Field dynamicField) {
        return getValue(dynamicField.getName());
    }

    public void setValue(String name, Object value) {
        if (value == null) values.remove(name);
        else values.put(name, value);
    }

    public Object getValue(String name) {
        return values.get(name);
    }
}
