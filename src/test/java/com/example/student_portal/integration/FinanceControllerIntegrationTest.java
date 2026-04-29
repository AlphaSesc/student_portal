package com.example.student_portal.integration;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.client.LibraryClient;
import com.example.student_portal.dto.finance.InvoiceType;
import com.example.student_portal.dto.finance.PayInvoiceRequest;
import com.example.student_portal.dto.finance.PayInvoiceResponse;
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
import org.mockito.ArgumentCaptor;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for FinanceController covering invoice payment and history retrieval.
// External Finance and Library service clients are MOCKED because those services
// are not running during tests - we only test the student-portal application logic.
class FinanceControllerIntegrationTest {

    
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
                .build();
        student = studentRepository.save(student);
    }

    @AfterEach
    void tearDown() {
        // Clear all data in dependency-safe order to keep tests independent
        studentRepository.deleteAll();
        portalUserRepository.deleteAll();
    }

    
    // Helper: builds a sample PayInvoiceResponse to be returned from mock
    
    private PayInvoiceResponse sampleInvoice(String reference) {
        return PayInvoiceResponse.builder()
                .id(1L)
                .studentId("STU-TEST-001")
                .courseCode("CS101")
                .invoiceType(InvoiceType.COURSE_ENROLLMENT)
                .amount(new BigDecimal("1500.00"))
                .reference(reference)
                .createdAt(LocalDateTime.now())
                .build();
    }

    
    // Test 1 – POST /api/finance/pay  →  successfully pays an invoice
    
    @Test
    void shouldPayInvoiceSuccessfully() {
        // Given – Finance service mock returns a paid invoice response
        PayInvoiceResponse mockResponse = sampleInvoice("INV-12345");
        when(financeClient.payInvoice(any(PayInvoiceRequest.class))).thenReturn(mockResponse);

        // Build payment request payload (only reference is needed from the client)
        Map<String, Object> request = new HashMap<>();
        request.put("reference", "INV-12345");

        // When – authenticated student sends POST request
        ResponseEntity<PayInvoiceResponse> response = restClient.post()
                .uri("/api/finance/pay")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(PayInvoiceResponse.class);

        // Then – response is 200 OK with invoice details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-TEST-001");
        assertThat(response.getBody().getReference()).isEqualTo("INV-12345");
        assertThat(response.getBody().getCourseCode()).isEqualTo("CS101");
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("1500.00");

        // Verify the FinanceClient was called with the correct studentId and reference
        ArgumentCaptor<PayInvoiceRequest> captor = ArgumentCaptor.forClass(PayInvoiceRequest.class);
        verify(financeClient, times(1)).payInvoice(captor.capture());
        PayInvoiceRequest sent = captor.getValue();
        assertThat(sent.getStudentId()).isEqualTo("STU-TEST-001");
        assertThat(sent.getReference()).isEqualTo("INV-12345");
    }

    
    // Test 2 – POST /api/finance/pay  →  fails when student profile missing
    
    @Test
    void shouldFailPaymentWhenStudentProfileNotFound() {
        // Given – delete the student profile (only PortalUser remains)
        studentRepository.deleteAll();

        Map<String, Object> request = new HashMap<>();
        request.put("reference", "INV-12345");

        // When – authenticated user (without student profile) sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/finance/pay")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UserNotFoundException: "Student profile not found")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify Finance service was never called
        verify(financeClient, never()).payInvoice(any());
    }

    
    // Test 3 – POST /api/finance/pay  →  fails when ADMIN tries to pay
    
    @Test
    void shouldRejectAdminFromPayingInvoice() {
        // Given – ADMIN user attempting payment
        Map<String, Object> request = new HashMap<>();
        request.put("reference", "INV-12345");

        // When – ADMIN tries to pay invoice
        ResponseEntity<String> response = restClient.post()
                .uri("/api/finance/pay")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UnauthorizedOperationException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify Finance service was never called
        verify(financeClient, never()).payInvoice(any());
    }

    
    // Test 4 – GET /api/finance/my-invoices  →  returns student's invoice list
    
    @Test
    void shouldGetMyInvoicesSuccessfully() {
        // Given – Finance service mock returns multiple invoices
        PayInvoiceResponse[] mockInvoices = new PayInvoiceResponse[]{
                sampleInvoice("INV-001"),
                sampleInvoice("INV-002")
        };
        when(financeClient.getInvoicesByStudentId(anyString())).thenReturn(mockInvoices);

        // When – authenticated student requests their invoices
        ResponseEntity<PayInvoiceResponse[]> response = restClient.get()
                .uri("/api/finance/my-invoices")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(PayInvoiceResponse[].class);

        // Then – response is 200 OK with both invoices
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()[0].getReference()).isEqualTo("INV-001");
        assertThat(response.getBody()[1].getReference()).isEqualTo("INV-002");

        // Verify Finance service was called with the correct student id
        verify(financeClient, times(1)).getInvoicesByStudentId("STU-TEST-001");
    }

    
    // Test 5 – GET /api/finance/my-invoices  →  returns empty list safely
    //          (FinanceService converts null/empty array to empty List)
    
    @Test
    void shouldReturnEmptyListWhenNoInvoicesExist() {
        // Given – Finance service mock returns null (no invoices)
        when(financeClient.getInvoicesByStudentId(anyString())).thenReturn(null);

        // When – authenticated student requests their invoices
        ResponseEntity<PayInvoiceResponse[]> response = restClient.get()
                .uri("/api/finance/my-invoices")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(PayInvoiceResponse[].class);

        // Then – response is 200 OK with empty array (service handles null safely)
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    
    // Test 6 – GET /api/finance/my-invoices  →  fails when student profile missing
    
    @Test
    void shouldFailGetInvoicesWhenStudentProfileNotFound() {
        // Given – delete the student profile
        studentRepository.deleteAll();

        // When – authenticated user requests invoices but no Student record exists
        ResponseEntity<String> response = restClient.get()
                .uri("/api/finance/my-invoices")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UserNotFoundException: "Student profile not found")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify Finance service was never called
        verify(financeClient, never()).getInvoicesByStudentId(anyString());
    }

    
    // Test 7 – GET /api/finance/my-invoices  →  rejects ADMIN role
    
    @Test
    void shouldRejectAdminFromGettingInvoices() {
        // When – ADMIN tries to access student's invoice history
        ResponseEntity<String> response = restClient.get()
                .uri("/api/finance/my-invoices")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UnauthorizedOperationException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify Finance service was never called
        verify(financeClient, never()).getInvoicesByStudentId(anyString());
    }

    
    // Test 8 – Endpoints require authentication
    
    @Test
    void shouldReturn401WhenNoTokenProvidedOnGetInvoices() {
        // When – unauthenticated GET is sent
        ResponseEntity<String> response = restClient.get()
                .uri("/api/finance/my-invoices")
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void shouldReturn401WhenNoTokenProvidedOnPay() {
        // Given – payment payload without Authorization header
        Map<String, Object> request = new HashMap<>();
        request.put("reference", "INV-12345");

        // When – unauthenticated POST is sent
        ResponseEntity<String> response = restClient.post()
                .uri("/api/finance/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}