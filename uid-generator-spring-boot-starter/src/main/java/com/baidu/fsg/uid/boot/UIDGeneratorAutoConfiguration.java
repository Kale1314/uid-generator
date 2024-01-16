package com.baidu.fsg.uid.boot;

import com.baidu.fsg.uid.core.impl.CachedUidGenerator;
import com.baidu.fsg.uid.core.worker.WorkerIdAssigner;
import com.baidu.fsg.uid.core.worker.WorkerNodeStorage;
import com.baidu.fsg.uid.jdbc.JdbcWorkerNodeStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration
@EnableConfigurationProperties({
        GeneratorProperties.class
})
public class UIDGeneratorAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public WorkerNodeStorage workerNodeStorage(DataSource dataSource, GeneratorProperties properties){
        return new JdbcWorkerNodeStorage(dataSource,properties.getJdbc());
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkerIdAssigner workerIdAssigner(ApplicationContext applicationContext,WorkerNodeStorage workerNodeStorage){
        return new SpringWorkerIdAssigner(applicationContext,workerNodeStorage);
    }


    @Bean
    @ConditionalOnMissingBean
    public CachedUidGenerator uidGenerator(WorkerIdAssigner workerIdAssigner, GeneratorProperties properties){
        return new CachedUidGenerator(workerIdAssigner,properties);
    }
}
