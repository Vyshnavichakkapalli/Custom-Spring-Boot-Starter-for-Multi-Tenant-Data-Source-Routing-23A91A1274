package com.example.demo.health;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class TenantDataSourcesHealthConfiguration {

    @Resource(name = "tenantDataSources")
    private Map<String, DataSource> tenantDataSources;

    @Bean(name = "datasourcesHealthContributor")
    public CompositeHealthContributor datasourcesHealthContributor() {
        Map<String, HealthIndicator> indicators = new LinkedHashMap<>();

        tenantDataSources.forEach((tenantId, dataSource) -> indicators.put(tenantId, () -> checkDataSource(dataSource)));

        return CompositeHealthContributor.fromMap(indicators);
    }

    private Health checkDataSource(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(2)) {
                return Health.down().withDetail("error", "Connection validation failed").build();
            }

            return Health.up()
                    .withDetail("database", connection.getMetaData().getDatabaseProductName())
                    .withDetail("url", connection.getMetaData().getURL())
                    .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
