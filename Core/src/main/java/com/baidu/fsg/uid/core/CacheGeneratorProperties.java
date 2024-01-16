package com.baidu.fsg.uid.core;

import com.baidu.fsg.uid.core.buffer.RingBuffer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@Accessors(chain = true)
public class CacheGeneratorProperties extends GeneratorProperties{
    private static final int DEFAULT_BOOST_POWER = 3;

    @Min(1)
    private Long scheduleInterval = 60L;
    
    @Min(1)
    @NotNull
    private Integer paddingFactor = RingBuffer.DEFAULT_PADDING_PERCENT;

    @Min(1)
    @NotNull
    private Integer boostPower = DEFAULT_BOOST_POWER;
}
