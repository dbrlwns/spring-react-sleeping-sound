import type { Voice } from '../domain/speech';

interface VoiceSelectorProps {
  voices: Voice[];
  value: string;
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
  onChange: (voiceId: string) => void;
  onRetry: () => void;
}

export function VoiceSelector({
  voices,
  value,
  status,
  error,
  onChange,
  onRetry,
}: VoiceSelectorProps) {
  const selectedVoice = voices.find((voice) => voice.id === value);
  const descriptionId = selectedVoice?.description ? 'voice-description' : undefined;

  if (status === 'error') {
    return (
      <div className="field-block">
        <span className="field-label">목소리</span>
        <div className="inline-error" role="alert">
          <span>{error}</span>
          <button className="text-button" type="button" onClick={onRetry}>
            다시 불러오기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="field-block">
      <label className="field-label" htmlFor="voice">
        목소리
      </label>
      <div className="select-wrap">
        <select
          id="voice"
          value={value}
          aria-busy={status === 'loading'}
          aria-describedby={descriptionId}
          disabled={status === 'loading' || voices.length === 0}
          onChange={(event) => onChange(event.target.value)}
        >
          {status === 'loading' && <option value="">목소리 불러오는 중…</option>}
          {voices.map((voice) => (
            <option key={voice.id} value={voice.id}>
              {voice.name}
              {voice.language ? ` · ${voice.language}` : ''}
            </option>
          ))}
        </select>
        <svg aria-hidden="true" viewBox="0 0 20 20">
          <path d="m5.5 7.5 4.5 4 4.5-4" />
        </svg>
      </div>
      {selectedVoice?.description && (
        <p className="field-help" id="voice-description">
          {selectedVoice.description}
        </p>
      )}
    </div>
  );
}
