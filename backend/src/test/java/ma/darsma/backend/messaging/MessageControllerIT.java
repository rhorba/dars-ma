package ma.darsma.backend.messaging;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class MessageControllerIT {

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

    @Test
    void parties_canExchangeMessagesOnABooking() throws Exception {
        AuthedUser student = registerAndLogin("student-msg@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-msg@example.com", "100.00");
        String bookingId = createBooking(student, tutor);

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/messages")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("body", "Hi, see you Monday"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Hi, see you Monday"));

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/messages")
                        .header("Authorization", "Bearer " + tutor.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("body", "Sounds good"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bookings/" + bookingId + "/messages")
                        .header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].body").value("Hi, see you Monday"))
                .andExpect(jsonPath("$[1].body").value("Sounds good"));
    }

    @Test
    void nonParty_cannotSendOrListMessages() throws Exception {
        AuthedUser student = registerAndLogin("student-msg-idor@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-msg-idor@example.com", "100.00");
        AuthedUser outsider = registerAndLogin("student-msg-outsider@example.com", "STUDENT");
        String bookingId = createBooking(student, tutor);

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/messages")
                        .header("Authorization", "Bearer " + outsider.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("body", "Hi"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/bookings/" + bookingId + "/messages")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyBody_isRejected() throws Exception {
        AuthedUser student = registerAndLogin("student-msg-empty@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-msg-empty@example.com", "100.00");
        String bookingId = createBooking(student, tutor);

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/messages")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("body", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noMessagesYet_returnsEmptyList() throws Exception {
        AuthedUser student = registerAndLogin("student-msg-empty2@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-msg-empty2@example.com", "100.00");
        String bookingId = createBooking(student, tutor);

        mockMvc.perform(get("/api/v1/bookings/" + bookingId + "/messages")
                        .header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
