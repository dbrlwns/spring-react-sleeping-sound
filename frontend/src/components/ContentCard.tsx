import {
  getCategoryLabel,
  getCategoryTone,
  type KnowledgeContentSummary,
} from '../domain/content';

interface ContentCardProps {
  content: KnowledgeContentSummary;
  onOpen: (id: string) => void;
}

export function ContentCard({ content, onOpen }: ContentCardProps) {
  const tone = getCategoryTone(content.category);
  return (
    <article className="content-card">
      <button type="button" className="content-card-button" onClick={() => onOpen(content.id)}>
        <span className={`content-art content-art--${tone}`} aria-hidden="true">
          <span className="orbit orbit--outer" />
          <span className="orbit orbit--inner" />
          <span className="celestial-dot" />
          <span className="art-index">{content.id.padStart(2, '0').slice(-2)}</span>
        </span>
        <span className="content-card-body">
          <span className="card-meta">
            <span className="category-label">{getCategoryLabel(content.category)}</span>
            <span>{content.estimatedMinutes > 0 ? `${content.estimatedMinutes}분` : '원고 보기'}</span>
          </span>
          <strong>{content.title}</strong>
          <span className="card-summary">{content.summary}</span>
          <span className="card-link">
            이야기 열기
            <svg viewBox="0 0 20 20" aria-hidden="true">
              <path d="M4 10h11m-4-4 4 4-4 4" />
            </svg>
          </span>
        </span>
      </button>
    </article>
  );
}
