/**
 * API와 화면이 함께 사용하는 음성 도메인 모델입니다.
 * 서버 응답의 필드가 바뀌어도 화면 컴포넌트는 이 타입만 바라보게 합니다.
 */
export interface Voice {
  id: string;
  name: string;
  language?: string;
  description?: string;
}

export interface NarrationOptions {
  voiceId: string;
  speed: number;
}

export interface GeneratedSpeech {
  url: string;
  fileName: string;
}

export const NARRATION_CONSTRAINTS = {
  minSpeed: 0.5,
  maxSpeed: 2,
  speedStep: 0.1,
} as const;
