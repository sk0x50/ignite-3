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

#include "ignite/client/transaction/transaction.h"
#include "ignite/client/detail/transaction/transaction_impl.h"

namespace ignite {

void transaction::commit_async(const ignite_callback<void> &callback) {
    m_impl->commit_async(callback);
}

void transaction::rollback_async(const ignite_callback<void> &callback) {
    m_impl->rollback_async(callback);
}

} // namespace ignite
