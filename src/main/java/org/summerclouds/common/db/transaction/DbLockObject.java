/**
 * Copyright (C) 2022 Mike Hummel (mh@mhus.de)
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

import org.summerclouds.common.db.DbComfortableObject;
import org.summerclouds.common.db.annotations.DbPersistent;
import org.summerclouds.common.db.annotations.DbPrimaryKey;
import org.summerclouds.common.db.sql.DbConnection;

public class DbLockObject extends DbComfortableObject {

    @DbPrimaryKey(auto_id = false)
    @DbPersistent(features = "cut")
    private String key;

    @DbPersistent private long created;

    @DbPersistent private String owner;

    @DbPersistent private String ownerStr;

    @Override
    public void doPreCreate(DbConnection con) {
        created = System.currentTimeMillis();
        super.doPreCreate(con);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getCreated() {
        return created;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getAge() {
        return System.currentTimeMillis() - created;
    }

    public String getOwnerStr() {
        return ownerStr;
    }

    public void setOwnerStr(String ownerStr) {
        this.ownerStr = ownerStr;
    }
}
