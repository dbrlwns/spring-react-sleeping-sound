import type { AuthCredentials, AuthSession } from '../domain/auth';
import { ApiError, apiRequest, apiRequestJson, clearCsrfProtection } from './httpClient';

interface RawAuthSession {
  authenticated?: unknown;
  username?: unknown;
}

function toAuthSession(value: unknown): AuthSession {
  if (!value || typeof value !== 'object') {
    throw new ApiError('로그인 상태 응답 형식이 올바르지 않습니다.', 500);
  }

  const raw = value as RawAuthSession;
  if (typeof raw.authenticated !== 'boolean') {
    throw new ApiError('로그인 상태 응답 형식이 올바르지 않습니다.', 500);
  }

  const username = typeof raw.username === 'string' ? raw.username.trim() : null;
  if (raw.authenticated && !username) {
    throw new ApiError('로그인 사용자 정보가 없습니다.', 500);
  }

  return { authenticated: raw.authenticated, username };
}

export async function getAuthSession(signal?: AbortSignal): Promise<AuthSession> {
  const payload = await apiRequestJson<unknown>('/api/v1/auth/session', { signal });
  return toAuthSession(payload);
}

async function authenticate(
  endpoint: 'login' | 'register',
  credentials: AuthCredentials,
): Promise<AuthSession> {
  const payload = await apiRequestJson<unknown>(`/api/v1/auth/${endpoint}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  });
  const session = toAuthSession(payload);
  clearCsrfProtection();
  return session;
}

export function login(credentials: AuthCredentials): Promise<AuthSession> {
  return authenticate('login', credentials);
}

export function register(credentials: AuthCredentials): Promise<AuthSession> {
  return authenticate('register', credentials);
}

export async function logout(): Promise<void> {
  await apiRequest('/api/v1/auth/logout', { method: 'POST' });
  clearCsrfProtection();
}
