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

package org.apache.ignite.internal.distributionzones;

import java.util.UUID;
import org.apache.ignite.internal.tostring.S;

/**
 * Node representation that we store in data nodes.
 * Includes {@code nodeName} that is unique identifier of the node, that is not changing after a restart, and
 * {@code nodeId} that is unique identifier of a node, that changes after a restart.
 * {@code nodeId} is needed to get node's attributes from the local state of the distribution zone manager.
 */
public class Node implements Comparable<Node> {
    private final String nodeName;

    private final UUID nodeId;

    public Node(String nodeName, UUID nodeId) {
        this.nodeName = nodeName;
        this.nodeId = nodeId;
    }

    @Override
    public int hashCode() {
        return nodeName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Node that = (Node) obj;

        return nodeName().equals(that.nodeName());
    }

    public String nodeName() {
        return nodeName;
    }

    public UUID nodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return S.toString(Node.class, this);
    }

    @Override
    public int compareTo(Node o) {
        int compareConsistentId = nodeName.compareTo(o.nodeName);

        if (compareConsistentId != 0) {
            return compareConsistentId;
        } else {
            return nodeId.compareTo(o.nodeId);
        }
    }
}
