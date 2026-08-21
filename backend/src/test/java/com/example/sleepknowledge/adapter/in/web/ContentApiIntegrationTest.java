package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.out.persistence.ContentSeedData;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.domain.model.AudioContent;
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

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    ContentApiIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void 시드된_콘텐츠_목록은_긴_원고를_제외한다() throws Exception {
        mockMvc.perform(get("/api/v1/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.contents[0].id").exists())
                .andExpect(jsonPath("$.contents[0].category").exists())
                .andExpect(content().string(not(containsString("\"script\""))));
    }

    @Test
    void 콘텐츠_상세에서_원고를_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/contents/{id}", ContentSeedData.QUANTUM_EPISODE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ContentSeedData.QUANTUM_EPISODE_ID.toString()))
                .andExpect(jsonPath("$.category").value("QUANTUM_PHYSICS"))
                .andExpect(jsonPath("$.script").isNotEmpty());
    }

    @Test
    void 콘텐츠를_생성한다() throws Exception {
        mockMvc.perform(post("/api/v1/contents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"블랙홀의 밤",
                                  "summary":"블랙홀을 차분히 알아봅니다.",
                                  "category":"ASTRONOMY",
                                  "script":"별이 남긴 깊고 조용한 흔적을 따라가 봅니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/contents/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("블랙홀의 밤"))
                .andExpect(jsonPath("$.script").isNotEmpty());
    }

    @Test
    void 콘텐츠를_수정한다() throws Exception {
        mockMvc.perform(put("/api/v1/contents/{id}", ContentSeedData.COSMOS_EPISODE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"수정된 우주 이야기",
                                  "summary":"팽창하는 우주를 다시 설명합니다.",
                                  "category":"COSMOLOGY",
                                  "script":"고요한 밤, 은하 사이 공간이 늘어나는 모습을 상상해 봅니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ContentSeedData.COSMOS_EPISODE_ID.toString()))
                .andExpect(jsonPath("$.title").value("수정된 우주 이야기"));
    }

    @Test
    void 저장된_콘텐츠를_wav_내레이션으로_만든다() throws Exception {
        mockMvc.perform(post("/api/v1/contents/{id}/narration", ContentSeedData.QUANTUM_EPISODE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"voiceId":"Yuna","speed":0.9}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"))
                .andExpect(content().bytes(new byte[]{82, 73, 70, 70}))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", containsString("narration-")));
    }

    @Test
    void 내레이션용_음성_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/narration/voices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voices[0].id").value("Yuna"))
                .andExpect(jsonPath("$.voices[0].locale").value("ko_KR"));
    }

    @Test
    void 잘못된_콘텐츠_입력은_problem_detail로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/contents")
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
        mockMvc.perform(post("/api/v1/contents")
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
        mockMvc.perform(get("/api/v1/contents/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("콘텐츠를 찾을 수 없습니다."));
    }

    @Test
    void 잘못된_UUID는_400_problem_detail로_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/contents/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("경로 값이 올바르지 않습니다."));
    }

    @Test
    void vite_개발_서버의_put_cors_preflight를_허용한다() throws Exception {
        mockMvc.perform(options("/api/v1/contents/{id}", ContentSeedData.QUANTUM_EPISODE_ID)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")));
    }

    static final class FakeSpeechSynthesisPort implements SpeechSynthesisPort {
        private final Voice yuna = new Voice("Yuna", "Yuna", "한국어 음성", "ko_KR");

        @Override
        public List<Voice> findAvailableVoices() {
            return List.of(yuna);
        }

        @Override
        public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
            return AudioContent.wav(new byte[]{82, 73, 70, 70});
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
