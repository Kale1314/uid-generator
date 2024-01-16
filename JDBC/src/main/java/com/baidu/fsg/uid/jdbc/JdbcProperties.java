package com.baidu.fsg.uid.jdbc;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class JdbcProperties {
    /**
     * 表名
     */
    @NotBlank
    private String table="t_worker_node";
    private String schema;


}
