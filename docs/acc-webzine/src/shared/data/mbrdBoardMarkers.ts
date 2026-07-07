/**
 * 마크다운 에디터(mbrd) 문서를 관리자 UI에서 구분하기 위한 시스템 태그.
 * - 공지사항 관리: notice-board
 * - 이벤트 게시판: event-board (공지와 동시에 쓰지 않음)
 */
export const NOTICE_BOARD_MARKER = "notice-board";
export const EVENT_BOARD_MARKER = "event-board";

export function hasNoticeBoardMarker(tags: readonly string[]): boolean {
  return tags.some(tag => tag.trim().toLowerCase() === NOTICE_BOARD_MARKER);
}

export function hasEventBoardMarker(tags: readonly string[]): boolean {
  return tags.some(tag => tag.trim().toLowerCase() === EVENT_BOARD_MARKER);
}

/** 이벤트 관리 목록/상세: 공지 전용 문서 제외 */
export function filterOutNoticeBoardDocuments<T extends { readonly tags: readonly string[] }>(
  items: readonly T[],
): T[] {
  return items.filter(item => !hasNoticeBoardMarker(item.tags));
}

/**
 * 이벤트 저장 시: 공지 마커 제거 후 event-board 부여(중복 없음).
 * 날짜 메타(event-start:/event-end:) 등은 그대로 둔다.
 */
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

/** 태그 입력 UI에 노출하지 않을 시스템 마커 */
export function isHiddenBoardMarkerTag(tag: string): boolean {
  const low = tag.trim().toLowerCase();
  return low === NOTICE_BOARD_MARKER || low === EVENT_BOARD_MARKER;
}
