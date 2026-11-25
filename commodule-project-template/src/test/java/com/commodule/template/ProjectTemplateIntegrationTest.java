package com.commodule.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.commodule.auth.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectTemplateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    @Qualifier("corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void loginReturnsTokenAndTraceId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("traceId").asText())
                .isEqualTo(result.getResponse().getHeader("X-Trace-Id"));
        assertThat(body.path("data").path("refreshToken").asText()).isNotEmpty();
        assertThat(body.path("data").path("refreshExpiresAt").asText()).isNotEmpty();
    }

    @Test
    void refreshTokenRotationAndLogoutWork() throws Exception {
        JsonNode login = loginData("admin", "admin123");
        String oldRefreshToken = login.path("refreshToken").asText();
        assertThat(oldRefreshToken).isNotEmpty();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();
        JsonNode refreshed = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .path("data");
        String accessToken = refreshed.path("token").asText();
        String currentRefreshToken = refreshed.path("refreshToken").asText();
        assertThat(currentRefreshToken).isNotEqualTo(oldRefreshToken);

        mockMvc.perform(get("/api/demo/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(oldRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .content(refreshJson(currentRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revoked").value(true));

        mockMvc.perform(get("/api/demo/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(currentRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/demo/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/demo/me").header(HttpHeaders.AUTHORIZATION, "bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void invalidLoginReturnsUnifiedValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("username")));
    }

    @Test
    void roleProtectionUsesUnifiedForbiddenResponse() throws Exception {
        String userToken = login("user", "user123");
        String adminToken = login("admin", "admin123");

        mockMvc.perform(get("/api/demo/admin").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(get("/api/demo/admin").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("admin access granted"));
    }

    @Test
    void fileRoundTripWorks() throws Exception {
        String adminToken = login("admin", "admin123");
        String userToken = login("user", "user123");
        byte[] content = "hello commodity".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").isNotEmpty())
                .andReturn();

        String key = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data")
                .path("key")
                .asText();

        mockMvc.perform(get("/api/files")
                        .param("key", key)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("hello.txt")));

        mockMvc.perform(delete("/api/files")
                        .param("key", key)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(delete("/api/files")
                        .param("key", key)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        mockMvc.perform(get("/api/files")
                        .param("key", key)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void fileDownloadFallsBackToOctetStreamForInvalidContentType() throws Exception {
        String adminToken = login("admin", "admin123");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.bin",
                "invalid-content-type",
                "payload".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        String key = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data")
                .path("key")
                .asText();

        mockMvc.perform(get("/api/files")
                        .param("key", key)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes("payload".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void springCachePreventsRepeatedLoads() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/demo/cache/1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loads").value(1));

        mockMvc.perform(get("/api/demo/cache/1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loads").value(1));
    }

    @Test
    void messageIsDeliveredAsynchronously() throws Exception {
        String token = login("admin", "admin123");
        String payload = "order-" + System.nanoTime();

        mockMvc.perform(post("/api/demo/events/{eventName}", payload)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageId").isNotEmpty());

        boolean received = false;
        for (int index = 0; index < 100 && !received; index++) {
            MvcResult result = authorized(get("/api/demo/events"), token)
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode events = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
            for (JsonNode event : events) {
                if (payload.equals(event.path("payload").asText())) {
                    received = true;
                    break;
                }
            }
            if (!received) {
                Thread.sleep(20);
            }
        }

        assertThat(received).isTrue();
    }

    @Test
    void dataPermissionFiltersOrdersByOwner() throws Exception {
        String adminToken = login("admin", "admin123");
        String userToken = login("user", "user123");

        mockMvc.perform(get("/api/demo/orders").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/api/demo/orders").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ownerId").value(2));

        mockMvc.perform(get("/api/demo/orders/page").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void codeGeneratorIsAdminOnlyAndProducesFiles() throws Exception {
        String adminToken = login("admin", "admin123");
        String userToken = login("user", "user123");
        String request = """
                {
                  "basePackage": "com.example.demo",
                  "className": "UserProfile",
                  "tableName": "user_profile",
                  "description": "用户资料",
                  "fields": [
                    {"name": "nickname", "javaType": "String", "required": true},
                    {"name": "age", "javaType": "Integer", "required": false}
                  ]
                }
                """;

        mockMvc.perform(post("/api/generator/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(post("/api/generator/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.files.length()").value(4))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("public record UserProfile")));

        MvcResult zipResult = mockMvc.perform(post("/api/generator/zip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andReturn();
        assertThat(zipResult.getResponse().getContentAsByteArray()).isNotEmpty();

        mockMvc.perform(post("/api/generator/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.replace("UserProfile", "user-profile"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void frameworkErrorsUseUnifiedResponses() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/demo/echo/0").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(get("/api/not-found").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));

        mockMvc.perform(post("/api/demo/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(40500))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(41500))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void corsAllowsConfiguredOriginsOnly() throws Exception {
        String token = login("admin", "admin123");
        assertThat(authProperties.getCorsAllowedOrigins()).contains("http://localhost:3000");
        assertThat(corsConfigurationSource)
                .as("CORS source bean type")
                .isInstanceOf(org.springframework.web.cors.UrlBasedCorsConfigurationSource.class);
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/demo/me")
        );
        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(configuration.checkOrigin("http://evil.example")).isNull();
        assertThat(configuration.checkHttpMethod(org.springframework.http.HttpMethod.GET)).isNotNull();

        mockMvc.perform(options("/api/demo/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"
                ));

        mockMvc.perform(options("/api/demo/me")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(get("/api/demo/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        org.hamcrest.Matchers.containsString("X-Trace-Id")
                ));
    }

    private String login(String username, String password) throws Exception {
        return loginData(username, password).path("token").asText();
    }

    private JsonNode loginData(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");
    }

    private org.springframework.test.web.servlet.ResultActions authorized(
            MockHttpServletRequestBuilder request,
            String token
    ) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String loginJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "username", username,
                "password", password
        ));
    }

    private String refreshJson(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "refreshToken", refreshToken
        ));
    }
}

