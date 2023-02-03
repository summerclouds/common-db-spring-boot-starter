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
package org.summerclouds.common.db.xdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.summerclouds.common.core.activator.Activator;
import org.summerclouds.common.core.cfg.BeanRefMap;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.lang.SummerApplicationLifecycle;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.tool.MSpring;
import org.summerclouds.common.db.DbManagerJdbc;
import org.summerclouds.common.db.annotations.DbEntity;
import org.summerclouds.common.db.sql.DbPool;
import org.summerclouds.common.db.sql.DbPoolBundle;

/**
 * Handles XdbServices
 *
 * @author mikehummel
 */
public class XdbManager extends MLog implements SummerApplicationLifecycle {

    private Map<String, XdbService> services = new HashMap<>();
    private BeanRefMap<XdbService> xdbServices = new BeanRefMap<>(XdbService.class);

    private List<Class<?>> entities;

    protected void setup() {

        entities = MSpring.findAnnotatedClasses(DbEntity.class, true);

        Map<String, List<Class<?>>> mapping = new HashMap<>();

        for (Class<?> entity : entities) {
            DbEntity entityAnno = entity.getAnnotation(DbEntity.class);
            String serviceName = "default";
            if (entityAnno != null) {
                serviceName = entityAnno.service();
            }

            List<Class<?>> list = mapping.get(serviceName);
            if (list == null) {
                list = new ArrayList<>();
                mapping.put(serviceName, list);
            }
            list.add(entity);
        }

        // init services
        for (Map.Entry<String, List<Class<?>>> entry : mapping.entrySet()) {
            try {
                XdbService service = findService(entry.getKey());
                if (service == null) {
                    log().e("Service not found {1}", entry.getKey());
                    continue;
                }
                log().d("initialize service {1}", entry.getKey());
                services.put(entry.getKey(), service);
                service.initialize(entry.getValue());
                service.updateSchema(false); // false ?
            } catch (Exception t) {
                log().e("can't setup service {1}", entry.getKey(), t);
            }
        }
    }

    protected XdbService findService(String name) throws Exception {
        Map<String, XdbService> map = xdbServices.beans();
        XdbService service = map.get(name);
        if (service == null) {
            log().i("init xdb service as JDBC {1}", name);

            Activator activator = MSpring.getDefaultActivator();

            INode config = MSpring.getValueNode("xdb." + name + ".pool", null);
            if (config == null) {
                log().e("config not found for xdb service xdb.{1}.pool - FAILED", name);
                return null;
            }
            DbPoolBundle poolBundle = new DbPoolBundle(config, activator);

            INode schemaConfig = MSpring.getValueNode("xdb." + name + ".schema", null);
            XdbDbSchema schema = null;
            Map<String, XdbDbSchema> beans = MSpring.getBeansOfType(XdbDbSchema.class);
            schema = beans.get(name);
            if (schema != null) {
                log().i("bean xdb schema {1}", name);
                schema.setConfig(schemaConfig);
            } else {
                log().i("default xdb schema {1}", name);
                schema = new XdbDbSchema(schemaConfig);
            }
            DbPool rwPool = poolBundle.getPool("rw");
            DbPool roPool = rwPool;
            try {
                roPool = poolBundle.getPool("ro");
            } catch (MException e) {
                log().d("can't load separate ro pool for {1}", name, e.toString());
            }

            service = new DbManagerJdbc(name, rwPool, roPool, schema);
        } else log().d("load xdb service as bean {1}", name);
        return service;
    }

    public Class<?>[] getEntities() {
        return entities.toArray(new Class<?>[0]);
    }

    public XdbService getService(String name) {
        return services.get(name);
    }

    public String[] getServiceNames() {
        return services.keySet().toArray(new String[0]);
    }

    @Override
    public void onSummerApplicationStart() throws Exception {
        setup();
    }

    @Override
    public void onSummerApplicationStop() throws Exception {}
}
