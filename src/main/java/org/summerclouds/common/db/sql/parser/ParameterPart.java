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
package org.summerclouds.common.db.sql.parser;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.error.ConflictRuntimeException;
import org.summerclouds.common.core.parser.ParseException;
import org.summerclouds.common.core.parser.ParseReader;
import org.summerclouds.common.core.parser.StringParsingPart;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.tool.MSystem;
import org.summerclouds.common.core.util.FallbackValueProvider;
import org.summerclouds.common.core.util.IValuesProvider;
import org.summerclouds.common.core.util.MapValuesProvider;
import org.summerclouds.common.core.util.Raw;
import org.summerclouds.common.db.sql.DbStatement;

public class ParameterPart extends StringParsingPart {

    private StringBuilder buffer;
    public String[] attribute;

    private ICompiler compiler;

    public ParameterPart(ICompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public void execute(StringBuilder out, IValuesProvider attributes) {

        Object value = attributes.get(attribute[0]);
        if (value == null) {
            out.append("null");
            return;
        }
        if (value.getClass().isArray()) {
            HashMap<String, Object> valueMap = new HashMap<String, Object>();
            FallbackValueProvider proxyMap =
                    new FallbackValueProvider(new MapValuesProvider(valueMap), attributes);
            for (int i = 0; i < ((Object[]) value).length; i++) {
                if (i != 0) out.append(attribute.length > 2 ? attribute[2] : ",");
                valueMap.put(attribute[0], ((Object[]) value)[i]);
                execute(out, proxyMap);
            }
            return;
        }
        if (value instanceof List) {
            HashMap<String, Object> valueMap = new HashMap<String, Object>();
            FallbackValueProvider proxyMap =
                    new FallbackValueProvider(new MapValuesProvider(valueMap), attributes);
            boolean first = true;
            for (Object obj : (List<?>) value) {
                if (!first) out.append(attribute.length > 2 ? attribute[2] : ",");
                valueMap.put(attribute[0], obj);
                execute(out, proxyMap);
                first = false;
            }
            return;
        }
        if (value instanceof InputStream) {
            if (!(attributes instanceof MapValuesProvider))
                throw new ConflictRuntimeException(
                        "attributes must be a map provider to provide blob streams");
            Map<String, Object> map = ((MapValuesProvider) attributes).getMap();
            out.append("?");
            DbStatement.addBinary(map, value);
            return;
        }
        String type = null;
        if (attribute.length > 1 && !MString.isEmptyTrim(attribute[1])) {
            type = attribute[1];
        } else {
            if (value instanceof Integer || value instanceof Short || value instanceof Byte)
                type = M.TYPE_INT;
            else if (value instanceof Long || value instanceof Character) type = M.TYPE_LONG;
            else if (value instanceof Double || value instanceof Float) type = M.TYPE_DOUBLE;
            else if (value instanceof Number || value instanceof Raw)
                type = M.TYPE_RAW; // direct toString() operation (via compiler request)
            else if (value instanceof Date
                    || value instanceof Calendar
                    || value instanceof java.sql.Date
                    || value instanceof LocalDate
                    || value instanceof LocalDateTime) type = M.TYPE_DATE;
            else if (value instanceof Boolean) type = M.TYPE_BOOL;
            else if (value instanceof Enum) type = M.TYPE_INT;
        }
        if (type == null) type = M.TYPE_TEXT;

        log().t(type, value);

        if (M.TYPE_TEXT.equals(type) || M.TYPE_STRING.equals(type))
            out.append("'").append(compiler.escape(String.valueOf(value))).append("'");
        else if (M.TYPE_INT.equals(type)) {
            if (value instanceof Enum)
                out.append(compiler.valueToNumber(((Enum<?>) value).ordinal()));
            else out.append(compiler.valueToNumber(value));
        } else if (M.TYPE_LONG.equals(type)) out.append(compiler.valueToNumber(value));
        else if (M.TYPE_FLOAT.equals(type) || M.TYPE_DOUBLE.equals(type))
            out.append(compiler.valueToFloating(value));
        else if (M.TYPE_DATE.equals(type)) out.append(compiler.toSqlDateValue(value));
        else if (M.TYPE_RAW.equals(type)) out.append(compiler.valueToString(value));
        else if (M.TYPE_BOOL.equals(type))
            out.append(compiler.toBoolValue(MCast.toboolean(value.toString(), false)));
        else log().w("Unknown attribute type:", type);
    }

    @Override
    public void doPreParse() {
        buffer = new StringBuilder();
    }

    @Override
    public void doPostParse() {
        attribute = MString.split(buffer.toString(), ",");
        buffer = null;
    }

    @Override
    public boolean parse(char c, ParseReader str) throws ParseException, IOException {

        str.consume();
        if (c == '$') {
            return false;
        }

        buffer.append(c);

        return true;
    }

    @Override
    public void dump(int level, StringBuilder out) {
        MString.appendRepeating(level, ' ', out);
        out.append(getClass().getCanonicalName()).append(attribute).append("\n");
    }

    @Override
    public String toString() {
        return MSystem.toString(this, new Object[] {attribute});
    }
}
