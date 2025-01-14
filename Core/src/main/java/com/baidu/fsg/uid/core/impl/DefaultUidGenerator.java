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
package com.baidu.fsg.uid.core.impl;

import com.baidu.fsg.uid.core.BitsAllocator;
import com.baidu.fsg.uid.core.GeneratorProperties;
import com.baidu.fsg.uid.core.UidGenerator;
import com.baidu.fsg.uid.core.exception.UidGenerateException;
import com.baidu.fsg.uid.core.worker.WorkerIdAssigner;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents an implementation of {@link UidGenerator}
 * <p>
 * The unique id has 64bits (long), default allocated as blow:<br>
 * <li>sign: The highest bit is 0
 * <li>delta seconds: The next 28 bits, represents delta seconds since a customer epoch(2016-05-20 00:00:00.000).
 * Supports about 8.7 years until to 2024-11-20 21:24:16
 * <li>worker id: The next 22 bits, represents the worker's id which assigns based on database, max id is about 420W
 * <li>sequence: The next 13 bits, represents a sequence within the same second, max for 8192/s<br><br>
 * <p>
 * The {@link DefaultUidGenerator#parseUID(long)} is a tool method to parse the bits
 *
 * <pre>{@code
 * +------+----------------------+----------------+-----------+
 * | sign |     delta seconds    | worker node id | sequence  |
 * +------+----------------------+----------------+-----------+
 *   1bit          28bits              22bits         13bits
 * }</pre>
 * <p>
 * You can also specified the bits by Spring property setting.
 * <li>timeBits: default as 28
 * <li>workerBits: default as 22
 * <li>seqBits: default as 13
 * <li>epochStr: Epoch date string format 'yyyy-MM-dd'. Default as '2016-05-20'<p>
 *
 * <b>Note that:</b> The total bits must be 64 -1
 *
 * @author yutianbao
 */
@Slf4j
public class DefaultUidGenerator implements UidGenerator {

    /**
     * Spring property
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * Stable fields after spring bean initializing
     */
    protected final BitsAllocator bitsAllocator;
    protected final long workerId;

    public DefaultUidGenerator(WorkerIdAssigner workerIdAssigner, GeneratorProperties properties) {
        this.timeBits = properties.getTimeBits();
        this.workerBits = properties.getWorkerBits();
        this.seqBits = properties.getSeqBits();
        this.epochSeconds = properties.getEpochSeconds();

        // initialize bits allocator
        bitsAllocator = new BitsAllocator(timeBits, workerBits, seqBits);

        // initialize worker id
        workerId = workerIdAssigner.assignWorkerId();
        if (workerId > bitsAllocator.getMaxWorkerId()) {
            throw new RuntimeException("Worker id " + workerId + " exceeds the max " + bitsAllocator.getMaxWorkerId());
        }

        log.info("Initialized bits(1, {}, {}, {}) for workerID:{}", timeBits, workerBits, seqBits, workerId);
    }

    /**
     * Bits allocate
     */
    protected final int timeBits;
    protected final int workerBits;
    protected final int seqBits;
    protected final long epochSeconds;



    /**
     * Volatile fields caused by nextId()
     */
    protected long sequence = 0L;
    protected long lastSecond = -1L;



    @Override
    public long getUID() throws UidGenerateException {
        try {
            return nextId();
        } catch (Exception e) {
            log.error("Generate unique id exception. ", e);
            throw new UidGenerateException(e);
        }
    }

    @Override
    public String parseUID(long uid) {
        long totalBits = BitsAllocator.TOTAL_BITS;
        long signBits = bitsAllocator.getSignBits();
        long timestampBits = bitsAllocator.getTimestampBits();
        long workerIdBits = bitsAllocator.getWorkerIdBits();
        long sequenceBits = bitsAllocator.getSequenceBits();

        // parse UID
        long sequence = (uid << (totalBits - sequenceBits)) >>> (totalBits - sequenceBits);
        long workerId = (uid << (timestampBits + signBits)) >>> (totalBits - workerIdBits);
        long deltaSeconds = uid >>> (workerIdBits + sequenceBits);

        Instant thatTime = Instant.ofEpochMilli(epochSeconds + deltaSeconds);
        String thatTimeStr = LocalDateTime.ofInstant(thatTime, ZoneId.systemDefault()).toString();

        // format as string
        return String.format("{\"UID\":\"%d\",\"timestamp\":\"%s\",\"workerId\":\"%d\",\"sequence\":\"%d\"}",
                uid, thatTimeStr, workerId, sequence);
    }

    /**
     * Get UID
     *
     * @return UID
     * @throws UidGenerateException in the case: Clock moved backwards; Exceeds the max timestamp
     */
    protected long nextId() {
        lock.lock();
        try {


            long currentSecond = getCurrentSecond();

            // Clock moved backwards, refuse to generate uid
            if (currentSecond < lastSecond) {
                long refusedSeconds = lastSecond - currentSecond;
                throw new UidGenerateException("Clock moved backwards. Refusing for %d seconds", refusedSeconds);
            }

            // At the same second, increase sequence
            if (currentSecond == lastSecond) {
                sequence = (sequence + 1) & bitsAllocator.getMaxSequence();
                // Exceed the max sequence, we wait the next second to generate uid
                if (sequence == 0) {
                    currentSecond = getNextSecond(lastSecond);
                }

                // At the different second, sequence restart from zero
            } else {
                sequence = 0L;
            }

            lastSecond = currentSecond;

            // Allocate bits for UID
            return bitsAllocator.allocate(currentSecond - epochSeconds, workerId, sequence);

        } finally {
            lock.unlock();
        }
    }

    /**
     * Get next millisecond
     */
    private long getNextSecond(long lastTimestamp) {
        long timestamp = getCurrentSecond();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentSecond();
        }

        return timestamp;
    }

    /**
     * Get current second
     */
    private long getCurrentSecond() {
        long currentSecond = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        if (currentSecond - epochSeconds > bitsAllocator.getMaxDeltaSeconds()) {
            throw new UidGenerateException("Timestamp bits is exhausted. Refusing UID generate. Now: " + currentSecond);
        }

        return currentSecond;
    }


}
