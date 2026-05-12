package com.strengthlabs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strengthlabs.api.StrengthLabsApiApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(classes = StrengthLabsApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("strengthlabs_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static { POSTGRES.start(); }

    @Autowired private WebApplicationContext applicationContext;
    @Autowired protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    @BeforeEach
    void initMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    record HttpResponse(int status, Map<String, Object> body, Map<String, List<String>> headers) {}

    protected HttpResponse doPost(String url, Object body) throws Exception {
        return parse(mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andReturn());
    }

    protected HttpResponse doPost(String url, Object body, String token) throws Exception {
        return parse(mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(objectMapper.writeValueAsString(body)))
                .andReturn());
    }

    protected HttpResponse doGet(String url) throws Exception {
        return parse(mockMvc.perform(get(url)).andReturn());
    }

    protected HttpResponse doGet(String url, String token) throws Exception {
        return parse(mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token))
                .andReturn());
    }

    protected HttpResponse doPut(String url, Object body, String token) throws Exception {
        return parse(mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(objectMapper.writeValueAsString(body)))
                .andReturn());
    }

    protected HttpResponse doPutWithHeader(String url, Object body, String token,
                                           String headerName, String headerValue) throws Exception {
        return parse(mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header(headerName, headerValue)
                .content(objectMapper.writeValueAsString(body)))
                .andReturn());
    }

    protected HttpResponse doDelete(String url, String token) throws Exception {
        return parse(mockMvc.perform(delete(url)
                .header("Authorization", "Bearer " + token))
                .andReturn());
    }

    protected String registerAndGetToken(String email) throws Exception {
        HttpResponse r = doPost("/auth/register",
                Map.of("name", "User", "email", email, "password", "Password1"));
        return (String) r.body().get("access_token");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse parse(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        String content = response.getContentAsString();
        if (response.getStatus() >= 500) {
            System.err.println("[IT] 5xx body: " + content);
            if (result.getResolvedException() != null) {
                result.getResolvedException().printStackTrace();
            }
        }
        Map<String, Object> body = content.isBlank() ? Map.of() :
                objectMapper.readValue(content, Map.class);
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, List.of(response.getHeader(name)));
        }
        return new HttpResponse(response.getStatus(), body, headers);
    }
}
