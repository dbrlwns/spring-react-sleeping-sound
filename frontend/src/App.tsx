import { useEffect, useState } from 'react';
import { AuthScreen } from './components/AuthScreen';
import { ContentDetail } from './components/ContentDetail';
import { ContentEditor } from './components/ContentEditor';
import { ContentLibrary } from './components/ContentLibrary';
import { NarrationPanel } from './components/NarrationPanel';
import type { AuthCredentials, AuthMode } from './domain/auth';
import { useAuth } from './hooks/useAuth';
import { useContentLibrary } from './hooks/useContentLibrary';

type Screen = 'library' | 'detail' | 'editor';
type WriteIntent =
  | { type: 'create' }
  | { type: 'edit'; contentId: string };

interface KnowledgeAppProps {
  isAuthenticated: boolean;
  isCheckingAuth: boolean;
  isSessionError: boolean;
  username: string | null;
  isAuthActionLoading: boolean;
  authError: string | null;
  onAuthenticate: (mode: AuthMode, credentials: AuthCredentials) => Promise<boolean>;
  onLogout: () => Promise<boolean>;
  onRetrySession: () => void;
  onClearAuthError: () => void;
}

export function KnowledgeApp({
  isAuthenticated,
  isCheckingAuth,
  isSessionError,
  username,
  isAuthActionLoading,
  authError,
  onAuthenticate,
  onLogout,
  onRetrySession,
  onClearAuthError,
}: KnowledgeAppProps) {
  const [screen, setScreen] = useState<Screen>('library');
  const [isCreating, setIsCreating] = useState(false);
  const [showAuth, setShowAuth] = useState(false);
  const [pendingWriteIntent, setPendingWriteIntent] = useState<WriteIntent | null>(null);
  const library = useContentLibrary();

  function showLibrary() {
    setScreen('library');
    setIsCreating(false);
  }

  function openContent(id: string) {
    setScreen('detail');
    void library.openContent(id);
  }

  function continueWriteIntent(intent: WriteIntent | null) {
    if (!intent) return;

    if (intent?.type === 'create') {
      setIsCreating(true);
      setScreen('editor');
      return;
    }

    if (
      intent?.type === 'edit' &&
      library.selectedContent?.id === intent.contentId
    ) {
      setIsCreating(false);
      setScreen('editor');
      return;
    }

    showLibrary();
  }

  function requestAuthentication(intent: WriteIntent | null = null) {
    setPendingWriteIntent(intent);
    setShowAuth(true);
  }

  function startCreate() {
    if (!isAuthenticated) {
      requestAuthentication({ type: 'create' });
      return;
    }
    setIsCreating(true);
    setScreen('editor');
  }

  function startEdit() {
    if (!library.selectedContent) return;
    if (!isAuthenticated) {
      requestAuthentication({
        type: 'edit',
        contentId: library.selectedContent.id,
      });
      return;
    }
    setIsCreating(false);
    setScreen('editor');
  }

  async function authenticate(mode: AuthMode, credentials: AuthCredentials) {
    const succeeded = await onAuthenticate(mode, credentials);
    if (!succeeded) return false;

    const intent = pendingWriteIntent;
    setPendingWriteIntent(null);
    setShowAuth(false);
    continueWriteIntent(intent);
    return true;
  }

  function closeAuth() {
    setPendingWriteIntent(null);
    setShowAuth(false);
    onClearAuthError();
  }

  async function logout() {
    if (!(await onLogout())) return;
    setPendingWriteIntent(null);
    setShowAuth(false);
    showLibrary();
  }

  useEffect(() => {
    if (!showAuth || !isAuthenticated) return;
    const intent = pendingWriteIntent;
    setPendingWriteIntent(null);
    setShowAuth(false);
    continueWriteIntent(intent);
    // authenticate()가 처리하지 않은 백그라운드 세션 복구만 이어갑니다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated, showAuth]);

  async function saveEditor(draft: Parameters<typeof library.saveContent>[0]) {
    const saved = await library.saveContent(
      draft,
      isCreating ? undefined : library.selectedContent?.id,
    );
    if (saved) {
      setIsCreating(false);
      setScreen('detail');
    }
  }

  function cancelEditor() {
    if (library.saveStatus === 'loading') return;
    setScreen(isCreating ? 'library' : 'detail');
    setIsCreating(false);
  }

  if (showAuth) {
    return (
      <AuthScreen
        isSubmitting={isAuthActionLoading}
        isSessionError={isSessionError}
        error={authError}
        onSubmit={authenticate}
        onBack={closeAuth}
        onRetrySession={onRetrySession}
        onModeChange={onClearAuthError}
      />
    );
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <button className="brand" type="button" onClick={showLibrary} aria-label="고요한 지식 홈">
          <span className="brand-mark" aria-hidden="true">
            <span className="moon-cutout" />
            <span className="brand-star" />
          </span>
          <span className="brand-copy"><strong>고요한 지식</strong><small>잠들며 듣는 이야기</small></span>
        </button>
        <nav aria-label="주요 메뉴">
          <button className="nav-link" type="button" onClick={showLibrary}>이야기 보관함</button>
          <button className="create-button" type="button" onClick={startCreate}>
            <svg viewBox="0 0 20 20" aria-hidden="true"><path d="M10 4v12M4 10h12" /></svg>
            새 이야기
          </button>
          {isAuthenticated && username ? (
            <>
              <span className="session-user" title={`${username} 계정으로 로그인됨`}>
                <span aria-hidden="true">●</span>{username}
              </span>
              <button
                className="logout-button"
                type="button"
                disabled={isAuthActionLoading}
                onClick={() => void logout()}
              >
                {isAuthActionLoading ? '로그아웃 중…' : '로그아웃'}
              </button>
            </>
          ) : (
            <button
              className="login-button"
              type="button"
              disabled={isCheckingAuth}
              onClick={() => requestAuthentication()}
            >
              {isCheckingAuth ? '확인 중…' : '로그인'}
            </button>
          )}
        </nav>
      </header>

      {authError && <div className="session-alert" role="alert">{authError}</div>}

      {screen === 'library' && (
        <main id="main" className="library-page">
          <section className="hero" aria-labelledby="page-title">
            <div className="hero-copy">
              <p className="eyebrow">KNOWLEDGE FOR DEEP REST</p>
              <h1 id="page-title">잠들기 전,<br /><em>깊은 이야기</em>를 천천히.</h1>
              <p>과학과 사회, 역사와 철학까지. 다양한 지식을 편안한 호흡의 원고로 만나보세요.</p>
              <button type="button" className="hero-action" onClick={() => document.getElementById('library-title')?.scrollIntoView()}>
                오늘의 이야기 둘러보기
                <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m6 8 4 4 4-4" /></svg>
              </button>
            </div>
            <div className="hero-visual" aria-hidden="true">
              <span className="hero-orbit hero-orbit--one" />
              <span className="hero-orbit hero-orbit--two" />
              <span className="hero-planet" />
              <span className="hero-moon" />
              <span className="hero-star hero-star--one">✦</span>
              <span className="hero-star hero-star--two">·</span>
              <span className="hero-caption">LISTEN · LEARN · REST</span>
            </div>
          </section>
          <ContentLibrary
            contents={library.contents}
            status={library.listStatus}
            error={library.listError}
            onOpen={openContent}
            onRetry={() => void library.loadContents()}
          />
        </main>
      )}

      {screen === 'detail' && library.detailStatus === 'loading' && (
        <main id="main" className="page-state" aria-busy="true">
          <span className="large-spinner" aria-hidden="true" />
          <p>이야기를 펼치고 있어요…</p>
        </main>
      )}

      {screen === 'detail' && library.detailStatus === 'error' && (
        <main id="main" className="page-state" role="alert">
          <span className="state-symbol">!</span>
          <h1>이야기를 열지 못했어요</h1>
          <p>{library.detailError}</p>
          <button type="button" className="secondary-button" onClick={showLibrary}>보관함으로 돌아가기</button>
        </main>
      )}

      {screen === 'detail' && library.selectedContent && library.detailStatus !== 'loading' && (
        <ContentDetail
          content={library.selectedContent}
          isAuthenticated={isAuthenticated}
          onBack={showLibrary}
          onEdit={startEdit}
        >
          <NarrationPanel
            contentId={library.selectedContent.id}
            title={library.selectedContent.title}
          />
        </ContentDetail>
      )}

      {screen === 'editor' && isAuthenticated && (
        <ContentEditor
          key={isCreating ? 'new' : library.selectedContent?.id}
          content={isCreating ? undefined : library.selectedContent ?? undefined}
          isSaving={library.saveStatus === 'loading'}
          error={library.saveError}
          onSave={(draft) => void saveEditor(draft)}
          onCancel={cancelEditor}
        />
      )}

      <footer>
        <div><strong>고요한 지식</strong><span>어려운 지식을 편안한 이야기로.</span></div>
        <span>Spring Boot · React · Vite</span>
      </footer>
    </div>
  );
}

function App() {
  const auth = useAuth();
  const isAuthenticated =
    auth.status === 'authenticated' &&
    auth.session?.authenticated === true;

  return (
    <KnowledgeApp
      isAuthenticated={isAuthenticated}
      isCheckingAuth={auth.status === 'checking'}
      isSessionError={auth.status === 'error'}
      username={isAuthenticated ? auth.session?.username ?? null : null}
      isAuthActionLoading={auth.actionStatus === 'loading'}
      authError={auth.error}
      onAuthenticate={auth.submitCredentials}
      onLogout={auth.endSession}
      onRetrySession={() => void auth.checkSession()}
      onClearAuthError={auth.clearError}
    />
  );
}

export default App;
