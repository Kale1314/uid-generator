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

import java.util.ArrayList;
import java.util.List;

import com.baidu.fsg.uid.core.CacheGeneratorProperties;
import com.baidu.fsg.uid.core.worker.WorkerIdAssigner;
import com.baidu.fsg.uid.core.BitsAllocator;
import com.baidu.fsg.uid.core.UidGenerator;
import com.baidu.fsg.uid.core.buffer.BufferPaddingExecutor;
import com.baidu.fsg.uid.core.buffer.RejectedPutBufferHandler;
import com.baidu.fsg.uid.core.buffer.RejectedTakeBufferHandler;
import com.baidu.fsg.uid.core.buffer.RingBuffer;
import com.baidu.fsg.uid.core.exception.UidGenerateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * Represents a cached implementation of {@link UidGenerator} extends
 * from {@link DefaultUidGenerator}, based on a lock free {@link RingBuffer}<p>
 * 
 * The spring properties you can specified as below:<br>
 * <li><b>boostPower:</b> RingBuffer size boost for a power of 2, Sample: boostPower is 3, it means the buffer size 
 *                        will be <code>({@link BitsAllocator#getMaxSequence()} + 1) &lt;&lt;
 *                        {@link #boostPower}</code>, Default as
 * <li><b>paddingFactor:</b> Represents a percent value of (0 - 100). When the count of rest available UIDs reach the 
 *                           threshold, it will trigger padding buffer. Default as{@link RingBuffer#DEFAULT_PADDING_PERCENT}
 *                           Sample: paddingFactor=20, bufferSize=1000 -> threshold=1000 * 20 /100, padding buffer will be triggered when tail-cursor<threshold
 * <li><b>scheduleInterval:</b> Padding buffer in a schedule, specify padding buffer interval, Unit as second
 * <li><b>rejectedPutBufferHandler:</b> Policy for rejected put buffer. Default as discard put request, just do logging
 * <li><b>rejectedTakeBufferHandler:</b> Policy for rejected take buffer. Default as throwing up an exception
 * 
 * @author yutianbao
 */
@Slf4j
public class CachedUidGenerator extends DefaultUidGenerator implements AutoCloseable {
    private final Integer boostPower;
    /** RingBuffer */
    private final RingBuffer ringBuffer;
    private final BufferPaddingExecutor bufferPaddingExecutor;

    public CachedUidGenerator(WorkerIdAssigner workerIdAssigner, CacheGeneratorProperties properties) {
        super(workerIdAssigner,properties);
        Long scheduleInterval = properties.getScheduleInterval();
        Integer paddingFactor = properties.getPaddingFactor();
        this.boostPower =  properties.getBoostPower();

        // initialize RingBuffer & RingBufferPaddingExecutor
        // initialize RingBuffer
        int bufferSize = ((int) bitsAllocator.getMaxSequence() + 1) << boostPower;
        this.ringBuffer = new RingBuffer(bufferSize, paddingFactor);
        log.info("Initialized ring buffer size:{}, paddingFactor:{}", bufferSize, paddingFactor);

        // initialize RingBufferPaddingExecutor
        boolean usingSchedule = (scheduleInterval != null);
        this.bufferPaddingExecutor = new BufferPaddingExecutor(ringBuffer, this::nextIdsForOneSecond, usingSchedule);
        if (usingSchedule) {
            bufferPaddingExecutor.setScheduleInterval(scheduleInterval);
        }

        log.info("Initialized BufferPaddingExecutor. Using schdule:{}, interval:{}", usingSchedule, scheduleInterval);

        // set rejected put/take handle policy
        this.ringBuffer.setBufferPaddingExecutor(bufferPaddingExecutor);
        // fill in all slots of the RingBuffer
        bufferPaddingExecutor.paddingBuffer();

        // start buffer padding threads
        bufferPaddingExecutor.start();
        log.info("Initialized RingBuffer successfully.");
    }





    @Override
    public long getUID() {
        try {
            return ringBuffer.take();
        } catch (Exception e) {
            log.error("Generate unique id exception. ", e);
            throw new UidGenerateException(e);
        }
    }

    @Override
    public String parseUID(long uid) {
        return super.parseUID(uid);
    }
    
    @Override
    public void close() {
        bufferPaddingExecutor.close();
    }

    /**
     * Get the UIDs in the same specified second under the max sequence
     * 
     * @param currentSecond
     * @return UID list, size of {@link BitsAllocator#getMaxSequence()} + 1
     */
    protected List<Long> nextIdsForOneSecond(long currentSecond) {
        // Initialize result list size of (max sequence + 1)
        int listSize = (int) bitsAllocator.getMaxSequence() + 1;
        List<Long> uidList = new ArrayList<>(listSize);

        // Allocate the first sequence of the second, the others can be calculated with the offset
        long firstSeqUid = bitsAllocator.allocate(currentSecond - epochSeconds, workerId, 0L);
        for (int offset = 0; offset < listSize; offset++) {
            uidList.add(firstSeqUid + offset);
        }

        return uidList;
    }
    
    public void setRejectedPutBufferHandler(RejectedPutBufferHandler rejectedPutBufferHandler) {
        this.ringBuffer.setRejectedPutHandler(rejectedPutBufferHandler);
    }

    public void setRejectedTakeBufferHandler(RejectedTakeBufferHandler rejectedTakeBufferHandler) {
        this.ringBuffer.setRejectedTakeHandler(rejectedTakeBufferHandler);
    }



}
