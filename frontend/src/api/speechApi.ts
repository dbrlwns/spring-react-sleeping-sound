import type { Voice } from '../domain/speech';
import { ApiError, apiRequestJson } from './httpClient';

interface RawVoice {
  id?: unknown;
  name?: unknown;
  displayName?: unknown;
  language?: unknown;
  locale?: unknown;
  description?: unknown;
}

function toVoice(value: unknown): Voice | null {
  if (!value || typeof value !== 'object') return null;

  const raw = value as RawVoice;
  const id = typeof raw.id === 'string' ? raw.id.trim() : '';
  const nameValue = raw.name ?? raw.displayName;
  const name = typeof nameValue === 'string' ? nameValue.trim() : '';

  if (!id || !name) return null;

  const languageValue = raw.language ?? raw.locale;
  return {
    id,
    name,
    language: typeof languageValue === 'string' ? languageValue : undefined,
    description: typeof raw.description === 'string' ? raw.description : undefined,
  };
}

/**
 * 외부 API 모양을 애플리케이션 도메인 모델로 변환하는 경계입니다.
 * 학습용 서버 구현이 배열 또는 { voices: [...] } 중 어느 형태를 반환해도 수용합니다.
 */
export async function getVoices(signal?: AbortSignal): Promise<Voice[]> {
  const payload = await apiRequestJson<unknown>('/api/v1/narration/voices', { signal });
  const values = Array.isArray(payload)
    ? payload
    : payload && typeof payload === 'object' && 'voices' in payload
      ? (payload as { voices: unknown }).voices
      : null;

  if (!Array.isArray(values)) {
    throw new ApiError('음성 목록 응답 형식이 올바르지 않습니다.', 500);
  }

  const voices = values.map(toVoice).filter((voice): voice is Voice => voice !== null);

  if (voices.length === 0) {
    throw new ApiError('사용 가능한 음성이 없습니다.', 404);
  }

  return voices;
}
