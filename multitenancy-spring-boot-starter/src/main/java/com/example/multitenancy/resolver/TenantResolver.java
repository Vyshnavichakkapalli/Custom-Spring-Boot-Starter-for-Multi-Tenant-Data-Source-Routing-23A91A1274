package com.example.multitenancy.resolver;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface TenantResolver {

    Optional<String> resolveTenantId(HttpServletRequest request);
}
