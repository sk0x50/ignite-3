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

package org.apache.ignite.configuration.validation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration validation exception.
 */
public class ConfigurationValidationException extends RuntimeException {
    /** Collection of configuration validation issues. */
    private Collection<ValidationIssue> issues;

    /**
     * Constructor.
     *
     * @param message Exception message.
     */
    public ConfigurationValidationException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param issues List of issues occurred during validation.
     */
    public ConfigurationValidationException(Collection<ValidationIssue> issues) {
        super(createMessageFromIssues(issues));

        this.issues = List.copyOf(issues);
    }

    /**
     * Returns list of issues occurred during validation.
     *
     * @return List of issues occurred during validation.
     */
    public Collection<ValidationIssue> getIssues() {
        return issues;
    }

    private static String createMessageFromIssues(Collection<ValidationIssue> issues) {
        return "Validation did not pass for keys: "
                + issues.stream().map(issue -> "[" + issue.key() + ", " + issue.message() + "]").collect(Collectors.joining(", "));
    }
}
