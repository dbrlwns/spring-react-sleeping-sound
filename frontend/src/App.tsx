import { ContentDetail } from './components/ContentDetail';
import { ContentEditor } from './components/ContentEditor';
import { ContentLibrary } from './components/ContentLibrary';
import { NarrationPanel } from './components/NarrationPanel';
import { useContentLibrary } from './hooks/useContentLibrary';
import { useNarrationGenerator } from './hooks/useNarrationGenerator';
import { useVoices } from './hooks/useVoices';
import { useState } from 'react';

type Screen = 'library' | 'detail' | 'editor';

function App() {
  const [screen, setScreen] = useState<Screen>('library');
  const [isCreating, setIsCreating] = useState(false);
  const library = useContentLibrary();
  const voiceState = useVoices();
  const narration = useNarrationGenerator();

  function showLibrary() {
    narration.clearResult();
    setScreen('library');
    setIsCreating(false);
  }

  function openContent(id: string) {
    narration.clearResult();
    setScreen('detail');
    void library.openContent(id);
  }

  function startCreate() {
    narration.clearResult();
    setIsCreating(true);
    setScreen('editor');
  }

  function startEdit() {
    setIsCreating(false);
    setScreen('editor');
  }

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
    setScreen(isCreating ? 'library' : 'detail');
    setIsCreating(false);
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
        </nav>
      </header>

      {screen === 'library' && (
        <main id="main" className="library-page">
          <section className="hero" aria-labelledby="page-title">
            <div className="hero-copy">
              <p className="eyebrow">KNOWLEDGE FOR DEEP REST</p>
              <h1 id="page-title">잠들기 전,<br /><em>깊은 이야기</em>를 천천히.</h1>
              <p>양자역학부터 우주의 탄생까지. 어렵지만 흥미로운 지식을 편안한 호흡의 원고로 만나보세요.</p>
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
        <ContentDetail content={library.selectedContent} onBack={showLibrary} onEdit={startEdit}>
          <NarrationPanel
            voices={voiceState.voices}
            selectedVoiceId={voiceState.selectedVoiceId}
            voiceStatus={voiceState.status}
            voiceError={voiceState.error}
            generationStatus={narration.status}
            generationError={narration.error}
            result={narration.result}
            onVoiceChange={voiceState.setSelectedVoiceId}
            onReloadVoices={voiceState.reload}
            onGenerate={(voiceId, speed) =>
              void narration.generate(
                library.selectedContent!.id,
                library.selectedContent!.title,
                voiceId,
                speed,
              )
            }
            onCancel={narration.cancel}
          />
        </ContentDetail>
      )}

      {screen === 'editor' && (
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

export default App;
