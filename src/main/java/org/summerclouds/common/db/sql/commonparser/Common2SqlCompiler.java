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
package org.summerclouds.common.db.sql.commonparser;

import org.summerclouds.common.core.parser.CompiledString;
import org.summerclouds.common.core.parser.ParseException;
import org.summerclouds.common.core.parser.Parser;
import org.summerclouds.common.core.parser.StringPart;
import org.summerclouds.common.core.tool.MXml;
import org.summerclouds.common.db.sql.parser.ICompiler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** <common> <select> <from></from> <where></where> </select> </common> */
public class Common2SqlCompiler implements Parser {

    protected ICompiler compiler;

    public Common2SqlCompiler(ICompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public CompiledString compileString(String in) throws ParseException {
        try {
            Document xml = MXml.loadXml(in);

            Element rootElement = xml.getDocumentElement();
            if (!rootElement.getNodeName().equals("common"))
                throw new ParseException(
                        "xml is not in common language, need to start with the tag <common>", in);

            return new CompiledString(new StringPart[] {});
        } catch (Exception e) {
            throw new ParseException(in, e);
        }
    }
}
