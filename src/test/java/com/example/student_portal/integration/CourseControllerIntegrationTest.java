package com.example.student_portal.integration;

import com.example.student_portal.entity.Course;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.repository.CourseRepository;
import com.example.student_portal.repository.PortalUserRepository;
import com.example.student_portal.security.CustomUserDetails;
import com.example.student_portal.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Full integration test for CourseController using a real MySQL container and JWT authentication
class CourseControllerIntegrationTest {

    // -------------------------------------------------------------------------
    // Testcontainers: starts a real MySQL container once for all tests
    // Using mysql:8.0 to avoid breaking changes introduced in MySQL 9.x
    // -------------------------------------------------------------------------
    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0");

    // -------------------------------------------------------------------------
    // Random port assigned to embedded server - needed to build full request URLs
    // -------------------------------------------------------------------------
    @LocalServerPort
    private int port;

    // -------------------------------------------------------------------------
    // Spring-managed dependencies injected for test setup
    // -------------------------------------------------------------------------
    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Shared test state
    // -------------------------------------------------------------------------
    private Course course;
    private String studentToken;   // JWT for a STUDENT user
    private String adminToken;     // JWT for an ADMIN user
    private RestClient restClient; // HTTP client used to call REST APIs

    // =========================================================================
    // Setup & Teardown
    // =========================================================================

    @BeforeEach
    void setUp() {
        // Build a RestClient pointing at the random server port
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on 4xx/5xx */ })
                .build();

        // --- Build a reusable course object for all tests ---
        course = Course.builder()
                .courseCode("CS101")
                .title("Introduction to Computer Science")
                .description("Fundamentals of programming and computer science.")
                .fee(new BigDecimal("1500.00"))
                .build();

        // --- Create a STUDENT user and generate its JWT ---
        PortalUser studentUser = PortalUser.builder()
                .email("student@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.STUDENT)
                .build();
        portalUserRepository.save(studentUser);
        studentToken = jwtService.generateToken(new CustomUserDetails(studentUser));

        // --- Create an ADMIN user and generate its JWT ---
        PortalUser adminUser = PortalUser.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build();
        portalUserRepository.save(adminUser);
        adminToken = jwtService.generateToken(new CustomUserDetails(adminUser));
    }

    @AfterEach
    void tearDown() {
        // Clean up all data after each test to keep tests independent
        courseRepository.deleteAll();
        portalUserRepository.deleteAll();
    }

    // =========================================================================
    // Test 1 – GET /api/courses  →  returns all courses
    // =========================================================================
    @Test
    void shouldGetAllCourses() {
        // Given – one course saved in the database
        courseRepository.save(course);

        // When – authenticated student sends GET request
        ResponseEntity<Course[]> response = restClient.get()
                .uri("/api/courses")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(Course[].class);

        // Then – response is 200 OK and contains the saved course
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getCourseCode()).isEqualTo("CS101");
    }

    // =========================================================================
    // Test 2 – GET /api/courses/{id}  →  returns course by ID
    // =========================================================================
    @Test
    void shouldGetCourseById() {
        // Given – course saved; its generated ID is used for lookup
        Course saved = courseRepository.save(course);

        // When – authenticated student sends GET request with ID
        ResponseEntity<Course> response = restClient.get()
                .uri("/api/courses/" + saved.getId())
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(Course.class);

        // Then – response is 200 OK with correct course data
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCourseCode()).isEqualTo("CS101");
        assertThat(response.getBody().getTitle()).isEqualTo("Introduction to Computer Science");
        assertThat(response.getBody().getFee()).isEqualByComparingTo("1500.00");
    }

    // =========================================================================
    // Test 3 – POST /api/courses  →  creates a new course
    // =========================================================================
    @Test
    void shouldCreateCourse() {
        // When – authenticated user sends POST request with course body
        ResponseEntity<Course> response = restClient.post()
                .uri("/api/courses")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(course)
                .retrieve()
                .toEntity(Course.class);

        // Then – response is 200 OK and body reflects the created course
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getCourseCode()).isEqualTo("CS101");
        assertThat(response.getBody().getTitle()).isEqualTo("Introduction to Computer Science");

        // Verify the record is actually persisted in the database
        assertThat(courseRepository.findByCourseCode("CS101")).isPresent();
    }

    // =========================================================================
    // Test 4 – PUT /api/courses/{id}  →  updates an existing course
    // =========================================================================
    @Test
    void shouldUpdateCourse() {
        // Given – course already saved in the database
        Course saved = courseRepository.save(course);

        // Build the updated course payload
        Course updatedCourse = Course.builder()
                .courseCode("CS101")
                .title("Advanced Computer Science")
                .description("Advanced topics in programming.")
                .fee(new BigDecimal("2000.00"))
                .build();

        // When – authenticated user sends PUT request
        ResponseEntity<Course> response = restClient.put()
                .uri("/api/courses/" + saved.getId())
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatedCourse)
                .retrieve()
                .toEntity(Course.class);

        // Then – response is 200 OK with updated values
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Advanced Computer Science");
        assertThat(response.getBody().getFee()).isEqualByComparingTo("2000.00");
    }

    // =========================================================================
    // Test 5 – DELETE /api/courses/{id}  →  deletes a course
    // =========================================================================
    @Test
    void shouldDeleteCourse() {
        // Given – course already saved in the database
        Course saved = courseRepository.save(course);

        // When – authenticated user sends DELETE request
        ResponseEntity<Void> response = restClient.delete()
                .uri("/api/courses/" + saved.getId())
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toBodilessEntity();

        // Then – response is 204 No Content and course no longer exists in DB
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(courseRepository.findById(saved.getId())).isEmpty();
    }

    // =========================================================================
    // Test 6 – GET /api/courses  →  returns 401 without token
    // =========================================================================
    @Test
    void shouldReturn401WhenNoTokenProvided() {
        // When – unauthenticated request is sent (no Authorization header)
        ResponseEntity<String> response = restClient.get()
                .uri("/api/courses")
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects the request with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    // =========================================================================
    // Test 7 – GET /api/courses  →  returns 401 with invalid token
    // =========================================================================
    @Test
    void shouldReturn401WhenInvalidTokenProvided() {
        // When – request is sent with a malformed/invalid JWT token
        ResponseEntity<String> response = restClient.get()
                .uri("/api/courses")
                .header("Authorization", "Bearer this.is.not.a.valid.token")
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects the request with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    // =========================================================================
    // Test 8 – GET /api/courses/{id}  →  returns error for non-existent ID
    // =========================================================================
    @Test
    void shouldReturnErrorWhenCourseNotFound() {
        // When – authenticated student requests a non-existent course
        ResponseEntity<String> response = restClient.get()
                .uri("/api/courses/9999")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(String.class);

        // Then – service throws ResourceNotFoundException (mapped to 4xx/5xx by your exception handler)
        HttpStatusCode status = response.getStatusCode();
        assertThat(status.is4xxClientError() || status.is5xxServerError()).isTrue();
    }
}