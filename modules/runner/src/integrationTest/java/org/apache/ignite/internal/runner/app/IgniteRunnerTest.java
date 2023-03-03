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

package org.apache.ignite.internal.runner.app;

import static org.apache.ignite.internal.testframework.matchers.CompletableFutureMatcher.willCompleteSuccessfully;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgnitionManager;
import org.apache.ignite.app.IgniteRunner;
import org.apache.ignite.internal.IgniteIntegrationTest;
import org.apache.ignite.internal.testframework.WorkDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the start ignite nodes.
 */
public class IgniteRunnerTest extends IgniteIntegrationTest {
    private static final String NODE_NAME = "node";

    @WorkDirectory
    private Path workDir;

    @AfterEach
    void tearDown() {
        IgnitionManager.stop(NODE_NAME);
    }

    /** TODO: Replace this test by full integration test on the cli side IGNITE-15097. */
    @Test
    public void smokeTestArgs() throws Exception {
        Path configPath = Path.of(IgniteRunnerTest.class.getResource("/ignite-config.json").toURI());

        CompletableFuture<Ignite> ign = IgniteRunner.start(
                "--config-path", configPath.toAbsolutePath().toString(),
                "--work-dir", workDir.resolve("node").toAbsolutePath().toString(),
                "--node-name", NODE_NAME
        );

        IgnitionManager.init(NODE_NAME, List.of(NODE_NAME), "cluster");

        assertThat(ign, willCompleteSuccessfully());
    }
}
