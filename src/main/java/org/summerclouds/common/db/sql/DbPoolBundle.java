/**
 * Copyright (C) 2020 Mike Hummel (mh@mhus.de)
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
package org.summerclouds.common.db.sql;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.summerclouds.common.core.activator.Activator;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.RC;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.tool.MSpring;
import org.summerclouds.common.core.tool.MSystem;

/**
 * The class holds a bundle of different database pools.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class DbPoolBundle extends MLog {

    private INode config;
    private Activator activator;
    private Map<String, DbPool> bundle = new HashMap<String, DbPool>();

    /**
     * Create a new bundle from default configuration. Load it from MApi with the key of this class.
     */
    public DbPoolBundle() {
        this(null, null);
    }

    /**
     * Create a new Bundle from configuration.
     *
     * @param config Config element or null. null will use the central MApi configuration.
     * @param activator Activator or null. null will use the central MApi activator.
     */
    public DbPoolBundle(INode config, Activator activator) {

        if (config == null) config = MSpring.getValueNode(MSystem.getOwnerName(DbPoolBundle.class), null);
        if (activator == null) activator = MSpring.getDefaultActivator();

        this.config = config;
        this.activator = activator;
    }

    /**
     * getPool.
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link de.mhus.lib.sql.DbPool} object.
     * @throws java.lang.Exception if any.
     */
    public DbPool getPool(String name) throws Exception {

        if (bundle == null) throw new MException(RC.CONFLICT, "Bundle already closed");

        synchronized (bundle) {
            DbPool pool = bundle.get(name);
            if (pool == null) {
                INode poolCon = config.getObject(name);
                if (poolCon != null) {
                    pool = new DefaultDbPool(poolCon, activator);
                    bundle.put(name, pool);
                } else {
                    throw new MException(RC.ERROR, "config for pool {1} not found", name);
                }
            }
            return pool;
        }
    }

    /**
     * getNames.
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getNames() {
        LinkedList<String> out = new LinkedList<String>();
        for (INode c : config.getObjects()) {
            out.add(c.getName());
        }
        return out.toArray(new String[out.size()]);
    }

    /**
     * Getter for the field <code>config</code>.
     *
     * @param name a {@link java.lang.String} object.
     * @return a object.
     */
    public INode getConfig(String name) {
        return config.getObjectOrNull(name);
    }

    /**
     * Getter for the field <code>config</code>.
     *
     * @return a object.
     */
    public INode getConfig() {
        return config;
    }

    /** reset. */
    public void reset() {
        bundle = new HashMap<String, DbPool>();
    }

    /** close. */
    public void close() {

        if (bundle == null) return;

        synchronized (bundle) {
            for (DbPool pool : bundle.values()) pool.close();
            bundle = null;
        }
    }
}
