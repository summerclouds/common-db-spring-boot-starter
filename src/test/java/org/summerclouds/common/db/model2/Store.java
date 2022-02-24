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
package org.summerclouds.common.db.model2;

import java.util.HashMap;
import java.util.UUID;

import org.summerclouds.common.db.DbComfortableObject;
import org.summerclouds.common.db.annotations.DbPersistent;
import org.summerclouds.common.db.annotations.DbPrimaryKey;

public class Store extends DbComfortableObject {

    @DbPrimaryKey private UUID id;
    @DbPersistent private String name;
    @DbPersistent private String address;
    @DbPersistent private UUID principal;
    @DbPersistent private String sqlDate;
    @DbPersistent private String floatValue;
    @DbPersistent private String charValue;
    @DbPersistent private String doubleValue;
    @DbPersistent private String intValue;
    @DbPersistent private String shortValue;
    @DbPersistent private String byteValue;
    @DbPersistent private String bigDecimalValue;
    @DbPersistent private HashMap<String, String> blobValue = new HashMap<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public UUID getPrincipal() {
        return principal;
    }

    public void setPrincipal(UUID principal) {
        this.principal = principal;
    }

    public String getSqlDate() {
        return sqlDate;
    }

    public void setSqlDate(String sqlDate) {
        this.sqlDate = sqlDate;
    }

    public String getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(String floatValue) {
        this.floatValue = floatValue;
    }

    public String getCharValue() {
        return charValue;
    }

    public void setCharValue(String charValue) {
        this.charValue = charValue;
    }

    public String getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(String doubleValue) {
        this.doubleValue = doubleValue;
    }

    public String getIntValue() {
        return intValue;
    }

    public void setIntValue(String intValue) {
        this.intValue = intValue;
    }

    public String getShortValue() {
        return shortValue;
    }

    public void setShortValue(String shortValue) {
        this.shortValue = shortValue;
    }

    public String getByteValue() {
        return byteValue;
    }

    public void setByteValue(String byteValue) {
        this.byteValue = byteValue;
    }

    public String getBigDecimalValue() {
        return bigDecimalValue;
    }

    public void setBigDecimalValue(String bigDecimalValue) {
        this.bigDecimalValue = bigDecimalValue;
    }

    public HashMap<String, String> getBlobValue() {
        return blobValue;
    }

    public void setBlobValue(HashMap<String, String> blobValue) {
        this.blobValue = blobValue;
    }
}
