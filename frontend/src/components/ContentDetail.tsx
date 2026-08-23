import {
  getCategoryLabel,
  getCategoryTone,
  type KnowledgeContent,
} from '../domain/content';

interface ContentDetailProps {
  content: KnowledgeContent;
  isAuthenticated: boolean;
  onBack: () => void;
  onEdit: () => void;
  children: React.ReactNode;
}

export function ContentDetail({
  content,
  isAuthenticated,
  onBack,
  onEdit,
  children,
}: ContentDetailProps) {
  return (
    <main id="main" className="detail-layout">
      <nav className="breadcrumb" aria-label="현재 위치">
        <button type="button" onClick={onBack}>
          이야기 보관함
        </button>
        <span aria-hidden="true">/</span>
        <span aria-current="page">{getCategoryLabel(content.category)}</span>
      </nav>

      <div className="detail-grid">
        <article className="story-article">
          <header className="story-header">
            <span className={`story-emblem story-emblem--${getCategoryTone(content.category)}`} aria-hidden="true">
              <span />
            </span>
            <div className="story-kicker">
              <span>{getCategoryLabel(content.category)}</span>
              <span aria-hidden="true">·</span>
              <span>약 {content.estimatedMinutes}분</span>
            </div>
            <h1>{content.title}</h1>
            <p>{content.summary}</p>
            <button type="button" className="edit-button" onClick={onEdit}>
              <svg viewBox="0 0 20 20" aria-hidden="true">
                <path d="m12.5 4.5 3 3M4 16l3.4-.7L16 6.7 13.3 4 4.7 12.6 4 16Z" />
              </svg>
              {isAuthenticated ? '원고 편집' : '로그인하고 원고 편집'}
            </button>
          </header>
          <div className="script-heading">
            <span>READING SCRIPT</span>
            <span>{content.script.length.toLocaleString()}자</span>
          </div>
          <div className="script-body">{content.script}</div>
        </article>

        <aside className="narration-column" aria-label="자동 내레이션">
          {children}
        </aside>
      </div>
    </main>
  );
}
