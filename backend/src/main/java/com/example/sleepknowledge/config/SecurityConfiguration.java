package com.example.sleepknowledge.config;

import com.example.sleepknowledge.authentication.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    private static final String CSRF_ENDPOINT = "/api/v1/auth/csrf";
    private static final String SESSION_ENDPOINT = "/api/v1/auth/session";
    private static final String REGISTER_ENDPOINT = "/api/v1/auth/register";
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final String LOGOUT_ENDPOINT = "/api/v1/auth/logout";
    private static final String CONTENTS_ENDPOINT = "/api/v1/contents";
    private static final String CONTENT_DETAIL_ENDPOINT = "/api/v1/contents/*";
    private static final String NARRATION_STATUS_ENDPOINT = "/api/v1/contents/*/narration/status";
    private static final String NARRATION_AUDIO_ENDPOINT = "/api/v1/contents/*/narration/audio";
    private static final String LATEST_NARRATION_PROPOSAL_ENDPOINT =
            "/api/v1/contents/*/narration-proposals/latest";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AuthenticationService authenticationService) {
        return authenticationService::loadUserByUsername;
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CookieCsrfTokenRepository csrfTokenRepository
    ) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)
        ));
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        // 브라우저 API 클라이언트가 JSON token 또는 표준 XSRF cookie를 사용할 수 있습니다.
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            CookieCsrfTokenRepository csrfTokenRepository
    ) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                )
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true)
                )
                .sessionManagement(session -> session
                        .sessionAuthenticationStrategy(sessionAuthenticationStrategy)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.GET, CSRF_ENDPOINT, SESSION_ENDPOINT).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                CONTENTS_ENDPOINT,
                                CONTENT_DETAIL_ENDPOINT,
                                NARRATION_STATUS_ENDPOINT,
                                NARRATION_AUDIO_ENDPOINT,
                                LATEST_NARRATION_PROPOSAL_ENDPOINT
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, REGISTER_ENDPOINT, LOGIN_ENDPOINT, LOGOUT_ENDPOINT).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeProblem(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "인증이 필요합니다.",
                                "로그인 후 다시 시도해 주세요."
                        ))
                        .accessDeniedHandler((request, response, exception) -> writeProblem(
                                response,
                                HttpStatus.FORBIDDEN,
                                "요청이 거부되었습니다.",
                                "요청을 수행할 권한이 없습니다."
                        ))
                )
                .requestCache(cache -> cache.disable())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_ENDPOINT)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                );

        return http.build();
    }

    private static void writeProblem(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s"}
                """.formatted(title, status.value(), detail));
    }
}
