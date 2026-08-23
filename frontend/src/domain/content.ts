export const CONTENT_CATEGORIES = [
  { id: 'ALL', label: '전체' },
  { id: 'SCIENCE', label: '과학' },
  { id: 'SOCIETY', label: '사회' },
  { id: 'HISTORY', label: '역사' },
  { id: 'PHILOSOPHY', label: '철학' },
  { id: 'ECONOMY', label: '경제' },
  { id: 'TECHNOLOGY', label: '기술' },
  { id: 'CULTURE', label: '문화' },
  { id: 'PSYCHOLOGY', label: '심리' },
] as const;

const LEGACY_CATEGORY_ALIASES: Readonly<Record<string, string>> = {
  QUANTUM_PHYSICS: 'SCIENCE',
  COSMOLOGY: 'SCIENCE',
  ASTRONOMY: 'SCIENCE',
  GENERAL_SCIENCE: 'SCIENCE',
};

export interface KnowledgeContentSummary {
  id: string;
  title: string;
  summary: string;
  category: string;
  scriptPreview?: string;
  estimatedMinutes: number;
}

export interface KnowledgeContent extends KnowledgeContentSummary {
  script: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ContentDraft {
  title: string;
  summary: string;
  category: string;
  script: string;
}

export const CONTENT_CONSTRAINTS = {
  maxTitleLength: 120,
  maxSummaryLength: 500,
  maxScriptLength: 20_000,
} as const;

const CATEGORY_LABELS: ReadonlyMap<string, string> = new Map(
  CONTENT_CATEGORIES.map((category) => [category.id, category.label]),
);

export function normalizeContentCategory(category: string): string {
  return LEGACY_CATEGORY_ALIASES[category] ?? category;
}

export function getCategoryLabel(category: string): string {
  const normalizedCategory = normalizeContentCategory(category);
  return (
    CATEGORY_LABELS.get(normalizedCategory) ??
    normalizedCategory
      .toLowerCase()
      .split('_')
      .filter(Boolean)
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ') ??
    '기타'
  );
}

export function estimateListeningMinutes(script: string): number {
  const normalizedLength = script.replace(/\s/g, '').length;
  return Math.max(1, Math.ceil(normalizedLength / 330));
}

export function getCategoryTone(category: string): string {
  const tones: Record<string, string> = {
    SCIENCE: 'violet',
    SOCIETY: 'navy',
    HISTORY: 'sand',
    PHILOSOPHY: 'blue',
    ECONOMY: 'green',
    TECHNOLOGY: 'navy',
    CULTURE: 'sand',
    PSYCHOLOGY: 'violet',
  };
  return tones[normalizeContentCategory(category)] ?? 'sand';
}
