package ma.darsma.backend.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.darsma.backend.matching.TutorEmbeddingRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class TutorProfileControllerIT {

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
    private TutorEmbeddingRepository tutorEmbeddingRepository;

    private String registerAndLogin(String email, String role) throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "supersecret1", "role", role, "fullName", "Test User"));
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of("email", email, "password", "supersecret1"));
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void tutor_canCreateAndFetchOwnProfile() throws Exception {
        String token = registerAndLogin("tutor-profile@example.com", "TUTOR");

        String profileBody = """
                {"bio":"Experienced math tutor","subjects":["Math","Physics"],"hourlyRateMad":150.00}
                """;
        mockMvc.perform(put("/api/v1/profile/tutor/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(profileBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Experienced math tutor"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/profile/tutor/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects[0]").value("Math"));

        var response = mockMvc.perform(get("/api/v1/profile/tutor/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        UUID tutorUserId = UUID.fromString(objectMapper.readTree(response).get("userId").asText());
        var embedding = tutorEmbeddingRepository.findById(tutorUserId).orElseThrow();
        assertThat(embedding.getEmbedding()).hasSize(384);
    }

    @Test
    void publicVisitor_canViewTutorVerificationStatusAndRating_withoutAuth() throws Exception {
        String token = registerAndLogin("tutor-public@example.com", "TUTOR");
        String profileBody = """
                {"bio":"Bio","subjects":["Chemistry"],"hourlyRateMad":100.00}
                """;
        var putResult = mockMvc.perform(put("/api/v1/profile/tutor/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(profileBody))
                .andExpect(status().isOk())
                .andReturn();
        String userId = objectMapper.readTree(putResult.getResponse().getContentAsString()).get("userId").asText();

        mockMvc.perform(get("/api/v1/profile/tutor/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));
    }

    @Test
    void student_cannotCreateTutorProfile() throws Exception {
        String token = registerAndLogin("student-profile@example.com", "STUDENT");
        String profileBody = """
                {"bio":"Bio","subjects":["Math"],"hourlyRateMad":100.00}
                """;
        mockMvc.perform(put("/api/v1/profile/tutor/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(profileBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_cannotCreateTutorProfile() throws Exception {
        String profileBody = """
                {"bio":"Bio","subjects":["Math"],"hourlyRateMad":100.00}
                """;
        mockMvc.perform(put("/api/v1/profile/tutor/me")
                        .contentType("application/json")
                        .content(profileBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicView_returns404ForUnknownTutor() throws Exception {
        mockMvc.perform(get("/api/v1/profile/tutor/" + java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
