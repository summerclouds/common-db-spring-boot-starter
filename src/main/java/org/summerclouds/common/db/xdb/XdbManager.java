package org.summerclouds.common.db.xdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.summerclouds.common.core.activator.Activator;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.tool.MSpring;
import org.summerclouds.common.db.DbManagerJdbc;
import org.summerclouds.common.db.annotations.DbEntity;
import org.summerclouds.common.db.sql.DbPool;
import org.summerclouds.common.db.sql.DbPoolBundle;

/**
 * Handles XdbServices
 * @author mikehummel
 *
 */
public class XdbManager extends MLog {

	private Map<String, XdbService> services = new HashMap<>();
	
	private List<Class<?>> entities;
	
	@PostConstruct
	protected void setup() {
		
		entities = MSpring.findAnnotatedClasses(DbEntity.class);
		
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
					log().e("Service not found {1}",entry.getKey());
					continue;
				}
				log().d("initialize service {1}",entry.getKey());
				services.put(entry.getKey(), service);
				service.initialize(entry.getValue());
				service.updateSchema(false); // false ?
			} catch (Throwable t) {
				log().e("can't setup service {1}",entry.getKey(), t);
			}
		}
		
		
	}


	protected XdbService findService(String name) throws Exception {
		Map<String, XdbService> map = MSpring.getBeansOfType(XdbService.class);
		XdbService service = map.get(name);
		if (service == null) {
			log().i("init xdb service as JDBC {1}",name);
			
			Activator activator = MSpring.getDefaultActivator();
			
			INode config = MSpring.getValueNode("xdb." + name + ".pool", null);
			if (config == null) {
				log().e("config not found for xdb service xdb.{1}.pool - FAILED",name);
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
	        	roPool= poolBundle.getPool("ro");
	        } catch (MException e) {
	        	log().d("can't load separate ro pool for {1}",name,e.toString());
	        }
	        
			service = new DbManagerJdbc(name, rwPool, roPool, schema);
		} else
			log().d("load xdb service as bean {1}",name);
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
    
}
