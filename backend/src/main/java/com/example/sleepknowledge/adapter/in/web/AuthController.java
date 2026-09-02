package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.in.web.dto.AuthCredentialsRequest;
import com.example.sleepknowledge.adapter.in.web.dto.AuthSessionResponse;
import com.example.sleepknowledge.adapter.in.web.dto.CsrfResponse;
import com.example.sleepknowledge.authentication.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 브라우저 세션을 시작하고 현재 로그인 상태를 JSON으로 노출합니다. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthController(
            AuthenticationService authenticationService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy
    ) {
        this.authenticationService = authenticationService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }

    @GetMapping("/session")
    public AuthSessionResponse session(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return AuthSessionResponse.anonymous();
        }
        return AuthSessionResponse.authenticated(authentication);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthSessionResponse> register(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        authenticationService.register(request.username(), request.password());
        Authentication authentication = authenticate(request);
        saveAuthenticatedSession(authentication, servletRequest, servletResponse);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthSessionResponse.authenticated(authentication));
    }

    @PostMapping("/login")
    public AuthSessionResponse login(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        Authentication authentication = authenticate(request);
        saveAuthenticatedSession(authentication, servletRequest, servletResponse);
        return AuthSessionResponse.authenticated(authentication);
    }

    private Authentication authenticate(AuthCredentialsRequest request) {
        // BCrypt의 72-byte 경계를 로그인에도 적용해 prefix가 같은 긴 비밀번호를 거절합니다.
        authenticationService.validatePassword(request.password());
        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        authenticationService.normalizeUsername(request.username()),
                        request.password()
                )
        );
    }

    private void saveAuthenticatedSession(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Controller 기반 로그인도 필터 로그인과 동일하게 기존 session id를 교체합니다.
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
