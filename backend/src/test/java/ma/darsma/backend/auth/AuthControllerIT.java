package ma.darsma.backend.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIT {

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

    @Test
    void registerThenLogin_returnsAccessAndRefreshTokens() throws Exception {
        String registerBody = """
                {"email":"yasmine@example.com","password":"supersecret1","role":"STUDENT","fullName":"Yasmine"}
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {"email":"yasmine@example.com","password":"supersecret1"}
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(not(org.hamcrest.Matchers.emptyOrNullString())))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String registerBody = """
                {"email":"karim@example.com","password":"supersecret1","role":"TUTOR","fullName":"Karim"}
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated());

        String wrongLoginBody = """
                {"email":"karim@example.com","password":"wrongpassword"}
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(wrongLoginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_rotatesTokenAndRejectsReuseOfTheOldOne() throws Exception {
        String registerBody = """
                {"email":"refresh-test@example.com","password":"supersecret1","role":"STUDENT","fullName":"Test"}
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {"email":"refresh-test@example.com","password":"supersecret1"}
                """;
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        Cookie originalRefreshCookie = loginResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(not(org.hamcrest.Matchers.emptyOrNullString())))
                .andExpect(cookie().exists("refresh_token"));

        // Reusing the now-rotated-away original refresh token must be rejected.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_asAdmin_isForbidden() throws Exception {
        String registerBody = """
                {"email":"wannabe-admin@example.com","password":"supersecret1","role":"ADMIN","fullName":"Nope"}
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isForbidden());
    }
}
