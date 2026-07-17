package ma.darsma.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import ma.darsma.backend.auth.Role;
import ma.darsma.backend.auth.User;
import ma.darsma.backend.auth.UserRepository;
import ma.darsma.backend.profile.TutorProfileRepository;
import ma.darsma.backend.profile.VerificationStatus;
import ma.darsma.backend.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class AdminAnalyticsControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("dars_ma_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    private record AuthedUser(String token, UUID userId) {}

    private AuthedUser registerAndLogin(String email, String role) throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "supersecret1", "role", role, "fullName", "Test User"));
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of("email", email, "password", "supersecret1"));
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("accessToken").asText();
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        UUID userId = UUID.fromString(objectMapper.readTree(payload).get("sub").asText());
        return new AuthedUser(token, userId);
    }

    private AuthedUser verifiedTutor(String email, String hourlyRateMad) throws Exception {
        AuthedUser tutor = registerAndLogin(email, "TUTOR");
        String profileBody = "{\"bio\":\"Bio\",\"subjects\":[\"Math\"],\"hourlyRateMad\":" + hourlyRateMad + "}";
        mockMvc.perform(put("/api/v1/profile/tutor/me")
                        .header("Authorization", "Bearer " + tutor.token())
                        .contentType("application/json")
                        .content(profileBody))
                .andExpect(status().isOk());
        var profile = tutorProfileRepository.findById(tutor.userId()).orElseThrow();
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        tutorProfileRepository.save(profile);
        return tutor;
    }

    private String adminToken() {
        User admin = User.builder()
                .email("admin-analytics-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("supersecret1"))
                .role(Role.ADMIN)
                .fullName("Admin")
                .build();
        admin = userRepository.save(admin);
        return jwtService.generateAccessToken(admin.getId(), Role.ADMIN);
    }

    private String createGig(AuthedUser student) throws Exception {
        String body = """
                {"subject":"Math","level":"High School","description":"Need help with calculus"}
                """;
        var result = mockMvc.perform(post("/api/v1/gigs")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createBooking(AuthedUser student, AuthedUser tutor) throws Exception {
        String gigId = createGig(student);
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private JsonNode fetchAnalytics(String adminToken) throws Exception {
        var result = mockMvc.perform(get("/api/v1/admin/analytics").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void admin_seesIncrementedSignupAndBookingCounts() throws Exception {
        String admin = adminToken();
        JsonNode before = fetchAnalytics(admin);

        AuthedUser student = registerAndLogin("student-analytics-1@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-analytics-1@example.com", "100.00");
        createBooking(student, tutor);

        JsonNode after = fetchAnalytics(admin);

        assertThat(after.get("studentSignups").asLong() - before.get("studentSignups").asLong()).isEqualTo(1);
        assertThat(after.get("tutorSignups").asLong() - before.get("tutorSignups").asLong()).isEqualTo(1);
        assertThat(after.get("totalBookings").asLong() - before.get("totalBookings").asLong()).isEqualTo(1);
        assertThat(after.get("completedBookings").asLong()).isEqualTo(before.get("completedBookings").asLong());
        assertThat(after.get("gmvMad").decimalValue()).isEqualByComparingTo(before.get("gmvMad").decimalValue());
    }

    @Test
    void admin_reflectsGmvAndCompletedCountAfterMutualCompletion() throws Exception {
        String admin = adminToken();
        JsonNode before = fetchAnalytics(admin);

        AuthedUser student = registerAndLogin("student-analytics-2@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-analytics-2@example.com", "100.00");
        String bookingId = createBooking(student, tutor);

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/complete").header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/complete").header("Authorization", "Bearer " + tutor.token()))
                .andExpect(status().isOk());

        JsonNode after = fetchAnalytics(admin);

        assertThat(after.get("completedBookings").asLong() - before.get("completedBookings").asLong()).isEqualTo(1);
        BigDecimal gmvDelta = after.get("gmvMad").decimalValue().subtract(before.get("gmvMad").decimalValue());
        assertThat(gmvDelta).isEqualByComparingTo("100.00");
    }

    @Test
    void nonAdmin_cannotAccessAnalytics() throws Exception {
        AuthedUser student = registerAndLogin("student-analytics-forbidden@example.com", "STUDENT");

        mockMvc.perform(get("/api/v1/admin/analytics").header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isForbidden());
    }
}
