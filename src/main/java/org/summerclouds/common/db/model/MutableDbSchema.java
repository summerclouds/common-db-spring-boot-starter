package org.summerclouds.common.db.model;

import java.util.ArrayList;
import java.util.List;

import org.summerclouds.common.core.node.INode;

public class MutableDbSchema extends SecurityDbSchema {

	private List<Class<?>> registry = new ArrayList<>();
	
	public MutableDbSchema(INode config) {
		super(config);
	}

	@Override
	public void findObjectTypes(List<Class<? extends Object>> list) {
		registry.forEach(v -> list.add(v));
	}
	
	public List<Class<?>> getRegistry() {
		return registry;
	}

}
