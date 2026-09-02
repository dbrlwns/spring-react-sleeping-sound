package com.example.sleepknowledge.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-auth-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

    @MockitoBean
    private TextToSpeechClient textToSpeechClient;

    private final MockMvc mockMvc;
    private final FilterChainProxy springSecurityFilterChain;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    AuthApiIntegrationTest(
            MockMvc mockMvc,
            FilterChainProxy springSecurityFilterChain,
            JdbcTemplate jdbcTemplate
    ) {
        this.mockMvc = mockMvc;
        this.springSecurityFilterChain = springSecurityFilterChain;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void filter_chain은_cookie_csrf_repository를_사용한다() {
        CsrfFilter csrfFilter = springSecurityFilterChain.getFilters("/api/v1/auth/csrf").stream()
                .filter(CsrfFilter.class::isInstance)
                .map(CsrfFilter.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(ReflectionTestUtils.getField(csrfFilter, "tokenRepository"))
                .isInstanceOf(CookieCsrfTokenRepository.class);
    }

    @Test
    void csrf_token을_json과_cookie로_발급한다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void 익명_session은_로그아웃_상태를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.username").value(nullValue()));
    }

    @Test
    void 공개하지_않은_api는_인증이_없으면_안전한_problem_detail을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/narration/voices"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("인증이 필요합니다."));
    }

    @Test
    void 실제_csrf_token으로_가입하고_세션을_유지한다() throws Exception {
        CsrfExchange csrfExchange = issueCsrfToken();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(csrfExchange.cookie())
                        .header(csrfExchange.headerName(), csrfExchange.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("quiet-reader", "correct-password")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("quiet-reader"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0))
                .andReturn();

        MockHttpSession session = (MockHttpSession) registerResult.getRequest().getSession(false);
        String storedPassword = jdbcTemplate.queryForObject(
                "select password_hash from user_accounts where username = ?",
                String.class,
                "quiet-reader"
        );
        assertThat(storedPassword)
                .startsWith("$2")
                .doesNotContain("correct-password");

        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("quiet-reader"));

        mockMvc.perform(get("/api/v1/contents").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void 등록된_사용자는_json_login으로_새_session을_만든다() throws Exception {
        register("returning-reader", "correct-password");
        MockHttpSession preAuthenticationSession = new MockHttpSession();
        String previousSessionId = preAuthenticationSession.getId();
        CsrfExchange csrfExchange = issueCsrfToken(preAuthenticationSession);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .session(preAuthenticationSession)
                        .cookie(csrfExchange.cookie())
                        .header(csrfExchange.headerName(), csrfExchange.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("returning-reader", "correct-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("returning-reader"))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void 잘못된_login은_자격증명_상세를_숨긴다() throws Exception {
        register("wrong-password-reader", "correct-password");
        CsrfExchange csrfExchange = issueCsrfToken();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfExchange.cookie())
                        .header(csrfExchange.headerName(), csrfExchange.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("wrong-password-reader", "incorrect-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("로그인할 수 없습니다."))
                .andExpect(jsonPath("$.detail").value("사용자 이름 또는 비밀번호를 확인해 주세요."));
    }

    @Test
    void bcrypt_72byte를_넘는_login_password는_prefix가_같아도_거절한다() throws Exception {
        String maximumPassword = "가".repeat(24);
        register("bcrypt-boundary-reader", maximumPassword);
        CsrfExchange csrfExchange = issueCsrfToken();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfExchange.cookie())
                        .header(csrfExchange.headerName(), csrfExchange.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("bcrypt-boundary-reader", maximumPassword + "x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("비밀번호는 8자 이상, UTF-8 기준 72바이트 이하여야 합니다."));
    }

    @Test
    void 중복_username은_409로_거절한다() throws Exception {
        register("duplicate-reader", "correct-password");
        CsrfExchange csrfExchange = issueCsrfToken();

        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(csrfExchange.cookie())
                        .header(csrfExchange.headerName(), csrfExchange.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("duplicate-reader", "another-password")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void 상태변경_auth_api도_csrf가_없으면_403이다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("csrf-reader", "correct-password")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void 로그인했어도_보호된_쓰기에는_csrf가_필요하다() throws Exception {
        MockHttpSession session = register("protected-writer", "correct-password");

        mockMvc.perform(post("/api/v1/contents")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"보호된 글",
                                  "summary":"CSRF 보호를 확인합니다.",
                                  "category":"GENERAL_SCIENCE",
                                  "script":"인증만으로는 상태를 변경할 수 없습니다."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void logout은_session과_cookie를_정리한다() throws Exception {
        MockHttpSession session = register("logout-reader", "correct-password");
        CsrfExchange csrfExchange = issueCsrfToken(session);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .cookie(csrfExchange.cookie())
                        .header(csrfExchange.headerName(), csrfExchange.token()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0));
    }

    private MockHttpSession register(String username, String password) throws Exception {
        CsrfExchange csrfExchange = issueCsrfToken();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(csrfExchange.cookie())
                        .header(csrfExchange.headerName(), csrfExchange.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private CsrfExchange issueCsrfToken() throws Exception {
        return issueCsrfToken(null);
    }

    private CsrfExchange issueCsrfToken(MockHttpSession session) throws Exception {
        var request = get("/api/v1/auth/csrf");
        if (session != null) {
            request.session(session);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        String headerName = JsonPath.read(body, "$.headerName");
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        return new CsrfExchange(token, headerName, cookie);
    }

    private String credentials(String username, String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
    }

    private record CsrfExchange(String token, String headerName, Cookie cookie) {
    }
}
