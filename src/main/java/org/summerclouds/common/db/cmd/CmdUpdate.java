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

import java.util.LinkedList;

import org.summerclouds.common.core.console.Console;
import org.summerclouds.common.core.operation.OperationComponent;
import org.summerclouds.common.core.operation.cmd.CmdArgument;
import org.summerclouds.common.core.operation.cmd.CmdOperation;
import org.summerclouds.common.core.operation.cmd.CmdOption;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.util.Pair;
import org.summerclouds.common.db.xdb.XdbType;

@OperationComponent(path = "xdb.update", description = "Update a single object in database")
public class CmdUpdate extends CmdOperation {

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
            description = "Id of the object or query",
            multiValued = false)
    String search;

    @CmdArgument(
            index = 2,
            name = "attributes",
            required = false,
            description = "Attributes to update, e.g user=alfons",
            multiValued = true)
    String[] attributes;

    @CmdOption(name = "-f", description = "Force Save", required = false)
    boolean force = false;

    @CmdOption(name = "-w", description = "RAW Save", required = false)
    boolean raw = false;

    @CmdOption(name = "-a", description = "Api Name", required = false)
    String apiName;

    @CmdOption(name = "-s", description = "Service Name", required = false)
    String serviceName;

    @CmdOption(name = "-y", description = "Automatic yes", required = false)
    boolean yes;

    @Override
    public String executeCmd() throws Exception {

        LinkedList<Pair<String, String>> attrObj = null;
        attrObj = new LinkedList<>();
        if (attributes != null) {
            for (String item : attributes) {
                String key = MString.beforeIndex(item, '=').trim();
                String value = MString.afterIndex(item, '=').trim();
                attrObj.add(new Pair<String, String>(key, value));
            }
        }

        XdbType<?> type = XdbUtil.getType(apiName, serviceName, typeName);

        if (!yes
                && Console.askQuestion(
                                "Really update " + type + " items?",
                                new char[] {'y', 'N'},
                                true,
                                false)
                        != 'y') {
            System.out.println("Canceled by user");
            return null;
        }

        for (Object object : XdbUtil.createObjectList(type, search, null)) {
            System.out.println(">>> UPDATE " + object);

            for (Pair<String, String> entry : attrObj) {
                String name = entry.getKey();
                Object v = XdbUtil.prepareValue(type, name, entry.getValue());
                System.out.println("--- SET " + name + "  = " + v);
                try {
                    XdbUtil.setValue(type, object, name, v);
                } catch (Exception t) {
                    System.out.println("*** Error: " + name);
                    t.printStackTrace();
                }
            }

            System.out.println("*** SAVE");
            if (force) type.saveObjectForce(object, raw);
            else type.saveObject(object);
        }
        return null;
    }
}
