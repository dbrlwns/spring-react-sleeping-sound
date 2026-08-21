import { describe, expect, it } from 'vitest';
import type { Voice } from '../domain/speech';
import { findPreferredVoiceId } from './useVoices';

const voice = (id: string, language: string): Voice => ({
  id,
  name: id,
  language,
});

describe('findPreferredVoiceId', () => {
  it('한국어 샘플에 맞춰 Yuna를 가장 먼저 선택한다', () => {
    expect(
      findPreferredVoiceId([
        voice('Majed', 'ar_001'),
        voice('Eddy Korean', 'ko_KR'),
        voice('Yuna', 'ko_KR'),
      ]),
    ).toBe('Yuna');
  });

  it('Yuna가 없으면 설치된 첫 한국어 음성을 선택한다', () => {
    expect(
      findPreferredVoiceId([
        voice('Samantha', 'en_US'),
        voice('Eddy Korean', 'ko_KR'),
      ]),
    ).toBe('Eddy Korean');
  });

  it('한국어 음성이 없으면 목록의 첫 음성을 사용한다', () => {
    expect(findPreferredVoiceId([voice('Samantha', 'en_US')])).toBe('Samantha');
  });
});
