package com.example.student_portal.integration;

import com.example.student_portal.dto.AuthResponse;
import com.example.student_portal.dto.UserResponse;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.repository.PortalUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for AuthController covering registration and login endpoints
class AuthControllerIntegrationTest {

    
    // Testcontainers: starts a real MySQL container once for all tests
    
    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0");

    
    // Random port assigned to embedded server - needed to build full request URLs
    
    @LocalServerPort
    private int port;

    
    // Spring-managed dependencies for verification and seeding test data
    
    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    
    // HTTP client for calling REST APIs
    
    private RestClient restClient;

    
    // Setup & Teardown
    

    @BeforeEach
    void setUp() {
        // Build a RestClient pointing at the random server port.
        // Custom status handler prevents RestClient from throwing on 4xx/5xx,
        // so we can assert error statuses directly.
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on errors */ })
                .build();
    }

    @AfterEach
    void tearDown() {
        // Clean up all data after each test to keep tests independent
        portalUserRepository.deleteAll();
    }

    
    // Test 1 – POST /api/auth/register  →  successfully registers a new user
    
    @Test
    void shouldRegisterNewUser() {
        // Given – a valid registration payload
        Map<String, Object> request = new HashMap<>();
        request.put("email", "newuser@test.com");
        request.put("password", "password123");
        request.put("role", "STUDENT");

        // When – POST /api/auth/register is called
        ResponseEntity<UserResponse> response = restClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(UserResponse.class);

        // Then – response is 200 OK and user details are returned
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("newuser@test.com");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.STUDENT);

        // Verify user is actually persisted in the database with encoded password
        PortalUser saved = portalUserRepository.findByEmail("newuser@test.com").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("newuser@test.com");
        assertThat(saved.getPassword()).isNotEqualTo("password123"); // password should be encoded
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    
    // Test 2 – POST /api/auth/register  →  fails when email already exists
    
    @Test
    void shouldFailRegistrationWhenEmailAlreadyExists() {
        // Given – an existing user with the same email
        PortalUser existingUser = PortalUser.builder()
                .email("existing@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.STUDENT)
                .build();
        portalUserRepository.save(existingUser);

        // Build registration payload with duplicate email
        Map<String, Object> request = new HashMap<>();
        request.put("email", "existing@test.com");
        request.put("password", "newpassword");
        request.put("role", "STUDENT");

        // When – POST /api/auth/register is called with duplicate email
        ResponseEntity<String> response = restClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server should reject with a 4xx error (ResourceAlreadyExistsException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 3 – POST /api/auth/login  →  successfully logs in with valid credentials
    
    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Given – an existing user in the database
        PortalUser user = PortalUser.builder()
                .email("login@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.STUDENT)
                .build();
        portalUserRepository.save(user);

        // Build login payload with correct credentials
        Map<String, Object> request = new HashMap<>();
        request.put("email", "login@test.com");
        request.put("password", "password123");

        // When – POST /api/auth/login is called
        ResponseEntity<AuthResponse> response = restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(AuthResponse.class);

        // Then – response is 200 OK with token and user details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getEmail()).isEqualTo("login@test.com");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.STUDENT);

        // JWT tokens have three dot-separated parts (header.payload.signature)
        assertThat(response.getBody().getToken().split("\\.")).hasSize(3);
    }

    
    // Test 4 – POST /api/auth/login  →  fails with wrong password
    
    @Test
    void shouldFailLoginWithWrongPassword() {
        // Given – an existing user
        PortalUser user = PortalUser.builder()
                .email("login@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.STUDENT)
                .build();
        portalUserRepository.save(user);

        // Build login payload with incorrect password
        Map<String, Object> request = new HashMap<>();
        request.put("email", "login@test.com");
        request.put("password", "wrongpassword");

        // When – POST /api/auth/login is called with wrong password
        ResponseEntity<String> response = restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects with a 4xx/5xx error (InvalidCredentialsException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 5 – POST /api/auth/login  →  fails when user does not exist
    
    @Test
    void shouldFailLoginWhenUserDoesNotExist() {
        // Given – no user with this email exists in the database

        // Build login payload with a non-existent email
        Map<String, Object> request = new HashMap<>();
        request.put("email", "nonexistent@test.com");
        request.put("password", "password123");

        // When – POST /api/auth/login is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects with a 4xx/5xx error (UserNotFoundException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 6 – Full flow: register a user, then log in with the same credentials
    
    @Test
    void shouldRegisterAndThenLoginSuccessfully() {
        // ---- Step 1: Register ----
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "flow@test.com");
        registerRequest.put("password", "password123");
        registerRequest.put("role", "STUDENT");

        ResponseEntity<UserResponse> registerResponse = restClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .retrieve()
                .toEntity(UserResponse.class);

        assertThat(registerResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getEmail()).isEqualTo("flow@test.com");

        // ---- Step 2: Login with same credentials ----
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "flow@test.com");
        loginRequest.put("password", "password123");

        ResponseEntity<AuthResponse> loginResponse = restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .retrieve()
                .toEntity(AuthResponse.class);

        // Verify login succeeded and returned a valid JWT token
        assertThat(loginResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getToken()).isNotBlank();
        assertThat(loginResponse.getBody().getEmail()).isEqualTo("flow@test.com");
        assertThat(loginResponse.getBody().getRole()).isEqualTo(UserRole.STUDENT);
    }

    
    // Test 7 – POST /api/auth/register  →  fails with invalid email format
    //          (relies on @Valid + Bean Validation in RegisterRequest DTO)
    
    @Test
    void shouldFailRegistrationWithInvalidEmailFormat() {
        // Given – payload with malformed email
        Map<String, Object> request = new HashMap<>();
        request.put("email", "not-an-email");
        request.put("password", "password123");
        request.put("role", "STUDENT");

        // When – POST /api/auth/register is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – validation error returns 400 Bad Request (only if @Email is set in the DTO)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }
}