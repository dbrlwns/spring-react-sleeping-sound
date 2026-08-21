export const CONTENT_CATEGORIES = [
  { id: 'ALL', label: '전체' },
  { id: 'QUANTUM_PHYSICS', label: '양자역학' },
  { id: 'COSMOLOGY', label: '우주론' },
  { id: 'ASTRONOMY', label: '천문학' },
  { id: 'GENERAL_SCIENCE', label: '과학 교양' },
] as const;

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

export function getCategoryLabel(category: string): string {
  return (
    CATEGORY_LABELS.get(category) ??
    category
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
    QUANTUM_PHYSICS: 'violet',
    COSMOLOGY: 'navy',
    ASTRONOMY: 'blue',
    GENERAL_SCIENCE: 'green',
  };
  return tones[category] ?? 'sand';
}
