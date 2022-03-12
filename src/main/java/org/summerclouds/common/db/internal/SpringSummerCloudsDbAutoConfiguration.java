package org.summerclouds.common.db.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.summerclouds.common.core.log.PlainLog;
import org.summerclouds.common.db.xdb.XdbManager;

public class SpringSummerCloudsDbAutoConfiguration {

	public SpringSummerCloudsDbAutoConfiguration() {
		PlainLog.i("Start SpringSummerCloudsDbAutoConfiguration");
	}
	
	@Bean
	@ConditionalOnProperty(name="org.summerclouds.db.xdb.enabled",havingValue = "true")
	XdbManager xdbManager() {
		return new XdbManager();
	}

}
