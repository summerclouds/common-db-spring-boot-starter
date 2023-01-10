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

import org.summerclouds.common.db.DbManager;

public class AttributeFeatureCut implements AttributeFeature {

    public static final String NAME = "cut";

    //	private Field field;
    private int size;

    @Override
    public void init(DbManager manager, Field field) {
        //		this.field = field;
        size = field.getSize();
    }

    @Override
    public Object set(Object pojo, Object value) {
        if (value != null && value instanceof String && ((String) value).length() > size)
            value = ((String) value).substring(0, size);
        return value;
    }

    @Override
    public Object get(Object pojo, Object value) {
        if (value != null && value instanceof String && ((String) value).length() > size)
            value = ((String) value).substring(0, size);
        return value;
    }
}
