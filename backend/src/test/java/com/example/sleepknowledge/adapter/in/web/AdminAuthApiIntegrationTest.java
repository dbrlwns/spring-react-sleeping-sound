package com.example.sleepknowledge.adapter.in.web;

import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.admin-usernames=admin-reader",
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-admin-auth-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AdminAuthApiIntegrationTest {

    @MockitoBean
    private TextToSpeechClient textToSpeechClient;

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    AdminAuthApiIntegrationTest(
            MockMvc mockMvc,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder
    ) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void setUpAccounts() {
        jdbcTemplate.update("delete from user_accounts");
        insertAccount("admin-reader");
        insertAccount("Admin-Reader");
    }

    @Test
    void 설정에_정확히_일치하는_기존_계정만_ADMIN으로_로그인한다() throws Exception {
        MvcResult login = login("admin-reader");
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mockMvc.perform(get("/api/v1/admin/narration/voices").session(session))
                .andExpect(status().isOk());

        MvcResult caseDifferent = login("Admin-Reader");
        MockHttpSession userSession = (MockHttpSession) caseDifferent.getRequest().getSession(false);
        mockMvc.perform(get("/api/v1/auth/session").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
        mockMvc.perform(get("/api/v1/admin/narration/voices").session(userSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowlist_관리자_이름은_신규_가입으로_선점할_수_없다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin-reader","password":"correct-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "관리자 사용자 이름은 먼저 일반 계정으로 가입한 뒤 서버 설정에서 지정해야 합니다."
                ));
    }

    private MvcResult login(String username) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"correct-password"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(
                        username.equals("admin-reader") ? "ADMIN" : "USER"
                ))
                .andReturn();
    }

    private void insertAccount(String username) {
        jdbcTemplate.update(
                "insert into user_accounts (id, username, password_hash, created_at) values (?, ?, ?, ?)",
                UUID.randomUUID(),
                username,
                passwordEncoder.encode("correct-password"),
                Timestamp.from(Instant.parse("2026-08-24T00:00:00Z"))
        );
    }
}
