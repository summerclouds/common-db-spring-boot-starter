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
package org.summerclouds.common.db.sql;

import java.util.UUID;

import org.summerclouds.common.core.node.MNode;
import org.summerclouds.common.core.tool.MSpring;

/**
 * HsqlDbProvider class.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class HsqlDbProvider extends JdbcProvider {

    /** Constructor for HsqlDbProvider. */
    public HsqlDbProvider() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Constructor for HsqlDbProvider.
     *
     * @param memoryDbName a {@link java.lang.String} object.
     */
    public HsqlDbProvider(String memoryDbName) {
        config = new MNode();
        config.setProperty("driver", "org.hsqldb.jdbcDriver");
        config.setProperty("url", "jdbc:hsqldb:mem:" + memoryDbName);
        config.setProperty("user", "sa");
        config.setProperty("pass", "");
        config.setProperty("name", memoryDbName);
        activator = MSpring.getDefaultActivator();
    }

    /**
     * Constructor for HsqlDbProvider.
     *
     * @param file a {@link java.lang.String} object.
     * @param user a {@link java.lang.String} object.
     * @param pass a {@link java.lang.String} object.
     */
    public HsqlDbProvider(String file, String user, String pass) {
        config = new MNode();
        config.setProperty("driver", "org.hsqldb.jdbcDriver");
        config.setProperty("url", "file:" + file);
        config.setProperty("user", user);
        config.setProperty("pass", pass);
        config.setProperty("name", file);
        activator = MSpring.getDefaultActivator();
    }
}
