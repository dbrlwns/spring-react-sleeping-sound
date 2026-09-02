package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.out.persistence.ContentSeedData;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import com.example.sleepknowledge.domain.model.Voice;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Import(ContentApiIntegrationTest.FakePortConfiguration.class)
class ContentApiIntegrationTest {

    @MockitoBean
    private TextToSpeechClient textToSpeechClient;

    private final MockMvc mockMvc;
    private final ContentRepositoryPort contentRepository;
    private final FakeSpeechSynthesisPort speechSynthesisPort;

    @Autowired
    ContentApiIntegrationTest(
            MockMvc mockMvc,
            ContentRepositoryPort contentRepository,
            FakeSpeechSynthesisPort speechSynthesisPort
    ) {
        this.mockMvc = mockMvc;
        this.contentRepository = contentRepository;
        this.speechSynthesisPort = speechSynthesisPort;
    }

    @Test
    void 익명으로_시드된_콘텐츠_목록을_조회하면_긴_원고를_제외한다() throws Exception {
        mockMvc.perform(get("/api/v1/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.contents[0].id").exists())
                .andExpect(jsonPath("$.contents[0].category").exists())
                .andExpect(jsonPath("$.contents[0].authorUsername").isNotEmpty())
                .andExpect(content().string(not(containsString("\"script\""))));
    }

    @Test
    void 익명으로_콘텐츠_상세와_원고를_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/contents/{id}", ContentSeedData.QUANTUM_EPISODE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ContentSeedData.QUANTUM_EPISODE_ID.toString()))
                .andExpect(jsonPath("$.category").value("SCIENCE"))
                .andExpect(jsonPath("$.authorUsername")
                        .value(com.example.sleepknowledge.domain.model.Episode.LEGACY_AUTHOR_USERNAME))
                .andExpect(jsonPath("$.script").isNotEmpty());
    }

    @Test
    void 콘텐츠를_생성한다() throws Exception {
        mockMvc.perform(post("/api/v1/contents").with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"도시의 밤",
                                  "summary":"도시에서 사람들이 관계를 맺는 방식을 살펴봅니다.",
                                  "category":"SOCIETY",
                                  "script":"불이 켜진 도시의 골목을 따라 사람과 사회의 연결을 살펴봅니다.",
                                  "authorUsername":"클라이언트가 위조한 작성자"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/contents/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("도시의 밤"))
                .andExpect(jsonPath("$.category").value("SOCIETY"))
                .andExpect(jsonPath("$.authorUsername").value("integration-user"))
                .andExpect(jsonPath("$.script").isNotEmpty());
    }

    @Test
    void 다른_사용자가_콘텐츠를_수정해도_최초_작성자를_보존한다() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/contents")
                        .with(user("first-author")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contentRequestWithScript("작성자 보존 이야기", "처음 원고입니다.")))
                .andExpect(status().isCreated())
                .andReturn();
        String contentPath = created.getResponse().getHeader("Location");

        mockMvc.perform(put(contentPath)
                        .with(user("second-editor")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contentRequestWithScript("수정된 이야기", "수정 원고입니다.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorUsername").value("first-author"));
    }

    @Test
    void 콘텐츠_원고는_공백을_포함해_5000자까지_허용한다() throws Exception {
        String script = "가" + " ".repeat(4_998) + "나";

        mockMvc.perform(post("/api/v1/contents").with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contentRequestWithScript("경계 길이 원고", script)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.script").value(script));
    }

    @Test
    void 콘텐츠_원고는_공백을_포함해_5001자부터_거절한다() throws Exception {
        String script = "가" + " ".repeat(4_999) + "나";

        mockMvc.perform(post("/api/v1/contents").with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contentRequestWithScript("제한 초과 원고", script)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.script").value("원고는 5000자 이하여야 합니다."));
    }

    @Test
    void 익명_사용자는_csrf가_있어도_콘텐츠를_생성하거나_수정할_수_없다() throws Exception {
        String requestBody = """
                {
                  "title":"익명 작성 시도",
                  "summary":"로그인하지 않은 사용자의 요청입니다.",
                  "category":"SOCIETY",
                  "script":"공개 페이지를 읽을 수 있어도 글을 쓰거나 수정할 수는 없습니다."
                }
                """;

        mockMvc.perform(post("/api/v1/contents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/contents/{id}", ContentSeedData.QUANTUM_EPISODE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 기존_과학_분류_요청은_받아들이고_범용_과학으로_응답한다() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/contents")
                        .with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"기존 분류 호환 이야기",
                                  "summary":"기존 분류 코드를 계속 받아들입니다.",
                                  "category":"QUANTUM_PHYSICS",
                                  "script":"예전 클라이언트가 보낸 과학 분류를 범용 과학으로 보여 줍니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("SCIENCE"))
                .andReturn();
        String contentPath = created.getResponse().getHeader("Location");

        mockMvc.perform(get(contentPath)
                        .with(user("integration-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("SCIENCE"));
        UUID contentId = UUID.fromString(contentPath.substring(contentPath.lastIndexOf('/') + 1));
        assertThat(contentRepository.findById(contentId).orElseThrow().category())
                .isEqualTo(ContentCategory.SCIENCE);
    }

    @Test
    void 콘텐츠_원고를_수정해도_자동_TTS를_실행하지_않는다() throws Exception {
        String narrationPath = "/api/v1/contents/" + ContentSeedData.COSMOS_EPISODE_ID + "/narration";
        String updatedScript = "고요한 밤, 은하 사이 공간이 늘어나는 새 모습을 상상해 봅니다.";

        mockMvc.perform(put("/api/v1/contents/{id}", ContentSeedData.COSMOS_EPISODE_ID)
                        .with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"수정된 우주 이야기",
                                  "summary":"팽창하는 우주를 다시 설명합니다.",
                                  "category":"COSMOLOGY",
                                  "script":"고요한 밤, 은하 사이 공간이 늘어나는 새 모습을 상상해 봅니다."
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(narrationPath + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_REQUESTED"));
        assertThat(speechSynthesisPort.lastSynthesizedScript()).isNotEqualTo(updatedScript);
    }

    @Test
    void 익명_상태_GET은_asset을_자동_생성하지_않는다() throws Exception {
        String narrationPath = "/api/v1/contents/" + ContentSeedData.QUANTUM_EPISODE_ID + "/narration";

        mockMvc.perform(get(narrationPath + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_REQUESTED"))
                .andExpect(jsonPath("$.audioUrl").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").exists());
        mockMvc.perform(get(narrationPath + "/audio"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("NOT_REQUESTED"));
    }

    @Test
    void 콘텐츠를_생성해도_자동_내레이션을_만들지_않는다() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/contents")
                        .with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"제안 대기 이야기",
                                  "summary":"관리자 승인 전에는 생성되지 않습니다.",
                                  "category":"GENERAL_SCIENCE",
                                  "script":"트랜잭션이 끝난 뒤 이 원고를 읽습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String contentPath = created.getResponse().getHeader("Location");

        mockMvc.perform(get(contentPath + "/narration/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_REQUESTED"));
    }

    @Test
    void 공개_latest_제안은_없으면_204를_반환한다() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/contents")
                        .with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"아직 제안 없는 이야기",
                                  "summary":"제안 상태 조회를 확인합니다.",
                                  "category":"GENERAL_SCIENCE",
                                  "script":"아직 음성 지원 제안이 없습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String contentPath = created.getResponse().getHeader("Location");

        mockMvc.perform(get(contentPath + "/narration-proposals/latest"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 기존_수동_내레이션_post는_더_이상_노출하지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/contents/{id}/narration", ContentSeedData.QUANTUM_EPISODE_ID)
                        .with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voiceId\":\"Yuna\",\"speed\":0.9}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 내레이션용_음성_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/narration/voices").with(user("integration-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voices.length()").value(6))
                .andExpect(jsonPath("$.voices[0].id").value("ko-KR-Chirp3-HD-Kore"))
                .andExpect(jsonPath("$.voices[0].locale").value("ko-KR"));
    }

    @Test
    void 잘못된_콘텐츠_입력은_problem_detail로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/contents").with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","summary":"","category":"GENERAL_SCIENCE","script":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.script").exists());
    }

    @Test
    void 알_수_없는_카테고리는_400으로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/contents").with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"제목",
                                  "summary":"요약",
                                  "category":"UNKNOWN",
                                  "script":"원고"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void 없는_콘텐츠는_404_problem_detail로_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/contents/00000000-0000-0000-0000-000000000000")
                        .with(user("integration-user")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("콘텐츠를 찾을 수 없습니다."));
    }

    @Test
    void 잘못된_UUID는_400_problem_detail로_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/contents/not-a-uuid").with(user("integration-user")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("경로 값이 올바르지 않습니다."));
    }

    @Test
    void vite_개발_서버의_put_cors_preflight를_허용한다() throws Exception {
        mockMvc.perform(options("/api/v1/contents/{id}", ContentSeedData.QUANTUM_EPISODE_ID)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Content-Type,X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-XSRF-TOKEN")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    private String contentRequestWithScript(String title, String script) {
        return """
                {
                  "title":"%s",
                  "summary":"원고 길이 제한의 경계를 확인합니다.",
                  "category":"SCIENCE",
                  "script":"%s"
                }
                """.formatted(title, script);
    }

    static final class FakeSpeechSynthesisPort implements SpeechSynthesisPort {
        private volatile String blockedScript;
        private volatile String failedScript;
        private volatile String lastSynthesizedScript;
        private volatile CountDownLatch synthesisStarted = new CountDownLatch(0);
        private volatile CountDownLatch synthesisRelease = new CountDownLatch(0);

        @Override
        public List<Voice> findAvailableVoices() {
            return NarrationVoiceOption.voices();
        }

        @Override
        public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
            lastSynthesizedScript = script;
            if (script.equals(failedScript)) {
                failedScript = null;
                throw new IllegalStateException("테스트 공급자 실패");
            }
            if (script.equals(blockedScript)) {
                synthesisStarted.countDown();
                try {
                    if (!synthesisRelease.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("테스트 내레이션 차단 시간이 초과되었습니다.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("테스트 내레이션 대기가 중단되었습니다.", exception);
                } finally {
                    blockedScript = null;
                }
            }
            return AudioContent.linear16Wav(new byte[]{82, 73, 70, 70});
        }

        void blockSynthesisFor(String script) {
            blockedScript = script;
            synthesisStarted = new CountDownLatch(1);
            synthesisRelease = new CountDownLatch(1);
        }

        void failSynthesisFor(String script) {
            failedScript = script;
        }

        boolean awaitSynthesisStarted() throws InterruptedException {
            return synthesisStarted.await(5, TimeUnit.SECONDS);
        }

        void releaseBlockedSynthesis() {
            synthesisRelease.countDown();
        }

        String lastSynthesizedScript() {
            return lastSynthesizedScript;
        }
    }

    private org.springframework.test.web.servlet.ResultActions awaitReady(String statusPath) throws Exception {
        return awaitStatus(statusPath, "READY");
    }

    private org.springframework.test.web.servlet.ResultActions awaitStatus(
            String statusPath,
            String expectedStatus
    ) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        MvcResult latest = null;
        while (System.nanoTime() < deadline) {
            latest = mockMvc.perform(get(statusPath)).andReturn();
            if (latest.getResponse().getStatus() == 200
                    && latest.getResponse().getContentAsString()
                    .contains("\"status\":\"" + expectedStatus + "\"")) {
                return mockMvc.perform(get(statusPath));
            }
            Thread.sleep(20);
        }
        String response = latest == null ? "no response" : latest.getResponse().getContentAsString();
        throw new AssertionError("내레이션이 " + expectedStatus + "가 되지 않았습니다: " + response);
    }

    private void assertThatSynthesisStarted() throws InterruptedException {
        if (!speechSynthesisPort.awaitSynthesisStarted()) {
            throw new AssertionError("비동기 내레이션 합성이 시작되지 않았습니다.");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakePortConfiguration {

        @Bean
        @Primary
        FakeSpeechSynthesisPort fakeSpeechSynthesisPort() {
            return new FakeSpeechSynthesisPort();
        }
    }
}
