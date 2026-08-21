const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? '';
const API_BASE_URL = configuredBaseUrl.replace(/\/$/, '');

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

/**
 * fetch의 HTTP 오류를 예외로 통일합니다. fetch는 4xx/5xx에도 reject하지 않기 때문에
 * 이 계층에서 성공 여부를 검사하면 각 기능 API에서 같은 코드를 반복하지 않아도 됩니다.
 */
export async function apiRequest(
  path: string,
  init: RequestInit = {},
): Promise<Response> {
  let response: Response;

  try {
    response = await fetch(buildApiUrl(path), init);
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    throw new ApiError('서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.', 0);
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
  const response = await apiRequest(path, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...init.headers,
    },
  });

  return response.json() as Promise<T>;
}
