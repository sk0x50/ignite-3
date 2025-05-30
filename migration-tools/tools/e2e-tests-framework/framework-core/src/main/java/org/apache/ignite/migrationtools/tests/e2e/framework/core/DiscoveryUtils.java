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

package org.apache.ignite.migrationtools.tests.e2e.framework.core;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** DiscoveryUtils. */
public class DiscoveryUtils {

    private DiscoveryUtils() {
        // Intentionally Left Blank
    }

    /**
     * Discovers all the test class implementations.
     *
     * @return The discovered test class implementations.
     */
    public static List<ExampleBasedCacheTest> discoverClasses() {
        // Load Service Providers
        var clsFromSrvcProvider = ServiceLoader.load(ExampleBasedCacheTestProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .flatMap(p -> p.provideTestClasses().stream());

        var directClasses = ServiceLoader.load(ExampleBasedCacheTest.class)
                .stream()
                .map(ServiceLoader.Provider::get);

        return Stream.concat(directClasses, clsFromSrvcProvider).collect(Collectors.toList());
    }

}
