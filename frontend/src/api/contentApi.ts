import type {
  ContentDraft,
  KnowledgeContent,
  KnowledgeContentSummary,
} from '../domain/content';
import { estimateListeningMinutes } from '../domain/content';
import type { NarrationOptions } from '../domain/speech';
import { ApiError, apiRequest, apiRequestJson } from './httpClient';

interface RawContent {
  id?: unknown;
  title?: unknown;
  summary?: unknown;
  category?: unknown;
  script?: unknown;
  scriptPreview?: unknown;
  estimatedMinutes?: unknown;
  estimatedDurationMinutes?: unknown;
  createdAt?: unknown;
  updatedAt?: unknown;
}

function requiredText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function contentId(value: unknown): string {
  return typeof value === 'string' || typeof value === 'number' ? String(value) : '';
}

function toSummary(value: unknown): KnowledgeContentSummary | null {
  if (!value || typeof value !== 'object') return null;

  const raw = value as RawContent;
  const id = contentId(raw.id);
  const title = requiredText(raw.title);
  const summary = requiredText(raw.summary);
  const category = requiredText(raw.category);
  if (!id || !title || !summary || !category) return null;

  const script = typeof raw.script === 'string' ? raw.script : '';
  const preview =
    typeof raw.scriptPreview === 'string'
      ? raw.scriptPreview
      : script
        ? script.slice(0, 160)
        : undefined;
  const reportedMinutes = raw.estimatedMinutes ?? raw.estimatedDurationMinutes;
  const estimatedMinutes =
    typeof reportedMinutes === 'number' && reportedMinutes > 0
      ? Math.ceil(reportedMinutes)
      : script || preview
        ? estimateListeningMinutes(script || preview || '')
        : 0;

  return {
    id,
    title,
    summary,
    category,
    scriptPreview: preview,
    estimatedMinutes,
  };
}

function toContent(value: unknown): KnowledgeContent {
  const summary = toSummary(value);
  const raw = value as RawContent;
  const script = typeof raw?.script === 'string' ? raw.script : '';

  if (!summary || !script.trim()) {
    throw new ApiError('콘텐츠 응답 형식이 올바르지 않습니다.', 500);
  }

  return {
    ...summary,
    script,
    estimatedMinutes: estimateListeningMinutes(script),
    createdAt: typeof raw.createdAt === 'string' ? raw.createdAt : undefined,
    updatedAt: typeof raw.updatedAt === 'string' ? raw.updatedAt : undefined,
  };
}

export async function getContents(signal?: AbortSignal): Promise<KnowledgeContentSummary[]> {
  const payload = await apiRequestJson<unknown>('/api/v1/contents', { signal });
  const values = Array.isArray(payload)
    ? payload
    : payload && typeof payload === 'object' && 'contents' in payload
      ? (payload as { contents: unknown }).contents
      : null;

  if (!Array.isArray(values)) {
    throw new ApiError('콘텐츠 목록 응답 형식이 올바르지 않습니다.', 500);
  }

  return values
    .map(toSummary)
    .filter((content): content is KnowledgeContentSummary => content !== null);
}

export async function getContent(
  id: string,
  signal?: AbortSignal,
): Promise<KnowledgeContent> {
  const payload = await apiRequestJson<unknown>(`/api/v1/contents/${encodeURIComponent(id)}`, {
    signal,
  });
  return toContent(payload);
}

export async function createContent(draft: ContentDraft): Promise<KnowledgeContent> {
  const payload = await apiRequestJson<unknown>('/api/v1/contents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(draft),
  });
  return toContent(payload);
}

export async function updateContent(
  id: string,
  draft: ContentDraft,
): Promise<KnowledgeContent> {
  const payload = await apiRequestJson<unknown>(
    `/api/v1/contents/${encodeURIComponent(id)}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(draft),
    },
  );
  return toContent(payload);
}

export async function createNarration(
  contentIdValue: string,
  request: NarrationOptions,
  signal?: AbortSignal,
): Promise<Blob> {
  const response = await apiRequest(
    `/api/v1/contents/${encodeURIComponent(contentIdValue)}/narration`,
    {
      method: 'POST',
      signal,
      headers: {
        Accept: 'audio/wav',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    },
  );
  const blob = await response.blob();
  if (blob.size === 0) {
    throw new ApiError('서버가 빈 나레이션 파일을 반환했습니다.', 502);
  }
  return blob;
}
