package ma.darsma.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.darsma.backend.auth.Role;
import ma.darsma.backend.auth.User;
import ma.darsma.backend.auth.UserRepository;
import ma.darsma.backend.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class AdminVerificationControllerIT {

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

    private String adminToken() {
        User admin = User.builder()
                .email("admin-" + java.util.UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("supersecret1"))
                .role(Role.ADMIN)
                .fullName("Admin")
                .build();
        admin = userRepository.save(admin);
        return jwtService.generateAccessToken(admin.getId(), Role.ADMIN);
    }

    private record TutorContext(String tutorToken, String documentId) {}

    private TutorContext createTutorWithPendingDocument(String email) throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "supersecret1", "role", "TUTOR", "fullName", "Test Tutor"));
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of("email", email, "password", "supersecret1"));
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tutorToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(put("/api/v1/profile/tutor/me")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType("application/json")
                        .content("""
                                {"bio":"Bio","subjects":["Math"],"hourlyRateMad":100.00}
                                """))
                .andExpect(status().isOk());

        MockMultipartFile file = new MockMultipartFile("file", "diploma.pdf", "application/pdf", "%PDF-1.4 content".getBytes());
        String uploadResponse = mockMvc.perform(multipart("/api/v1/profile/tutor/me/verification-documents")
                        .file(file)
                        .param("docType", "DIPLOMA")
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(uploadResponse).get("id").asText();

        return new TutorContext(tutorToken, documentId);
    }

    @Test
    void admin_canApprovePendingDocument() throws Exception {
        TutorContext ctx = createTutorWithPendingDocument("queue-approve@example.com");
        String adminToken = adminToken();

        mockMvc.perform(get("/api/v1/admin/verification/queue").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.documentId=='" + ctx.documentId() + "')]").exists());

        mockMvc.perform(post("/api/v1/admin/verification/documents/" + ctx.documentId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/verification/queue").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.documentId=='" + ctx.documentId() + "')]").doesNotExist());
    }

    @Test
    void admin_canRejectWithReason() throws Exception {
        TutorContext ctx = createTutorWithPendingDocument("queue-reject@example.com");
        String adminToken = adminToken();

        mockMvc.perform(post("/api/v1/admin/verification/documents/" + ctx.documentId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {"reason":"Image is blurry"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void admin_canFetchDecryptedDocumentContent() throws Exception {
        TutorContext ctx = createTutorWithPendingDocument("queue-content@example.com");
        String adminToken = adminToken();

        mockMvc.perform(get("/api/v1/admin/verification/documents/" + ctx.documentId() + "/content")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("%PDF-1.4 content".getBytes()));
    }

    @Test
    void nonAdmin_cannotAccessQueue() throws Exception {
        TutorContext ctx = createTutorWithPendingDocument("queue-forbidden@example.com");

        mockMvc.perform(get("/api/v1/admin/verification/queue").header("Authorization", "Bearer " + ctx.tutorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_isRejectedForAlreadyReviewedDocument() throws Exception {
        TutorContext ctx = createTutorWithPendingDocument("queue-double-approve@example.com");
        String adminToken = adminToken();

        mockMvc.perform(post("/api/v1/admin/verification/documents/" + ctx.documentId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/admin/verification/documents/" + ctx.documentId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }
}
