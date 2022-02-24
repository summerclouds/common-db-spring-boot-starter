package org.summerclouds.common.db.model;

import java.util.ArrayList;
import java.util.List;

import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.db.DbSchema;

public class MutableDbSchema extends DbSchema {

	private List<Class<?>> registry = new ArrayList<>();
	private INode config;
	
	public MutableDbSchema(INode config) {
		this.config = config;
	}

	@Override
	public void findObjectTypes(List<Class<? extends Object>> list) {
		registry.forEach(v -> list.add(v));
	}
	
	public List<Class<?>> getRegistry() {
		return registry;
	}

}
