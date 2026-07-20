package ma.darsma.backend.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class VerificationDocumentControllerIT {

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

    private String registerLoginAndCreateProfile(String email) throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "supersecret1", "role", "TUTOR", "fullName", "Test Tutor"));
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of("email", email, "password", "supersecret1"));
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("accessToken").asText();

        String profileBody = """
                {"bio":"Bio","subjects":["Math"],"hourlyRateMad":100.00}
                """;
        mockMvc.perform(put("/api/v1/profile/tutor/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(profileBody))
                .andExpect(status().isOk());
        return token;
    }

    @Test
    void tutor_canUploadValidDiploma() throws Exception {
        String token = registerLoginAndCreateProfile("upload-valid@example.com");
        MockMultipartFile file = new MockMultipartFile("file", "diploma.pdf", "application/pdf", "%PDF-1.4 diploma-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/profile/tutor/me/verification-documents")
                        .file(file)
                        .param("docType", "DIPLOMA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.docType").value("DIPLOMA"))
                .andExpect(jsonPath("$.originalFilename").value("diploma.pdf"));
    }

    @Test
    void upload_rejectsWrongFileType() throws Exception {
        String token = registerLoginAndCreateProfile("upload-wrongtype@example.com");
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", "bad".getBytes());

        mockMvc.perform(multipart("/api/v1/profile/tutor/me/verification-documents")
                        .file(file)
                        .param("docType", "DIPLOMA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_withoutAuth_isForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "diploma.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/profile/tutor/me/verification-documents")
                        .file(file)
                        .param("docType", "DIPLOMA"))
                .andExpect(status().isForbidden());
    }
}
