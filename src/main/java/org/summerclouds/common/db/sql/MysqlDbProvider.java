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
package org.summerclouds.common.db.sql;

import org.summerclouds.common.core.node.MNode;
import org.summerclouds.common.core.tool.MSpring;

public class MysqlDbProvider extends JdbcProvider {

    public MysqlDbProvider(String host, String db, String user, String pass) {
        this(
                "jdbc:mysql://" + host + (host.indexOf(':') < 0 ? ":3306" : "") + "/" + db,
                user,
                pass);
    }

    public MysqlDbProvider(String url, String user, String pass) {
        config = new MNode();
        config.setProperty("driver", "com.mysql.jdbc.Driver");
        config.setProperty("url", url);
        config.setProperty("user", user);
        config.setProperty("pass", pass);
        config.setProperty("name", url);
        activator = MSpring.getDefaultActivator();
    }
}
