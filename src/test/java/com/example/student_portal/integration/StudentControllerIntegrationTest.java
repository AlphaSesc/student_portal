package com.example.student_portal.integration;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.client.LibraryClient;
import com.example.student_portal.dto.GraduationEligibilityResponse;
import com.example.student_portal.dto.StudentProfileResponse;
import com.example.student_portal.dto.finance.OutstandingBalanceResponse;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.repository.PortalUserRepository;
import com.example.student_portal.repository.StudentRepository;
import com.example.student_portal.security.CustomUserDetails;
import com.example.student_portal.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for StudentController covering profile read/update and graduation eligibility check.
// External Finance and Library service clients are MOCKED because those services
// are not running during tests - we only test the student-portal application logic.
class StudentControllerIntegrationTest {

    
    // Mock external service clients so the test doesn't try to reach real
    // Finance/Library microservices over HTTP. @Primary ensures these mocks
    // override the real beans during this test.
    
    @TestConfiguration
    static class MockedExternalClientsConfig {
        @Bean
        @Primary
        public FinanceClient financeClient() {
            return mock(FinanceClient.class);
        }

        @Bean
        @Primary
        public LibraryClient libraryClient() {
            return mock(LibraryClient.class);
        }
    }

    
    // Testcontainers: real MySQL container as backend database
    
    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0");

    
    // Random port assigned to embedded server
    
    @LocalServerPort
    private int port;

    
    // Spring-managed dependencies
    
    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FinanceClient financeClient;   // mock injected from TestConfiguration

    @Autowired
    private LibraryClient libraryClient;   // mock injected from TestConfiguration

    
    // Shared test state
    
    private RestClient restClient;
    private String studentToken;
    private String adminToken;
    private PortalUser studentUser;
    private Student student;

    
    // Setup & Teardown
    

    @BeforeEach
    void setUp() {
        // Reset mocks so previous test interactions don't leak in
        reset(financeClient, libraryClient);

        // Build RestClient pointing at the embedded server
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on 4xx/5xx */ })
                .build();

        // ---- Persist a STUDENT user and generate JWT ----
        studentUser = PortalUser.builder()
                .email("student@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.STUDENT)
                .build();
        portalUserRepository.save(studentUser);
        studentToken = jwtService.generateToken(new CustomUserDetails(studentUser));

        // ---- Persist an ADMIN user (used to verify role-based restriction) ----
        PortalUser adminUser = PortalUser.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build();
        portalUserRepository.save(adminUser);
        adminToken = jwtService.generateToken(new CustomUserDetails(adminUser));

        // ---- Persist a Student profile linked to the studentUser ----
        student = Student.builder()
                .studentId("STU-TEST-001")
                .portalUser(studentUser)
                .firstName("John")
                .lastName("Doe")
                .phone("9876543210")
                .address("123 Main Street")
                .build();
        student = studentRepository.save(student);
    }

    @AfterEach
    void tearDown() {
        // Clear all data in dependency-safe order to keep tests independent
        studentRepository.deleteAll();
        portalUserRepository.deleteAll();
    }

    
    // Test 1 – GET /api/students/me  →  returns current student's profile
    
    @Test
    void shouldGetCurrentStudentProfile() {
        // When – authenticated student requests their profile
        ResponseEntity<StudentProfileResponse> response = restClient.get()
                .uri("/api/students/me")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(StudentProfileResponse.class);

        // Then – response is 200 OK with the student's profile data
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-TEST-001");
        assertThat(response.getBody().getFirstName()).isEqualTo("John");
        assertThat(response.getBody().getLastName()).isEqualTo("Doe");
        assertThat(response.getBody().getPhone()).isEqualTo("9876543210");
        assertThat(response.getBody().getAddress()).isEqualTo("123 Main Street");
        assertThat(response.getBody().getEmail()).isEqualTo("student@test.com");
    }

    
    // Test 2 – GET /api/students/me  →  fails when student profile missing
    
    @Test
    void shouldFailGetProfileWhenStudentNotFound() {
        // Given – delete the student profile (only PortalUser remains)
        studentRepository.deleteAll();

        // When – authenticated user requests profile but no Student record exists
        ResponseEntity<String> response = restClient.get()
                .uri("/api/students/me")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException: "Student profile not found")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 3 – PUT /api/students/me  →  updates profile fields
    
    @Test
    void shouldUpdateCurrentStudentProfile() {
        // Given – payload with new profile values
        Map<String, Object> request = new HashMap<>();
        request.put("firstName", "Jane");
        request.put("lastName", "Smith");
        request.put("phone", "1234567890");
        request.put("address", "456 New Avenue");

        // When – authenticated student sends PUT request
        ResponseEntity<StudentProfileResponse> response = restClient.put()
                .uri("/api/students/me")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(StudentProfileResponse.class);

        // Then – response is 200 OK with updated values
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFirstName()).isEqualTo("Jane");
        assertThat(response.getBody().getLastName()).isEqualTo("Smith");
        assertThat(response.getBody().getPhone()).isEqualTo("1234567890");
        assertThat(response.getBody().getAddress()).isEqualTo("456 New Avenue");

        // Verify changes were persisted in the database
        Student updated = studentRepository.findById(student.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Jane");
        assertThat(updated.getLastName()).isEqualTo("Smith");
        assertThat(updated.getPhone()).isEqualTo("1234567890");
        assertThat(updated.getAddress()).isEqualTo("456 New Avenue");
    }

    
    // Test 4 – PUT /api/students/me  →  fails when phone format is invalid
    //          (Bean Validation @Pattern on phone field)
    
    @Test
    void shouldFailProfileUpdateWhenPhoneInvalid() {
        // Given – payload with invalid phone (non-numeric)
        Map<String, Object> request = new HashMap<>();
        request.put("firstName", "Jane");
        request.put("lastName", "Smith");
        request.put("phone", "not-a-phone");
        request.put("address", "456 New Avenue");

        // When – authenticated student sends PUT request
        ResponseEntity<String> response = restClient.put()
                .uri("/api/students/me")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – validation fails with 400 Bad Request
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    
    // Test 5 – PUT /api/students/me  →  fails when first name too short
    //          (Bean Validation @Size on firstName field)
    
    @Test
    void shouldFailProfileUpdateWhenFirstNameTooShort() {
        // Given – payload with first name shorter than 2 chars
        Map<String, Object> request = new HashMap<>();
        request.put("firstName", "J");
        request.put("lastName", "Smith");
        request.put("phone", "1234567890");
        request.put("address", "456 New Avenue");

        // When – authenticated student sends PUT request
        ResponseEntity<String> response = restClient.put()
                .uri("/api/students/me")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – validation fails with 400 Bad Request
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    
    // Test 6 – GET /api/students/me/graduation-eligibility  →  eligible (no balance)
    
    @Test
    void shouldReturnEligibleWhenNoOutstandingBalance() {
        // Given – Finance service mock returns no outstanding balance
        OutstandingBalanceResponse balance = OutstandingBalanceResponse.builder()
                .hasOutstandingBalance(false)
                .build();
        when(financeClient.checkOutstandingBalance(anyString())).thenReturn(balance);

        // When – authenticated student requests graduation eligibility
        ResponseEntity<GraduationEligibilityResponse> response = restClient.get()
                .uri("/api/students/me/graduation-eligibility")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(GraduationEligibilityResponse.class);

        // Then – response confirms eligibility
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-TEST-001");
        assertThat(response.getBody().isEligible()).isTrue();
        assertThat(response.getBody().getMessage()).contains("eligible to graduate");

        // Verify Finance service was called exactly once with correct student id
        verify(financeClient, times(1)).checkOutstandingBalance("STU-TEST-001");
    }

    
    // Test 7 – GET /api/students/me/graduation-eligibility  →  NOT eligible
    
    @Test
    void shouldReturnNotEligibleWhenOutstandingBalanceExists() {
        // Given – Finance service mock returns outstanding balance
        OutstandingBalanceResponse balance = OutstandingBalanceResponse.builder()
                .hasOutstandingBalance(true)
                .build();
        when(financeClient.checkOutstandingBalance(anyString())).thenReturn(balance);

        // When – authenticated student requests graduation eligibility
        ResponseEntity<GraduationEligibilityResponse> response = restClient.get()
                .uri("/api/students/me/graduation-eligibility")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(GraduationEligibilityResponse.class);

        // Then – response indicates NOT eligible due to outstanding invoices
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isEligible()).isFalse();
        assertThat(response.getBody().getMessage()).contains("not eligible");

        // Verify Finance service was called
        verify(financeClient, times(1)).checkOutstandingBalance("STU-TEST-001");
    }

    
    // Test 8 – Endpoints reject ADMIN role (only STUDENT can access these)
    
    @Test
    void shouldRejectAdminFromAccessingStudentEndpoints() {
        // When – ADMIN user tries to access /me profile endpoint
        ResponseEntity<String> response = restClient.get()
                .uri("/api/students/me")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UnauthorizedOperationException from getCurrentStudentUser)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 9 – Endpoints require authentication
    
    @Test
    void shouldReturn401WhenNoTokenProvided() {
        // When – unauthenticated GET is sent
        ResponseEntity<String> response = restClient.get()
                .uri("/api/students/me")
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}