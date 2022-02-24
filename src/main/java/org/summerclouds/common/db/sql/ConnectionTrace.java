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

import java.util.Date;

import org.summerclouds.common.core.log.Log;
import org.summerclouds.common.core.tool.MDate;

public class ConnectionTrace implements Comparable<ConnectionTrace> {

    private StackTraceElement[] stackTrace;
    private long time;

    public ConnectionTrace(DbConnection con) {
        time = System.currentTimeMillis();
        stackTrace = Thread.currentThread().getStackTrace();
    }

    @Override
    public String toString() {
        return MDate.toIsoDateTime(new Date(time));
    }

    //	public StackTraceElement[] getStackTrace() {
    //		return stackTrace;
    //	}

    @Override
    public int compareTo(ConnectionTrace o) {
        return Long.compare(time, o.time);
    }

    public void log(Log log) {
        log.w("Connection", this);
        for (StackTraceElement element : stackTrace) {
            log.w("trace", "  " + element);
        }
    }
    

}
