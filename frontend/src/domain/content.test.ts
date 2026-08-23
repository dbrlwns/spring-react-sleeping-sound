import { describe, expect, it } from 'vitest';
import {
  CONTENT_CATEGORIES,
  estimateListeningMinutes,
  getCategoryLabel,
  getCategoryTone,
  normalizeContentCategory,
} from './content';

describe('content presentation helpers', () => {
  it('전체와 8개의 넓은 주제를 제공한다', () => {
    expect(CONTENT_CATEGORIES).toEqual([
      { id: 'ALL', label: '전체' },
      { id: 'SCIENCE', label: '과학' },
      { id: 'SOCIETY', label: '사회' },
      { id: 'HISTORY', label: '역사' },
      { id: 'PHILOSOPHY', label: '철학' },
      { id: 'ECONOMY', label: '경제' },
      { id: 'TECHNOLOGY', label: '기술' },
      { id: 'CULTURE', label: '문화' },
      { id: 'PSYCHOLOGY', label: '심리' },
    ]);
  });

  it('기존 과학 분류를 과학 주제로 합쳐 표시한다', () => {
    expect(normalizeContentCategory('COSMOLOGY')).toBe('SCIENCE');
    expect(getCategoryLabel('COSMOLOGY')).toBe('과학');
    expect(getCategoryTone('COSMOLOGY')).toBe(getCategoryTone('SCIENCE'));
  });

  it('새로운 분류 코드도 화면을 깨뜨리지 않고 읽을 수 있게 표시한다', () => {
    expect(getCategoryLabel('HISTORY_OF_SCIENCE')).toBe('History Of Science');
    expect(getCategoryTone('HISTORY_OF_SCIENCE')).toBe('sand');
  });

  it('한국어 원고 길이를 기준으로 최소 1분 이상의 청취 시간을 계산한다', () => {
    expect(estimateListeningMinutes('짧은 이야기')).toBe(1);
    expect(estimateListeningMinutes('가'.repeat(661))).toBe(3);
  });
});
