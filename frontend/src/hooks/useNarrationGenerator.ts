import { useCallback, useEffect, useRef, useState } from 'react';
import { createNarration } from '../api/contentApi';
import type { GeneratedSpeech } from '../domain/speech';
import { isAbortError, toErrorMessage } from '../utils/errors';

type GenerationStatus = 'idle' | 'loading' | 'success' | 'error';

function safeFileName(title: string): string {
  const normalized = title
    .trim()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 48);
  return `${normalized || 'knowledge-narration'}.wav`;
}

export function useNarrationGenerator() {
  const [status, setStatus] = useState<GenerationStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<GeneratedSpeech | null>(null);
  const controllerRef = useRef<AbortController | null>(null);
  const objectUrlRef = useRef<string | null>(null);

  const clearResult = useCallback(() => {
    controllerRef.current?.abort();
    controllerRef.current = null;
    if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
    objectUrlRef.current = null;
    setResult(null);
    setError(null);
    setStatus('idle');
  }, []);

  const generate = useCallback(
    async (contentId: string, title: string, voiceId: string, speed: number) => {
      controllerRef.current?.abort();
      const controller = new AbortController();
      controllerRef.current = controller;
      setStatus('loading');
      setError(null);
      try {
        const blob = await createNarration(contentId, { voiceId, speed }, controller.signal);
        if (controller.signal.aborted) return;
        const url = URL.createObjectURL(blob);
        if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
        objectUrlRef.current = url;
        setResult({ url, fileName: safeFileName(title) });
        setStatus('success');
      } catch (generationError) {
        if (isAbortError(generationError)) return;
        setError(toErrorMessage(generationError));
        setStatus('error');
      } finally {
        if (controllerRef.current === controller) controllerRef.current = null;
      }
    },
    [],
  );

  const cancel = useCallback(() => {
    controllerRef.current?.abort();
    controllerRef.current = null;
    setStatus(result ? 'success' : 'idle');
  }, [result]);

  useEffect(
    () => () => {
      controllerRef.current?.abort();
      if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
    },
    [],
  );

  return { status, error, result, generate, cancel, clearResult };
}
