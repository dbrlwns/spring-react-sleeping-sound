import { useCallback, useEffect, useState } from 'react';
import { getAuthSession, login, logout, register } from '../api/authApi';
import type { AuthCredentials, AuthMode, AuthSession } from '../domain/auth';
import { isAbortError, toErrorMessage } from '../utils/errors';

type AuthStatus = 'checking' | 'authenticated' | 'unauthenticated' | 'error';
type AuthActionStatus = 'idle' | 'loading';

export function useAuth() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [status, setStatus] = useState<AuthStatus>('checking');
  const [actionStatus, setActionStatus] = useState<AuthActionStatus>('idle');
  const [error, setError] = useState<string | null>(null);

  const checkSession = useCallback(async (signal?: AbortSignal) => {
    setStatus('checking');
    setError(null);
    try {
      const loaded = await getAuthSession(signal);
      setSession(loaded);
      setStatus(loaded.authenticated ? 'authenticated' : 'unauthenticated');
    } catch (checkError) {
      if (isAbortError(checkError)) return;
      setSession(null);
      setError(toErrorMessage(checkError));
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void checkSession(controller.signal);
    return () => controller.abort();
  }, [checkSession]);

  const submitCredentials = useCallback(
    async (mode: AuthMode, credentials: AuthCredentials): Promise<boolean> => {
      setActionStatus('loading');
      setError(null);
      try {
        const authenticated = await (mode === 'login'
          ? login(credentials)
          : register(credentials));
        setSession(authenticated);
        setStatus('authenticated');
        return true;
      } catch (authError) {
        setError(toErrorMessage(authError));
        setStatus('unauthenticated');
        return false;
      } finally {
        setActionStatus('idle');
      }
    },
    [],
  );

  const endSession = useCallback(async (): Promise<boolean> => {
    setActionStatus('loading');
    setError(null);
    try {
      await logout();
      setSession({ authenticated: false, username: null });
      setStatus('unauthenticated');
      return true;
    } catch (logoutError) {
      setError(toErrorMessage(logoutError));
      return false;
    } finally {
      setActionStatus('idle');
    }
  }, []);

  return {
    session,
    status,
    actionStatus,
    error,
    checkSession,
    submitCredentials,
    endSession,
    clearError: () => setError(null),
  };
}
