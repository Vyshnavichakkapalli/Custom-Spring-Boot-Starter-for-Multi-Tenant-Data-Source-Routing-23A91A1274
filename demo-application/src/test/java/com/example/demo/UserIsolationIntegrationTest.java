package com.example.demo;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.web.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserIsolationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateUserForTenantAndKeepIsolation() {
        CreateUserRequest request = new CreateUserRequest("Alice", "alice@tenant1.test");

        ResponseEntity<UserResponse> createResponse = restTemplate.exchange(
                "/api/users",
                HttpMethod.POST,
                requestForTenant("tenant1", request),
                UserResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().email()).isEqualTo("alice@tenant1.test");

        ResponseEntity<List<UserResponse>> tenant1Users = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                requestForTenant("tenant1", null),
                new ParameterizedTypeReference<>() {
                }
        );

        ResponseEntity<List<UserResponse>> tenant2Users = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                requestForTenant("tenant2", null),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(tenant1Users.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tenant2Users.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tenant1Users.getBody()).extracting(UserResponse::email).containsExactly("alice@tenant1.test");
        assertThat(tenant2Users.getBody()).isEmpty();
    }

    @Test
    void shouldListOnlyUsersBelongingToTenantHeader() {
        restTemplate.exchange(
                "/api/users",
                HttpMethod.POST,
                requestForTenant("tenant1", new CreateUserRequest("Tenant One", "one@tenant.test")),
                UserResponse.class
        );

        restTemplate.exchange(
                "/api/users",
                HttpMethod.POST,
                requestForTenant("tenant2", new CreateUserRequest("Tenant Two", "two@tenant.test")),
                UserResponse.class
        );

        ResponseEntity<List<UserResponse>> tenant1Users = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                requestForTenant("tenant1", null),
                new ParameterizedTypeReference<>() {
                }
        );

        ResponseEntity<List<UserResponse>> tenant2Users = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                requestForTenant("tenant2", null),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(tenant1Users.getBody()).extracting(UserResponse::email).containsExactly("one@tenant.test");
        assertThat(tenant2Users.getBody()).extracting(UserResponse::email).containsExactly("two@tenant.test");
    }

    @Test
    void shouldGetUserByIdWithinTenantScope() {
        ResponseEntity<UserResponse> createResponse = restTemplate.exchange(
                "/api/users",
                HttpMethod.POST,
                requestForTenant("tenant1", new CreateUserRequest("Scoped User", "scoped@tenant1.test")),
                UserResponse.class
        );

        assertThat(createResponse.getBody()).isNotNull();
        Long userId = createResponse.getBody().id();

        ResponseEntity<UserResponse> tenant1Get = restTemplate.exchange(
                "/api/users/" + userId,
                HttpMethod.GET,
                requestForTenant("tenant1", null),
                UserResponse.class
        );

        ResponseEntity<ApiError> tenant2Get = restTemplate.exchange(
                "/api/users/" + userId,
                HttpMethod.GET,
                requestForTenant("tenant2", null),
                ApiError.class
        );

        assertThat(tenant1Get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tenant1Get.getBody()).isNotNull();
        assertThat(tenant1Get.getBody().email()).isEqualTo("scoped@tenant1.test");
        assertThat(tenant2Get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectRequestWithoutTenantHeader() {
        ResponseEntity<ApiError> response = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                requestWithoutTenantHeader(),
                ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("X-Tenant-ID header is missing");
    }

    @Test
    void shouldRejectUnknownTenantHeader() {
        ResponseEntity<ApiError> response = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                requestForTenant("unknown_tenant", null),
                ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Tenant not found: unknown_tenant");
    }

    @Test
    void shouldExposeHealthForAllTenantDataSources() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/actuator/health/datasources",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");

        Map<String, Object> components = (Map<String, Object>) response.getBody().get("components");
        assertThat(components).containsKeys("tenant1", "tenant2", "tenant3");
    }

    private <T> HttpEntity<T> requestForTenant(String tenantId, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> requestWithoutTenantHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }
}
