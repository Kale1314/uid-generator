package com.baidu.fsg.uid.boot;

import com.baidu.fsg.uid.jdbc.JdbcProperties;
import com.baidu.fsg.uid.core.CacheGeneratorProperties;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = "com.baidu.fsg.uid")
@Validated
public class GeneratorProperties extends CacheGeneratorProperties implements InitializingBean {

    @Valid
    private Jdbc jdbc;

    @Override
    public void afterPropertiesSet() {
        if (jdbc==null){
            setJdbc(new Jdbc());
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Accessors(chain = true)
    public static class Jdbc extends JdbcProperties{

    }

}
