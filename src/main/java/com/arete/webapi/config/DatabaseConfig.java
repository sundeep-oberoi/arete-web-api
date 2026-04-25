package com.arete.webapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${REINITIALIZE_DB:false}")
    private String reinitializeDb;

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

        if ("true".equalsIgnoreCase(reinitializeDb)) {
            log.info("REINITIALIZE_DB=true: dropping existing tables before schema creation");
            populator.addScript(new ClassPathResource("schema-drop.sql"));
        }

        populator.addScript(new ClassPathResource("schema.sql"));
        populator.addScript(new ClassPathResource("data.sql"));

        initializer.setDatabasePopulator(populator);
        log.info("Database initialization configured (reinitialize={})", reinitializeDb);
        return initializer;
    }
}
