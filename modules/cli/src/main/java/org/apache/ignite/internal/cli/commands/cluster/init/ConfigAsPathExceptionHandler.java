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

package org.apache.ignite.internal.cli.commands.cluster.init;

import static org.apache.ignite.internal.cli.commands.Options.Constants.CLUSTER_CONFIG_FILE_OPTION;

import org.apache.ignite.internal.cli.core.exception.ExceptionHandler;
import org.apache.ignite.internal.cli.core.exception.ExceptionWriter;
import org.apache.ignite.internal.cli.core.style.component.ErrorUiComponent;
import org.apache.ignite.internal.cli.core.style.element.UiElements;

/**
 * Handler for {@link ConfigAsPathException}.
 */
public class ConfigAsPathExceptionHandler implements ExceptionHandler<ConfigAsPathException> {
    @Override
    public int handle(ExceptionWriter err, ConfigAsPathException e) {
        err.write(
                ErrorUiComponent.builder()
                        .header(String.format("Failed to parse configuration file."
                                        + " Looks like config file path passed."
                                        + " Did you mean %s option?",
                                UiElements.command(CLUSTER_CONFIG_FILE_OPTION)))
                        .build().render()
        );

        return 1;
    }

    @Override
    public Class<ConfigAsPathException> applicableException() {
        return ConfigAsPathException.class;
    }
}
