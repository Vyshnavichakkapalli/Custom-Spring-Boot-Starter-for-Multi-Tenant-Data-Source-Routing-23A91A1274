package com.example.multitenancy.autoconfigure;

import com.example.multitenancy.properties.MultiTenancyProperties;
import com.example.multitenancy.resolver.HeaderTenantResolver;
import com.example.multitenancy.resolver.TenantResolver;
import com.example.multitenancy.routing.TenantAwareRoutingDataSource;
import com.example.multitenancy.web.TenantInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(MultiTenancyProperties.class)
@ConditionalOnClass({AbstractRoutingDataSource.class, HandlerInterceptor.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "multitenancy", name = "enabled", havingValue = "true")
public class MultiTenancyAutoConfiguration {

    @Bean("tenantDataSources")
    @ConditionalOnMissingBean(name = "tenantDataSources")
    public Map<String, DataSource> tenantDataSources(MultiTenancyProperties properties) {
        List<MultiTenancyProperties.TenantDataSourceProperties> configuredTenants = properties.getTenants();
        if (configuredTenants == null || configuredTenants.isEmpty()) {
            throw new IllegalStateException("At least one tenant datasource must be configured under multitenancy.tenants");
        }

        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        for (MultiTenancyProperties.TenantDataSourceProperties tenant : configuredTenants) {
            HikariDataSource dataSource = DataSourceBuilder.create()
                    .type(HikariDataSource.class)
                    .url(tenant.getUrl())
                    .username(tenant.getUsername())
                    .password(tenant.getPassword())
                    .driverClassName(tenant.getDriverClassName())
                    .build();
            dataSource.setPoolName("tenant-" + tenant.getId() + "-pool");
            dataSources.put(tenant.getId(), dataSource);
        }

        return dataSources;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(@Qualifier("tenantDataSources") Map<String, DataSource> tenantDataSources) {
        TenantAwareRoutingDataSource routingDataSource = new TenantAwareRoutingDataSource();
        routingDataSource.setTargetDataSources(new LinkedHashMap<>(tenantDataSources));
        routingDataSource.setDefaultTargetDataSource(tenantDataSources.values().iterator().next());
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantResolver tenantResolver() {
        return new HeaderTenantResolver(HeaderTenantResolver.DEFAULT_TENANT_HEADER);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantInterceptor tenantInterceptor(TenantResolver tenantResolver, MultiTenancyProperties properties) {
        return new TenantInterceptor(tenantResolver, properties.getTenantIds());
    }

    @Bean
    public WebMvcConfigurer tenantWebMvcConfigurer(TenantInterceptor tenantInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(tenantInterceptor).addPathPatterns("/api/**");
            }
        };
    }
}
