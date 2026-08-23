import { useMemo, useState } from 'react';
import {
  CONTENT_CATEGORIES,
  normalizeContentCategory,
  type KnowledgeContentSummary,
} from '../domain/content';
import { ContentCard } from './ContentCard';

interface ContentLibraryProps {
  contents: KnowledgeContentSummary[];
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
  onOpen: (id: string) => void;
  onRetry: () => void;
}

export function ContentLibrary({
  contents,
  status,
  error,
  onOpen,
  onRetry,
}: ContentLibraryProps) {
  const [category, setCategory] = useState('ALL');
  const [query, setQuery] = useState('');
  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return contents.filter((content) => {
      const categoryMatches =
        category === 'ALL' || normalizeContentCategory(content.category) === category;
      const queryMatches =
        !normalizedQuery ||
        `${content.title} ${content.summary}`.toLowerCase().includes(normalizedQuery);
      return categoryMatches && queryMatches;
    });
  }, [category, contents, query]);

  return (
    <section className="library-section" aria-labelledby="library-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">TONIGHT'S LIBRARY</p>
          <h2 id="library-title">오늘 밤의 이야기</h2>
        </div>
        <label className="search-field">
          <span className="sr-only">콘텐츠 검색</span>
          <svg viewBox="0 0 20 20" aria-hidden="true">
            <circle cx="8.5" cy="8.5" r="5.5" />
            <path d="m13 13 4 4" />
          </svg>
          <input
            type="search"
            value={query}
            placeholder="이야기 검색"
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
      </div>

      <div className="category-tabs" aria-label="주제별 보기">
        {CONTENT_CATEGORIES.map((item) => (
          <button
            key={item.id}
            type="button"
            className={category === item.id ? 'category-tab is-active' : 'category-tab'}
            aria-pressed={category === item.id}
            onClick={() => setCategory(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>

      {status === 'loading' && (
        <div className="card-grid" aria-busy="true" aria-label="콘텐츠 불러오는 중">
          {[0, 1, 2].map((item) => (
            <div className="content-card card-skeleton" key={item} aria-hidden="true">
              <span />
              <span />
            </div>
          ))}
        </div>
      )}

      {status === 'error' && (
        <div className="state-panel" role="alert">
          <span className="state-symbol">!</span>
          <h3>이야기를 불러오지 못했어요</h3>
          <p>{error}</p>
          <button type="button" className="secondary-button" onClick={onRetry}>
            다시 불러오기
          </button>
        </div>
      )}

      {status === 'success' && filtered.length > 0 && (
        <div className="card-grid">
          {filtered.map((content) => (
            <ContentCard key={content.id} content={content} onOpen={onOpen} />
          ))}
        </div>
      )}

      {status === 'success' && filtered.length === 0 && (
        <div className="state-panel">
          <span className="state-symbol" aria-hidden="true">⌁</span>
          <h3>조건에 맞는 이야기가 없어요</h3>
          <p>검색어를 바꾸거나 다른 주제를 둘러보세요.</p>
        </div>
      )}
    </section>
  );
}
