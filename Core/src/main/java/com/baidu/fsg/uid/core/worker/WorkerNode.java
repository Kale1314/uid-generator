/*
 * Copyright (c) 2017 Baidu, Inc. All Rights Reserve.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.fsg.uid.core.worker;

import java.time.Instant;
import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Entity for M_WORKER_NODE
 *
 * @author yutianbao
 */
@Data
@Accessors(chain = true)
public class WorkerNode {

    /**
     * Entity unique id (table unique)
     */
    private Long id;

    /**
     * Type of CONTAINER: HostName, ACTUAL : IP.
     */
    private String hostName;

    /**
     * Type of CONTAINER: Port, ACTUAL : Timestamp + Random(0-10000)
     */
    private String port;

    /**
     * type of {@link WorkerNodeType}
     */
    private Integer type;

    /**
     * Worker launch date, default now
     */
    private LocalDate launchAt = LocalDate.now();

    /**
     * Created time
     */
    private Instant createdAt = Instant.now();

    /**
     * Last modified
     */
    private Instant updatedAt = Instant.now();



}
