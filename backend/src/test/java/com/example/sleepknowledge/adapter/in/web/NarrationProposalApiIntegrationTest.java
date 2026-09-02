package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.application.port.out.AudioTranscodingPort;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.NarrationVoiceOption;
import com.example.sleepknowledge.domain.model.Voice;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sleep-knowledge-proposal-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Import(NarrationProposalApiIntegrationTest.FakePortConfiguration.class)
class NarrationProposalApiIntegrationTest {

    private static final byte[] MP3_BYTES = {
            (byte) 0xff, (byte) 0xfb, (byte) 0x90, 0x64
    };

    @MockitoBean
    private TextToSpeechClient textToSpeechClient;

    private final MockMvc mockMvc;
    private final RecordingSpeechPort speech;

    @Autowired
    NarrationProposalApiIntegrationTest(MockMvc mockMvc, RecordingSpeechPort speech) {
        this.mockMvc = mockMvc;
        this.speech = speech;
    }

    @Test
    void 일반_사용자는_제안하고_공개_latest는_계정정보를_노출하지_않는다() throws Exception {
        String contentPath = createContent("제안할 이야기", "아직 음성이 없는 원고입니다.");
        String preferredVoiceId = NarrationVoiceOption.LEDA.voiceId();

        mockMvc.perform(post(contentPath + "/narration-proposals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":\"%s\"}".formatted(preferredVoiceId)))
                .andExpect(status().isUnauthorized());

        MvcResult createdProposal = mockMvc.perform(post(contentPath + "/narration-proposals")
                        .with(user("reader").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":\"%s\"}".formatted(preferredVoiceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.preferredVoiceId").value(preferredVoiceId))
                .andExpect(jsonPath("$.requestedBy").doesNotExist())
                .andReturn();
        String proposalId = com.jayway.jsonpath.JsonPath.read(
                createdProposal.getResponse().getContentAsString(), "$.id"
        );

        mockMvc.perform(post(contentPath + "/narration-proposals")
                        .with(user("other-reader").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":\"%s\"}".formatted(
                                NarrationVoiceOption.CHARON.voiceId()
                        )))
                .andExpect(status().isConflict());

        mockMvc.perform(get(contentPath + "/narration-proposals/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.preferredVoiceId").value(preferredVoiceId))
                .andExpect(jsonPath("$.requestedBy").doesNotExist())
                .andExpect(jsonPath("$.decidedBy").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/narration-proposals?status=PENDING")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposals[*].id", hasItem(proposalId)))
                .andExpect(jsonPath("$.proposals[*].requestedBy", hasItem("reader")))
                .andExpect(jsonPath("$.proposals[*].preferredVoiceId", hasItem(preferredVoiceId)))
                .andExpect(jsonPath("$.proposals[*].contentTitle", hasItem("제안할 이야기")));
    }

    @Test
    void 새_제안의_희망_voice는_필수이고_허용목록_밖은_400이다() throws Exception {
        String invalidContentPath = createContent("잘못된 희망 음성", "허용 목록 검증입니다.");
        mockMvc.perform(post(invalidContentPath + "/narration-proposals")
                        .with(user("reader").roles("USER")).with(csrf()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(invalidContentPath + "/narration-proposals")
                        .with(user("reader").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":null}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(invalidContentPath + "/narration-proposals")
                        .with(user("reader").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(invalidContentPath + "/narration-proposals")
                        .with(user("reader").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":\"ko-KR-Chirp3-HD-Puck\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(invalidContentPath + "/narration-proposals/latest"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 일반_사용자는_admin_API에_접근할_수_없고_관리자는_여섯_voice를_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/narration-proposals")
                        .with(user("reader").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/narration/voices")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voices.length()").value(6))
                .andExpect(jsonPath("$.voices[*].id", hasItem(NarrationVoiceOption.KORE.voiceId())))
                .andExpect(jsonPath("$.voices[*].id", hasItem(NarrationVoiceOption.ACHIRD.voiceId())))
                .andExpect(jsonPath("$.voices[*].gender", hasItem("FEMALE")))
                .andExpect(jsonPath("$.voices[*].gender", hasItem("MALE")));
    }

    @Test
    void 관리자가_voice를_승인하면_commit_후_즉시_같은_voice로_합성하고_재승인을_막는다() throws Exception {
        String contentPath = createContent("승인할 이야기", "관리자가 선택한 목소리로 읽습니다.");
        String proposalId = suggest(contentPath);
        String voiceId = NarrationVoiceOption.ACHIRD.voiceId();

        mockMvc.perform(post("/api/v1/admin/narration-proposals/{id}/approve", proposalId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voiceId\":\"%s\"}".formatted(voiceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.preferredVoiceId")
                        .value(NarrationVoiceOption.KORE.voiceId()))
                .andExpect(jsonPath("$.selectedVoiceId").value(voiceId))
                .andExpect(jsonPath("$.requestedBy").value("reader"))
                .andExpect(jsonPath("$.decidedBy").value("admin"));

        awaitStatus(contentPath + "/narration/status", "READY")
                .andExpect(jsonPath("$.selectedVoiceId").value(voiceId));
        assertThat(speech.lastVoiceId).isEqualTo(voiceId);
        mockMvc.perform(get(contentPath + "/narration/audio"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(AudioContent.MP3_MEDIA_TYPE))
                .andExpect(header().string(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        containsString(".mp3")
                ))
                .andExpect(content().bytes(MP3_BYTES));
        mockMvc.perform(post(contentPath + "/narration-proposals")
                        .with(user("reader-2").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":\"%s\"}".formatted(
                                NarrationVoiceOption.SCHEDAR.voiceId()
                        )))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/admin/narration-proposals/{id}/approve", proposalId)
                        .with(user("other-admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voiceId\":\"%s\"}".formatted(NarrationVoiceOption.KORE.voiceId())))
                .andExpect(status().isConflict());
    }

    @Test
    void 원고가_같은_수정은_audio를_보존하고_원고_변경은_삭제한_뒤_재제안을_받는다() throws Exception {
        String originalScript = "같은 원고는 기존 오디오를 유지합니다.";
        String contentPath = createContent("수정할 이야기", originalScript);
        String proposalId = suggest(contentPath);
        approve(proposalId, NarrationVoiceOption.KORE.voiceId());
        awaitStatus(contentPath + "/narration/status", "READY");
        int synthesisCount = speech.synthesisCount.get();

        updateContent(contentPath, "제목만 바뀐 이야기", originalScript);
        mockMvc.perform(get(contentPath + "/narration/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
        assertThat(speech.synthesisCount.get()).isEqualTo(synthesisCount);

        updateContent(contentPath, "원고도 바뀐 이야기", "승인 이후 새로 수정된 원고입니다.");
        mockMvc.perform(get(contentPath + "/narration/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_REQUESTED"));
        mockMvc.perform(get(contentPath + "/narration/audio"))
                .andExpect(status().isConflict());
        assertThat(speech.synthesisCount.get()).isEqualTo(synthesisCount);

        mockMvc.perform(post(contentPath + "/narration-proposals")
                        .with(user("reader-2").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":\"%s\"}".formatted(
                                NarrationVoiceOption.SCHEDAR.voiceId()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 허용하지_않은_voice는_400이고_제안은_PENDING으로_남는다() throws Exception {
        String contentPath = createContent("잘못된 음성 이야기", "허용 목록을 검사합니다.");
        String proposalId = suggest(contentPath);

        mockMvc.perform(post("/api/v1/admin/narration-proposals/{id}/approve", proposalId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voiceId\":\"ko-KR-Chirp3-HD-Puck\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(contentPath + "/narration-proposals/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        mockMvc.perform(get(contentPath + "/narration/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_REQUESTED"));
    }

    @Test
    void 관리자는_PENDING_제안을_거절할_수_있다() throws Exception {
        String contentPath = createContent("거절할 이야기", "관리자 검토에서 거절합니다.");
        String proposalId = suggest(contentPath);

        mockMvc.perform(post("/api/v1/admin/narration-proposals/{id}/reject", proposalId)
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.preferredVoiceId")
                        .value(NarrationVoiceOption.KORE.voiceId()))
                .andExpect(jsonPath("$.selectedVoiceId").doesNotExist())
                .andExpect(jsonPath("$.decidedBy").value("admin"));

        mockMvc.perform(get(contentPath + "/narration-proposals/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    private String createContent(String title, String script) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/contents")
                        .with(user("writer").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "summary":"음성 지원 흐름을 검증합니다.",
                                  "category":"SOCIETY",
                                  "script":"%s"
                                }
                                """.formatted(title, script)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getHeader("Location");
    }

    private String suggest(String contentPath) throws Exception {
        MvcResult result = mockMvc.perform(post(contentPath + "/narration-proposals")
                        .with(user("reader").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredVoiceId\":\"%s\"}".formatted(
                                NarrationVoiceOption.KORE.voiceId()
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void approve(String proposalId, String voiceId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/narration-proposals/{id}/approve", proposalId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voiceId\":\"%s\"}".formatted(voiceId)))
                .andExpect(status().isOk());
    }

    private void updateContent(String contentPath, String title, String script) throws Exception {
        mockMvc.perform(put(contentPath)
                        .with(user("writer").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "summary":"수정된 요약입니다.",
                                  "category":"SOCIETY",
                                  "script":"%s"
                                }
                                """.formatted(title, script)))
                .andExpect(status().isOk());
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
        throw new AssertionError("내레이션 상태 대기 실패: "
                + (latest == null ? "no response" : latest.getResponse().getContentAsString()));
    }

    static final class RecordingSpeechPort implements SpeechSynthesisPort {
        private final AtomicInteger synthesisCount = new AtomicInteger();
        private volatile String lastVoiceId;

        @Override
        public List<Voice> findAvailableVoices() {
            return NarrationVoiceOption.voices();
        }

        @Override
        public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
            synthesisCount.incrementAndGet();
            lastVoiceId = voice.id();
            return AudioContent.linear16Wav(new byte[]{82, 73, 70, 70});
        }
    }

    static final class FakeAudioTranscodingPort implements AudioTranscodingPort {

        @Override
        public void verifyAvailable() {
            // 외부 실행 파일 없이 API 흐름만 검증합니다.
        }

        @Override
        public AudioContent encodeMp3(AudioContent linear16Wav) {
            assertThat(linear16Wav.mediaType()).isEqualTo(AudioContent.LINEAR16_WAV_MEDIA_TYPE);
            return AudioContent.mp3(MP3_BYTES);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakePortConfiguration {

        @Bean
        @Primary
        RecordingSpeechPort recordingSpeechPort() {
            return new RecordingSpeechPort();
        }

        @Bean
        @Primary
        FakeAudioTranscodingPort fakeAudioTranscodingPort() {
            return new FakeAudioTranscodingPort();
        }
    }
}
