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
package org.summerclouds.common.db.transaction;

import java.util.HashMap;
import java.util.WeakHashMap;

import org.summerclouds.common.core.log.Log;
import org.summerclouds.common.core.tool.MSystem;
import org.summerclouds.common.db.annotations.DbTransactionable;
import org.summerclouds.common.db.annotations.TransactionConnection;

public class Encapsulation {

    private HashMap<String, TransactionConnection> connections = new HashMap<>();
    private WeakHashMap<Object, String> objectIdList = new WeakHashMap<>();

    public boolean append(DbTransactionable owner) {
        String id = getObjectId(owner);
        synchronized (connections) {
            if (connections.containsKey(id)) return true; // already set
            TransactionConnection con = owner.createTransactionalConnection();
            if (con != null) {
                connections.put(id, con);
                return true;
            }
            return false;
        }
    }

    public boolean commit() {
        boolean success = true;
        synchronized (connections) {
            for (TransactionConnection con : connections.values())
                try {
                    con.commit();
                } catch (Throwable e) {
                    success = false;
                    Log.getLog(Encapsulation.class).w(e);
                }
        }
        return success;
    }

    public boolean rollback() {
        boolean success = true;
        synchronized (connections) {
            for (TransactionConnection con : connections.values())
                try {
                    con.rollback();
                } catch (Throwable e) {
                    success = false;
                    Log.getLog(Encapsulation.class).w(e);
                }
        }
        return success;
    }

    public void clear() {
        synchronized (connections) {
            for (TransactionConnection con : connections.values())
                try {
                    con.close();
                } catch (Throwable e) {
                	Log.getLog(Encapsulation.class).w(e);
                }
            connections.clear();
        }
    }

    public TransactionConnection getCurrent(DbTransactionable owner) {
        String id = getObjectId(owner);
        synchronized (connections) {
            return connections.get(id);
        }
    }

    public boolean isEmpty() {
        return connections.isEmpty();
    }

    public boolean isInTransaction(DbTransactionable owner) {
        String id = getObjectId(owner);
        synchronized (connections) {
            return connections.containsKey(id);
        }
    }

    public synchronized String getObjectId(DbTransactionable owner) {
        String id = objectIdList.get(owner);
        if (id == null) {
            id = MSystem.getObjectId(owner);
            objectIdList.put(owner, id);
        }
        return id;
    }
}
