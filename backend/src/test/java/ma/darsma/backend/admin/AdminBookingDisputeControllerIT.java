package ma.darsma.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.darsma.backend.auth.Role;
import ma.darsma.backend.auth.User;
import ma.darsma.backend.auth.UserRepository;
import ma.darsma.backend.booking.EscrowStatus;
import ma.darsma.backend.booking.EscrowTransactionRepository;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class AdminBookingDisputeControllerIT {

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

    @Autowired
    private EscrowTransactionRepository escrowTransactionRepository;

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
                .email("admin-dispute-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("supersecret1"))
                .role(Role.ADMIN)
                .fullName("Admin")
                .build();
        admin = userRepository.save(admin);
        return jwtService.generateAccessToken(admin.getId(), Role.ADMIN);
    }

    /** Creates a gig, a booking against it (escrow held), and raises a dispute on it. */
    private String disputedBookingId(AuthedUser student, AuthedUser tutor) throws Exception {
        String gigBody = """
                {"subject":"Math","level":"High School","description":"desc"}
                """;
        var gigResult = mockMvc.perform(post("/api/v1/gigs")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(gigBody))
                .andExpect(status().isCreated())
                .andReturn();
        String gigId = objectMapper.readTree(gigResult.getResponse().getContentAsString()).get("id").asText();

        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var bookingResult = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/dispute").header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isOk());
        return bookingId;
    }

    @Test
    void admin_seesDisputedBookingInQueue() throws Exception {
        AuthedUser student = registerAndLogin("student-adm-dispute@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-adm-dispute@example.com", "100.00");
        String bookingId = disputedBookingId(student, tutor);

        mockMvc.perform(get("/api/v1/admin/bookings/disputes").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + bookingId + "')]").exists());
    }

    @Test
    void admin_resolvesWithRelease_releasesEscrowToTutor() throws Exception {
        AuthedUser student = registerAndLogin("student-adm-release@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-adm-release@example.com", "100.00");
        String bookingId = disputedBookingId(student, tutor);

        String resolveBody = """
                {"resolution":"RELEASE"}
                """;
        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/disputes/resolve")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType("application/json")
                        .content(resolveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        var escrow = escrowTransactionRepository.findByBookingId(UUID.fromString(bookingId)).orElseThrow();
        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.RELEASED);
    }

    @Test
    void admin_resolvesWithRefund_refundsEscrowToStudent() throws Exception {
        AuthedUser student = registerAndLogin("student-adm-refund@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-adm-refund@example.com", "100.00");
        String bookingId = disputedBookingId(student, tutor);

        String resolveBody = """
                {"resolution":"REFUND"}
                """;
        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/disputes/resolve")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType("application/json")
                        .content(resolveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        var escrow = escrowTransactionRepository.findByBookingId(UUID.fromString(bookingId)).orElseThrow();
        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.REFUNDED);
    }

    @Test
    void nonAdmin_cannotAccessDisputeQueueOrResolve() throws Exception {
        AuthedUser student = registerAndLogin("student-adm-forbidden@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-adm-forbidden@example.com", "100.00");
        String bookingId = disputedBookingId(student, tutor);

        mockMvc.perform(get("/api/v1/admin/bookings/disputes").header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isForbidden());

        String resolveBody = """
                {"resolution":"RELEASE"}
                """;
        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/disputes/resolve")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(resolveBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void resolve_bookingNotDisputed_returnsConflict() throws Exception {
        AuthedUser student = registerAndLogin("student-adm-notdisputed@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-adm-notdisputed@example.com", "100.00");
        String gigBody = """
                {"subject":"Math","level":"High School","description":"desc"}
                """;
        var gigResult = mockMvc.perform(post("/api/v1/gigs")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(gigBody))
                .andExpect(status().isCreated())
                .andReturn();
        String gigId = objectMapper.readTree(gigResult.getResponse().getContentAsString()).get("id").asText();
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var bookingResult = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).get("id").asText();

        String resolveBody = """
                {"resolution":"RELEASE"}
                """;
        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/disputes/resolve")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType("application/json")
                        .content(resolveBody))
                .andExpect(status().isConflict());
    }
}
