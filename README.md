# 고요한 지식 — Spring + React

양자역학과 우주처럼 어렵지만 흥미로운 과학 이야기를 저장하고, 잠들기 전에 편안하게 들을 수 있는 내레이션으로 만들어 보는 학습용 웹 서비스입니다.

이 프로젝트의 핵심은 **TTS 자체가 아니라 지식 콘텐츠 보관함과 제작 화면**입니다. TTS는 저장된 원고를 미리 듣고 WAV 파일로 내보내기 위한 보조 기능입니다. 웹은 REST API만 사용하므로 향후 React Native, Flutter 또는 네이티브 모바일 앱에서도 같은 Spring 백엔드를 재사용할 수 있습니다.

## 현재 MVP에서 할 수 있는 일

- 과학 콘텐츠 목록 조회
- 제목과 소개를 이용한 화면 내 검색
- 카테고리별 화면 필터링
- 콘텐츠 상세 원고와 예상 청취 시간 확인
- 새 콘텐츠 작성 및 기존 콘텐츠 수정
- H2 파일 데이터베이스에 콘텐츠 영속 저장
- macOS 음성 및 읽기 속도 선택
- 저장된 원고 전체를 WAV 내레이션으로 생성
- 브라우저에서 내레이션 재생 및 파일 다운로드

첫 실행 시 양자역학과 우주론 예시 콘텐츠가 빈 데이터베이스에 자동으로 추가됩니다.

## 기술 구성

| 영역 | 기술 | 역할 |
|---|---|---|
| 백엔드 | Java 17, Spring Boot 4.1.0, Gradle | 콘텐츠 CRUD, 검증, 트랜잭션, 내레이션 생성 API |
| 저장소 | Spring Data JPA, H2 file database | 콘텐츠를 서버 재시작 후에도 보존 |
| 프론트엔드 | React, TypeScript, Vite | 보관함, 상세 화면, 원고 편집기, 내레이션 제작 패널 |
| MVP 내레이션 제공자 | macOS `/usr/bin/say` | 저장된 원고를 WAV 음성으로 합성 |

## 먼저 준비할 것

- macOS: 현재 내레이션 어댑터가 `/usr/bin/say`를 사용합니다.
- JDK 17 이상
- Node.js 20.19 이상인 20.x, 또는 22.12 이상과 npm

설치 상태를 확인합니다.

```bash
java -version
node --version
npm --version
say -v '?'
```

`say -v '?'`는 현재 Mac에 설치된 음성 목록을 보여 줍니다. 한국어 음성이 없다면 macOS의 **시스템 설정 > 손쉬운 사용 > 읽기 콘텐츠 > 시스템 음성**에서 내려받을 수 있습니다.

## 실행하기

터미널 두 개를 사용합니다.

### 1. Spring 백엔드

```bash
cd backend
./gradlew bootRun
```

백엔드는 `http://localhost:8080`에서 실행됩니다. 실행 중 Gradle이 `EXECUTING` 상태로 보이는 것은 Spring 서버가 요청을 기다리고 있기 때문이며 정상입니다. 백엔드는 HTML 홈 화면을 제공하지 않으므로 `http://localhost:8080/`은 404가 될 수 있습니다.

Gradle Wrapper가 포함되어 있어 별도의 Gradle 설치는 필요하지 않습니다.

- `build.gradle`: 플러그인, Java 버전, 저장소, 의존성과 테스트 설정
- `settings.gradle`: Gradle 프로젝트 이름
- `gradlew`, `gradlew.bat`: 고정된 Gradle 환경을 실행하는 Wrapper 스크립트
- `gradle/wrapper/gradle-wrapper.properties`: Wrapper가 사용할 Gradle 배포판 설정

### 2. React 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`을 엽니다. 개발 중에는 Vite가 `/api` 요청을 `http://127.0.0.1:8080`으로 프록시합니다.

다른 주소의 API를 사용하려면 `frontend/.env.example`을 참고해 `frontend/.env.local`을 만들 수 있습니다.

```dotenv
VITE_API_BASE_URL=https://api.example.com
```

`VITE_`로 시작하는 값은 브라우저 번들에 노출되므로 API 키나 비밀번호를 넣으면 안 됩니다.

## 화면 사용 흐름

현재 React 앱은 별도 URL 라우터 없이 하나의 SPA 안에서 다음 화면을 전환합니다.

```text
이야기 보관함
  -> 콘텐츠 상세 및 원고 읽기
      -> 내레이션 음성·속도 선택
      -> WAV 미리 듣기·다운로드
      -> 원고 편집

이야기 보관함
  -> 새 이야기
      -> 제목·소개·카테고리·원고 작성
      -> 저장 후 상세 화면
```

목록 검색과 카테고리 필터는 현재 React가 이미 조회한 목록 안에서 처리합니다. 서버 측 검색, 필터, 정렬과 페이지네이션은 아직 구현하지 않았습니다.

## 데이터베이스

콘텐츠는 메모리가 아닌 H2 파일 데이터베이스에 저장됩니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/sleepknowledge;AUTO_SERVER=TRUE
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
```

백엔드를 `backend` 디렉터리에서 실행하면 데이터 파일은 `backend/data` 아래에 생성됩니다. 서버를 종료했다가 다시 실행해도 작성한 콘텐츠가 남습니다. 데이터베이스가 비어 있을 때만 예시 콘텐츠 두 편이 추가됩니다.

H2와 `ddl-auto: update`는 로컬 학습용 선택입니다. 운영 환경에서는 PostgreSQL 같은 운영용 DB와 Flyway 같은 명시적 스키마 마이그레이션 도구를 사용하는 편이 안전합니다.

## 콘텐츠 카테고리

API는 표시 문구 대신 안정적인 분류 코드를 사용합니다.

| 코드 | 화면 표시 |
|---|---|
| `QUANTUM_PHYSICS` | 양자역학 |
| `COSMOLOGY` | 우주론 |
| `ASTRONOMY` | 천문학 |
| `GENERAL_SCIENCE` | 과학 교양 |

지원하지 않는 코드가 요청에 포함되면 `400 Bad Request`가 반환됩니다.

## REST API

### 콘텐츠 목록

```http
GET /api/v1/contents
```

```bash
curl http://localhost:8080/api/v1/contents
```

목록 응답은 긴 원고를 제외한 요약 정보만 반환합니다.

```json
{
  "contents": [
    {
      "id": "47ef2924-4ddd-45f1-9c9c-76fb2ba11001",
      "title": "잠들기 전에 만나는 양자역학",
      "summary": "아주 작은 세계에서 입자와 가능성이 어떻게 움직이는지 편안하게 살펴봅니다.",
      "category": "QUANTUM_PHYSICS",
      "createdAt": "2026-01-01T21:00:00Z",
      "updatedAt": "2026-01-01T21:00:00Z"
    }
  ]
}
```

### 콘텐츠 상세

```http
GET /api/v1/contents/{contentId}
```

```bash
curl http://localhost:8080/api/v1/contents/47ef2924-4ddd-45f1-9c9c-76fb2ba11001
```

상세 응답에는 내레이션 제작에 사용하는 `script`가 포함됩니다.

### 콘텐츠 생성

```http
POST /api/v1/contents
Content-Type: application/json
```

```bash
curl --request POST http://localhost:8080/api/v1/contents \
  --header 'Content-Type: application/json' \
  --data '{
    "title": "별은 어떻게 태어날까",
    "summary": "차가운 성간 구름에서 별이 시작되는 과정을 천천히 살펴봅니다.",
    "category": "ASTRONOMY",
    "script": "편안히 눈을 감고, 아주 넓고 차가운 우주의 구름을 떠올려 봅시다."
  }'
```

성공하면 `201 Created`, 생성된 콘텐츠 상세 정보와 `Location` 헤더가 반환됩니다.

### 콘텐츠 수정

```http
PUT /api/v1/contents/{contentId}
Content-Type: application/json
```

`PUT` 요청은 제목, 소개, 카테고리, 원고를 모두 전달합니다. 존재하지 않는 UUID는 `404 Not Found`입니다.

콘텐츠 입력 제한:

| 필드 | 규칙 |
|---|---|
| `title` | 공백 제외 필수, 최대 120자 |
| `summary` | 공백 제외 필수, 최대 500자 |
| `category` | 네 가지 카테고리 코드 중 하나 |
| `script` | 공백 제외 필수, 최대 20,000자 |

### 내레이션 음성 목록

```http
GET /api/v1/narration/voices
```

```bash
curl http://localhost:8080/api/v1/narration/voices
```

설치된 시스템 음성에 따라 결과가 달라집니다.

```json
{
  "voices": [
    {
      "id": "Yuna",
      "name": "Yuna",
      "description": "안녕하세요. 제 이름은 유나입니다.",
      "locale": "ko_KR"
    }
  ]
}
```

### 저장된 콘텐츠의 내레이션 생성

```http
POST /api/v1/contents/{contentId}/narration
Content-Type: application/json
Accept: audio/wav
```

요청에는 원고를 다시 보내지 않습니다. Spring이 `contentId`로 저장된 콘텐츠를 조회한 뒤 그 원고를 합성합니다.

```json
{
  "voiceId": "Yuna",
  "speed": 0.9
}
```

`speed`는 `0.5` 이상 `2.0` 이하입니다.

```bash
curl --request POST \
  http://localhost:8080/api/v1/contents/47ef2924-4ddd-45f1-9c9c-76fb2ba11001/narration \
  --header 'Content-Type: application/json' \
  --header 'Accept: audio/wav' \
  --data '{"voiceId":"Yuna","speed":0.9}' \
  --output narration.wav
```

성공 응답은 Base64 JSON이 아닌 `audio/wav` 바이너리입니다. 브라우저는 이를 `Blob`으로 받아 바로 재생하고 다운로드 링크를 만듭니다.

## 현재 내레이션의 범위

현재 엔드포인트는 요청을 받은 HTTP 스레드에서 다음 과정을 **동기적으로** 수행합니다.

```text
콘텐츠 조회 -> 음성 확인 -> say 실행 -> WAV 전체 읽기 -> HTTP 응답
```

이 방식은 로컬에서 원고를 미리 듣거나 WAV를 한 번 내보내는 MVP에는 단순하고 이해하기 쉽습니다. 그러나 긴 수면 콘텐츠를 반복적으로 생성하고 운영하는 최종 구조는 아닙니다.

- 생성 중에는 HTTP 요청이 계속 열려 있습니다.
- WAV 전체를 서버 메모리와 브라우저 Blob으로 보관합니다.
- 생성한 오디오는 서버에 저장되거나 캐시되지 않습니다.
- 같은 설정으로 다시 요청하면 매번 새로 합성합니다.
- 공급자별 글자 수 제한, 부분 재시도와 진행률을 처리하지 않습니다.
- 브라우저의 “화면 대기 중단”은 요청 대기를 중단하지만 이미 시작된 서버 프로세스 작업을 즉시 취소한다는 보장은 없습니다.

따라서 현재 기능은 **동기 미리 듣기·수동 WAV 내보내기**로 이해해야 합니다.

## 오류 응답

API 오류는 RFC 9457 `application/problem+json` 형태로 반환됩니다.

| 상황 | 상태 코드 |
|---|---|
| 필수값 누락, 길이 초과, 잘못된 카테고리·속도·음성 | `400 Bad Request` |
| UUID 형식이 잘못된 경로 | `400 Bad Request` |
| 존재하지 않는 콘텐츠 | `404 Not Found` |
| macOS `say` 실행 또는 WAV 처리 실패 | `502 Bad Gateway` |
| 예상하지 못한 서버 오류 | `500 Internal Server Error` |

스택 트레이스와 외부 프로세스의 상세 출력은 HTTP 응답에 노출하지 않고 서버 로그에 남깁니다.

## 프로젝트 구조

```text
tts-spring-react-demo/
├── backend/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew
│   ├── data/                         실행 후 생성되는 H2 데이터
│   └── src/main/java/com/example/sleepknowledge/
│       ├── adapter/in/web/           콘텐츠·내레이션 Controller, DTO, 오류 처리
│       ├── adapter/out/persistence/  JPA entity, Spring Data, repository adapter, seed
│       ├── adapter/out/narration/    macOS say 내레이션 adapter
│       ├── application/port/in/      콘텐츠와 내레이션 유스케이스 계약
│       ├── application/port/out/     저장소와 음성 합성 포트
│       ├── application/service/      콘텐츠·내레이션 흐름 조정
│       ├── domain/model/             Episode, EpisodeDraft, Category, Voice 등
│       └── config/                   Spring Bean과 설정 속성 조립
├── frontend/
│   └── src/
│       ├── api/                      콘텐츠·내레이션 HTTP 호출
│       ├── components/               보관함, 상세, 편집기, 내레이션 패널
│       ├── domain/                   프론트 모델, 카테고리, 제약 조건
│       └── hooks/                    화면 상태와 비동기 요청 흐름
├── ARCHITECTURE.md                   계층과 요청 흐름 설명
└── README.md                         실행과 API 사용 안내
```

백엔드는 간결한 Ports and Adapters 구조를 사용합니다.

```text
React
  -> Web adapter
  -> Inbound use case
  -> Application service
  -> Outbound port
      -> JPA/H2 persistence adapter
      -> macOS say narration adapter
```

도메인 모델에는 JPA나 Spring MVC 애노테이션을 넣지 않습니다. 저장 기술과 TTS 제공자 세부 구현은 바깥 어댑터에 격리합니다. 자세한 흐름은 [ARCHITECTURE.md](./ARCHITECTURE.md)를 참고하세요.

## TTS 제공자 교체하기

`MacOsSayNarrationAdapter`는 `SpeechSynthesisPort`를 구현한 로컬 MVP 어댑터입니다. 클라우드 TTS로 교체할 때 콘텐츠 API와 `ContentService`는 변경할 필요가 없습니다.

1. `SpeechSynthesisPort`를 구현하는 새 outbound adapter를 만듭니다.
2. `script`, `NarrationOptions`, `Voice`를 공급자 요청으로 변환합니다.
3. 공급자 결과를 현재 계약인 WAV로 반환하거나 출력 형식 계약을 명시적으로 확장합니다.
4. `NarrationConfiguration`에서 환경 설정에 따라 구현 Bean을 선택합니다.
5. 공급자 API 키는 백엔드 환경 변수나 비밀 저장소에서만 읽습니다.
6. 가짜 공급자 서버 또는 공급자 테스트 도구로 adapter 계약 테스트를 작성합니다.

현재 설정은 `backend/src/main/resources/application.yml`에 있습니다.

| 설정 | 기본값 | 의미 |
|---|---|---|
| `app.narration.command` | `/usr/bin/say` | 실행할 내레이션 명령 |
| `app.narration.base-words-per-minute` | `180` | `speed: 1.0`의 기준 속도 |
| `app.narration.timeout` | `5m` | 동기 합성 프로세스 제한 시간 |
| `app.cors.allowed-origins` | `localhost:5173`, `127.0.0.1:5173` | 허용하는 로컬 프론트 출처 |
| `server.address` | `127.0.0.1` | 이 Mac 내부에만 서버 노출 |

## 테스트와 빌드

백엔드:

```bash
cd backend
./gradlew test
./gradlew build
```

백엔드 테스트는 다음 경계를 나누어 확인합니다.

- `ContentServiceTest`: 콘텐츠 생성·수정·조회 규칙과 저장소 포트
- `NarrationServiceTest`: 저장된 원고 선택, 음성 검증, 합성 포트 호출
- `ContentApiIntegrationTest`: HTTP JSON, 검증, 상태 코드와 JPA 연동
- `MacOsSayNarrationAdapterTest`: 시스템 음성 목록 파싱

단위 테스트에서 실제 음성을 매번 합성하지 않고 가짜 포트를 사용합니다. 실제 `/usr/bin/say` 실행과 청취는 화면 또는 `curl`로 별도 smoke test를 수행합니다.

프론트엔드:

```bash
cd frontend
npm test
npm run build
```

프론트 테스트는 콘텐츠 응답 변환, CRUD 요청, WAV Blob 처리, 카테고리·예상 시간 계산과 음성 기본 선택을 확인합니다.

## Spring 학습 포인트

1. `ContentController`에서 JSON, validation, `201 Created`, `Location` 헤더를 확인합니다.
2. `ContentService`에서 읽기 전용 트랜잭션과 쓰기 트랜잭션의 차이를 확인합니다.
3. `ContentRepositoryPort`와 `JpaContentRepositoryAdapter`를 비교해 의존성 역전을 살펴봅니다.
4. 순수한 `Episode`와 JPA 전용 `ContentJpaEntity`를 분리한 이유를 확인합니다.
5. `NarrationService`가 원고를 요청에서 받지 않고 콘텐츠 저장소에서 조회하는 흐름을 따라갑니다.
6. `SpeechSynthesisPort`와 `MacOsSayNarrationAdapter`를 비교해 제공자 교체 경계를 확인합니다.
7. `@ConfigurationProperties`가 `NarrationProperties`, `CorsProperties`에 연결되는 과정을 살펴봅니다.
8. `ApiExceptionHandler`에서 validation·도메인·외부 시스템 오류가 HTTP 의미로 변환되는 방식을 확인합니다.

## 장문 제작과 YouTube 로드맵

실제 30분~수시간 콘텐츠는 다음 구조로 확장하는 것이 적합합니다.

```text
POST 내레이션 작업 생성 -> 202 Accepted + jobId
  -> 문장 경계와 공급자 제한에 맞춰 원고 분할
  -> 청크별 합성과 부분 재시도
  -> 음량·포맷 정규화와 파일 병합
  -> 객체 저장소 업로드
  -> READY 상태 저장
  -> CDN 또는 Range 요청으로 재생
```

이때 DB에는 오디오 바이너리 대신 작업 상태, 원고 체크섬, 음성 설정, 저장 키, 길이와 오류 정보를 기록합니다. 동일한 원고와 설정은 체크섬으로 재사용해 API 비용을 줄일 수 있습니다. 로컬 `@Async`는 학습용 중간 단계로 사용할 수 있지만, 운영에서는 재시작 후에도 복구되는 작업 큐와 worker가 필요합니다.

YouTube 배포는 내레이션과 분리된 후속 기능입니다.

```text
준비된 내레이션
  -> 배경 이미지·영상, 자막, 챕터 결합
  -> FFmpeg로 영상 렌더링
  -> 사람이 사실·저작권·품질 검수
  -> 비공개 또는 미등록 업로드
  -> 최종 공개
```

초기에는 WAV/MP3, 원고, SRT 자막, 제목·설명 정보를 묶은 내보내기 패키지부터 만드는 것이 안전합니다. 이후 YouTube OAuth, 업로드 재시도와 할당량, 썸네일, 배경음악·이미지 권리, TTS 음성의 상업 이용 조건, 과학 자료 출처와 AI 생성 콘텐츠 고지를 검토해야 합니다.

## 현재 보안과 운영 범위

현재 API에는 로그인, 작성자 권한, 요청 빈도 제한, 동시 합성 제한이 없습니다. 저장한 모든 콘텐츠는 목록 API에 바로 노출되며 초안과 공개 상태도 구분하지 않습니다. 인터넷이나 공용 LAN에 그대로 배포하지 마세요. CORS는 인증이나 보안 경계가 아닙니다.

모바일 실기기에서 이 Mac의 서버를 시험할 때만 `server.address`를 `0.0.0.0`으로 바꾸고, 방화벽과 신뢰할 수 있는 로컬 네트워크를 확인하세요. 테스트가 끝나면 `127.0.0.1`로 되돌리는 것이 안전합니다.
