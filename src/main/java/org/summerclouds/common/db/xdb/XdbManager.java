package org.summerclouds.common.db.xdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.summerclouds.common.core.activator.Activator;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.tool.MSpring;
import org.summerclouds.common.core.tool.MSystem;
import org.summerclouds.common.db.DbManagerJdbc;
import org.summerclouds.common.db.DbSchema;
import org.summerclouds.common.db.annotations.DbEntity;
import org.summerclouds.common.db.model.MutableDbSchema;
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
	
	@Value("${xdbmanager.scan.packages}")
	private String packages;

	@PostConstruct
	protected void setup() {
		
		findAnnotatedClasses(packages);
		
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
					log().e("Service nut found {1}",entry.getKey());
					continue;
				}
				log().d("initialize service {1}",entry.getKey());
				services.put(entry.getKey(), service);
				service.initialize(entry.getValue());
				service.updateSchema(false); // false ?
			} catch (Throwable t) {
				log().e("can't setup service {1}",entry.getKey());
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
				log().e("contif not found for xdb service xdb.{1}.pool",name);
				return null;
			}
	        DbPoolBundle poolBundle = new DbPoolBundle(config, activator);

			INode schemaConfig = MSpring.getValueNode("xdb." + name + ".schema", null);
	        DbSchema schema = new MutableDbSchema(schemaConfig);
	        
	        DbPool rwPool = poolBundle.getPool("rw");
	        DbPool roPool = rwPool;
	        try {
	        	roPool= poolBundle.getPool("ro");
	        } catch (MException e) {
	        	log().d("can't load separate ro pool for {1}",name,e.toString());
	        }
	        
			service = new DbManagerJdbc("", rwPool, roPool, schema);
		} else
			log().d("load xdb service as bean {1}",name);
		return service;
	}
	
	
	
	private void findAnnotatedClasses(String scanPackage) {
		entities = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider provider = createComponentScanner();
        for (BeanDefinition beanDef : provider.findCandidateComponents(scanPackage)) {
        	try {
	        	Class<?> cl = MSystem.getClass(beanDef.getBeanClassName());
	        	entities.add(cl);
        	} catch (Throwable t) {
        		log().e("can't load xdb entity {1}",beanDef.getBeanClassName());
        	}
        }
    }
 
    private ClassPathScanningCandidateComponentProvider createComponentScanner() {
        // Don't pull default filters (@Component, etc.):
        ClassPathScanningCandidateComponentProvider provider
                = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(DbEntity.class));
        return provider;
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
