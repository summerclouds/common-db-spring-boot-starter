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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.internal.SpringSummerCloudsCoreAutoConfiguration;
import org.summerclouds.common.core.tool.MCollection;
import org.summerclouds.common.db.internal.SpringSummerCloudsDbAutoConfiguration;
import org.summerclouds.common.db.query.Db;
import org.summerclouds.common.db.xdb.XdbManager;
import org.summerclouds.common.db.xdb.XdbService;
import org.summerclouds.common.db.xdb.XdbType;
import org.summerclouds.common.db.xdb1.Book;
import org.summerclouds.common.db.xdb1.Person;
import org.summerclouds.common.junit.TestCase;

@SpringBootTest(
        classes = {
            //		Xdb1Configuration.class,
            SpringSummerCloudsCoreAutoConfiguration.class,
            SpringSummerCloudsDbAutoConfiguration.class
        },
        properties = {
            "org.summerclouds.scan.packages=org.summerclouds.common.db.xdb1",
            "xdb.default.pool.rw.driver=org.hsqldb.jdbcDriver",
            "xdb.default.pool.rw.url=jdbc:hsqldb:mem:xdb1",
            "xdb.default.pool.rw.user=sa",
            "xdb.default.pool.rw.password=",
            "org.summerclouds.db.xdb.enabled=true",

            //				"xdb.default.schema.dings=a",

        })
public class Xdb1Test extends TestCase {

    @Autowired XdbManager manager;

    @Test
    public void testEntities() {
        assertNotNull(manager);

        Class<?>[] entities = manager.getEntities();
        assertEquals(2, entities.length);

        assertTrue(MCollection.contains(entities, Book.class));
        assertTrue(MCollection.contains(entities, Person.class));
    }

    @Test
    public void testService() throws NotFoundException {
        assertNotNull(manager);

        String[] names = manager.getServiceNames();
        assertEquals(1, names.length);
        assertEquals("default", names[0]);

        XdbService service = manager.getService("default");
        assertNotNull(service);

        System.out.println("Service: " + service.getDataSourceName());
        assertEquals("default", service.getDataSourceName());

        List<String> typeNames = service.getTypeNames();
        System.out.println(typeNames);
        assertEquals(2, typeNames.size());
        assertTrue(typeNames.contains("Book"));
        assertTrue(typeNames.contains("Person"));

        XdbType<Object> bookType = service.getType("Book");
        assertNotNull(bookType);

        XdbType<Object> personType = service.getType("Person");
        assertNotNull(personType);
    }

    @Test
    public void testSql() throws MException {
        assertNotNull(manager);

        XdbService service = manager.getService("default");

        Book book = service.inject(new Book());
        book.setName("test");
        book.save();

        Book book2 =
                service.getObjectByQualification(Db.query(Book.class).eq("name", book.getName()));
        assertNotNull(book2);
        assertEquals(book.getId(), book2.getId());
    }
}
