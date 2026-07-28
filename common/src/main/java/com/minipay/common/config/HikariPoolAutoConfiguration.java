package com.minipay.common.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(HikariDataSource.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(HikariPoolProperties.class)
public class HikariPoolAutoConfiguration {

    @Bean
    public InitializingBean minipayHikariPoolCustomizer(DataSource dataSource, HikariPoolProperties properties) {
        return () -> {
            if (!properties.isEnabled() || !(dataSource instanceof HikariDataSource hikariDataSource)) {
                return;
            }
            hikariDataSource.setMaximumPoolSize(Math.max(1, properties.getMaximumPoolSize()));
            hikariDataSource.setMinimumIdle(Math.max(0, Math.min(properties.getMinimumIdle(), properties.getMaximumPoolSize())));
            hikariDataSource.setConnectionTimeout(properties.getConnectionTimeout());
            hikariDataSource.setValidationTimeout(properties.getValidationTimeout());
            hikariDataSource.setIdleTimeout(properties.getIdleTimeout());
            hikariDataSource.setMaxLifetime(properties.getMaxLifetime());
        };
    }
}
