package ma.darsma.backend.gig;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.darsma.backend.matching.GigEmbeddingRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class GigRequestControllerIT {

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
    private GigEmbeddingRepository gigEmbeddingRepository;

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
    void student_canCreateAndFetchOwnGigRequest() throws Exception {
        String token = registerAndLogin("student-gig@example.com", "STUDENT");

        String body = """
                {"subject":"Math","level":"High School","description":"Need help with calculus","budgetMinMad":100.00,"budgetMaxMad":200.00}
                """;
        var createResult = mockMvc.perform(post("/api/v1/gigs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        String gigId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/gigs/" + gigId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Math"));

        var embedding = gigEmbeddingRepository.findById(UUID.fromString(gigId)).orElseThrow();
        assertThat(embedding.getEmbedding()).hasSize(384);
    }

    @Test
    void create_rejectsInvalidInputWithValidationError() throws Exception {
        String token = registerAndLogin("student-gig-invalid@example.com", "STUDENT");
        String body = """
                {"subject":"","level":"High School","description":""}
                """;
        mockMvc.perform(post("/api/v1/gigs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejectsBudgetMinGreaterThanMax() throws Exception {
        String token = registerAndLogin("student-gig-budget@example.com", "STUDENT");
        String body = """
                {"subject":"Math","level":"High School","description":"desc","budgetMinMad":300.00,"budgetMaxMad":200.00}
                """;
        mockMvc.perform(post("/api/v1/gigs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tutor_cannotCreateGigRequest() throws Exception {
        String token = registerAndLogin("tutor-gig@example.com", "TUTOR");
        String body = """
                {"subject":"Math","level":"High School","description":"desc"}
                """;
        mockMvc.perform(post("/api/v1/gigs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_cannotCreateGigRequest() throws Exception {
        String body = """
                {"subject":"Math","level":"High School","description":"desc"}
                """;
        mockMvc.perform(post("/api/v1/gigs").contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwner_cannotFetchAnotherStudentsGigRequest() throws Exception {
        String ownerToken = registerAndLogin("student-gig-owner@example.com", "STUDENT");
        String otherToken = registerAndLogin("student-gig-other@example.com", "STUDENT");
        String body = """
                {"subject":"Math","level":"High School","description":"desc"}
                """;
        var createResult = mockMvc.perform(post("/api/v1/gigs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String gigId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/gigs/" + gigId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOwn_returns404ForUnknownGig() throws Exception {
        String token = registerAndLogin("student-gig-404@example.com", "STUDENT");
        mockMvc.perform(get("/api/v1/gigs/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
