package com.example.sleepknowledge.adapter.out.persistence;

import com.example.sleepknowledge.application.port.out.ContentRepositoryPort;
import com.example.sleepknowledge.domain.model.ContentCategory;
import com.example.sleepknowledge.domain.model.Episode;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** 빈 데이터베이스에만 예시 콘텐츠를 넣어 첫 실행 화면을 바로 살펴볼 수 있게 합니다. */
@Component
public class ContentSeedData implements ApplicationRunner {

    public static final UUID QUANTUM_EPISODE_ID = UUID.fromString("47ef2924-4ddd-45f1-9c9c-76fb2ba11001");
    public static final UUID COSMOS_EPISODE_ID = UUID.fromString("47ef2924-4ddd-45f1-9c9c-76fb2ba11002");

    private final ContentRepositoryPort contentRepository;

    public ContentSeedData(ContentRepositoryPort contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (contentRepository.count() != 0) {
            return;
        }

        Instant firstPublishedAt = Instant.parse("2026-01-01T21:00:00Z");
        contentRepository.save(new Episode(
                QUANTUM_EPISODE_ID,
                "잠들기 전에 만나는 양자역학",
                "아주 작은 세계에서 입자와 가능성이 어떻게 움직이는지 편안하게 살펴봅니다.",
                ContentCategory.SCIENCE,
                "불을 조금 낮추고, 우리가 볼 수 없는 아주 작은 세계를 떠올려 봅시다. " +
                        "양자역학에서 전자 같은 입자는 언제나 한 자리에 고정된 작은 공처럼 행동하지 않습니다. " +
                        "관측하기 전에는 여러 가능성이 함께 존재하고, 측정하는 순간 그중 하나의 결과를 만나게 됩니다. " +
                        "이 낯선 규칙은 멀리 있는 이야기가 아닙니다. 반도체와 레이저처럼 우리가 매일 쓰는 기술도 이 작은 세계의 법칙 위에 서 있습니다.",
                firstPublishedAt,
                firstPublishedAt
        ));

        Instant secondPublishedAt = Instant.parse("2026-01-02T21:00:00Z");
        contentRepository.save(new Episode(
                COSMOS_EPISODE_ID,
                "우주는 왜 계속 팽창할까",
                "은하 사이의 공간이 늘어나는 우주 팽창과 암흑 에너지의 개념을 차분히 설명합니다.",
                ContentCategory.SCIENCE,
                "밤하늘의 은하들은 단순히 빈 공간을 가로질러 달아나는 것이 아닙니다. " +
                        "우주 전체의 공간 자체가 천천히 늘어나면서, 멀리 있는 은하 사이의 거리도 함께 커지고 있습니다. " +
                        "빛의 파장이 늘어나는 적색편이는 이 팽창을 알려 주는 중요한 단서입니다. " +
                        "과학자들은 팽창이 점점 빨라지는 이유를 암흑 에너지라는 이름으로 부르지만, 그 정체는 아직 완전히 알지 못합니다. " +
                        "우리가 모른다는 사실은 우주에 아직 발견할 이야기가 많이 남아 있다는 뜻이기도 합니다.",
                secondPublishedAt,
                secondPublishedAt
        ));
    }
}
