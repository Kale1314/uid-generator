package com.baidu.fsg.uid.core;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GeneratorProperties {
    @Min(1)
    @NotNull
    private Integer timeBits = 31;
    @Min(1)
    @NotNull
    private Integer workerBits = 20;
    @Min(1)
    @NotNull
    private Integer seqBits = 12;
    @Min(1)
    @NotNull
    private Long epochSeconds = 1640966400L;
}
