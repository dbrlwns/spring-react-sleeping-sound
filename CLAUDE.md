# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

"고요한 지식" — Spring Boot 백엔드와 React 프론트엔드로 구성된 학습용 프로젝트입니다. 핵심 제품은 **지식 콘텐츠 보관함과 제작 도구**입니다.(여러 분야에 대한 긴 원고를 작성/조회/수정). macOS `say`를 이용한 TTS 내레이션은 원고를 WAV로 미리 듣기 위한 보조 기능일 뿐, 핵심 제품이 아닙니다. 웹 클라이언트는 REST API만 사용하므로, 같은 백엔드를 향후 모바일 클라이언트에서도 재사용할 수 있게 설계되어 있습니다.

이 파일보다 더 깊이 있는 문서가 저장소 루트에 두 개 있으며, 해당 영역을 작업할 때 참고해야 합니다:
- `ARCHITECTURE.md` — 계층별 책임, 도메인 모델, 요청 흐름, TTS 제공자 교체 방법, 장문 콘텐츠/YouTube 로드맵.
- `FLOW.md` — 모든 사용자 흐름(목록, 상세, 생성, 수정, 음성 목록, 내레이션)의 매우 상세한 런타임 추적과 디버깅 플레이북, 구조 변경 시 권장 리뷰 순서.

## 명령어

### 백엔드 (`backend/`, Java 17, Spring Boot 4.1.0, Gradle wrapper)

```bash
cd backend
./gradlew bootRun          # http://localhost:8080 에서 서버 실행
./gradlew test             # 전체 테스트 실행 (JUnit 5 platform)
./gradlew test --tests "com.example.sleepknowledge.application.service.ContentServiceTest"   # 단일 테스트 클래스
./gradlew test --tests "*.ContentServiceTest.someMethodName"                                  # 단일 테스트 메서드
./gradlew build
```

`bootRun`은 반드시 `backend` 디렉터리에서 실행합니다 — H2 파일 DB가 현재 작업 디렉터리 기준 `backend/data/`에 생성됩니다.

### 프론트엔드 (`frontend/`, React + TypeScript + Vite)

```bash
cd frontend
npm install
npm run dev                # http://localhost:5173, /api 요청을 http://127.0.0.1:8080 으로 프록시
npm test                   # vitest run (전체 테스트)
npx vitest run src/domain/content.test.ts     # 단일 테스트 파일
npm run test:watch
npm run build               # tsc -b && vite build
```

앱이 처음부터 끝까지 동작하려면 두 서버가 모두 실행 중이어야 합니다. 백엔드는 HTML을 제공하지 않으므로 `/`는 의도적으로 404가 될 수 있습니다.

## 아키텍처

백엔드는 `backend/src/main/java/com/example/sleepknowledge/` 아래에 간결한 **Ports and Adapters(헥사고날)** 구조를 사용합니다:

```
adapter/in/web/           HTTP 컨트롤러, 요청/응답 DTO, ApiExceptionHandler
adapter/out/persistence/  JPA entity, Spring Data repository, repository port adapter, seed 데이터
adapter/out/narration/    macOS `say` adapter (SpeechSynthesisPort 구현체)
application/port/in/      inbound 유스케이스 인터페이스 (콘텐츠 조회/생성/수정, 내레이션)
application/port/out/     outbound port 인터페이스 (ContentRepositoryPort, SpeechSynthesisPort)
application/service/      ContentService, NarrationService — 트랜잭션과 흐름 조정만 담당
domain/model/             Episode, EpisodeDraft, ContentCategory, NarrationOptions, Voice, AudioContent
config/                   Spring @Configuration, @ConfigurationProperties 조립
```

의존 방향: `Web adapter -> Inbound port <- Application service -> Outbound port <- Outbound adapter`이며, application service는 domain model에도 의존합니다. 구체적으로 `ContentService`/`NarrationService`는 `ContentRepositoryPort`/`SpeechSynthesisPort` 인터페이스에만 의존하고 `JpaContentRepositoryAdapter`나 `MacOsSayNarrationAdapter`를 직접 참조하지 않습니다 — 실제 구현 Bean 연결은 Spring 설정이 담당합니다. 도메인 모델(`Episode` 등)에는 JPA나 Spring MVC 애노테이션이 전혀 없으며, `ContentJpaEntity`는 별도의 persistence 전용 타입입니다.

내레이션 코드를 다루기 전에 알아야 할 핵심 동작:
- `NarrationService`는 내레이션 요청에 원고를 받지 **않습니다** — `contentId`로 저장소에서 `Episode`를 조회한 뒤 그 저장된 원고를 합성합니다. 클라이언트는 `voiceId`와 `speed`(0.5~2.0)만 전송합니다.
- WAV 합성은 완전히 **동기적**입니다: HTTP 스레드가 `say` 실행과 WAV 전체 읽기가 끝날 때까지 블로킹됩니다. 오디오는 서버에 캐시되거나 저장되지 않으며, 동일한 요청도 매번 다시 합성됩니다. 이는 MVP에서 의도된 설계입니다 — 장문 콘텐츠를 위한 비동기 작업 큐 설계는 `ARCHITECTURE.md` 15장, 별도 기능인 YouTube 배포 로드맵은 16장을 참고하세요.
- 새 TTS 제공자를 추가하려면: `SpeechSynthesisPort`를 구현하고 `NarrationConfiguration`에서 연결하며, 제공자 API 키는 코드가 아닌 환경 변수/비밀 저장소에서만 읽습니다. `ContentController`/`ContentService`는 변경할 필요가 없습니다.

프론트엔드(`frontend/src/`)는 별도 라우터 없이 하나의 SPA 화면으로 구성됩니다:

```
api/         contentApi.ts, speechApi.ts, httpClient.ts — REST 호출
domain/      프론트 쪽 모델, 카테고리 표시 매핑, 검증 제약 조건
components/  ContentLibrary, ContentDetail, ContentEditor, NarrationPanel, VoiceSelector, SpeedControl, AudioResult
hooks/       useContentLibrary, useNarrationGenerator, useVoices — 화면 상태와 비동기 흐름
```

목록 검색/필터와 카테고리 표시 변환은 이미 받아온 목록을 대상으로 클라이언트에서 처리합니다 — 서버 측 검색/페이지네이션은 아직 없습니다. `VITE_API_BASE_URL`(`.env.local`에서 설정, `.env.example` 참고)은 프록시를 거치지 않는 별도 API origin을 가리킬 때 사용하며, 비워두면 Vite 개발 프록시를 사용합니다.

## 도메인 참고 사항

- `ContentCategory`는 고정된 코드 enum입니다 (`QUANTUM_PHYSICS`, `COSMOLOGY`, `ASTRONOMY`, `GENERAL_SCIENCE`). 프론트엔드가 이 코드를 표시 문구로 매핑하며 그 반대는 없습니다. 알 수 없는 코드는 400으로 거부됩니다.
- `EpisodeDraft` 검증(필수 필드, 120/500/20,000자 제한, 공백 트림)은 HTTP DTO(Bean Validation, 필드별 오류 메시지 제공용)와 도메인 생성자 양쪽에서 모두 적용됩니다 — 웹이 아닌 다른 진입점에서도 규칙이 무너지지 않도록 하기 위함입니다.
- API 오류는 RFC 9457 `application/problem+json` 형식입니다. 스택 트레이스와 외부 프로세스 출력은 HTTP 응답에 노출하지 않고 서버 로그에만 남깁니다 (`ApiExceptionHandler` 참고).
- H2는 **메모리가 아닌 파일** 데이터베이스입니다 (`backend/data/`, `ddl-auto: update`) — 서버 재시작 후에도 데이터가 유지됩니다. Seed 콘텐츠는 DB가 비어 있을 때만 삽입됩니다. 이 DB/마이그레이션 구성은 명시적으로 로컬 학습용 선택이며 운영 환경에는 적합하지 않습니다(Flyway 없음, Postgres 아님).
- 인증, 요청 빈도 제한, 초안/공개 구분이 없습니다 — 저장된 모든 항목이 목록 API에 그대로 노출됩니다. 현재 백엔드를 인터넷에 그대로 배포할 수 있는 상태로 취급하지 마세요 — 그런 이유로 `server.address` 기본값이 `127.0.0.1`입니다.

## 테스트 컨벤션

백엔드 테스트는 경계별로 나뉘어 있으며 이름도 그에 맞춰져 있습니다 — 새 테스트를 추가할 때도 서비스 테스트에 통합 테스트 성격을 섞지 말고 이 구분을 따르세요:
- `ContentServiceTest` — fake repository port를 사용한 콘텐츠 CRUD 규칙 검증
- `NarrationServiceTest` — fake를 사용한 음성 검증, 원고 조회, 합성 포트 호출 검증
- `ContentApiIntegrationTest` — HTTP JSON, 검증, 상태 코드, 실제 JPA 연동
- `MacOsSayNarrationAdapterTest` — 시스템 음성 목록 파싱

단위 테스트는 실제로 `/usr/bin/say`를 호출하지 않고 fake port를 사용합니다. 실제 합성은 화면 또는 `curl`을 통한 수동 smoke test로만 확인합니다.
