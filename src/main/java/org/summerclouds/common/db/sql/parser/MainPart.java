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
package org.summerclouds.common.db.sql.parser;

import java.io.IOException;
import java.util.LinkedList;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.MRuntimeException;
import org.summerclouds.common.core.parser.ConstantPart;
import org.summerclouds.common.core.parser.ParseException;
import org.summerclouds.common.core.parser.ParseReader;
import org.summerclouds.common.core.parser.ParsingPart;
import org.summerclouds.common.core.parser.StringParsingPart;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.util.IValuesProvider;

public class MainPart extends StringParsingPart {

    private LinkedList<ParsingPart> parts = new LinkedList<ParsingPart>();
    private boolean stopOnComma;
    private int brakCount;
    private ParsingPart last;
    private boolean parseAttributes = true;
    private ICompiler compiler;

    public MainPart(ICompiler compiler) {
        this.compiler = compiler;
        setParseAttributes(compiler.isParseAttributes());
    }

    @Override
    public void execute(StringBuilder out, IValuesProvider attributes) {
        try {
            for (ParsingPart p : parts) {
                p.execute(out, attributes);
            }
        } catch (MException e) {
            throw new MRuntimeException(e);
        }
    }

    public void add(ParsingPart pp) {
        parts.add(pp);
        last = pp;
    }

    @Override
    public void doPreParse() {}

    @Override
    public void doPostParse() {}

    @Override
    public boolean parse(char c, ParseReader str) throws ParseException, IOException {

        if (stopOnComma && (c == ',' || (c == ')' && brakCount == 0))) {
            return false;
        } else if (stopOnComma && c == '(') {
            ParsingPart pp = new OnePart(compiler);
            add(pp);
            pp.parse(str);
            brakCount++;
            return true;
        } else if (stopOnComma && c == ')') {
            ParsingPart pp = new OnePart(compiler);
            add(pp);
            pp.parse(str);
            brakCount--;
            return true;
        } else if (c >= '0' && c <= '9') {
            ParsingPart pp = new NumberPart(compiler);
            add(pp);
            pp.parse(str);
            return true;
        } else if (c == '-' || c == '+' || c == '(' || c == ')' || c == ' ' || c == '\n'
                || c == '\r' || c == '\t' || c == '*' || c == '=' || c == '>' || c == '<'
                || c == '!' || c == '/' || c == ',' || c == '.' || c == '|' || c == '&'
                || c == '%') {

            if (last != null && last instanceof OnePart) {
                ((OnePart) last).append(c);
                str.consume();
            } else {
                ParsingPart pp = new OnePart(compiler);
                add(pp);
                pp.parse(str);
            }
            return true;
        } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_') {
            ConstWordPart pp = new ConstWordPart(compiler);
            pp.parse(str);
            // remove spaces - not allowed yet
            //			boolean consumed;
            //			do {
            //				consumed = false;
            //				char c2 = str.character();
            //				if (c2 == ' ' || c2 == '\t') {
            //					str.consume();
            //					consumed = true;
            //				}
            //			} while (consumed);
            // it's a function?
            if (!str.isClosed() && str.character() == '(') {
                ParsingPart pp2 = new FunctionPart(compiler, pp.getContent());
                str.consume(); // consume ( on this place
                pp2.parse(str);
                pp2 = compiler.compileFunction((FunctionPart) pp2);
                add(pp2);
            } else {
                add(pp);
            }
            return true;
        } else
        //		if (c == '(') {
        //			str.consume();
        //			ParsingPart pp = new MyParsingPart();
        //			add(pp);
        //			pp.parse(str);
        //			return true;
        //		} else
        //		if (c == ')') {
        //			str.consume();
        //			return false;
        //		} else
        if (c == '\'' || c == '"') {
            ParsingPart pp = new QuotPart(compiler);
            add(pp);
            pp.parse(str);
            return true;
        } else if (isParseAttributes() && c == '$') {
            str.consume();
            if (str.isClosed()) {
                add(new ConstantPart("$"));
                return false;
            }
            if (str.character() == '$') {
                str.consume();
                add(new ConstantPart("$"));
                return true;
            }
            ParsingPart pp = new ParameterPart(compiler);
            add(pp);
            pp.parse(str);
            return true;
        }

        throw new ParseException("unknown character", c, str.getPosition()); // TODO more info
    }

    @Override
    public void dump(int level, StringBuilder out) {
        MString.appendRepeating(level, ' ', out);
        out.append(getClass().getCanonicalName()).append(" (").append("\n");
        for (ParsingPart p : parts) {
            p.dump(level + 1, out);
        }
        MString.appendRepeating(level, ' ', out);
        out.append(")").append("\n");
    }

    public void setStopOnComma(boolean b) {
        stopOnComma = b;
    }

    public void setParseAttributes(boolean parseAttributes) {
        this.parseAttributes = parseAttributes;
    }

    public boolean isParseAttributes() {
        return parseAttributes;
    }
}
