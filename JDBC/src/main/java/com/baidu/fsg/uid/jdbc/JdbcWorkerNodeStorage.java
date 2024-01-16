package com.baidu.fsg.uid.jdbc;

import com.baidu.fsg.uid.core.worker.WorkerNode;
import com.baidu.fsg.uid.core.worker.WorkerNodeStorage;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Objects;

public class JdbcWorkerNodeStorage implements WorkerNodeStorage {

    private final JdbcClient jdbcClient;
    private final String table;


    public JdbcWorkerNodeStorage(DataSource dataSource,JdbcProperties properties) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.table = getTable(properties);
    }

    private String getTable(JdbcProperties properties){
        if (StringUtils.hasLength(properties.getSchema())){
            return properties.getSchema()+"."+properties.getTable();
        }
        return properties.getTable();
    }

    /**
     * Get {@link WorkerNode} by node host
     *
     * @param host
     * @param port
     * @return
     */
    @Override
    public WorkerNode getWorkerNodeByHostPort(String host, String port) {
        return jdbcClient.sql("""
                        SELECT id,host_name,port,type,launch_at,created_at,updated_at
                        FROM %s
                        WHERE host_name=:host and port=:port
                        """
                        .formatted(table))
                .param("host",host)
                .param("port",port)
                .query(WorkerNode.class)
                .optional().orElse(null);
    }

    /**
     * Add {@link WorkerNode}
     *
     * @param workerNode
     */
    @Override
    public void addWorkerNode(WorkerNode workerNode) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO %s(host_name,port,type,launch_at,created_at,updated_at)
                        values(:host_name,:port,:type,:launch_at,:created_at,:updated_at)
                        """.formatted(table)
                ).param("host_name", workerNode.getHostName())
                .param("port", workerNode.getPort())
                .param("type", workerNode.getType())
                .param("launch_at", workerNode.getLaunchAt())
                .param("created_at", workerNode.getCreatedAt())
                .param("updated_at", workerNode.getUpdatedAt())
                .update(keyHolder);

        workerNode.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
    }
}
