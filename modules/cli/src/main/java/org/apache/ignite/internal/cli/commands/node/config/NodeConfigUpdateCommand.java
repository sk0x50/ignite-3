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

package org.apache.ignite.internal.cli.commands.node.config;

import static org.apache.ignite.internal.cli.commands.Options.Constants.CONFIG_UPDATE_FILE_OPTION;
import static org.apache.ignite.internal.cli.commands.Options.Constants.CONFIG_UPDATE_FILE_OPTION_DESC;

import jakarta.inject.Inject;
import java.io.File;
import java.util.concurrent.Callable;
import org.apache.ignite.internal.cli.call.configuration.NodeConfigUpdateCall;
import org.apache.ignite.internal.cli.call.configuration.NodeConfigUpdateCallInput;
import org.apache.ignite.internal.cli.commands.BaseCommand;
import org.apache.ignite.internal.cli.commands.SpacedParameterMixin;
import org.apache.ignite.internal.cli.commands.node.NodeUrlProfileMixin;
import org.apache.ignite.internal.cli.core.call.CallExecutionPipeline;
import org.apache.ignite.internal.cli.util.ConfigUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Command that updates node configuration.
 */
@Command(name = "update", description = "Updates node configuration")
public class NodeConfigUpdateCommand extends BaseCommand implements Callable<Integer> {
    /** Node URL option. */
    @Mixin
    private NodeUrlProfileMixin nodeUrl;

    /** Configuration from CLI that will be updated. */
    @Mixin
    private SpacedParameterMixin configFromArgs;

    /** Configuration from file that will be updated. */
    @Option(names = CONFIG_UPDATE_FILE_OPTION, description = CONFIG_UPDATE_FILE_OPTION_DESC)
    private File configFile;

    @Inject
    private NodeConfigUpdateCall call;

    /** {@inheritDoc} */
    @Override
    public Integer call() {
        return runPipeline(CallExecutionPipeline.builder(call)
                .inputProvider(this::buildCallInput)
        );
    }

    private NodeConfigUpdateCallInput buildCallInput() {
        return NodeConfigUpdateCallInput.builder()
                .nodeUrl(nodeUrl.getNodeUrl())
                .config(ConfigUtils.formUpdateConfig(configFile, configFromArgs))
                .build();
    }
}
