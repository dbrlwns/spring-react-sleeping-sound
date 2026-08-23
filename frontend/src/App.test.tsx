import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it, vi } from 'vitest';
import { KnowledgeApp } from './App';
import { AuthScreen } from './components/AuthScreen';

const appProps = {
  isAuthenticated: false,
  isCheckingAuth: false,
  isSessionError: false,
  username: null,
  isAuthActionLoading: false,
  authError: null,
  onAuthenticate: vi.fn().mockResolvedValue(true),
  onLogout: vi.fn().mockResolvedValue(true),
  onRetrySession: vi.fn(),
  onClearAuthError: vi.fn(),
};

describe('공개 앱 진입 화면', () => {
  it('세션 확인 중에도 로그인 화면 대신 공개 보관함을 보여준다', () => {
    const html = renderToStaticMarkup(
      <KnowledgeApp {...appProps} isCheckingAuth />,
    );

    expect(html).toContain('오늘 밤의 이야기');
    expect(html).toContain('새 이야기');
    expect(html).toContain('확인 중…');
    expect(html).not.toContain('auth-page');
  });

  it('비회원 헤더에는 로그인 동작을 제공한다', () => {
    const html = renderToStaticMarkup(<KnowledgeApp {...appProps} />);

    expect(html).toContain('class="login-button"');
    expect(html).toContain('>로그인</button>');
    expect(html).not.toContain('로그아웃');
  });

  it('인증된 사용자에게는 계정과 로그아웃 동작을 보여준다', () => {
    const html = renderToStaticMarkup(
      <KnowledgeApp
        {...appProps}
        isAuthenticated
        username="quiet-user"
      />,
    );

    expect(html).toContain('quiet-user');
    expect(html).toContain('로그아웃');
    expect(html).not.toContain('class="login-button"');
  });
});

describe('요청형 로그인 화면', () => {
  it('언제든 공개 보관함으로 취소할 수 있는 경로를 제공한다', () => {
    const html = renderToStaticMarkup(
      <AuthScreen
        isSubmitting={false}
        isSessionError={false}
        error={null}
        onSubmit={vi.fn().mockResolvedValue(true)}
        onBack={vi.fn()}
        onRetrySession={vi.fn()}
        onModeChange={vi.fn()}
      />,
    );

    expect(html).toContain('이야기 보관함으로 돌아가기');
    expect(html).toContain('로그인하면 새로운 원고를 쓰거나 기존 이야기를 편집할 수 있어요.');
  });
});
