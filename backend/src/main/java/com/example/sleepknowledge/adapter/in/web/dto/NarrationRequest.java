package com.example.sleepknowledge.adapter.in.web.dto;

import com.example.sleepknowledge.domain.model.NarrationOptions;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record NarrationRequest(
        @NotBlank(message = "음성을 선택해 주세요.")
        @Size(max = 100, message = "음성 ID는 100자 이하여야 합니다.")
        String voiceId,

        @NotNull(message = "재생 속도를 입력해 주세요.")
        @DecimalMin(value = "0.5", message = "재생 속도는 0.5 이상이어야 합니다.")
        @DecimalMax(value = "2.0", message = "재생 속도는 2.0 이하여야 합니다.")
        BigDecimal speed
) {

    public NarrationOptions toOptions() {
        return new NarrationOptions(voiceId, speed.doubleValue());
    }
}
