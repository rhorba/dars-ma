package ma.darsma.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.darsma.backend.profile.TutorProfileRepository;
import ma.darsma.backend.profile.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class NotificationControllerIT {

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
        return new AuthedUser(token, extractUserId(token));
    }

    private UUID extractUserId(String jwt) {
        String payload = jwt.split("\\.")[1];
        String decoded = new String(java.util.Base64.getUrlDecoder().decode(payload));
        try {
            return UUID.fromString(objectMapper.readTree(decoded).get("sub").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private void createBooking(AuthedUser student, AuthedUser tutor) throws Exception {
        String gigId = createGig(student);
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated());
    }

    @Test
    void tutor_seesBookingCreatedNotification_andCanMarkItRead() throws Exception {
        AuthedUser student = registerAndLogin("student-notif@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-notif@example.com", "100.00");
        createBooking(student, tutor);

        var listResult = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tutor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("BOOKING_CREATED"))
                .andExpect(jsonPath("$[0].readAt").doesNotExist())
                .andReturn();
        String notificationId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .get(0).get("id").asText();

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + tutor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").exists());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tutor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readAt").exists());
    }

    @Test
    void otherUser_cannotMarkSomeoneElsesNotificationRead() throws Exception {
        AuthedUser student = registerAndLogin("student-notif-idor@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-notif-idor@example.com", "100.00");
        AuthedUser outsider = registerAndLogin("student-notif-outsider@example.com", "STUDENT");
        createBooking(student, tutor);

        var listResult = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tutor.token()))
                .andExpect(status().isOk())
                .andReturn();
        String notificationId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .get(0).get("id").asText();

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void noNotificationsYet_returnsEmptyList() throws Exception {
        AuthedUser student = registerAndLogin("student-notif-empty@example.com", "STUDENT");

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
