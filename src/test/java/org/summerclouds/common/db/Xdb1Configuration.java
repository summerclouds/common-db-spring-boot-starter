package org.summerclouds.common.db;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.summerclouds.common.db.xdb.XdbManager;

@Configuration
public class Xdb1Configuration {

	
	@Bean
	XdbManager xdbManager() {
		return new XdbManager();
	}
	
}
