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

import org.summerclouds.common.core.operation.OperationComponent;
import org.summerclouds.common.core.operation.cmd.CmdArgument;
import org.summerclouds.common.core.operation.cmd.CmdOperation;
import org.summerclouds.common.core.operation.cmd.CmdOption;
import org.summerclouds.common.db.xdb.XdbType;

@OperationComponent(path = "xdb.delete", description = "Delete a single object from database")
public class CmdDelete extends CmdOperation {

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

    @CmdOption(name = "-a", description = "Api Name", required = false)
    String apiName;

    @CmdOption(name = "-s", description = "Service Name", required = false)
    String serviceName;

    @Override
    public String executeCmd() throws Exception {

        XdbType<?> type = XdbUtil.getType(apiName, serviceName, typeName);

        for (Object object : XdbUtil.createObjectList(type, search, null)) {
            System.out.println("*** DELETE " + object);
            type.deleteObject(object);
        }

        return null;
    }
}
