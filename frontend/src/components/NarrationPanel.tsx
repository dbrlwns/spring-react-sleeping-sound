import { useAutomaticNarration } from '../hooks/useAutomaticNarration';
import { AudioResult } from './AudioResult';

interface NarrationPanelProps {
  contentId: string;
  title: string;
}

function formatUpdatedAt(value: string | undefined): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export function NarrationPanel({ contentId, title }: NarrationPanelProps) {
  const narration = useAutomaticNarration(contentId, title);
  const updatedAt = formatUpdatedAt(narration.job?.updatedAt);
  const isPending = narration.job?.status === 'PENDING';
  const isProcessing = narration.job?.status === 'PROCESSING';
  const hasFailed = narration.job?.status === 'FAILED';

  return (
    <div className="narration-panel">
      <div className="narration-heading">
        <span className="narration-icon" aria-hidden="true">
          <i /><i /><i /><i /><i />
        </span>
        <div><p>자동 내레이션</p><h2>이야기 듣기</h2></div>
      </div>
      <p className="narration-intro">
        원고를 저장하면 서버가 자동으로 WAV 내레이션을 만들어요. 별도의 목소리나 속도 설정은 필요하지 않습니다.
      </p>

      {(narration.isChecking || isPending || isProcessing || narration.isLoadingAudio) && (
        <div className="narration-progress" role="status" aria-live="polite">
          <span className="large-spinner" aria-hidden="true" />
          <div>
            <strong>
              {narration.isChecking
                ? '내레이션 상태를 확인하고 있어요'
                : isPending
                  ? '음성 변환을 기다리고 있어요'
                  : narration.isLoadingAudio
                    ? '완성된 오디오를 불러오고 있어요'
                    : '원고를 차분한 음성으로 만들고 있어요'}
            </strong>
            <p>
              {isPending
                ? '작업 순서가 되면 자동으로 시작합니다.'
                : '이 화면을 열어 두면 완료 상태를 자동으로 확인합니다.'}
            </p>
          </div>
        </div>
      )}

      {hasFailed && (
        <div className="narration-failed" role="alert">
          <span className="state-symbol" aria-hidden="true">!</span>
          <div>
            <strong>내레이션을 만들지 못했어요</strong>
            <p>{narration.job?.errorMessage ?? '잠시 후 상태를 다시 확인하거나 원고를 다시 저장해 주세요.'}</p>
            <button type="button" className="text-button" onClick={narration.retry}>상태 다시 확인</button>
          </div>
        </div>
      )}

      {narration.requestError && (
        <div className="inline-error narration-request-error" role="alert">
          <span>{narration.requestError}</span>
          <button className="text-button" type="button" onClick={narration.retry}>다시 시도</button>
        </div>
      )}

      {narration.result && <AudioResult result={narration.result} />}

      {updatedAt && <p className="narration-updated">마지막 상태 변경 · {updatedAt}</p>}
      <p className="narration-note">
        <span aria-hidden="true">i</span>
        새 원고를 저장하면 기존 오디오 대신 새 버전을 자동으로 준비합니다.
      </p>
    </div>
  );
}
