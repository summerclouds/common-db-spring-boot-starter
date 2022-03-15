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
package org.summerclouds.common.db.cmd;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.console.ConsoleTable;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.matcher.Condition;
import org.summerclouds.common.core.operation.OperationComponent;
import org.summerclouds.common.core.operation.cmd.CmdArgument;
import org.summerclouds.common.core.operation.cmd.CmdOperation;
import org.summerclouds.common.core.operation.cmd.CmdOption;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.db.DbCollection;
import org.summerclouds.common.db.QueryParser;
import org.summerclouds.common.db.xdb.XdbService;
import org.summerclouds.common.db.xdb.XdbType;

@OperationComponent(
        path = "xdb.select",
        description = "Select data from DB DataSource and print the results")
// @Parsing(XdbParser.class) see
// https://github.com/apache/karaf/tree/master/jdbc/src/main/java/org/apache/karaf/jdbc/command/parsing
public class CmdSelect extends CmdOperation {

    @CmdArgument(
            index = 0,
            name = "select",
            required = false,
            description = "Select, e.g. * from Book where a=b",
            multiValued = true)
    String[] select;

    @CmdOption(
            name = "-l",
            description = "Disable multi line output in table cells",
            required = false)
    boolean oneLine = false;

    @CmdOption(
            name = "-f",
            description = "Additional filters after loading or results",
            required = false)
    String filter;

    @CmdOption(
            name = "-m",
            description = "Maximum amount of chars for a value (if not full)",
            required = false)
    int max = 40;

    @CmdOption(name = "-a", description = "Api Name", required = false)
    String apiName = null;

    @CmdOption(name = "-s", description = "Service Name", required = false)
    String serviceName = null;

    @CmdOption(name = "-v", description = "CSV Style", required = false)
    boolean csv = false;

    @CmdOption(
            name = "-n",
            description =
                    "Number of lines f<n> (first n lines) or l<n> (last n lines) or p[<page size>,]<page>",
            required = false)
    String page = null;

    @CmdOption(
            name = "-p",
            description = "Define a parameter key=value",
            required = false,
            multiValued = true)
    String[] parameters = null;

    @CmdOption(name = "-q", description = "xdb query parser", required = false)
    boolean xdbQuery = false;

    private Condition condition;

    @Override
    public String executeCmd() throws Exception {

        HashMap<String, Object> queryParam = null;
        if (parameters != null) {
            queryParam = new HashMap<>();
            for (String p : parameters) {
                String k = MString.beforeIndex(p, '=');
                String v = MString.afterIndex(p, '=');
                queryParam.put(k, v);
            }
        }

        if (MString.isSet(filter)) condition = new Condition(filter);

        XdbService service = XdbUtil.getService(apiName, serviceName);

        boolean isCount = false;
        if (select != null && select.length > 0 && select[0].equalsIgnoreCase("count(*)")) {
            isCount = true;
            select[0] = "*";
        }

        String sql = "SELECT " + MString.join(select, ' ');
        QueryParser parser = QueryParser.parse(service, sql);

        String typeName = parser.getEntityName();

        XdbType<?> type = XdbUtil.getType(apiName, serviceName, typeName);

        List<String> columns = parser.getColumnNames();

        if (isCount) {
            long cnt = type.count(parser.getQualification(), queryParam);
            System.out.println("Count: " + cnt);
            return String.valueOf(cnt);
        } else if (columns.size() == 1 && columns.get(0).equals("*")) {
            columns = null;
        }

        // sort columns to print
        final LinkedList<String> fieldNames = new LinkedList<>();
        if (columns == null) {
            for (String name : type.getAttributeNames()) {
                fieldNames.add(name);
            }

            fieldNames.sort(
                    new Comparator<String>() {

                        @Override
                        public int compare(String o1, String o2) {
                            boolean pk1 = type.isPrimaryKey(o1);
                            boolean pk2 = type.isPrimaryKey(o2);
                            if (pk1 == pk2) return o1.compareTo(o2);
                            if (pk1) return -1;
                            // if (pk2) return 1;
                            return 1;
                        }
                    });

        } else {
            for (String name : columns) fieldNames.add(name);
        }

        ConsoleTable out = createTable();
        if (csv) {
            out.setColSeparator(";");
            out.setCellSpacer(false);
        }
        if (oneLine) out.setMultiLine(false);
        //		if (!full)
        //			out.setMaxColSize(max);
        for (String name : fieldNames) {
            if (type.isPrimaryKey(name)) name = name + "*";
            out.addHeader(name);
        }

        //		if (xdbQuery) {
        //		    AQuery<?> query = Db.parse(type, qualification);
        //		}

        if (page == null) {
            for (Object object : type.getByQualification(parser.getQualification(), queryParam)) {

                if (skipResult(type, object)) continue;

                ConsoleTable.Row row = out.addRow();
                for (String name : fieldNames) {
                    Object value = getValueValue(type, object, name);
                    row.add(value);
                }
            }
        } else if (page.startsWith("f")) {
            int lines = MCast.toint(page.substring(1), 100);
            DbCollection<?> res = type.getByQualification(parser.getQualification(), null);
            for (Object object : res) {

                if (skipResult(type, object)) continue;

                ConsoleTable.Row row = out.addRow();
                for (String name : fieldNames) {
                    Object value = getValueValue(type, object, name);
                    row.add(value);
                }
                lines--;
                if (lines <= 0) {
                    res.close();
                    break;
                }
            }
        } else if (page.startsWith("l")) {
            int lines = MCast.toint(page.substring(1), 100);
            for (Object object : type.getByQualification(parser.getQualification(), null)) {

                if (skipResult(type, object)) continue;

                ConsoleTable.Row row = out.addRow();
                for (String name : fieldNames) {
                    Object value = getValueValue(type, object, name);
                    row.add(value);
                }
                if (out.size() > lines) out.removeFirstRow();
            }
        } else if (page.startsWith("p")) {
            int lines = 100;
            int p = 0;
            if (MString.isIndex(page, ',')) {
                lines = MCast.toint(MString.beforeIndex(page, ','), lines);
                p = MCast.toint(MString.afterIndex(page, ','), p);
            } else {
                p = MCast.toint(page, p);
            }
            System.out.println("Page size: " + lines + ", Page: " + p);

            DbCollection<?> res = type.getByQualification(parser.getQualification(), null);
            int cnt = 0;
            Iterator<?> iter = res.iterator();
            while (iter.hasNext()) {
                iter.next();
                cnt++;
                if (cnt >= p * lines) break;
            }
            while (iter.hasNext()) {
                Object object = iter.next();

                if (skipResult(type, object)) continue;

                ConsoleTable.Row row = out.addRow();
                for (String name : fieldNames) {
                    Object value = getValueValue(type, object, name);
                    row.add(value);
                }
                lines--;
                if (lines <= 0) {
                    res.close();
                    break;
                }
            }
        }

        out.print();

        return null;
    }

    private boolean skipResult(XdbType<?> type, Object object) throws MException {
        if (condition == null) return false;

        return condition.matches(new ConditionMap(type, object));
    }

    private class ConditionMap extends HashMap<String, Object> {

        private static final long serialVersionUID = 1L;
        private Object object;
        private XdbType<?> type;

        ConditionMap(XdbType<?> type, Object object) {
            this.type = type;
            this.object = object;
        }

        @Override
        public Object get(Object key) {
            try {
                return getValueValue(type, object, String.valueOf(key));
            } catch (MException e) {
                return null;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private Object getValueValue(XdbType<?> type, Object object, String name) throws MException {
        int pos = name.indexOf('.');
        Object value = null;
        if (pos < 0) {
            value = type.get(object, name);
        } else {
            String key = name.substring(pos + 1);
            name = name.substring(0, pos);
            value = type.get(object, name);
            if (value == null) {
                // nothing
            } else if (value instanceof List) {
                int idx = M.to(key, 0);
                List c = (List) value;
                if (idx < c.size()) value = c.get(idx);
                else value = null;
            } else if (value.getClass().isArray()) {
                int idx = M.to(key, 0);
                Object[] a = (Object[]) value;
                if (idx < a.length) value = a[idx];
                else a = null;
            } else if (value instanceof Map) {
                Map m = (Map) value;
                value = m.get(key);
            }
        }
        if (value == null) return "[null]";
        return value;
    }
}
