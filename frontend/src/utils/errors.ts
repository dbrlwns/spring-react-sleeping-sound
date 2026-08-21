export function toErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) return error.message;
  return '알 수 없는 오류가 발생했습니다.';
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}
