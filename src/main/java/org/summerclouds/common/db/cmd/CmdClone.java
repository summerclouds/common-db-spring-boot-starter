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
package org.summerclouds.common.db.cmd;

import java.util.HashMap;
import java.util.Map.Entry;

import org.summerclouds.common.core.operation.OperationComponent;
import org.summerclouds.common.core.operation.cmd.CmdArgument;
import org.summerclouds.common.core.operation.cmd.CmdOperation;
import org.summerclouds.common.core.operation.cmd.CmdOption;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.db.xdb.XdbType;

@OperationComponent(
        path = "xdb.clone",
        description = "Load a object out of the database and store it as a clone.")
public class CmdClone extends CmdOperation {

    @CmdArgument(
            index = 0,
            name = "type",
            required = true,
            description = "Type to select",
            multiValued = false)
    String typeName;

    @CmdArgument(
            index = 1,
            name = "search",
            required = true,
            description = "Id of the object to clone or query",
            multiValued = false)
    String search;

    @CmdArgument(
            index = 2,
            name = "attributes",
            required = false,
            description = "Attributes for the initial creation",
            multiValued = true)
    String[] attributes;

    @CmdOption(name = "-a", description = "Api Name", required = false)
    String apiName;

    @CmdOption(name = "-s", description = "Service Name", required = false)
    String serviceName;

    @Override
    public String executeCmd() throws Exception {

        XdbType<?> type = XdbUtil.getType(apiName, serviceName, typeName);

        for (Object object : XdbUtil.createObjectList(type, search, null)) {

            System.out.println(">>> CLONE " + object);
            HashMap<String, Object> attrObj = null;
            attrObj = new HashMap<>();
            if (attributes != null) {
                for (String item : attributes) {
                    String key = MString.beforeIndex(item, '=').trim();
                    String value = MString.afterIndex(item, '=').trim();
                    attrObj.put(key, value);
                }
            }

            for (Entry<String, Object> entry : attrObj.entrySet()) {
                String name = entry.getKey();
                Object v = XdbUtil.prepareValue(type, name, entry.getValue());
                System.out.println("--- SET " + name + "  = " + v);
                XdbUtil.setValue(type, object, name, v);
            }

            //			for (String name : type.getAttributeNames()) {
            //				if (attrObj.containsKey(name)) {
            //					Object v = type.prepareValue(name, attrObj.get(name) );
            //					System.out.println("--- SET " + name + "  = " + v );
            //					XdbUtil.setValue(type, object, name, v);
            //				}
            //			}

            System.out.print("*** CREATE ");
            type.createObject(object);
            System.out.println(type.getIdAsString(object));
        }

        return null;
    }
}
