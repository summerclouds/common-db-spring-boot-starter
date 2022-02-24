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

import org.summerclouds.common.core.log.MLog;

public abstract class LockStrategy extends MLog {

    public abstract void lock(Object object, String key, LockBase transaction, long timeout);

    public abstract void releaseLock(Object object, String key, LockBase transaction);

    public abstract boolean isLocked(Object object, String key, LockBase transaction);

    public abstract boolean isLockedByOwner(Object object, String key, LockBase transaction);
}
