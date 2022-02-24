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

/**
 * FeatureCut class.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class FeatureCut extends Feature {

    /** Constant <code>NAME</code> */
    public static final Object NAME = "cut";

    private boolean cutAll;

    /** {@inheritDoc} */
    @Override
    public void doInit() throws MException {
        cutAll = table.getAttributes().getBoolean("cut_all", false);
    }

    /** {@inheritDoc} */
    @Override
    public Object getValue(Object obj, Field field, Object val) throws MException {

        if ((cutAll || field.getAttributes().getBoolean("cut", false))
                && val != null
                && val instanceof String
                && ((String) val).length() > field.getSize()) {
            log().t("cut", field);
            val = ((String) val).substring(0, field.getSize());
        }
        return val;
    }
}
