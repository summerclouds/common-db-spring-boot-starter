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

import java.util.LinkedList;
import java.util.Set;

import org.summerclouds.common.core.error.TimeoutRuntimeException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.db.DbManager;

public abstract class LockBase extends MLog {

    private LinkedList<LockBase> nested;

    public LockBase() {}

    public abstract void lock(long timeout) throws TimeoutRuntimeException;

    public abstract void release();

    public abstract DbManager getDbManager();

    public abstract String getName();

    public synchronized void pushNestedLock(LockBase transaction) {
        if (nested == null) nested = new LinkedList<>();
        nested.add(transaction);
    }

    public LockBase popNestedLock() {
        if (nested == null || nested.size() == 0) return null;
        return nested.removeLast();
    }

    public LockBase getNested() {
        if (nested == null || nested.size() == 0) return null;
        return nested.getLast();
    }

    public abstract Set<String> getLockKeys();

    protected abstract boolean isLocked();
}
