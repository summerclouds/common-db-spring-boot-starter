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

public class DialectFactory {

    public Dialect findDialect(String driver) {
        Dialect dialect = null;
        if (driver != null) {
            driver = driver.toLowerCase();
            if (driver.indexOf("hsql") > -1) dialect = new DialectHsqldb();
            else if (driver.indexOf("mysql") > -1 || driver.indexOf("mariadb") > -1)
                dialect = new DialectMysql();
            else if (driver.indexOf("postgresql") > -1) dialect = new DialectPostgresql();
            else if (driver.indexOf("h2") > -1) dialect = new DialectH2();
        }
        if (dialect == null) {
            dialect = new DialectDefault();
        }
        return dialect;
    }
}
