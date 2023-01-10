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
package org.summerclouds.common.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.summerclouds.common.db.model.Person;
import org.summerclouds.common.db.util.ParserJdbcDebug;
import org.summerclouds.common.junit.TestCase;

public class ParserTest extends TestCase {

    @BeforeAll
    public static void begin() {}

    @Test
    public void testLimit() throws Throwable {
        System.out.println(">>> testSelectAll");
        ParserJdbcDebug parser = new ParserJdbcDebug();
        parser.init(null);
        QueryParser.parse("select * from book limit 1", parser);

        System.out.println("Table: " + parser.getEntityName());
        System.out.println("Columns: " + parser.getColumnNames());
        System.out.println("Query: " + parser.getQualification());

        assertEquals("book", parser.getEntityName());
        assertEquals("LIMIT 1", parser.getQualification().trim());
        assertEquals("[*]", parser.getColumnNames().toString());
    }

    @Test
    public void testOrderAsc() throws Throwable {
        System.out.println(">>> testSelectAll");
        ParserJdbcDebug parser = new ParserJdbcDebug();
        parser.init(null);
        QueryParser.parse("select * from book order by name", parser);

        System.out.println("Table: " + parser.getEntityName());
        System.out.println("Columns: " + parser.getColumnNames());
        System.out.println("Query: " + parser.getQualification());

        assertEquals("book", parser.getEntityName());
        assertEquals("ORDER BY $db.book.name$ ASC", parser.getQualification().trim());
        assertEquals("[*]", parser.getColumnNames().toString());
    }

    @Test
    public void testOrderDesc() throws Throwable {
        System.out.println(">>> testSelectAll");
        ParserJdbcDebug parser = new ParserJdbcDebug();
        parser.init(null);
        QueryParser.parse("select * from book order by name desc", parser);

        System.out.println("Table: " + parser.getEntityName());
        System.out.println("Columns: " + parser.getColumnNames());
        System.out.println("Query: " + parser.getQualification());

        assertEquals("book", parser.getEntityName());
        assertEquals("ORDER BY $db.book.name$ DESC", parser.getQualification().trim());
        assertEquals("[*]", parser.getColumnNames().toString());
    }

    @Test
    public void testSelectAll() throws Throwable {
        System.out.println(">>> testSelectAll");
        ParserJdbcDebug parser = new ParserJdbcDebug();
        parser.init(null);
        QueryParser.parse("select * from book", parser);

        System.out.println("Table: " + parser.getEntityName());
        System.out.println("Columns: " + parser.getColumnNames());
        System.out.println("Query: " + parser.getQualification());

        assertEquals("book", parser.getEntityName());
        assertEquals("", parser.getQualification());
        assertEquals("[*]", parser.getColumnNames().toString());
    }

    @Test
    public void testSelect() throws Throwable {
        System.out.println(">>> testSelect");

        ParserJdbcDebug parser = new ParserJdbcDebug();
        parser.init(null);
        QueryParser.parse("select * from Book where name='test' and created < '1.1.2020'", parser);

        System.out.println("Table: " + parser.getEntityName());
        System.out.println("Columns: " + parser.getColumnNames());
        System.out.println("Query: " + parser.getQualification());

        assertEquals("book", parser.getEntityName());
        assertEquals(
                "$db.book.name$ = 'test' AND $db.book.created$ < '1.1.2020'",
                parser.getQualification());
        assertEquals("[*]", parser.getColumnNames().toString());
    }

    @Test
    public void testSelectColumns() throws Throwable {
        System.out.println(">>> testSelectColumns");

        ParserJdbcDebug parser = new ParserJdbcDebug();
        parser.init(null);
        QueryParser.parse("select name,created from book where name='test'", parser);

        System.out.println("Table: " + parser.getEntityName());
        System.out.println("Columns: " + parser.getColumnNames());
        System.out.println("Query: " + parser.getQualification());

        assertEquals("book", parser.getEntityName());
        assertEquals("$db.book.name$ = 'test'", parser.getQualification());
        assertEquals("[name, created]", parser.getColumnNames().toString());
    }

    @Test
    public void testRealQuery() throws Throwable {
        DbManager manager = AdbTest.createBookstoreManager();

        // create persons
        Person p = new Person();
        p.setName("Klaus Mustermann");
        manager.createObject(p);
        @SuppressWarnings("unused")
        UUID p1 = p.getId();

        p.setId(null);
        p.setName("Alex Admin");
        manager.createObject(p);
        UUID p2 = p.getId();

        DbCollection<Person> res =
                QueryParser.getByQualification(
                        manager, "select * from Person where name='Alex Admin'", null);
        assertTrue(res.hasNext());

        Person current = res.next();
        assertEquals(p2, current.getId());

        assertFalse(res.hasNext());

        manager.getPool().close();
    }
}
