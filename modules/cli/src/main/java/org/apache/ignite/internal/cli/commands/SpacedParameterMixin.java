/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.cli.commands;

import java.util.Arrays;
import java.util.stream.Collectors;
import picocli.CommandLine.Parameters;

/**
 * A mixin for a parameter that combines all parameters into one.
 * With this mixin, we allow the user to specify one parameter with spaces without quotation.
 */
public class SpacedParameterMixin {
    @Parameters(arity = "0..1")
    private String[] args;

    @Override
    public String toString() {
        return Arrays.stream(args).map(SpacedParameterMixin::unquote).collect(Collectors.joining(" "));
    }

    private static String unquote(String rawString) {
        if (isQuoted(rawString, '"')
                || isQuoted(rawString, '\'')
                || isQuoted(rawString, '`')
        ) {
            return rawString.substring(1, rawString.length() - 1);
        }
        return rawString;
    }

    private static boolean isQuoted(String string, char quoteChar) {
        return string.charAt(0) == quoteChar && string.charAt(string.length() - 1) == quoteChar;
    }

    public boolean hasContent() {
        return args != null && args.length > 0;
    }
}
