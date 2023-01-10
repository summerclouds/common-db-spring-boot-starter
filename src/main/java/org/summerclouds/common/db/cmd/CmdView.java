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

import java.util.Comparator;
import java.util.List;

import org.summerclouds.common.core.console.ConsoleTable;
import org.summerclouds.common.core.operation.OperationComponent;
import org.summerclouds.common.core.operation.cmd.CmdArgument;
import org.summerclouds.common.core.operation.cmd.CmdOperation;
import org.summerclouds.common.core.operation.cmd.CmdOption;
import org.summerclouds.common.db.xdb.XdbType;

@OperationComponent(path = "xdb.view", description = "Show a object")
public class CmdView extends CmdOperation {

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
            required = false,
            description = "Id of the object or query in brakets e.g '($db.table.field$ = 1)'",
            multiValued = false)
    String search;

    @CmdOption(
            name = "-o",
            description = "Comma separated list of fields to print",
            required = false)
    String fieldsComma = null;

    @CmdOption(
            name = "-v",
            description = "Try to analyse Objects and print the values separately",
            required = false)
    boolean verbose = false;

    @CmdOption(
            name = "-m",
            description = "Maximum amount of chars for a value (if not full)",
            required = false)
    int max = 40;

    @CmdOption(name = "-a", description = "Api Name", required = false)
    String apiName;

    @CmdOption(name = "-s", description = "Service Name", required = false)
    String serviceName;

    @Override
    public String executeCmd() throws Exception {

        XdbType<?> type = XdbUtil.getType(apiName, serviceName, typeName);

        for (Object object : XdbUtil.createObjectList(type, search, null)) {

            if (object == null) {
                System.out.println("*** Object not found");
                continue;
            }
            System.out.println(">>> VIEW " + type.getIdAsString(object));

            ConsoleTable out = createTable();
            out.setHeaderValues("Field", "Value", "Type");

            List<String> fieldNames = type.getAttributeNames();
            fieldNames.sort(
                    new Comparator<String>() {

                        @Override
                        public int compare(String o1, String o2) {
                            boolean pk1 = type.isPrimaryKey(o1);
                            boolean pk2 = type.isPrimaryKey(o2);
                            if (pk1 == pk2) return o1.compareTo(o2);
                            if (pk1) return -1;
                            // if (pk2) return 1;
                            return 1;
                        }
                    });

            for (String name : fieldNames) {
                Object v = type.get(object, name);
                out.addRowValues(name, v, type.getAttributeType(name));
            }

            out.print();
        }

        return null;
    }
}
