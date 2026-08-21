import { useCallback, useEffect, useState } from 'react';
import { getVoices } from '../api/speechApi';
import type { Voice } from '../domain/speech';
import { isAbortError, toErrorMessage } from '../utils/errors';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export function findPreferredVoiceId(voices: Voice[]): string {
  return (
    voices.find((voice) => voice.id === 'Yuna')?.id ??
    voices.find((voice) => voice.language?.toLowerCase().startsWith('ko'))?.id ??
    voices[0]?.id ??
    ''
  );
}

export function useVoices() {
  const [voices, setVoices] = useState<Voice[]>([]);
  const [selectedVoiceId, setSelectedVoiceId] = useState('');
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const [reloadCount, setReloadCount] = useState(0);

  const reload = useCallback(() => {
    setReloadCount((count) => count + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setStatus('loading');
      setError(null);

      try {
        const loadedVoices = await getVoices(controller.signal);
        setVoices(loadedVoices);
        setSelectedVoiceId((current) =>
          loadedVoices.some((voice) => voice.id === current)
            ? current
            : findPreferredVoiceId(loadedVoices),
        );
        setStatus('success');
      } catch (loadError) {
        if (isAbortError(loadError)) return;
        setVoices([]);
        setSelectedVoiceId('');
        setError(toErrorMessage(loadError));
        setStatus('error');
      }
    }

    void load();
    return () => controller.abort();
  }, [reloadCount]);

  return {
    voices,
    selectedVoiceId,
    setSelectedVoiceId,
    status,
    error,
    reload,
  };
}
