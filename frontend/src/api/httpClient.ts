const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? '';
const API_BASE_URL = configuredBaseUrl.replace(/\/$/, '');
const CSRF_ENDPOINT = '/api/v1/auth/csrf';

interface CsrfProtection {
  token: string;
  headerName: string;
}

let csrfProtection: CsrfProtection | null = null;
let csrfRequest: Promise<CsrfProtection> | null = null;

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

async function readErrorMessage(response: Response): Promise<string> {
  const fallback = `요청을 처리하지 못했습니다. (${response.status})`;

  try {
    const contentType = response.headers.get('content-type') ?? '';

    if (contentType.includes('json')) {
      const payload = (await response.json()) as Record<string, unknown>;
      const candidate = payload.detail ?? payload.message ?? payload.title;
      return typeof candidate === 'string' && candidate.trim() ? candidate : fallback;
    }

    const text = await response.text();
    return text.trim() || fallback;
  } catch {
    return fallback;
  }
}

function isAbortError(error: unknown): boolean {
  return (
    typeof DOMException !== 'undefined' &&
    error instanceof DOMException &&
    error.name === 'AbortError'
  );
}

function networkError(error: unknown): Error {
  if (isAbortError(error)) return error as Error;
  return new ApiError('서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.', 0);
}

async function loadCsrfProtection(): Promise<CsrfProtection> {
  let response: Response;

  try {
    response = await fetch(buildApiUrl(CSRF_ENDPOINT), {
      credentials: 'include',
      headers: { Accept: 'application/json' },
    });
  } catch (error) {
    throw networkError(error);
  }

  if (!response.ok) {
    throw new ApiError(await readErrorMessage(response), response.status);
  }

  try {
    const payload = (await response.json()) as Record<string, unknown>;
    const token = typeof payload.token === 'string' ? payload.token.trim() : '';
    const headerName = typeof payload.headerName === 'string' ? payload.headerName.trim() : '';

    if (!token || !headerName) throw new Error('invalid csrf response');
    return { token, headerName };
  } catch {
    throw new ApiError('보안 토큰 응답 형식이 올바르지 않습니다.', 500);
  }
}

/**
 * Spring Security가 세션 인증 성공/로그아웃 시 기존 CSRF 토큰을 교체하므로
 * 인증 경계가 바뀐 직후 캐시를 버리고 다음 변경 요청 전에 다시 발급받습니다.
 */
export function clearCsrfProtection(): void {
  csrfProtection = null;
  csrfRequest = null;
}

export async function ensureCsrfProtection(): Promise<CsrfProtection> {
  if (csrfProtection) return csrfProtection;
  if (!csrfRequest) {
    csrfRequest = loadCsrfProtection()
      .then((loaded) => {
        csrfProtection = loaded;
        return loaded;
      })
      .finally(() => {
        csrfRequest = null;
      });
  }
  return csrfRequest;
}

function requiresCsrfToken(method: string | undefined): boolean {
  return !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes((method ?? 'GET').toUpperCase());
}

/**
 * fetch의 HTTP 오류를 예외로 통일합니다. fetch는 4xx/5xx에도 reject하지 않기 때문에
 * 이 계층에서 성공 여부를 검사하면 각 기능 API에서 같은 코드를 반복하지 않아도 됩니다.
 */
export async function apiRequest(
  path: string,
  init: RequestInit = {},
): Promise<Response> {
  const headers = new Headers(init.headers);

  if (requiresCsrfToken(init.method)) {
    const csrf = await ensureCsrfProtection();
    if (!headers.has(csrf.headerName)) headers.set(csrf.headerName, csrf.token);
  }

  let response: Response;

  try {
    response = await fetch(buildApiUrl(path), {
      ...init,
      credentials: 'include',
      headers,
    });
  } catch (error) {
    throw networkError(error);
  }

  if (!response.ok) {
    throw new ApiError(await readErrorMessage(response), response.status);
  }

  return response;
}

export async function apiRequestJson<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');

  const response = await apiRequest(path, {
    ...init,
    headers,
  });

  return response.json() as Promise<T>;
}
