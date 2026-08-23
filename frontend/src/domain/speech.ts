export interface GeneratedSpeech {
  url: string;
  fileName: string;
}

export type NarrationJobStatus = 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED';

export interface NarrationJob {
  status: NarrationJobStatus;
  errorMessage?: string;
  updatedAt?: string;
}
