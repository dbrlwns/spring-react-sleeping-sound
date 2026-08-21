import { useState, type FormEvent } from 'react';
import {
  CONTENT_CATEGORIES,
  CONTENT_CONSTRAINTS,
  estimateListeningMinutes,
  getCategoryLabel,
  type ContentDraft,
  type KnowledgeContent,
} from '../domain/content';

interface ContentEditorProps {
  content?: KnowledgeContent;
  isSaving: boolean;
  error: string | null;
  onSave: (draft: ContentDraft) => void;
  onCancel: () => void;
}

const EMPTY_DRAFT: ContentDraft = {
  title: '',
  summary: '',
  category: 'QUANTUM_PHYSICS',
  script: '',
};

export function ContentEditor({
  content,
  isSaving,
  error,
  onSave,
  onCancel,
}: ContentEditorProps) {
  const [draft, setDraft] = useState<ContentDraft>(
    content
      ? {
          title: content.title,
          summary: content.summary,
          category: content.category,
          script: content.script,
        }
      : EMPTY_DRAFT,
  );
  const isValid = Boolean(
    draft.title.trim() &&
      draft.summary.trim() &&
      draft.category &&
      draft.script.trim() &&
      !isSaving,
  );
  const knownCategories = CONTENT_CATEGORIES.filter((item) => item.id !== 'ALL');
  const hasUnknownCategory = !knownCategories.some((item) => item.id === draft.category);

  function update<K extends keyof ContentDraft>(field: K, value: ContentDraft[K]) {
    setDraft((current) => ({ ...current, [field]: value }));
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!isValid) return;
    onSave({
      title: draft.title.trim(),
      summary: draft.summary.trim(),
      category: draft.category,
      script: draft.script.trim(),
    });
  }

  return (
    <main id="main" className="editor-page">
      <button type="button" className="back-button" onClick={onCancel}>
        <svg viewBox="0 0 20 20" aria-hidden="true"><path d="M16 10H5m4-4-4 4 4 4" /></svg>
        {content ? '이야기로 돌아가기' : '보관함으로 돌아가기'}
      </button>
      <header className="editor-header">
        <p className="eyebrow">STORY WORKSPACE</p>
        <h1>{content ? '원고 다듬기' : '새로운 이야기 쓰기'}</h1>
        <p>잠들기 전 천천히 따라갈 수 있도록, 쉬운 문장과 충분한 쉼을 담아보세요.</p>
      </header>

      <form className="editor-form" onSubmit={submit}>
        <section className="editor-card">
          <div className="editor-section-title">
            <span>01</span>
            <div><h2>이야기 정보</h2><p>보관함에서 보일 제목과 소개예요.</p></div>
          </div>
          <div className="editor-fields-grid">
            <label className="input-field input-field--wide">
              <span>제목</span>
              <input
                type="text"
                value={draft.title}
                maxLength={CONTENT_CONSTRAINTS.maxTitleLength}
                placeholder="예: 관측하기 전 달은 어디에 있을까"
                required
                onChange={(event) => update('title', event.target.value)}
              />
              <small>{draft.title.length} / {CONTENT_CONSTRAINTS.maxTitleLength}자</small>
            </label>
            <label className="input-field">
              <span>주제</span>
              <select value={draft.category} onChange={(event) => update('category', event.target.value)}>
                {hasUnknownCategory && <option value={draft.category}>{getCategoryLabel(draft.category)}</option>}
                {knownCategories.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}
              </select>
            </label>
            <label className="input-field input-field--wide">
              <span>짧은 소개</span>
              <textarea
                className="summary-input"
                value={draft.summary}
                maxLength={CONTENT_CONSTRAINTS.maxSummaryLength}
                rows={3}
                placeholder="이 이야기를 한두 문장으로 소개해 주세요."
                required
                onChange={(event) => update('summary', event.target.value)}
              />
              <small>{draft.summary.length} / {CONTENT_CONSTRAINTS.maxSummaryLength}자</small>
            </label>
          </div>
        </section>

        <section className="editor-card">
          <div className="editor-section-title">
            <span>02</span>
            <div><h2>나레이션 원고</h2><p>문단을 나누고 문장 부호를 넣으면 호흡이 더 자연스러워져요.</p></div>
          </div>
          <label className="input-field">
            <span className="sr-only">나레이션 원고</span>
            <textarea
              className="script-input"
              value={draft.script}
              maxLength={CONTENT_CONSTRAINTS.maxScriptLength}
              rows={18}
              placeholder={'오늘은 아주 작은 세계로 여행을 떠나보겠습니다.\n\n편안히 눈을 감고…'}
              required
              onChange={(event) => update('script', event.target.value)}
            />
            <small>
              약 {estimateListeningMinutes(draft.script)}분 분량 · {draft.script.length.toLocaleString()} / {CONTENT_CONSTRAINTS.maxScriptLength.toLocaleString()}자
            </small>
          </label>
        </section>

        {error && <div className="request-error" role="alert">{error}</div>}
        <div className="editor-actions">
          <button type="button" className="secondary-button" onClick={onCancel} disabled={isSaving}>취소</button>
          <button type="submit" className="primary-button" disabled={!isValid}>
            {isSaving ? <><span className="spinner" aria-hidden="true" />저장하는 중…</> : '이야기 저장'}
          </button>
        </div>
      </form>
    </main>
  );
}
