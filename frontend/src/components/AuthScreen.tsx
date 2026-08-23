import { useState, type FormEvent } from 'react';
import type { AuthCredentials, AuthMode } from '../domain/auth';

interface AuthScreenProps {
  isSubmitting: boolean;
  isSessionError: boolean;
  error: string | null;
  onSubmit: (mode: AuthMode, credentials: AuthCredentials) => Promise<boolean>;
  onBack: () => void;
  onRetrySession: () => void;
  onModeChange: () => void;
}

export function AuthScreen({
  isSubmitting,
  isSessionError,
  error,
  onSubmit,
  onBack,
  onRetrySession,
  onModeChange,
}: AuthScreenProps) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirmation, setPasswordConfirmation] = useState('');

  const trimmedUsername = username.trim();
  const usernameIsAllowed = /^[\p{L}\p{N}._-]+$/u.test(trimmedUsername);
  const passwordByteLength = new TextEncoder().encode(password).length;
  const passwordsMatch = mode === 'login' || password === passwordConfirmation;
  const isValid = Boolean(
    trimmedUsername.length >= 3 &&
      trimmedUsername.length <= 50 &&
      usernameIsAllowed &&
      password.length >= 8 &&
      passwordByteLength <= 72 &&
      passwordsMatch &&
      !isSubmitting,
  );

  function changeMode(nextMode: AuthMode) {
    setMode(nextMode);
    setPassword('');
    setPasswordConfirmation('');
    onModeChange();
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!isValid) return;
    void onSubmit(mode, { username: trimmedUsername, password });
  }

  return (
    <main className="auth-page">
      <section className="auth-visual" aria-hidden="true">
        <span className="auth-orbit auth-orbit--one" />
        <span className="auth-orbit auth-orbit--two" />
        <span className="auth-planet" />
        <span className="auth-star auth-star--one">✦</span>
        <span className="auth-star auth-star--two">·</span>
        <div className="auth-visual-copy">
          <p>QUIET KNOWLEDGE</p>
          <strong>오늘의 지식을<br />고요한 목소리로.</strong>
        </div>
      </section>

      <section className="auth-card" aria-labelledby="auth-title">
        <button
          type="button"
          className="auth-back-button"
          disabled={isSubmitting}
          onClick={onBack}
        >
          <span aria-hidden="true">←</span>
          이야기 보관함으로 돌아가기
        </button>
        <div className="auth-brand">
          <span className="brand-mark" aria-hidden="true">
            <span className="moon-cutout" />
            <span className="brand-star" />
          </span>
          <span><strong>고요한 지식</strong><small>잠들며 듣는 이야기</small></span>
        </div>
        <p className="eyebrow">MEMBERS' LIBRARY</p>
        <h1 id="auth-title">{mode === 'login' ? '다시 만나 반가워요.' : '이야기 보관함에 함께해요.'}</h1>
        <p className="auth-intro">
          {mode === 'login'
            ? '로그인하면 새로운 원고를 쓰거나 기존 이야기를 편집할 수 있어요.'
            : '계정을 만들면 원고를 작성하고 자동 내레이션을 준비할 수 있어요.'}
        </p>

        <div className="auth-tabs" aria-label="계정 메뉴">
          <button type="button" disabled={isSubmitting} aria-pressed={mode === 'login'} className={mode === 'login' ? 'is-active' : ''} onClick={() => changeMode('login')}>로그인</button>
          <button type="button" disabled={isSubmitting} aria-pressed={mode === 'register'} className={mode === 'register' ? 'is-active' : ''} onClick={() => changeMode('register')}>회원가입</button>
        </div>

        <form className="auth-form" onSubmit={submit}>
          <label className="input-field">
            <span>사용자 이름</span>
            <input
              type="text"
              value={username}
              minLength={3}
              maxLength={50}
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              required
              aria-invalid={trimmedUsername.length > 0 && !usernameIsAllowed}
              aria-describedby="username-help"
              placeholder="3자 이상의 사용자 이름"
              onChange={(event) => setUsername(event.target.value)}
            />
            <small id="username-help" className={!usernameIsAllowed && trimmedUsername ? 'field-error' : undefined}>
              문자, 숫자, 마침표, 밑줄과 하이픈만 사용할 수 있어요.
            </small>
          </label>
          <label className="input-field">
            <span>비밀번호</span>
            <input
              type="password"
              value={password}
              minLength={8}
              maxLength={72}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              required
              placeholder="8자 이상의 비밀번호"
              onChange={(event) => setPassword(event.target.value)}
            />
            <small>8자 이상, UTF-8 기준 72바이트 이하로 입력해 주세요.</small>
          </label>
          {mode === 'register' && (
            <label className="input-field">
              <span>비밀번호 확인</span>
              <input
                type="password"
                value={passwordConfirmation}
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                required
                aria-invalid={passwordConfirmation.length > 0 && !passwordsMatch}
                aria-describedby="password-confirmation-help"
                placeholder="비밀번호를 한 번 더 입력해 주세요"
                onChange={(event) => setPasswordConfirmation(event.target.value)}
              />
              {passwordConfirmation.length > 0 && !passwordsMatch && (
                <small id="password-confirmation-help" className="field-error">비밀번호가 서로 다릅니다.</small>
              )}
            </label>
          )}

          {error && (
            <div className="request-error auth-error" role="alert">
              <span>{error}</span>
              {isSessionError && <button type="button" onClick={onRetrySession}>서버 다시 확인</button>}
            </div>
          )}

          <button className="primary-button auth-submit" type="submit" disabled={!isValid}>
            {isSubmitting ? <><span className="spinner" aria-hidden="true" />처리하는 중…</> : mode === 'login' ? '로그인' : '계정 만들기'}
          </button>
        </form>
        <p className="auth-note">세션 쿠키는 브라우저에 안전하게 보관되며, 비밀번호는 화면에 저장하지 않습니다.</p>
      </section>
    </main>
  );
}
