package ma.darsma.backend.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.darsma.backend.gig.GigRequestRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class BookingControllerIT {

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

    @Autowired
    private GigRequestRepository gigRequestRepository;

    @Autowired
    private BookingRepository bookingRepository;

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

    @Test
    void student_canBookVerifiedTutor_escrowHeldAndGigMarkedMatched() throws Exception {
        AuthedUser student = registerAndLogin("student-book@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-book@example.com", "100.00");
        String gigId = createGig(student);

        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 2));
        var result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ESCROW_HELD"))
                .andExpect(jsonPath("$.agreedPriceMad").value(200.00))
                .andReturn();
        String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/gigs/" + gigId).header("Authorization", "Bearer " + student.token()))
                .andExpect(jsonPath("$.status").value("MATCHED"));

        var escrow = escrowTransactionRepository.findByBookingId(UUID.fromString(bookingId)).orElseThrow();
        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.HELD);
    }

    @Test
    void student_cannotBookUnverifiedTutor() throws Exception {
        AuthedUser student = registerAndLogin("student-unverified@example.com", "STUDENT");
        AuthedUser tutor = registerAndLogin("tutor-unverified@example.com", "TUTOR");
        String profileBody = """
                {"bio":"Bio","subjects":["Math"],"hourlyRateMad":100.00}
                """;
        mockMvc.perform(put("/api/v1/profile/tutor/me")
                        .header("Authorization", "Bearer " + tutor.token())
                        .contentType("application/json")
                        .content(profileBody))
                .andExpect(status().isOk());
        String gigId = createGig(student);

        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void tutor_cannotCreateBooking() throws Exception {
        AuthedUser tutor = verifiedTutor("tutor-noncreate@example.com", "100.00");
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", UUID.randomUUID().toString(), "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tutor.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerStudent_cannotBookAgainstAnotherStudentsGig() throws Exception {
        AuthedUser owner = registerAndLogin("student-idor-owner@example.com", "STUDENT");
        AuthedUser other = registerAndLogin("student-idor-other@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-idor@example.com", "100.00");
        String gigId = createGig(owner);

        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + other.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateBooking_forSameGig_returnsConflict() throws Exception {
        AuthedUser student = registerAndLogin("student-dup@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-dup@example.com", "100.00");
        String gigId = createGig(student);
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isConflict());
    }

    @Test
    void paymentFailure_leavesPendingPaymentAndNoEscrowRow() throws Exception {
        AuthedUser student = registerAndLogin("student-payfail@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-payfail@example.com",
                MockEscrowPaymentProvider.FAILURE_TRIGGER_AMOUNT_MAD.toPlainString());
        String gigId = createGig(student);

        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn();
        String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        assertThat(escrowTransactionRepository.findByBookingId(UUID.fromString(bookingId))).isEmpty();
        mockMvc.perform(get("/api/v1/gigs/" + gigId).header("Authorization", "Bearer " + student.token()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void nonParty_cannotGetOrCompleteBooking() throws Exception {
        AuthedUser student = registerAndLogin("student-nonparty@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-nonparty@example.com", "100.00");
        AuthedUser outsider = registerAndLogin("student-outsider@example.com", "STUDENT");
        String gigId = createGig(student);
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/bookings/" + bookingId).header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/complete").header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void booking_notFound_returns404() throws Exception {
        AuthedUser student = registerAndLogin("student-404@example.com", "STUDENT");
        mockMvc.perform(get("/api/v1/bookings/" + UUID.randomUUID()).header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void completion_onlyOnePartyThenBoth_transitionsToCompletedAndReleasesEscrow() throws Exception {
        AuthedUser student = registerAndLogin("student-complete@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-complete@example.com", "100.00");
        String gigId = createGig(student);
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/complete").header("Authorization", "Bearer " + student.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCROW_HELD"));

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/complete").header("Authorization", "Bearer " + tutor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        var escrow = escrowTransactionRepository.findByBookingId(UUID.fromString(bookingId)).orElseThrow();
        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.RELEASED);
    }

    @Test
    void concurrentCompletion_byBothParties_transitionsExactlyOnceToCompleted() throws Exception {
        AuthedUser student = registerAndLogin("student-race@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-race@example.com", "100.00");
        String gigId = createGig(student);
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn();
        UUID bookingId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            var studentCall = executor.submit(() -> {
                ready.countDown();
                go.await();
                return mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/complete")
                        .header("Authorization", "Bearer " + student.token())).andReturn().getResponse().getStatus();
            });
            var tutorCall = executor.submit(() -> {
                ready.countDown();
                go.await();
                return mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/complete")
                        .header("Authorization", "Bearer " + tutor.token())).andReturn().getResponse().getStatus();
            });
            ready.await();
            go.countDown();

            assertThat(studentCall.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(tutorCall.get(10, TimeUnit.SECONDS)).isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(finalBooking.getStudentConfirmedAt()).isNotNull();
        assertThat(finalBooking.getTutorConfirmedAt()).isNotNull();

        List<EscrowTransaction> escrowRows = escrowTransactionRepository.findAll().stream()
                .filter(tx -> tx.getBookingId().equals(bookingId)).toList();
        assertThat(escrowRows).hasSize(1);
        assertThat(escrowRows.get(0).getStatus()).isEqualTo(EscrowStatus.RELEASED);
    }

    @Test
    void party_canRaiseDispute_transitionsBookingToDisputed() throws Exception {
        AuthedUser student = registerAndLogin("student-dispute@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-dispute@example.com", "100.00");
        String gigId = createGig(student);
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/dispute").header("Authorization", "Bearer " + tutor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPUTED"));
    }

    @Test
    void nonParty_cannotRaiseDispute() throws Exception {
        AuthedUser student = registerAndLogin("student-dispute-idor@example.com", "STUDENT");
        AuthedUser tutor = verifiedTutor("tutor-dispute-idor@example.com", "100.00");
        AuthedUser outsider = registerAndLogin("student-dispute-outsider@example.com", "STUDENT");
        String gigId = createGig(student);
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "gigRequestId", gigId, "tutorUserId", tutor.userId().toString(), "durationHours", 1));
        var result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType("application/json")
                        .content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/dispute").header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }
}
