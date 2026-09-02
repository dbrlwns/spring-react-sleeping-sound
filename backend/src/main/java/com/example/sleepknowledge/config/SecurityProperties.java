package com.example.sleepknowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 로컬 계정 중 관리자 권한을 부여할 사용자 이름을 운영 설정으로 관리합니다.
 * 비밀번호나 Google 자격 증명은 이 설정에 포함하지 않습니다.
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(List<String> adminUsernames) {

    public SecurityProperties {
        adminUsernames = adminUsernames == null
                ? List.of()
                : adminUsernames.stream()
                .filter(username -> username != null && !username.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public boolean isAdmin(String username) {
        return username != null
                && adminUsernames.contains(username.trim());
    }
}
