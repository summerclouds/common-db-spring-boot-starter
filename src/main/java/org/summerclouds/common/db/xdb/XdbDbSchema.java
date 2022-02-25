package org.summerclouds.common.db.xdb;

import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.db.model.MutableDbSchema;

public class XdbDbSchema extends MutableDbSchema {

	public XdbDbSchema() {
		super(null);
	}
	
	public XdbDbSchema(INode config) {
		super(config);
	}

	@Override
    public void setConfig(INode config) {
    	super.setConfig(config);
    }


}
