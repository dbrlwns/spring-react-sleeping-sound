import { useEffect, useState } from 'react';
import { getNarrationAudio, getNarrationStatus } from '../api/contentApi';
import type { GeneratedSpeech, NarrationJob } from '../domain/speech';
import { isAbortError, toErrorMessage } from '../utils/errors';

const POLL_INTERVAL_MS = 2_000;

function safeFileName(title: string): string {
  const normalized = title
    .trim()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 48);
  return `${normalized || 'knowledge-narration'}.wav`;
}

export function useAutomaticNarration(contentId: string, title: string) {
  const [job, setJob] = useState<NarrationJob | null>(null);
  const [result, setResult] = useState<GeneratedSpeech | null>(null);
  const [isChecking, setIsChecking] = useState(true);
  const [isLoadingAudio, setIsLoadingAudio] = useState(false);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    let objectUrl: string | null = null;

    setJob(null);
    setResult(null);
    setIsChecking(true);
    setIsLoadingAudio(false);
    setRequestError(null);

    async function poll() {
      try {
        const current = await getNarrationStatus(contentId, controller.signal);
        if (controller.signal.aborted) return;
        setJob(current);
        setIsChecking(false);

        if (current.status === 'PENDING' || current.status === 'PROCESSING') {
          timer = setTimeout(() => void poll(), POLL_INTERVAL_MS);
          return;
        }

        if (current.status === 'READY') {
          setIsLoadingAudio(true);
          const blob = await getNarrationAudio(contentId, controller.signal);
          if (controller.signal.aborted) return;
          objectUrl = URL.createObjectURL(blob);
          setResult({ url: objectUrl, fileName: safeFileName(title) });
          setIsLoadingAudio(false);
        }
      } catch (error) {
        if (isAbortError(error)) return;
        setJob(null);
        setIsChecking(false);
        setIsLoadingAudio(false);
        setRequestError(toErrorMessage(error));
      }
    }

    void poll();
    return () => {
      controller.abort();
      if (timer) clearTimeout(timer);
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [contentId, retryCount, title]);

  return {
    job,
    result,
    isChecking,
    isLoadingAudio,
    requestError,
    retry: () => setRetryCount((count) => count + 1),
  };
}
