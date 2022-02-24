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
package org.summerclouds.common.db.sql.analytics;

import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.tool.MPeriod;

public class SqlRuntimeWarning extends MLog implements SqlAnalyzer {

    private long traceMaxRuntime = MPeriod.MINUTE_IN_MILLISECONDS;

    @Override
    public void doAnalyze(
            long connectionId, String original, String query, long delta, Throwable t) {
        if (t != null) return;
        if (delta > traceMaxRuntime) {
            log().f("Query Runtime Warning", connectionId, delta, query);
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                log().w(""+connectionId, "  " + element);
            }
        }
    }

    public long getTraceMaxRuntime() {
        return traceMaxRuntime;
    }

    public void setTraceMaxRuntime(long traceMaxRuntime) {
        this.traceMaxRuntime = traceMaxRuntime;
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public void doConfigure(INode config) {
        traceMaxRuntime = config.getLong("traceMaxRuntime", traceMaxRuntime);
    }

//    @Override
//    public void doInitialize(IApiInternal internal, MCfgManager manager, INode config) {
//        if (config != null) doConfigure(config);
//        SqlAnalytics.setAnalyzer(this);
//    }
}
