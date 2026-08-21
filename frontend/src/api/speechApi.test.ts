import { afterEach, describe, expect, it, vi } from 'vitest';
import { getVoices } from './speechApi';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('getVoices', () => {
  it('서버 응답을 화면에서 사용하는 Voice 모델로 변환한다', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          voices: [
            {
              id: 'ko-female-1',
              displayName: '다정한 목소리',
              locale: 'ko-KR',
            },
          ],
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(getVoices()).resolves.toEqual([
      { id: 'ko-female-1', name: '다정한 목소리', language: 'ko-KR' },
    ]);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/narration/voices',
      expect.objectContaining({ headers: expect.objectContaining({ Accept: 'application/json' }) }),
    );
  });

  it('사용할 수 있는 음성이 없으면 의미 있는 오류를 반환한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );

    await expect(getVoices()).rejects.toMatchObject({ status: 404 });
  });
});
