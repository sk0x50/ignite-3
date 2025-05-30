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

namespace Apache.Ignite.Transactions
{
    using System;
    using System.Threading.Tasks;

    /// <summary>
    /// Ignite transaction.
    /// <para />
    /// Use <see cref="ITransactions.BeginAsync()"/> to start a new transaction.
    /// </summary>
    public interface ITransaction : IAsyncDisposable, IDisposable
    {
        /// <summary>
        /// Gets a value indicating whether this transaction is read-only.
        /// </summary>
        bool IsReadOnly { get; }

        /// <summary>
        /// Commits the transaction.
        /// </summary>
        /// <returns>A <see cref="Task"/> representing the asynchronous operation.</returns>
        Task CommitAsync();

        /// <summary>
        /// Rolls back the transaction.
        /// </summary>
        /// <returns>A <see cref="Task"/> representing the asynchronous operation.</returns>
        Task RollbackAsync();
    }
}
