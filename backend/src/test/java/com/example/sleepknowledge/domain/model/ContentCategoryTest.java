package com.example.sleepknowledge.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentCategoryTest {

    @Test
    void 범용_분류는_그대로_유지한다() {
        assertThat(ContentCategory.SCIENCE.canonical()).isEqualTo(ContentCategory.SCIENCE);
        assertThat(ContentCategory.SOCIETY.canonical()).isEqualTo(ContentCategory.SOCIETY);
        assertThat(ContentCategory.HISTORY.canonical()).isEqualTo(ContentCategory.HISTORY);
        assertThat(ContentCategory.PHILOSOPHY.canonical()).isEqualTo(ContentCategory.PHILOSOPHY);
        assertThat(ContentCategory.ECONOMY.canonical()).isEqualTo(ContentCategory.ECONOMY);
        assertThat(ContentCategory.TECHNOLOGY.canonical()).isEqualTo(ContentCategory.TECHNOLOGY);
        assertThat(ContentCategory.CULTURE.canonical()).isEqualTo(ContentCategory.CULTURE);
        assertThat(ContentCategory.PSYCHOLOGY.canonical()).isEqualTo(ContentCategory.PSYCHOLOGY);
    }

    @Test
    void 기존_과학_세부_분류는_모두_과학으로_정규화한다() {
        assertThat(ContentCategory.QUANTUM_PHYSICS.canonical()).isEqualTo(ContentCategory.SCIENCE);
        assertThat(ContentCategory.COSMOLOGY.canonical()).isEqualTo(ContentCategory.SCIENCE);
        assertThat(ContentCategory.ASTRONOMY.canonical()).isEqualTo(ContentCategory.SCIENCE);
        assertThat(ContentCategory.GENERAL_SCIENCE.canonical()).isEqualTo(ContentCategory.SCIENCE);
    }
}
