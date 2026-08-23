package com.example.sleepknowledge.authentication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 로그인 자격 증명만 보관하는 로컬 사용자 계정입니다. 콘텐츠 소유권과는 분리합니다. */
@Entity
@Table(name = "user_accounts")
class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {
        // JPA가 리플렉션으로 객체를 만들 때 사용합니다.
    }

    private UserAccount(UUID id, String username, String passwordHash, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.username = requireText(username, "username");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    static UserAccount register(UUID id, String username, String passwordHash, Instant createdAt) {
        return new UserAccount(id, username, passwordHash, createdAt);
    }

    String username() {
        return username;
    }

    String passwordHash() {
        return passwordHash;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
