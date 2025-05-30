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

package org.apache.ignite.internal.configuration.hocon;

import static java.lang.String.format;
import static org.apache.ignite.internal.configuration.hocon.HoconPrimitiveConfigurationSource.formatArrayPath;
import static org.apache.ignite.internal.configuration.hocon.HoconPrimitiveConfigurationSource.unwrapPrimitive;
import static org.apache.ignite.internal.configuration.hocon.HoconPrimitiveConfigurationSource.wrongTypeException;
import static org.apache.ignite.internal.configuration.util.ConfigurationUtil.KEY_SEPARATOR;
import static org.apache.ignite.internal.configuration.util.ConfigurationUtil.appendKey;
import static org.apache.ignite.internal.configuration.util.ConfigurationUtil.join;

import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import org.apache.ignite.configuration.KeyIgnorer;
import org.apache.ignite.internal.configuration.TypeUtils;
import org.apache.ignite.internal.configuration.tree.ConfigurationSource;
import org.apache.ignite.internal.configuration.tree.ConstructableTreeNode;
import org.apache.ignite.internal.configuration.tree.NamedListNode;

/**
 * {@link ConfigurationSource} created from a HOCON list.
 */
class HoconListConfigurationSource implements ConfigurationSource {
    /**
     * Current path inside the top-level HOCON object.
     */
    private final List<String> path;

    /** Determines if key should be ignored. */
    private final KeyIgnorer keyIgnorer;

    /**
     * HOCON list that this source has been created from.
     */
    private final ConfigList hoconCfgList;

    /**
     * Creates a {@link ConfigurationSource} from the given HOCON list.
     *
     * @param keyIgnorer Determines if key should be ignored.
     * @param path Current path inside the top-level HOCON object. Can be empty if the given {@code hoconCfgList} is the top-level
     *         object.
     * @param hoconCfgList HOCON list.
     */
    HoconListConfigurationSource(KeyIgnorer keyIgnorer, List<String> path, ConfigList hoconCfgList) {
        this.keyIgnorer = keyIgnorer;
        this.path = path;
        this.hoconCfgList = hoconCfgList;
    }

    /** {@inheritDoc} */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (!clazz.isArray()) {
            throw wrongTypeException(clazz, path, -1);
        }

        int size = hoconCfgList.size();

        Class<?> componentType = clazz.getComponentType();
        Class<?> boxedComponentType = box(componentType);

        Object resArray = Array.newInstance(componentType, size);

        int idx = 0;
        for (Iterator<ConfigValue> iterator = hoconCfgList.iterator(); iterator.hasNext(); idx++) {
            ConfigValue hoconCfgListElement = iterator.next();

            switch (hoconCfgListElement.valueType()) {
                case OBJECT:
                case LIST:
                    throw wrongTypeException(boxedComponentType, path, idx);

                default:
                    Array.set(resArray, idx, unwrapPrimitive(hoconCfgListElement, boxedComponentType, path, idx));
            }
        }

        return (T) resArray;
    }

    /** {@inheritDoc} */
    @Override
    public void descend(ConstructableTreeNode node) {
        if (!(node instanceof NamedListNode)) {
            throw new IllegalArgumentException(
                    format("'%s' configuration is expected to be a composite configuration node, not a list", join(path))
            );
        }

        String syntheticKeyName = ((NamedListNode<?>) node).syntheticKeyName();

        if (keyIgnorer.shouldIgnore(join(appendKey(path, syntheticKeyName)))) {
            return;
        }

        for (int idx = 0; idx < hoconCfgList.size(); idx++) {
            ConfigValue next = hoconCfgList.get(idx);

            if (next.valueType() != ConfigValueType.OBJECT) {
                throw new IllegalArgumentException(format(
                        "'%s' is expected to be a composite configuration node, not a single value",
                        formatArrayPath(path, idx)
                ));
            }

            ConfigObject hoconCfg = (ConfigObject) next;

            String key;

            List<String> path;

            ConfigValue keyValue = hoconCfg.get(syntheticKeyName);

            if (keyValue != null && keyValue.valueType() == ConfigValueType.STRING) {
                // If the synthetic key is present, check that it has the correct type and use it as the key.
                key = (String) keyValue.unwrapped();
                path = appendKey(this.path, key);
            } else if (keyValue == null && hoconCfg.size() == 1) {
                // If the synthetic key is not present explicitly, we need to handle the case when a configuration uses InjectedValue.
                // This means that this object must only have one key.
                key = hoconCfg.entrySet().iterator().next().getKey();
                path = this.path;
            } else {
                throw new IllegalArgumentException(format(
                        "'%s' configuration value is mandatory and must be a String",
                        formatArrayPath(this.path, idx) + KEY_SEPARATOR + syntheticKeyName
                ));
            }

            node.construct(key, new HoconObjectConfigurationSource(syntheticKeyName, keyIgnorer, path, hoconCfg), false);
        }
    }

    /**
     * Non-null wrapper over {@link TypeUtils#boxed}.
     *
     * @param clazz Class, either primitive or not.
     * @return Boxed version of passed class.
     */
    public static Class<?> box(Class<?> clazz) {
        Class<?> boxed = TypeUtils.boxed(clazz);

        return boxed == null ? clazz : boxed;
    }
}
