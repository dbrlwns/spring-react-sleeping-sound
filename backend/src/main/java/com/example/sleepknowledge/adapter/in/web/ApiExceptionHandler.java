package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.application.exception.ContentNotFoundException;
import com.example.sleepknowledge.application.exception.NarrationNotReadyException;
import com.example.sleepknowledge.application.exception.NarrationAlreadySupportedException;
import com.example.sleepknowledge.application.exception.NarrationProposalAlreadyPendingException;
import com.example.sleepknowledge.application.exception.NarrationProposalConflictException;
import com.example.sleepknowledge.application.exception.NarrationProposalNotFoundException;
import com.example.sleepknowledge.application.exception.SpeechSynthesisException;
import com.example.sleepknowledge.application.exception.UnsupportedVoiceException;
import com.example.sleepknowledge.authentication.UsernameAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** 모든 오류를 RFC 9457 Problem Details 형태로 일관되게 반환합니다. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "요청 값이 올바르지 않습니다.",
                "입력값을 확인해 주세요."
        );
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException exception) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "요청 본문을 읽을 수 없습니다.",
                "JSON 형식, 카테고리 값과 각 필드의 타입을 확인해 주세요."
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "경로 값이 올바르지 않습니다.",
                "콘텐츠 ID는 UUID 형식이어야 합니다."
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ContentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ContentNotFoundException exception) {
        ProblemDetail problem = problem(
                HttpStatus.NOT_FOUND,
                "콘텐츠를 찾을 수 없습니다.",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(NarrationProposalNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProposalNotFound(NarrationProposalNotFoundException exception) {
        ProblemDetail problem = problem(
                HttpStatus.NOT_FOUND,
                "음성 지원 제안을 찾을 수 없습니다.",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler({
            NarrationProposalAlreadyPendingException.class,
            NarrationProposalConflictException.class,
            NarrationAlreadySupportedException.class
    })
    public ResponseEntity<ProblemDetail> handleProposalConflict(RuntimeException exception) {
        ProblemDetail problem = problem(
                HttpStatus.CONFLICT,
                "음성 지원 제안을 처리할 수 없습니다.",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(AuthenticationException exception) {
        ProblemDetail problem = problem(
                HttpStatus.UNAUTHORIZED,
                "로그인할 수 없습니다.",
                "사용자 이름 또는 비밀번호를 확인해 주세요."
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUsernameConflict(UsernameAlreadyExistsException exception) {
        ProblemDetail problem = problem(
                HttpStatus.CONFLICT,
                "사용자 이름을 사용할 수 없습니다.",
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler({UnsupportedVoiceException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException exception) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "요청을 처리할 수 없습니다.",
                exception.getMessage()
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(SpeechSynthesisException.class)
    public ResponseEntity<ProblemDetail> handleSynthesisFailure(SpeechSynthesisException exception) {
        log.error("Narration synthesis failed", exception);
        ProblemDetail problem = problem(
                HttpStatus.BAD_GATEWAY,
                "내레이션 생성에 실패했습니다.",
                "TTS 엔진 설정과 서버 로그를 확인해 주세요."
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(NarrationNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleNarrationNotReady(NarrationNotReadyException exception) {
        ProblemDetail problem = problem(
                HttpStatus.CONFLICT,
                "내레이션이 아직 준비되지 않았습니다.",
                exception.getMessage()
        );
        problem.setProperty("status", exception.status());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .headers(errorResponse.getHeaders())
                    .body(errorResponse.getBody());
        }

        log.error("Unexpected server error", exception);
        ProblemDetail problem = problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 오류가 발생했습니다.",
                "잠시 후 다시 시도해 주세요."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
