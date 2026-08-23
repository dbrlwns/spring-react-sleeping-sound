import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  createContent,
  getContent,
  getContents,
  getNarrationAudio,
  getNarrationStatus,
  updateContent,
} from './contentApi';
import { clearCsrfProtection } from './httpClient';

const detail = {
  id: 'f29b975e-4c42-4f08-a765-02d729695d69',
  title: '달은 바라볼 때만 존재할까',
  summary: '관측과 양자 세계를 천천히 살펴봅니다.',
  category: 'QUANTUM_PHYSICS',
  script: '아인슈타인은 이런 질문을 던졌습니다. 달은 우리가 바라볼 때만 존재할까요?',
  createdAt: '2026-08-20T00:00:00Z',
  updatedAt: '2026-08-20T00:00:00Z',
};

afterEach(() => {
  clearCsrfProtection();
  vi.unstubAllGlobals();
});

describe('content API', () => {
  it('목록 envelope를 가벼운 도메인 모델로 변환한다', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ contents: [{ ...detail, script: undefined }] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(getContents()).resolves.toEqual([
      expect.objectContaining({
        id: detail.id,
        title: detail.title,
        category: 'SCIENCE',
        estimatedMinutes: 0,
      }),
    ]);
    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(path).toBe('/api/v1/contents');
    expect(init.credentials).toBe('include');
    expect(new Headers(init.headers).get('Accept')).toBe('application/json');
  });

  it('상세 원고를 조회하고 분량을 계산한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(detail), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );
    await expect(getContent(detail.id)).resolves.toEqual(
      expect.objectContaining({ id: detail.id, script: detail.script, estimatedMinutes: 1 }),
    );
  });

  it.each([
    ['POST', createContent, '/api/v1/contents'],
    ['PUT', (draft: Parameters<typeof updateContent>[1]) => updateContent(detail.id, draft), `/api/v1/contents/${detail.id}`],
  ] as const)('%s 요청으로 원고를 저장한다', async (method, action, path) => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: 'csrf-token', headerName: 'X-XSRF-TOKEN' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(detail), {
          status: method === 'POST' ? 201 : 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    vi.stubGlobal('fetch', fetchMock);
    const draft = {
      title: detail.title,
      summary: detail.summary,
      category: 'SCIENCE',
      script: detail.script,
    };

    await expect(action(draft)).resolves.toEqual(expect.objectContaining({ id: detail.id }));
    const [requestPath, init] = fetchMock.mock.calls[1] as [string, RequestInit];
    expect(requestPath).toBe(path);
    expect(init).toEqual(expect.objectContaining({ method, body: JSON.stringify(draft), credentials: 'include' }));
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('csrf-token');
  });

  it('자동 내레이션 처리 상태를 조회한다', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        status: 'PROCESSING',
        updatedAt: '2026-08-23T01:00:00Z',
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(getNarrationStatus(detail.id)).resolves.toEqual({
      status: 'PROCESSING',
      updatedAt: '2026-08-23T01:00:00Z',
    });
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      `/api/v1/contents/${detail.id}/narration/status`,
    );
  });

  it('READY 상태에서 사용할 WAV 오디오를 조회한다', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(new Uint8Array([82, 73, 70, 70]), {
        status: 200,
        headers: { 'Content-Type': 'audio/wav' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(getNarrationAudio(detail.id)).resolves.toHaveProperty('size', 4);
    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(path).toBe(`/api/v1/contents/${detail.id}/narration/audio`);
    expect(init.method).toBeUndefined();
    expect(init.credentials).toBe('include');
    expect(new Headers(init.headers).get('Accept')).toBe('audio/wav');
  });
});
