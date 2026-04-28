package com.example.student_portal.integration;

import com.example.student_portal.client.FinanceClient;
import com.example.student_portal.client.LibraryClient;
import com.example.student_portal.dto.EnrollmentResponse;
import com.example.student_portal.entity.Course;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.Student;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.repository.CourseRepository;
import com.example.student_portal.repository.EnrollmentRepository;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for EnrollmentController covering enrollment creation and retrieval.
// External Finance and Library service clients are MOCKED because those services
// are not running during tests - we only test the student-portal application logic.
class EnrollmentControllerIntegrationTest {

    // -------------------------------------------------------------------------
    // Mock external service clients so the test doesn't try to reach real
    // Finance/Library microservices over HTTP. @Primary ensures these mocks
    // override the real beans during this test.
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Testcontainers: real MySQL container as backend database
    // -------------------------------------------------------------------------
    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0");

    // -------------------------------------------------------------------------
    // Random port assigned to embedded server
    // -------------------------------------------------------------------------
    @LocalServerPort
    private int port;

    // -------------------------------------------------------------------------
    // Spring-managed dependencies
    // -------------------------------------------------------------------------
    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FinanceClient financeClient;   // mock injected from TestConfiguration

    @Autowired
    private LibraryClient libraryClient;   // mock injected from TestConfiguration

    // -------------------------------------------------------------------------
    // Shared test state
    // -------------------------------------------------------------------------
    private RestClient restClient;
    private String studentToken;
    private String adminToken;
    private PortalUser studentUser;
    private Course course;

    // =========================================================================
    // Setup & Teardown
    // =========================================================================

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

        // ---- Persist a course available for enrollment ----
        course = Course.builder()
                .courseCode("CS101")
                .title("Introduction to Computer Science")
                .description("Fundamentals of programming.")
                .fee(new BigDecimal("1500.00"))
                .build();
        course = courseRepository.save(course);
    }

    @AfterEach
    void tearDown() {
        // Clear all data in dependency-safe order to keep tests independent
        enrollmentRepository.deleteAll();
        studentRepository.deleteAll();
        courseRepository.deleteAll();
        portalUserRepository.deleteAll();
    }

    // =========================================================================
    // Test 1 – POST /api/enrollments  →  successfully enrolls a student
    //          (also creates a Student profile and calls finance + library)
    // =========================================================================
    @Test
    void shouldEnrollStudentSuccessfully() {
        // Given – payload with the persisted course id
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", course.getId());

        // When – authenticated student sends POST request
        ResponseEntity<EnrollmentResponse> response = restClient.post()
                .uri("/api/enrollments")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(EnrollmentResponse.class);

        // Then – response is 200 OK with enrollment details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEnrollmentId()).isNotNull();
        assertThat(response.getBody().getCourseId()).isEqualTo(course.getId());
        assertThat(response.getBody().getCourseCode()).isEqualTo("CS101");
        assertThat(response.getBody().getCourseTitle()).isEqualTo("Introduction to Computer Science");
        assertThat(response.getBody().getStudentId()).isNotBlank();
        assertThat(response.getBody().getEnrolledAt()).isNotNull();

        // Verify enrollment is persisted in the database
        assertThat(enrollmentRepository.findAll()).hasSize(1);

        // Verify Student profile was created during first enrollment
        assertThat(studentRepository.findByPortalUser(studentUser)).isPresent();

        // Verify external services were called exactly once
        verify(financeClient, times(1)).createAccount(any());   // for new student
        verify(libraryClient, times(1)).registerStudent(any()); // for new student
        verify(financeClient, times(1)).createInvoice(any());   // for the enrollment
    }

    // =========================================================================
    // Test 2 – POST /api/enrollments  →  fails when course doesn't exist
    // =========================================================================
    @Test
    void shouldFailEnrollmentWhenCourseNotFound() {
        // Given – payload with a non-existent course id
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", 9999L);

        // When – authenticated student sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/enrollments")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects with a 4xx/5xx error (ResourceNotFoundException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify no external calls were made
        verify(financeClient, never()).createInvoice(any());
        verify(libraryClient, never()).registerStudent(any());
    }

    // =========================================================================
    // Test 3 – POST /api/enrollments  →  fails on duplicate enrollment
    // =========================================================================
    @Test
    void shouldFailWhenStudentAlreadyEnrolled() {
        // Given – student is already enrolled in this course
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", course.getId());

        // First enrollment succeeds
        restClient.post()
                .uri("/api/enrollments")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(EnrollmentResponse.class);

        // When – student tries to enroll in the same course again
        ResponseEntity<String> response = restClient.post()
                .uri("/api/enrollments")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceAlreadyExistsException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify only one enrollment exists in the database
        assertThat(enrollmentRepository.findAll()).hasSize(1);
    }

    // =========================================================================
    // Test 4 – POST /api/enrollments  →  forbidden for ADMIN role
    //          (only STUDENT users can enroll - enforced by AuthenticatedUserService)
    // =========================================================================
    @Test
    void shouldFailEnrollmentWhenUserIsNotStudent() {
        // Given – payload with a valid course id
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", course.getId());

        // When – ADMIN user tries to enroll
        ResponseEntity<String> response = restClient.post()
                .uri("/api/enrollments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UnauthorizedOperationException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    // =========================================================================
    // Test 5 – POST /api/enrollments  →  fails validation when courseId missing
    // =========================================================================
    @Test
    void shouldFailEnrollmentWhenCourseIdMissing() {
        // Given – empty payload (no courseId)
        Map<String, Object> request = new HashMap<>();

        // When – authenticated student sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/enrollments")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    // =========================================================================
    // Test 6 – GET /api/enrollments/me  →  returns empty list for new student
    // =========================================================================
    @Test
    void shouldReturnEmptyListWhenStudentHasNoEnrollments() {
        // Given – student profile exists but no enrollments
        Student student = Student.builder()
                .studentId("STU-TEST-001")
                .portalUser(studentUser)
                .build();
        studentRepository.save(student);

        // When – authenticated student requests their enrollments
        ResponseEntity<EnrollmentResponse[]> response = restClient.get()
                .uri("/api/enrollments/me")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(EnrollmentResponse[].class);

        // Then – response is 200 OK with empty array
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    // =========================================================================
    // Test 7 – GET /api/enrollments/me  →  returns enrollments after enrolling
    // =========================================================================
    @Test
    void shouldReturnEnrollmentsForCurrentStudent() {
        // Given – student enrolls in a course first
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", course.getId());

        restClient.post()
                .uri("/api/enrollments")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(EnrollmentResponse.class);

        // When – authenticated student requests their enrollments
        ResponseEntity<EnrollmentResponse[]> response = restClient.get()
                .uri("/api/enrollments/me")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(EnrollmentResponse[].class);

        // Then – response contains the enrollment we just created
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(1);
        assertThat(response.getBody()[0].getCourseCode()).isEqualTo("CS101");
        assertThat(response.getBody()[0].getCourseTitle()).isEqualTo("Introduction to Computer Science");
    }

    // =========================================================================
    // Test 8 – GET /api/enrollments/me  →  fails when student profile missing
    // =========================================================================
    @Test
    void shouldFailGetEnrollmentsWhenStudentProfileNotFound() {
        // Given – studentUser exists but no Student profile created yet

        // When – student requests their enrollments before any enrollment
        ResponseEntity<String> response = restClient.get()
                .uri("/api/enrollments/me")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException: "Student profile not found")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    // =========================================================================
    // Test 9 – Endpoints require authentication
    // =========================================================================
    @Test
    void shouldReturn401WhenNoTokenProvidedOnEnroll() {
        // Given – payload with valid course id but no Authorization header
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", course.getId());

        // When – unauthenticated POST is sent
        ResponseEntity<String> response = restClient.post()
                .uri("/api/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}