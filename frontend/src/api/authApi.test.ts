import { afterEach, describe, expect, it, vi } from 'vitest';
import { getAuthSession, login, logout, register } from './authApi';
import { clearCsrfProtection } from './httpClient';

afterEach(() => {
  clearCsrfProtection();
  vi.unstubAllGlobals();
});

describe('auth API', () => {
  it('세션 쿠키를 포함해 현재 로그인 상태를 조회한다', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ authenticated: false, username: null }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(getAuthSession()).resolves.toEqual({ authenticated: false, username: null });
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/auth/session',
      expect.objectContaining({ credentials: 'include' }),
    );
  });

  it.each([
    ['login', login],
    ['register', register],
  ] as const)('%s 전에 CSRF 토큰을 받고 세션 쿠키와 헤더를 전송한다', async (endpoint, action) => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: 'csrf-token', headerName: 'X-XSRF-TOKEN' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ authenticated: true, username: 'quiet-user' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    vi.stubGlobal('fetch', fetchMock);
    const credentials = { username: 'quiet-user', password: 'long-password' };

    await expect(action(credentials)).resolves.toEqual({
      authenticated: true,
      username: 'quiet-user',
    });
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/v1/auth/csrf');
    const [path, init] = fetchMock.mock.calls[1] as [string, RequestInit];
    expect(path).toBe(`/api/v1/auth/${endpoint}`);
    expect(init).toEqual(expect.objectContaining({
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify(credentials),
    }));
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('csrf-token');
  });

  it('로그아웃 요청도 CSRF 보호를 적용한다', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: 'logout-token', headerName: 'X-XSRF-TOKEN' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(logout()).resolves.toBeUndefined();
    const [path, init] = fetchMock.mock.calls[1] as [string, RequestInit];
    expect(path).toBe('/api/v1/auth/logout');
    expect(init.credentials).toBe('include');
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('logout-token');
  });
});
