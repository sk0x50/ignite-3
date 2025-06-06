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

package org.apache.ignite.internal.tx.message;

import java.util.ArrayList;
import java.util.List;
import org.apache.ignite.internal.network.annotations.Transferable;
import org.apache.ignite.internal.tx.TransactionMeta;
import org.apache.ignite.internal.tx.TxMeta;
import org.apache.ignite.internal.tx.impl.EnlistedPartitionGroup;

/** Message for transferring a {@link TxMeta}. */
@Transferable(TxMessageGroup.TX_META_MESSAGE)
public interface TxMetaMessage extends TransactionMetaMessage {
    /** List of enlisted partition groups. */
    List<EnlistedPartitionGroupMessage> enlistedPartitions();

    /** Converts to {@link TxMeta}. */
    default TxMeta asTxMeta() {
        List<EnlistedPartitionGroupMessage> enlistedPartitionMessages = enlistedPartitions();
        var enlistedPartitions = new ArrayList<EnlistedPartitionGroup>(enlistedPartitionMessages.size());

        for (EnlistedPartitionGroupMessage enlistedPartitionMessage : enlistedPartitionMessages) {
            enlistedPartitions.add(enlistedPartitionMessage.asPartitionInfo());
        }

        return new TxMeta(txState(), enlistedPartitions, commitTimestamp());
    }

    @Override
    default TransactionMeta asTransactionMeta() {
        return asTxMeta();
    }
}
