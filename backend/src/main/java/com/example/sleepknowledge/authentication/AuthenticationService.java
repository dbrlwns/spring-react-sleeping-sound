package com.example.sleepknowledge.authentication;

import com.example.sleepknowledge.config.SecurityProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;

/** 계정 등록과 Spring Security용 사용자 조회를 담당합니다. */
@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserAccountRepository userAccounts;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecurityProperties securityProperties;

    public AuthenticationService(
            UserAccountRepository userAccounts,
            PasswordEncoder passwordEncoder,
            Clock clock,
            SecurityProperties securityProperties
    ) {
        this.userAccounts = userAccounts;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public void register(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(rawPassword);

        // 관리자는 기존 일반 계정을 설정으로 승격합니다. 예약된 이름을 새로 선점할 수는 없습니다.
        if (securityProperties.isAdmin(normalizedUsername)) {
            throw new IllegalArgumentException(
                    "관리자 사용자 이름은 먼저 일반 계정으로 가입한 뒤 서버 설정에서 지정해야 합니다."
            );
        }

        if (userAccounts.existsByUsername(normalizedUsername)) {
            throw new UsernameAlreadyExistsException();
        }

        UserAccount account = UserAccount.register(
                UUID.randomUUID(),
                normalizedUsername,
                passwordEncoder.encode(rawPassword),
                clock.instant()
        );

        try {
            // unique constraint까지 이 메서드 안에서 확인해 동시 가입도 동일한 409로 변환합니다.
            userAccounts.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new UsernameAlreadyExistsException();
        }
    }

    public UserDetails loadUserByUsername(String username) {
        String normalizedUsername = normalizeUsername(username);
        UserAccount account = userAccounts.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        UserRole role = securityProperties.isAdmin(account.username()) ? UserRole.ADMIN : UserRole.USER;
        return User.withUsername(account.username())
                .password(account.passwordHash())
                .roles(role.name())
                .build();
    }

    public String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    public void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("비밀번호는 8자 이상, UTF-8 기준 72바이트 이하여야 합니다.");
        }
    }
}
