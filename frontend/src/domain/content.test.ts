import { describe, expect, it } from 'vitest';
import { estimateListeningMinutes, getCategoryLabel, getCategoryTone } from './content';

describe('content presentation helpers', () => {
  it('알려진 API 분류 코드를 한국어로 표시한다', () => {
    expect(getCategoryLabel('COSMOLOGY')).toBe('우주론');
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
