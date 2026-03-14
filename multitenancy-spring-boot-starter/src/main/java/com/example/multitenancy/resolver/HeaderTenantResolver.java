package com.example.multitenancy.resolver;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public class HeaderTenantResolver implements TenantResolver {

    public static final String DEFAULT_TENANT_HEADER = "X-Tenant-ID";

    private final String tenantHeader;

    public HeaderTenantResolver(String tenantHeader) {
        this.tenantHeader = tenantHeader;
    }

    @Override
    public Optional<String> resolveTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(tenantHeader);
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(tenantId.trim());
    }
}
