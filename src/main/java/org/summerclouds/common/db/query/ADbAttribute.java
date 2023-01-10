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
package org.summerclouds.common.db.query;

import org.summerclouds.common.core.parser.AttributeMap;

/**
 * ADbAttribute class.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class ADbAttribute extends AAttribute {

    private String attribute;
    private Class<?> clazz;
    //	private static Log log = Log.getLog(ADbAttribute.class);

    /**
     * Constructor for ADbAttribute.
     *
     * @param clazz a {@link java.lang.Class} object.
     * @param attribute a {@link java.lang.String} object.
     */
    public ADbAttribute(Class<?> clazz, String attribute) {
        this.clazz = clazz;
        this.attribute = attribute.toLowerCase();
    }

    /** {@inheritDoc} */
    @Override
    public void getAttributes(AQuery<?> query, AttributeMap map) {}

    public String getAttribute() {
        return attribute;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
