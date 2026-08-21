import type { GeneratedSpeech } from '../domain/speech';

interface AudioResultProps {
  result: GeneratedSpeech;
}

export function AudioResult({ result }: AudioResultProps) {
  return (
    <section
      className="audio-result"
      aria-labelledby="result-title"
      aria-live="polite"
      role="status"
    >
      <div className="audio-result-heading">
        <span className="success-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="m6.5 12.5 3.25 3.25L17.5 8" />
          </svg>
        </span>
        <div>
          <h3 id="result-title">나레이션이 준비됐어요</h3>
          <p>재생해 보고 파일로 저장하세요.</p>
        </div>
      </div>
      <audio key={result.url} controls preload="metadata" src={result.url}>
        브라우저가 오디오 재생을 지원하지 않습니다.
      </audio>
      <a className="download-button" href={result.url} download={result.fileName}>
        <svg aria-hidden="true" viewBox="0 0 24 24">
          <path d="M12 4v10m0 0 4-4m-4 4-4-4M5 19h14" />
        </svg>
        WAV 다운로드
      </a>
    </section>
  );
}
