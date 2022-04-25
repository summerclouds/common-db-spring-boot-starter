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
