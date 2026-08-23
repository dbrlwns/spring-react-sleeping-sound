package com.example.sleepknowledge.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthCredentialsRequest(
        @NotBlank(message = "사용자 이름을 입력해 주세요.")
        @Size(min = 3, max = 50, message = "사용자 이름은 3자 이상 50자 이하여야 합니다.")
        @Pattern(
                regexp = "^[\\p{L}\\p{N}._-]+$",
                message = "사용자 이름에는 문자, 숫자, 마침표, 밑줄과 하이픈만 사용할 수 있습니다."
        )
        String username,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
        String password
) {
}
