package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.out.persistence.ContentSeedData;
import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.Voice;
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
                .andExpect(content().string(not(containsString("\"script\""))));
    }

    @Test
    void 익명으로_콘텐츠_상세와_원고를_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/contents/{id}", ContentSeedData.QUANTUM_EPISODE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ContentSeedData.QUANTUM_EPISODE_ID.toString()))
                .andExpect(jsonPath("$.category").value("SCIENCE"))
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
                                  "script":"불이 켜진 도시의 골목을 따라 사람과 사회의 연결을 살펴봅니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/contents/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("도시의 밤"))
                .andExpect(jsonPath("$.category").value("SOCIETY"))
                .andExpect(jsonPath("$.script").isNotEmpty());
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
    void 콘텐츠를_수정하면_기존_wav를_즉시_무효화하고_다시_생성한다() throws Exception {
        String narrationPath = "/api/v1/contents/" + ContentSeedData.COSMOS_EPISODE_ID + "/narration";
        awaitReady(narrationPath + "/status");
        String updatedScript = "고요한 밤, 은하 사이 공간이 늘어나는 새 모습을 상상해 봅니다.";
        speechSynthesisPort.blockSynthesisFor(updatedScript);

        try {
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
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ContentSeedData.COSMOS_EPISODE_ID.toString()))
                    .andExpect(jsonPath("$.title").value("수정된 우주 이야기"));

            assertThatSynthesisStarted();
            mockMvc.perform(get(narrationPath + "/status").with(user("integration-user")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROCESSING"))
                    .andExpect(header().string("Cache-Control", "no-store"));
            mockMvc.perform(get(narrationPath + "/audio").with(user("integration-user")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));
        } finally {
            speechSynthesisPort.releaseBlockedSynthesis();
        }

        awaitReady(narrationPath + "/status");
        mockMvc.perform(get(narrationPath + "/audio"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{82, 73, 70, 70}));
    }

    @Test
    void 익명으로_내레이션_상태와_저장된_wav를_조회한다() throws Exception {
        String narrationPath = "/api/v1/contents/" + ContentSeedData.QUANTUM_EPISODE_ID + "/narration";

        awaitReady(narrationPath + "/status");
        mockMvc.perform(get(narrationPath + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.audioUrl").value(narrationPath + "/audio"))
                .andExpect(jsonPath("$.updatedAt").exists());
        mockMvc.perform(get(narrationPath + "/audio"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"))
                .andExpect(content().bytes(new byte[]{82, 73, 70, 70}))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", containsString("narration-")));
    }

    @Test
    void 콘텐츠_생성_commit_후_자동_내레이션을_비동기로_저장한다() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/contents")
                        .with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"자동 내레이션 이야기",
                                  "summary":"저장 직후 자동으로 생성됩니다.",
                                  "category":"GENERAL_SCIENCE",
                                  "script":"트랜잭션이 끝난 뒤 이 원고를 읽습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String contentPath = created.getResponse().getHeader("Location");

        awaitReady(contentPath + "/narration/status")
                .andExpect(jsonPath("$.audioUrl").value(contentPath + "/narration/audio"));
        mockMvc.perform(get(contentPath + "/narration/audio").with(user("integration-user")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{82, 73, 70, 70}));
    }

    @Test
    void 자동_합성_실패는_errorMessage와_failed_상태로_저장한다() throws Exception {
        String failedScript = "이 원고는 테스트 공급자 실패를 재현합니다.";
        speechSynthesisPort.failSynthesisFor(failedScript);
        MvcResult created = mockMvc.perform(post("/api/v1/contents")
                        .with(user("integration-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"실패 상태 이야기",
                                  "summary":"실패 상태 저장을 확인합니다.",
                                  "category":"GENERAL_SCIENCE",
                                  "script":"이 원고는 테스트 공급자 실패를 재현합니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String narrationPath = created.getResponse().getHeader("Location") + "/narration";

        awaitStatus(narrationPath + "/status", "FAILED")
                .andExpect(jsonPath("$.errorMessage").value("테스트 공급자 실패"));
        mockMvc.perform(get(narrationPath + "/audio").with(user("integration-user")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("FAILED"));
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
                .andExpect(jsonPath("$.voices[0].id").value("Yuna"))
                .andExpect(jsonPath("$.voices[0].locale").value("ko_KR"));
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

    static final class FakeSpeechSynthesisPort implements SpeechSynthesisPort {
        private final Voice yuna = new Voice("Yuna", "Yuna", "한국어 음성", "ko_KR");
        private volatile String blockedScript;
        private volatile String failedScript;
        private volatile CountDownLatch synthesisStarted = new CountDownLatch(0);
        private volatile CountDownLatch synthesisRelease = new CountDownLatch(0);

        @Override
        public List<Voice> findAvailableVoices() {
            return List.of(yuna);
        }

        @Override
        public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
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
            return AudioContent.wav(new byte[]{82, 73, 70, 70});
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
