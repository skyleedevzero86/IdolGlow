export const NOTICE_BOARD_MARKER = "notice-board";
export const EVENT_BOARD_MARKER = "event-board";

export function hasNoticeBoardMarker(tags: readonly string[]): boolean {
  return tags.some(tag => tag.trim().toLowerCase() === NOTICE_BOARD_MARKER);
}

export function hasEventBoardMarker(tags: readonly string[]): boolean {
  return tags.some(tag => tag.trim().toLowerCase() === EVENT_BOARD_MARKER);
}

export function filterOutNoticeBoardDocuments<T extends { readonly tags: readonly string[] }>(
  items: readonly T[],
): T[] {
  return items.filter(item => !hasNoticeBoardMarker(item.tags));
}

export function normalizeEventTagsForSave(tags: readonly string[]): string[] {
  const cleaned = tags
    .map(tag => tag.trim())
    .filter(Boolean)
    .filter(tag => {
      const low = tag.toLowerCase();
      return low !== NOTICE_BOARD_MARKER && low !== EVENT_BOARD_MARKER;
    });
  return Array.from(new Set([...cleaned, EVENT_BOARD_MARKER]));
}

export function isHiddenBoardMarkerTag(tag: string): boolean {
  const low = tag.trim().toLowerCase();
  return low === NOTICE_BOARD_MARKER || low === EVENT_BOARD_MARKER;
}
