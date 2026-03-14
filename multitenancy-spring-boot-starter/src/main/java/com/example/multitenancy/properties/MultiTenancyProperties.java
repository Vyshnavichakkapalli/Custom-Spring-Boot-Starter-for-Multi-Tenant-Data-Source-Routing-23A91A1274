package com.example.multitenancy.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "multitenancy")
public class MultiTenancyProperties {

    private boolean enabled;
    private List<TenantDataSourceProperties> tenants = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<TenantDataSourceProperties> getTenants() {
        return tenants;
    }

    public void setTenants(List<TenantDataSourceProperties> tenants) {
        this.tenants = tenants;
    }

    public Set<String> getTenantIds() {
        return tenants.stream().map(TenantDataSourceProperties::getId).collect(Collectors.toSet());
    }

    public static class TenantDataSourceProperties {

        private String id;
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }
}
