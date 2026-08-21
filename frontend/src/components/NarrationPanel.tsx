import { useState, type FormEvent } from 'react';
import type { GeneratedSpeech, Voice } from '../domain/speech';
import { AudioResult } from './AudioResult';
import { SpeedControl } from './SpeedControl';
import { VoiceSelector } from './VoiceSelector';

interface NarrationPanelProps {
  voices: Voice[];
  selectedVoiceId: string;
  voiceStatus: 'idle' | 'loading' | 'success' | 'error';
  voiceError: string | null;
  generationStatus: 'idle' | 'loading' | 'success' | 'error';
  generationError: string | null;
  result: GeneratedSpeech | null;
  onVoiceChange: (voiceId: string) => void;
  onReloadVoices: () => void;
  onGenerate: (voiceId: string, speed: number) => void;
  onCancel: () => void;
}

export function NarrationPanel({
  voices,
  selectedVoiceId,
  voiceStatus,
  voiceError,
  generationStatus,
  generationError,
  result,
  onVoiceChange,
  onReloadVoices,
  onGenerate,
  onCancel,
}: NarrationPanelProps) {
  const [speed, setSpeed] = useState(0.9);
  const isGenerating = generationStatus === 'loading';
  const canGenerate = Boolean(selectedVoiceId && !isGenerating);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (canGenerate) onGenerate(selectedVoiceId, speed);
  }

  return (
    <div className="narration-panel">
      <div className="narration-heading">
        <span className="narration-icon" aria-hidden="true">
          <i /><i /><i /><i /><i />
        </span>
        <div><p>제작 도구</p><h2>나레이션 만들기</h2></div>
      </div>
      <p className="narration-intro">완성한 원고를 차분한 음성으로 들어보고 WAV 파일로 저장하세요.</p>
      <form onSubmit={submit} className="narration-form">
        <VoiceSelector
          voices={voices}
          value={selectedVoiceId}
          status={voiceStatus}
          error={voiceError}
          onChange={onVoiceChange}
          onRetry={onReloadVoices}
        />
        <SpeedControl value={speed} onChange={setSpeed} />
        {generationError && <div className="request-error" role="alert">{generationError}</div>}
        <button type="submit" className="primary-button" disabled={!canGenerate}>
          {isGenerating ? <><span className="spinner" aria-hidden="true" />음성을 만드는 중…</> : <><svg viewBox="0 0 20 20" aria-hidden="true"><path d="m7 5 8 5-8 5V5Z" /></svg>원고 음성화</>}
        </button>
        {isGenerating && <button type="button" className="cancel-link" onClick={onCancel}>화면 대기 중단</button>}
      </form>
      {result && <AudioResult result={result} />}
      <p className="narration-note"><span aria-hidden="true">i</span> 음성 생성은 원고를 저장한 뒤 사용할 수 있어요.</p>
    </div>
  );
}
