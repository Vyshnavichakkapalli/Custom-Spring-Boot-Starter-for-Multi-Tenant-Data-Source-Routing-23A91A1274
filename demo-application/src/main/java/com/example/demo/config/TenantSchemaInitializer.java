package com.example.demo.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.util.Map;

@Component
public class TenantSchemaInitializer {

    private static final String CREATE_USERS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL,
                CONSTRAINT uk_users_email UNIQUE (email)
            )
            """;

    @Resource(name = "tenantDataSources")
    private Map<String, DataSource> tenantDataSources;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeTenantSchemas() {
        tenantDataSources.values().forEach(dataSource -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute(CREATE_USERS_TABLE_SQL);
        });
    }
}
