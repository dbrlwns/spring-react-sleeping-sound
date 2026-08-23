# 고요한 지식 — Spring + React

과학부터 사회, 역사, 철학까지 다양한 지식 이야기를 저장하고, 잠들기 전에 편안하게 들을 수 있는 내레이션으로 만들어 보는 학습용 웹 서비스입니다.

이 프로젝트의 핵심은 **누구나 둘러볼 수 있는 지식 콘텐츠 보관함과, 로그인한 사용자를 위한 제작 화면**입니다. 저장·수정된 원고는 백그라운드에서 자동으로 WAV가 되며, 상세 화면은 처리 상태와 완성된 오디오를 보여 줍니다. 웹은 REST API만 사용하므로 향후 React Native, Flutter 또는 네이티브 모바일 앱에서도 같은 Spring 백엔드를 재사용할 수 있습니다.

## 현재 MVP에서 할 수 있는 일

- 회원가입, 로그인과 로그아웃
- 로그인 없이 범용 지식 콘텐츠 목록·원고·완성된 오디오 조회
- 세션 인증과 CSRF 보호가 적용된 콘텐츠 작성·수정
- 제목과 소개를 이용한 화면 내 검색
- 카테고리별 화면 필터링
- 콘텐츠 상세 원고와 예상 청취 시간 확인
- 새 콘텐츠 작성 및 기존 콘텐츠 수정
- H2 파일 데이터베이스에 콘텐츠 영속 저장
- 저장·수정 commit 후 선택한 TTS 제공자로 내레이션 자동 생성
- `PENDING`, `PROCESSING`, `READY`, `FAILED` 처리 상태 확인
- 브라우저에서 내레이션 재생 및 파일 다운로드

첫 실행 시 양자역학과 우주론 예시 콘텐츠가 빈 데이터베이스에 자동으로 추가됩니다.

## 기술 구성

| 영역 | 기술 | 역할 |
|---|---|---|
| 백엔드 | Java 17, Spring Boot 4.1.0, Spring Security, Gradle | 세션 인증, 콘텐츠 CRUD, 자동 내레이션 상태·오디오 API |
| 저장소 | Spring Data JPA, H2 file database | 계정, 콘텐츠, 내레이션 상태와 WAV를 로컬 파일 DB에 보존 |
| 프론트엔드 | React, TypeScript, Vite | 인증 화면, 보관함, 원고 편집기, 자동 처리 상태와 재생 전용 패널 |
| 내레이션 제공자 | 기본 macOS `/usr/bin/say`, 선택 Google Cloud Chirp 3: HD | 같은 포트 뒤에서 저장된 원고를 WAV 음성으로 합성 |

## 먼저 준비할 것

- JDK 17 이상
- Node.js 20.19 이상인 20.x, 또는 22.12 이상과 npm
- 기본 제공자인 `macos-say`를 사용할 macOS와 `/usr/bin/say`

설치 상태를 확인합니다.

```bash
java -version
node --version
npm --version
say -v '?'
```

`say -v '?'`는 기본 `macos-say` 제공자가 사용할 수 있는 음성 목록을 보여 줍니다. 한국어 음성이 없다면 macOS의 **시스템 설정 > 손쉬운 사용 > 읽기 콘텐츠 > 시스템 음성**에서 내려받을 수 있습니다. Google Cloud를 설정하지 않아도 기본 로컬 제공자로 프로젝트를 실행하고 학습할 수 있습니다.

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

브라우저에서 `http://localhost:5173`을 엽니다. 개발 중에는 Vite가 `/api` 요청을 `http://127.0.0.1:8080`으로 프록시합니다. 이 프록시는 로컬 개발 편의 기능이며 운영 토폴로지가 아닙니다. 운영에서는 배포된 정적 프론트와 API를 동일 오리진의 reverse proxy로 묶거나, 아래의 명시적 API base URL을 사용해 별도로 배포합니다.

다른 주소의 API를 사용하려면 `frontend/.env.example`을 참고해 `frontend/.env.local`을 만들 수 있습니다.

```dotenv
VITE_API_BASE_URL=https://api.example.com
```

`VITE_`로 시작하는 값은 브라우저 번들에 노출되므로 API 키나 비밀번호를 넣으면 안 됩니다.

## 화면 사용 흐름

현재 React 앱은 별도 URL 라우터 없이 하나의 SPA 안에서 다음 화면을 전환합니다.

```text
익명 방문
  -> 이야기 보관함
      -> 콘텐츠 상세 및 원고 읽기
          -> 자동 내레이션 상태 확인
          -> READY이면 WAV 재생·다운로드

명시적으로 로그인을 선택하거나 새 이야기·원고 편집을 시도
  -> 로그인 또는 회원가입
  -> 제목·소개·카테고리·원고 작성·수정
  -> 저장 후 상세 화면
  -> 서버가 비동기 내레이션 작업 시작
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
| `SCIENCE` | 과학 |
| `SOCIETY` | 사회 |
| `HISTORY` | 역사 |
| `PHILOSOPHY` | 철학 |
| `ECONOMY` | 경제 |
| `TECHNOLOGY` | 기술 |
| `CULTURE` | 문화 |
| `PSYCHOLOGY` | 심리 |

새로 작성하거나 수정하는 콘텐츠는 이 8개 코드를 사용합니다. 기존 H2 데이터의 `QUANTUM_PHYSICS`, `COSMOLOGY`, `ASTRONOMY`, `GENERAL_SCIENCE`는 호환성을 위해 계속 읽으며 API와 화면에서 `SCIENCE`(과학)로 정규화합니다. 따라서 기존 DB를 초기화하지 않아도 과학 필터에 함께 표시됩니다.

지원하지 않는 코드가 요청에 포함되면 `400 Bad Request`가 반환됩니다.

## REST API

### 인증과 CSRF

목록, 상세 원고, 내레이션 상태와 READY 오디오를 읽는 다음 `GET`은 로그인 없이 사용할 수 있습니다.

```http
GET /api/v1/contents
GET /api/v1/contents/{contentId}
GET /api/v1/contents/{contentId}/narration/status
GET /api/v1/contents/{contentId}/narration/audio
```

콘텐츠 `POST`·`PUT`과 진단용 음성 목록 API는 로그인 세션이 필요합니다. 세션 cookie를 쓰는 `POST`, `PUT`, 로그아웃 같은 상태 변경 요청은 CSRF 토큰도 필요하며, 보호 쓰기에 인증이 없으면 `401 Unauthorized`, 토큰이 없거나 틀리면 `403 Forbidden`입니다. 회원가입·로그인은 인증 진입점이지만 상태를 변경하므로 CSRF 검사를 받습니다.

```http
GET  /api/v1/auth/csrf
GET  /api/v1/auth/session
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
```

CSRF 응답은 토큰과 실제 헤더 이름을 함께 반환하고 `XSRF-TOKEN` 쿠키도 만듭니다.

```json
{
  "token": "...",
  "headerName": "X-XSRF-TOKEN"
}
```

회원가입과 로그인 요청은 같은 형식입니다. 사용자 이름은 3~50자의 문자·숫자·마침표·밑줄·하이픈만 허용합니다. 비밀번호는 8~72자이면서 BCrypt 경계인 UTF-8 72바이트 이하여야 하며, DB에는 BCrypt hash로만 저장됩니다.

```json
{
  "username": "quiet.reader",
  "password": "at-least-eight-characters"
}
```

브라우저 클라이언트는 모든 fetch에 `credentials: "include"`를 사용하고, 변경 요청 전에 `/csrf` 응답의 토큰을 해당 헤더에 자동으로 넣습니다. `curl`에서는 cookie jar와 토큰을 함께 관리해야 합니다.

```bash
# 1. 응답 JSON의 token 값을 복사합니다.
curl --cookie-jar cookies.txt http://localhost:8080/api/v1/auth/csrf

# 2. 회원가입은 즉시 로그인 세션도 만듭니다. 기존 계정이면 /login을 사용합니다.
curl --request POST http://localhost:8080/api/v1/auth/register \
  --cookie cookies.txt --cookie-jar cookies.txt \
  --header 'Content-Type: application/json' \
  --header 'X-XSRF-TOKEN: <CSRF_TOKEN>' \
  --data '{"username":"quiet.reader","password":"at-least-eight-characters"}'

# 3. 인증 경계가 바뀐 뒤 이후 쓰기 요청용 토큰을 다시 확인합니다.
curl --cookie cookies.txt --cookie-jar cookies.txt \
  http://localhost:8080/api/v1/auth/csrf
```

현재 세션은 `{ "authenticated": true, "username": "quiet.reader" }` 또는 익명 응답 `{ "authenticated": false, "username": null }`로 확인합니다. 로그아웃은 현재 토큰 헤더와 cookie를 포함한 `POST`이며 성공 시 `204 No Content`입니다.

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
      "category": "SCIENCE",
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
  --cookie cookies.txt \
  --header 'Content-Type: application/json' \
  --header 'X-XSRF-TOKEN: <CSRF_TOKEN>' \
  --data '{
    "title": "별은 어떻게 태어날까",
    "summary": "차가운 성간 구름에서 별이 시작되는 과정을 천천히 살펴봅니다.",
    "category": "SCIENCE",
    "script": "편안히 눈을 감고, 아주 넓고 차가운 우주의 구름을 떠올려 봅시다."
  }'
```

성공하면 `201 Created`, 생성된 콘텐츠 상세 정보와 `Location` 헤더가 반환됩니다. 같은 트랜잭션에서 내레이션 상태가 `PENDING`으로 초기화되고, commit 후 백그라운드 합성이 시작됩니다.

### 콘텐츠 수정

```http
PUT /api/v1/contents/{contentId}
Content-Type: application/json
```

`PUT` 요청은 제목, 소개, 카테고리, 원고를 모두 전달하며 cookie와 CSRF 헤더가 필요합니다. 존재하지 않는 UUID는 `404 Not Found`입니다. 수정하는 즉시 기존 WAV는 더 이상 `READY`로 제공되지 않고 새 세대가 `PENDING`에서 다시 시작합니다.

콘텐츠 입력 제한:

| 필드 | 규칙 |
|---|---|
| `title` | 공백 제외 필수, 최대 120자 |
| `summary` | 공백 제외 필수, 최대 500자 |
| `category` | 8가지 범용 카테고리 코드 중 하나 |
| `script` | 공백 제외 필수, 최대 20,000자 |

### 자동 내레이션 상태

콘텐츠를 저장하거나 수정한 뒤 클라이언트는 별도의 생성 `POST`를 보내지 않습니다. 상세 화면은 다음 상태 API를 약 2초 간격으로 조회합니다. 기존 seed처럼 상태 행이 없는 콘텐츠도 첫 상태 조회에서 `PENDING` 상태가 만들어지고 합성이 예약됩니다.

```http
GET /api/v1/contents/{contentId}/narration/status
```

```json
{
  "status": "PROCESSING",
  "errorMessage": null,
  "updatedAt": "2026-08-23T08:00:00Z",
  "audioUrl": null
}
```

| 상태 | 의미 |
|---|---|
| `PENDING` | 저장은 끝났고 worker 시작을 기다리는 상태 |
| `PROCESSING` | 선택된 TTS 제공자가 원고를 합성하는 상태 |
| `READY` | WAV가 DB에 저장되어 재생 가능한 상태; `audioUrl`도 반환 |
| `FAILED` | 합성 실패; `errorMessage`로 화면에 안내 |

worker의 제품 기본 속도는 `0.9`입니다. 제공자가 반환한 목록에서 첫 한국어 음성을 고르고, 없으면 첫 음성을 사용합니다. `macos-say`는 설치된 음성 목록을 반환하고, `google-chirp3`는 설정한 `ko-KR` Chirp 3: HD 음성 하나를 반환해 그 이름과 속도를 요청에 명시합니다. Chirp 3 전용 문서에서 pace control의 `speaking_rate` 범위는 0.25~2.0이지만 **Preview** 기능이므로, 이 프로젝트는 고정 `0.9`만 사용하며 화면에 속도 설정을 노출하지 않습니다. 배포 전 [Chirp 3: HD voice controls](https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd)을 다시 확인하세요. `GET /api/v1/narration/voices`는 선택된 adapter를 진단하는 계약으로 남아 있지만 React 상세 화면은 이를 호출하거나 음성 선택 UI를 제공하지 않습니다.

### 준비된 내레이션 오디오

상태가 `READY`일 때만 오디오를 요청합니다.

```http
GET /api/v1/contents/{contentId}/narration/audio
Accept: audio/wav
```

```bash
curl --header 'Accept: audio/wav' \
  http://localhost:8080/api/v1/contents/47ef2924-4ddd-45f1-9c9c-76fb2ba11001/narration/audio \
  --output narration.wav
```

성공하면 `200 audio/wav`, `Cache-Control: no-store`와 다운로드 파일명이 반환됩니다. 아직 `READY`가 아니면 현재 상태를 포함한 `409 Conflict`입니다. 브라우저는 WAV를 `Blob` URL로 바꿔 재생·다운로드하고 상세 화면을 벗어나면 URL을 해제합니다.

## 현재 내레이션의 범위

현재 MVP는 저장 트랜잭션과 합성을 분리한 in-process 비동기 흐름을 사용합니다.

```text
콘텐츠 저장 + PENDING 기록 -> commit -> @Async worker -> PROCESSING
  -> 선택된 TTS adapter 호출 -> READY/FAILED 기록 -> status polling -> READY audio GET
```

HTTP 저장 요청은 합성 완료를 기다리지 않으며, 조회 HTTP thread도 합성을 직접 수행하지 않습니다. 원고의 `updatedAt`과 generation ID를 함께 저장해 수정 전의 늦은 작업이 최신 WAV를 덮어쓰지 못하게 합니다. 그러나 `@Async`는 내구성 있는 작업 큐가 아니므로 긴 수면 콘텐츠를 반복적으로 생성하는 운영 구조는 아닙니다.

Google Cloud의 동기 합성은 요청 하나당 UTF-8 입력이 최대 5,000 bytes입니다. `GoogleChirp3NarrationAdapter`는 문자 중간을 자르지 않고 설정된 byte 한도에 맞춰 원고를 여러 요청으로 나눕니다. 각 `LINEAR16` 응답에는 이미 WAV 헤더가 있으므로 첫 헤더를 그대로 이어 붙이지 않고 PCM 형식을 확인한 뒤 data chunk들을 합쳐 하나의 RIFF/WAV를 다시 만듭니다. 이는 한 worker 안에서의 기술적 분할이며, 내구성 있는 장문 작업이나 부분 재개 기능은 아닙니다. [Google Cloud 입력 한도](https://cloud.google.com/text-to-speech/quotas)와 [LINEAR16 형식](https://cloud.google.com/text-to-speech/docs/reference/rest/v1/AudioEncoding)은 변경될 수 있으므로 운영 전 다시 확인해야 합니다.

- `PENDING`은 상태 재조회로 다시 예약되지만 서버가 `PROCESSING` 중 종료되면 lease·복구 정책이 없어 멈출 수 있습니다.
- 작업 큐, 청크별 상태·부분 재개, 진행률, 취소 API와 worker 동시성 제한이 없습니다.
- WAV 전체를 서버 byte 배열과 H2 LOB, 브라우저 Blob으로 보관합니다.
- 원고 수정마다 새 합성을 시작하며 script checksum 기반 재사용은 없습니다.
- Google 분할 호출 수는 `max-chunks`로 제한되며, 한도를 넘으면 작업 전체가 `FAILED`가 됩니다.

따라서 현재 기능은 **짧은 원고를 위한 자동 비동기 MVP**로 이해해야 합니다. 장문 운영 구조는 아래 로드맵처럼 durable queue, 영속 청크 상태와 object storage가 필요합니다.

## 오류 응답

API 오류는 RFC 9457 `application/problem+json` 형태로 반환됩니다.

| 상황 | 상태 코드 |
|---|---|
| 필수값 누락, 길이 초과, 잘못된 카테고리·계정 형식 | `400 Bad Request` |
| 로그인 실패 또는 쓰기·진단 API의 익명 요청 | `401 Unauthorized` |
| CSRF 토큰 누락·불일치 | `403 Forbidden` |
| UUID 형식이 잘못된 경로 | `400 Bad Request` |
| 존재하지 않는 콘텐츠 | `404 Not Found` |
| 중복 사용자 이름, 준비되지 않은 오디오 | `409 Conflict` |
| 로컬 명령 또는 Google Cloud 합성 실패 | 상태가 `FAILED`로 저장됨 |
| 예상하지 못한 서버 오류 | `500 Internal Server Error` |

스택 트레이스와 내부 예외 정보는 HTTP 응답에 노출하지 않고 서버 로그에 남깁니다.

## 프로젝트 구조

```text
tts-spring-react-demo/
├── backend/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew
│   ├── data/                         실행 후 생성되는 H2 데이터
│   └── src/main/java/com/example/sleepknowledge/
│       ├── adapter/in/web/           인증·콘텐츠·내레이션 Controller, DTO, 오류 처리
│       ├── adapter/out/persistence/  JPA entity, Spring Data, repository adapter, seed
│       ├── adapter/out/narration/    macOS say와 Google Chirp 3 내레이션 adapter
│       ├── application/port/in/      콘텐츠와 내레이션 유스케이스 계약
│       ├── application/port/out/     저장소와 음성 합성 포트
│       ├── application/service/      콘텐츠·자동 내레이션 흐름과 비동기 worker
│       ├── authentication/           로컬 계정, BCrypt 인증과 JPA repository
│       ├── domain/model/             Episode, NarrationState, Status, Voice 등
│       └── config/                   Security, CSRF, CORS, TTS 설정 조립
├── frontend/
│   └── src/
│       ├── api/                      인증·콘텐츠·상태·오디오 HTTP 호출
│       ├── components/               인증, 보관함, 편집기, 재생 전용 패널
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
      -> 설정으로 선택된 macOS say 또는 Google Chirp 3 adapter
```

도메인 모델에는 JPA나 Spring MVC 애노테이션을 넣지 않습니다. 저장 기술과 TTS 제공자 세부 구현은 바깥 어댑터에 격리합니다. 자세한 흐름은 [ARCHITECTURE.md](./ARCHITECTURE.md)를 참고하세요.

## TTS 제공자 선택하기

두 adapter는 같은 `SpeechSynthesisPort`를 구현합니다. 기본 `macos-say`는 별도 클라우드 계정 없이 로컬 학습에 사용하고, `google-chirp3`는 Google Cloud Text-to-Speech의 한국어 Chirp 3: HD 음성을 사용합니다. Controller, `ContentService`, 상태 API와 React는 어느 구현이 선택됐는지 알 필요가 없습니다.

`NarrationConfiguration`의 `@ConditionalOnProperty`가 `app.narration.provider` 값에 맞는 Bean 하나만 조립합니다. Google을 선택했을 때만 `TextToSpeechClient`를 만들기 때문에 기본 실행에서는 ADC나 네트워크 연결을 요구하지 않습니다. Google client Bean은 Spring 종료 시 `close()`되어 channel을 정리하며, 테스트에서는 `@ConditionalOnMissingBean` 경계로 가짜 client를 주입할 수 있습니다.

현재 설정은 `backend/src/main/resources/application.yml`에 있습니다.

| 설정 | 기본값 | 의미 |
|---|---|---|
| `app.narration.provider` | `macos-say` | `macos-say` 또는 `google-chirp3` 중 사용할 adapter |
| `app.narration.macos-say.command` | `/usr/bin/say` | 로컬에서 실행할 명령 |
| `app.narration.macos-say.base-words-per-minute` | `180` | worker의 속도 `0.9`를 `say -r`로 환산하는 기준 |
| `app.narration.macos-say.timeout` | `5m` | 로컬 프로세스 제한 시간 |
| `app.narration.google-chirp3.endpoint` | `texttospeech.googleapis.com:443` | Google Cloud Text-to-Speech endpoint |
| `app.narration.google-chirp3.language-code` | `ko-KR` | 요청의 BCP-47 언어 코드이자 음성 이름 검증 기준 |
| `app.narration.google-chirp3.voice-name` | `ko-KR-Chirp3-HD-Kore` | `${language-code}-Chirp3-HD-`로 시작해야 하는 음성 이름 |
| `app.narration.google-chirp3.max-input-bytes` | `5000` | 동기 요청 한 청크의 UTF-8 byte 상한; Google 한도보다 크게 설정할 수 없음 |
| `app.narration.google-chirp3.max-chunks` | `32` | 원고 하나가 만들 수 있는 최대 원격 요청 수 |
| `app.narration.google-chirp3.max-audio-bytes` | `268435456` | 병합할 PCM data의 최대 크기(256 MiB); JVM/H2 메모리 보호 |
| `app.narration.google-chirp3.rpc-timeout` | `30s` | 자동 재시도 없이 각 Google 합성 RPC에 적용할 timeout |
| `app.cors.allowed-origins` | `localhost:5173`, `127.0.0.1:5173` | credential과 CSRF 헤더를 허용하는 로컬 출처 |
| `server.address` | `127.0.0.1` | 이 Mac 내부에만 서버 노출 |

Spring Boot의 relaxed binding을 사용하므로 다음 환경 변수처럼 저장소 밖에서 값을 바꿀 수 있습니다.

```bash
export APP_NARRATION_PROVIDER=google-chirp3
export APP_NARRATION_GOOGLE_CHIRP3_LANGUAGE_CODE=ko-KR
export APP_NARRATION_GOOGLE_CHIRP3_VOICE_NAME=ko-KR-Chirp3-HD-Kore
./gradlew bootRun
```

provider 값, 양수가 아닌 timeout·청크/오디오 제한, 5,000을 넘는 입력 byte 설정과 언어에 맞지 않는 Chirp 3 음성 이름은 애플리케이션 부팅 때 실패합니다. Google 합성은 비용과 중복 결과를 숨긴 채 반복하지 않도록 client 자동 재시도를 끈 상태입니다. 일시적인 실패도 현재 generation은 `FAILED`가 되며, 운영용 재시도는 향후 job ID·backoff·idempotency와 함께 설계해야 합니다.

### Google Chirp 3 준비

1. Google Cloud 프로젝트에 billing을 연결하고 Cloud Text-to-Speech API를 활성화합니다. 시작 조건은 [공식 시작 안내](https://cloud.google.com/text-to-speech/docs/get-started)를 확인합니다.
2. 로컬 개발 환경에서는 Google Cloud CLI로 Application Default Credentials를 만듭니다.

   ```bash
   gcloud auth application-default login
   gcloud auth application-default set-quota-project PROJECT_ID
   ```

3. 위 환경 변수로 `google-chirp3`를 선택하고 실행합니다. 한국어 Chirp 3: HD 음성 이름은 [지원 음성 목록](https://cloud.google.com/text-to-speech/docs/voices)에서 확인해 바꿀 수 있습니다.

Java client library는 [ADC를 자동 탐색](https://cloud.google.com/text-to-speech/docs/authentication)합니다. API key나 credential JSON 본문을 JavaScript의 `VITE_` 변수, Git 저장소 또는 `application.yml`에 넣지 마세요. Google Cloud 환경에서는 실행 리소스에 최소 권한 service account를 연결하고, 외부 환경에서는 가능하면 Workload Identity Federation 같은 keyless 방식을 사용합니다.

Cloud Text-to-Speech는 billing과 요청 quota가 적용되며 가격은 바뀔 수 있습니다. 배포 전 [현재 가격](https://cloud.google.com/text-to-speech/pricing)과 [quota 및 5,000-byte 제한](https://cloud.google.com/text-to-speech/quotas)을 확인하세요. 특히 이 MVP의 공개 status GET은 상태 행이 없거나 원고 버전이 달라졌을 때 `PENDING`을 만들고 작업을 재예약할 수 있습니다. 익명 호출이 과금되는 Google 요청으로 이어질 수 있으므로 rate limit, bounded worker, idempotency/checksum, 사용자별 quota와 예산 알림 없이 인터넷에 공개하면 안 됩니다.

## 테스트와 빌드

백엔드:

```bash
cd backend
./gradlew test
./gradlew build
```

백엔드 테스트는 다음 경계를 나누어 확인합니다.

- `ContentServiceTest`: 콘텐츠 생성·수정·조회 규칙과 저장소 포트
- `NarrationServiceTest`: 상태 생성·재예약, 원고 버전 무효화, READY 오디오 규칙
- `NarrationGenerationWorkerTest`: 기본 음성 선택, READY/FAILED 전이와 오래된 결과 차단
- `AuthApiIntegrationTest`: 가입·로그인·로그아웃, 세션, 실제 CSRF cookie/header와 익명 쓰기의 401/403
- `ContentApiIntegrationTest`: 익명 목록·상세·status·audio GET, 인증된 생성·수정, commit 후 비동기 생성과 JPA 연동
- `MacOsSayNarrationAdapterTest`: 시스템 음성 목록 파싱
- `NarrationPropertiesTest`, `NarrationConfigurationTest`: 기본 로컬 제공자, relaxed binding과 조건부 Bean 선택
- `Utf8TextChunkerTest`, `Linear16WavTest`: Unicode·UTF-8 분할과 RIFF/fmt/data 검증·PCM 병합
- `GoogleChirp3NarrationAdapterTest`: 설정 음성·속도 요청, 최대 청크와 안전한 provider 실패 경계

단위 테스트에서 실제 음성을 매번 합성하지 않고 가짜 포트나 client를 사용합니다. 실제 `/usr/bin/say` 실행과 Google Cloud 호출·청취는 제공자별 smoke test로 분리합니다. Google smoke test는 ADC, API 활성화와 billing이 필요하고 실제 quota·비용을 사용하므로 기본 테스트에서 자동 실행하지 않습니다.

프론트엔드:

```bash
cd frontend
npm test
npm run build
```

프론트 테스트는 세션 확인 중에도 공개 보관함을 보여 주는지, 익명 헤더와 요청형 로그인 화면, CSRF 선발급과 credential/header 전송, 콘텐츠 CRUD, 자동 내레이션 status/audio 변환, WAV Blob과 카테고리·예상 시간 계산을 확인합니다.

## Spring 학습 포인트

1. `ContentController`에서 JSON, validation, `201 Created`, `Location` 헤더를 확인합니다.
2. `ContentService`에서 읽기 전용 트랜잭션과 쓰기 트랜잭션의 차이를 확인합니다.
3. `ContentRepositoryPort`와 `JpaContentRepositoryAdapter`를 비교해 의존성 역전을 살펴봅니다.
4. 순수한 `Episode`와 JPA 전용 `ContentJpaEntity`를 분리한 이유를 확인합니다.
5. `SecurityConfiguration`과 `AuthController`에서 세션 저장, BCrypt, CSRF와 JSON 401/403 경계를 확인합니다.
6. `ContentService`가 commit할 데이터와 내레이션 이벤트를 함께 만들고 `@TransactionalEventListener(AFTER_COMMIT)`이 worker로 넘기는 흐름을 따라갑니다.
7. `NarrationService`의 status/audio 조회와 `NarrationGenerationWorker`의 TTS 실행 책임을 비교합니다.
8. `SpeechSynthesisPort`, 두 adapter와 `NarrationConfiguration`을 비교해 `@ConditionalOnProperty`가 구현을 선택하는 composition root를 확인합니다.
9. `NarrationProperties`의 중첩 `@ConfigurationProperties`가 YAML과 환경 변수를 타입·기본값·검증으로 바꾸는 과정을 확인합니다.
10. `GoogleChirp3NarrationAdapter`에서 Google SDK 타입을 port 밖에 격리하고, UTF-8 청크와 RIFF/WAV를 애플리케이션 계약으로 변환하는 과정을 확인합니다.
11. `ApiExceptionHandler`에서 validation·인증·도메인 오류가 HTTP 의미로 변환되는 방식을 확인합니다.

## 장문 제작과 YouTube 로드맵

현재의 `@Async` 자동 합성은 저장 HTTP 요청에서 TTS를 분리하는 학습용 중간 단계입니다. Google adapter도 5,000-byte 제한에 맞춰 한 worker 안에서만 요청을 나누고 메모리에서 WAV를 합칠 뿐, 청크 상태를 저장하거나 재시작 후 이어서 처리하지는 않습니다. 실제 30분~수시간 콘텐츠는 다음 구조로 확장하는 것이 적합합니다.

```text
POST 내레이션 작업 생성 -> 202 Accepted + jobId
  -> 문장 경계와 공급자 제한에 맞춰 원고 분할 및 청크 상태 저장
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

초기에는 WAV/MP3, 원고, SRT 자막, 제목·설명 정보를 묶은 내보내기 패키지부터 만드는 것이 안전합니다. 이후 YouTube OAuth, 업로드 재시도와 할당량, 썸네일, 배경음악·이미지 권리, TTS 음성의 상업 이용 조건, 원고 자료 출처와 AI 생성 콘텐츠 고지를 검토해야 합니다.

## 현재 보안과 운영 범위

현재 API에는 Spring Security 세션 로그인과 CSRF 방어가 있습니다. 비밀번호는 BCrypt hash로 저장되고 콘텐츠 작성·수정은 인증과 CSRF가 필요합니다.

반면 **목록, 상세 원고, 내레이션 상태와 완성된 WAV는 누구나 조회할 수 있는 공개 콘텐츠**입니다. 이는 첫 화면부터 이야기를 둘러보게 하려는 현재 MVP의 제품 결정이며, 비공개 원고나 유료 오디오를 보관하는 경계로 사용하면 안 됩니다. 게시·초안 상태와 별도 공개 권한이 필요하면 모델과 조회 정책을 먼저 추가해야 합니다.

또한 계정과 콘텐츠 소유권이 아직 연결되지 않아 **어느 로그인 사용자든 전체 콘텐츠를 수정**할 수 있습니다. 요청 빈도, 로그인 brute-force 방어, 사용자별 quota, worker 동시 합성 제한, 초안·공개 구분도 없습니다. `google-chirp3`에서는 공개 status GET도 조건에 따라 외부 합성을 재예약해 비용을 만들 수 있습니다. 인터넷이나 공용 LAN에 그대로 배포하지 마세요. CORS는 인증이나 권한 경계가 아닙니다.

모바일 실기기에서 이 Mac의 서버를 시험할 때만 `server.address`를 `0.0.0.0`으로 바꾸고, 방화벽과 신뢰할 수 있는 로컬 네트워크를 확인하세요. 테스트가 끝나면 `127.0.0.1`로 되돌리는 것이 안전합니다.
