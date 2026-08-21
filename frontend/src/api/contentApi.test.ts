import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  createContent,
  createNarration,
  getContent,
  getContents,
  updateContent,
} from './contentApi';

const detail = {
  id: 'f29b975e-4c42-4f08-a765-02d729695d69',
  title: '달은 바라볼 때만 존재할까',
  summary: '관측과 양자 세계를 천천히 살펴봅니다.',
  category: 'QUANTUM_PHYSICS',
  script: '아인슈타인은 이런 질문을 던졌습니다. 달은 우리가 바라볼 때만 존재할까요?',
  createdAt: '2026-08-20T00:00:00Z',
  updatedAt: '2026-08-20T00:00:00Z',
};

afterEach(() => vi.unstubAllGlobals());

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
        category: 'QUANTUM_PHYSICS',
        estimatedMinutes: 0,
      }),
    ]);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/contents',
      expect.objectContaining({ headers: expect.objectContaining({ Accept: 'application/json' }) }),
    );
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
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(detail), {
        status: method === 'POST' ? 201 : 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const draft = {
      title: detail.title,
      summary: detail.summary,
      category: detail.category,
      script: detail.script,
    };

    await expect(action(draft)).resolves.toEqual(expect.objectContaining({ id: detail.id }));
    expect(fetchMock).toHaveBeenCalledWith(
      path,
      expect.objectContaining({ method, body: JSON.stringify(draft) }),
    );
  });

  it('콘텐츠에 종속된 나레이션 endpoint로 설정만 전송한다', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(new Uint8Array([82, 73, 70, 70]), {
        status: 200,
        headers: { 'Content-Type': 'audio/wav' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(createNarration(detail.id, { voiceId: 'Yuna', speed: 0.9 })).resolves.toHaveProperty('size', 4);
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/v1/contents/${detail.id}/narration`,
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ voiceId: 'Yuna', speed: 0.9 }),
      }),
    );
  });
});
