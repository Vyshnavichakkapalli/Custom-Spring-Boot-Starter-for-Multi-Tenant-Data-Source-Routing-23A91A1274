package com.example.multitenancy.web;

import com.example.multitenancy.TenantContext;
import com.example.multitenancy.exception.MissingTenantHeaderException;
import com.example.multitenancy.exception.UnknownTenantException;
import com.example.multitenancy.resolver.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

public class TenantInterceptor implements HandlerInterceptor {

    private final TenantResolver tenantResolver;
    private final Set<String> configuredTenants;

    public TenantInterceptor(TenantResolver tenantResolver, Set<String> configuredTenants) {
        this.tenantResolver = tenantResolver;
        this.configuredTenants = configuredTenants;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = tenantResolver.resolveTenantId(request)
                .orElseThrow(() -> new MissingTenantHeaderException("X-Tenant-ID header is missing"));

        if (!configuredTenants.contains(tenantId)) {
            throw new UnknownTenantException("Tenant not found: " + tenantId);
        }

        TenantContext.setCurrentTenant(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
