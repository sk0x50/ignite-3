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

package org.apache.ignite.internal.configuration;

import static org.apache.ignite.internal.configuration.SystemDistributedConfigurationSchema.DEFAULT_IDLE_SAFE_TIME_SYNC_INTERVAL_MILLIS;

import com.google.auto.service.AutoService;
import java.util.Collection;
import java.util.List;
import org.apache.ignite.configuration.ConfigurationModule;
import org.apache.ignite.configuration.SuperRootChange;
import org.apache.ignite.configuration.annotation.ConfigurationType;

/** {@link ConfigurationModule} for cluster-wide system configuration. */
@AutoService(ConfigurationModule.class)
public class SystemDistributedConfigurationModule implements ConfigurationModule {
    @Override
    public ConfigurationType type() {
        return ConfigurationType.DISTRIBUTED;
    }

    @Override
    public Collection<Class<?>> schemaExtensions() {
        return List.of(SystemDistributedExtensionConfigurationSchema.class);
    }

    @Override
    public void migrateDeprecatedConfigurations(SuperRootChange superRootChange) {
        SystemDistributedExtensionView rootView = superRootChange.viewRoot(SystemDistributedExtensionConfiguration.KEY);
        SystemDistributedExtensionChange rootChange = superRootChange.changeRoot(SystemDistributedExtensionConfiguration.KEY);

        MetaStorageView metaStorageView = rootView.metaStorage();

        if (metaStorageView.idleSyncTimeInterval() != DEFAULT_IDLE_SAFE_TIME_SYNC_INTERVAL_MILLIS) {
            rootChange.changeSystem().changeIdleSafeTimeSyncIntervalMillis(metaStorageView.idleSyncTimeInterval());
        }
    }
}
