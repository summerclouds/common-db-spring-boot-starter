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

import org.summerclouds.common.core.parser.ConstantParsingPart;
import org.summerclouds.common.core.parser.ParseException;
import org.summerclouds.common.core.parser.ParseReader;

/**
 * QuotPart class.
 *
 * @author mikehummel
 * @version $Id: $Id
 */
public class QuotPart extends ConstantParsingPart {

    boolean first = true;
    private char marker;

    /**
     * Constructor for QuotPart.
     *
     * @param compiler a {@link de.mhus.lib.sql.parser.ICompiler} object.
     */
    public QuotPart(ICompiler compiler) {}

    /** {@inheritDoc} */
    @Override
    public boolean parse(char c, ParseReader str) throws ParseException, IOException {

        if (first) {
            marker = c;
            str.consume();
            first = false;
            buffer.append(marker);
            return true;
        } else if (c == marker) {
            str.consume();
            buffer.append(marker);
            if (str.isClosed()) return false;
            char c2 = str.character();
            if (c2 == marker) {
                str.consume();
                buffer.append(marker);
                return true;
            }
            return false;
            //		} else
            //		if (c == '$') {
            //
        } else {
            buffer.append(c);
            str.consume();
        }

        return true;
    }
}
