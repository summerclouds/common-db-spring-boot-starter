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
package org.summerclouds.common.db.cmd;

import java.util.Comparator;
import java.util.List;

import org.summerclouds.common.core.console.ConsoleTable;
import org.summerclouds.common.core.operation.OperationComponent;
import org.summerclouds.common.core.operation.cmd.CmdArgument;
import org.summerclouds.common.core.operation.cmd.CmdOperation;
import org.summerclouds.common.core.operation.cmd.CmdOption;
import org.summerclouds.common.db.xdb.XdbType;

@OperationComponent(path = "xdb.info", description = "Show information of a type")
public class CmdInfo extends CmdOperation {

    @CmdArgument(
            index = 0,
            name = "type",
            required = true,
            description = "Type to select",
            multiValued = false)
    String typeName;

    @CmdOption(name = "-a", description = "Api Name", required = false)
    String apiName;

    @CmdOption(name = "-s", description = "Service Name", required = false)
    String serviceName;

    @Override
    public String executeCmd() throws Exception {

        XdbType<?> type = XdbUtil.getType(apiName, serviceName, typeName);

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

        ConsoleTable out = new ConsoleTable(tblOpt);
        out.setHeaderValues("Field Name", "Type", "PrimaryKey", "Persistent", "Mapping");
        for (String name : fieldNames) {
            out.addRowValues(
                    name,
                    type.getAttributeType(name),
                    type.isPrimaryKey(name),
                    type.isPersistent(name),
                    type.getTechnicalName(name));
        }

        out.print();

        return null;
    }
}
