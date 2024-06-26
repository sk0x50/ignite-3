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

#pragma once

#include "ignite/client/compute/job_state.h"
#include "ignite/common/ignite_timestamp.h"
#include "ignite/common/uuid.h"

#include <optional>

namespace ignite {

/**
 * Compute job state.
 */
struct job_status {
    /// Job ID.
    uuid id{};

    /// State.
    job_state state{job_state::QUEUED};

    /// Create time.
    ignite_timestamp create_time{};

    /// Start time (@c std::nullopt when not yet started).
    std::optional<ignite_timestamp> start_time{};

    /// Finish time (@c std::nullopt when not yet finished).
    std::optional<ignite_timestamp> finish_time{};
};

} // namespace ignite
