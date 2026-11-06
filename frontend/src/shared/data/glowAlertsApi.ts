import { getApiBaseUrl } from '../../auth/authConfig';

export type GlowAlertTab = 'alert' | 'conversation';
export type GlowAlertStatus = 'unread' | 'read';
export type GlowAlertCategoryId = 'all' | 'verification' | 'activity' | 'finance';

export type GlowAlertItem = {
  id: number;
  tab: GlowAlertTab;
  category: Exclude<GlowAlertCategoryId, 'all'>;
  categoryLabel: string;
  senderName: string;
  channelLabel: string | null;
  message: string;
  receivedAt: string;
  receivedAtLabel: string;
  iconText: string;
  iconTone: string;
  unread: boolean;
};

export type GlowAlertCategory = {
  id: GlowAlertCategoryId;
  label: string;
  count: number;
};

export type GlowAlertPageResponse = {
  items: GlowAlertItem[];
  categories: GlowAlertCategory[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  tab: GlowAlertStatus;
  activeCategory: GlowAlertCategoryId;
};

export type FetchGlowAlertsParams = {
  page: number;
  size: number;
  status: GlowAlertStatus;
  category: GlowAlertCategoryId;
  q?: string;
  accessToken?: string | null;
};

const READ_STORAGE_KEY = 'idolglow:glow-alert-read-ids';
export const GLOW_ALERTS_CHANGED_EVENT = 'glow-alerts:changed';

const FALLBACK_ITEMS: GlowAlertItem[] = [
  {
    id: 1,
    tab: 'alert',
    category: 'verification',
    categoryLabel: '회원인증',
    senderName: '라온시큐어/예술경영지원...',
    channelLabel: null,
    message: '네이버 인증서로 안전하게 인증되었어요. 사용 이력을 확인해보세요.',
    receivedAt: '2026-04-28T01:22:00',
    receivedAtLabel: '1일전',
    iconText: 'CM',
    iconTone: '#fff1f6',
    unread: true,
  },
  {
    id: 2,
    tab: 'alert',
    category: 'activity',
    categoryLabel: '활동소식',
    senderName: '야생뉴',
    channelLabel: '블로그',
    message: '네 로드3 부스터라고 강화에요 no3 강화일까요?',
    receivedAt: '2026-04-26T14:12:00',
    receivedAtLabel: '3일전',
    iconText: '야',
    iconTone: '#f2e7e2',
    unread: false,
  },
  {
    id: 3,
    tab: 'alert',
    category: 'activity',
    categoryLabel: '활동소식',
    senderName: '마왕군간부',
    channelLabel: '블로그',
    message: '게다가 신장에 녹스의 미드나잇새도우 폼이 나오네요 기대돼요',
    receivedAt: '2026-04-25T18:34:00',
    receivedAtLabel: '4일전',
    iconText: '마',
    iconTone: '#ece6dc',
    unread: false,
  },
  {
    id: 4,
    tab: 'alert',
    category: 'finance',
    categoryLabel: '금융·자산',
    senderName: '신용점수',
    channelLabel: null,
    message: '이번 달 내 신용점수의 예상 최저금리가 변경되었어요. 대출리포트에서 확인해보세요.',
    receivedAt: '2026-04-22T09:05:00',
    receivedAtLabel: '1주전',
    iconText: 'W',
    iconTone: '#dcfce7',
    unread: false,
  },
  {
    id: 5,
    tab: 'alert',
    category: 'verification',
    categoryLabel: '회원인증',
    senderName: 'IDOL GLOW',
    channelLabel: '보안',
    message: '새 기기에서 로그인했어요. 본인이 맞는지 로그인 기록을 확인해 주세요.',
    receivedAt: '2026-04-21T20:16:00',
    receivedAtLabel: '1주전',
    iconText: 'IG',
    iconTone: '#eef2ff',
    unread: true,
  },
  {
    id: 6,
    tab: 'alert',
    category: 'activity',
    categoryLabel: '활동소식',
    senderName: 'Glow 큐레이션',
    channelLabel: '아카이브',
    message: '관심 아티스트의 신규 공연 아카이브가 업데이트되었습니다.',
    receivedAt: '2026-04-20T11:18:00',
    receivedAtLabel: '1주전',
    iconText: 'G',
    iconTone: '#fef3c7',
    unread: false,
  },
  {
    id: 7,
    tab: 'alert',
    category: 'finance',
    categoryLabel: '금융·자산',
    senderName: '결제알림',
    channelLabel: '예약',
    message: '예매 결제 영수증이 발급되었어요. 마이페이지에서 확인할 수 있습니다.',
    receivedAt: '2026-04-18T16:40:00',
    receivedAtLabel: '1주전',
    iconText: 'P',
    iconTone: '#e0f2fe',
    unread: false,
  },
  {
    id: 8,
    tab: 'alert',
    category: 'activity',
    categoryLabel: '활동소식',
    senderName: '공지',
    channelLabel: '뉴스',
    message: '이번 주 인기 아티클과 이벤트를 모아봤어요.',
    receivedAt: '2026-04-17T08:00:00',
    receivedAtLabel: '1주전',
    iconText: 'N',
    iconTone: '#f1f5f9',
    unread: false,
  },
  {
    id: 9,
    tab: 'conversation',
    category: 'activity',
    categoryLabel: '활동소식',
    senderName: 'Glow 매니저',
    channelLabel: '대화',
    message: '문의하신 전시 예약 변경 가능 시간을 안내드렸습니다.',
    receivedAt: '2026-04-28T10:30:00',
    receivedAtLabel: '1일전',
    iconText: 'GM',
    iconTone: '#ede9fe',
    unread: true,
  },
  {
    id: 10,
    tab: 'conversation',
    category: 'verification',
    categoryLabel: '회원인증',
    senderName: '고객센터',
    channelLabel: '인증',
    message: '본인 확인 절차가 완료되어 답변을 이어서 확인할 수 있어요.',
    receivedAt: '2026-04-27T15:05:00',
    receivedAtLabel: '2일전',
    iconText: 'CS',
    iconTone: '#fee2e2',
    unread: false,
  },
  {
    id: 11,
    tab: 'conversation',
    category: 'finance',
    categoryLabel: '금융·자산',
    senderName: '결제상담',
    channelLabel: '대화',
    message: '환불 접수 상태가 업데이트되었습니다.',
    receivedAt: '2026-04-24T12:12:00',
    receivedAtLabel: '5일전',
    iconText: '₩',
    iconTone: '#dcfce7',
    unread: false,
  },
  {
    id: 12,
    tab: 'alert',
    category: 'activity',
    categoryLabel: '활동소식',
    senderName: '만료 테스트',
    channelLabel: '아카이브',
    message: '한 달이 지난 알림은 데이터가 남아 있어도 목록에 노출되지 않습니다.',
    receivedAt: '2026-03-20T10:00:00',
    receivedAtLabel: '5주전',
    iconText: 'OLD',
    iconTone: '#e5e7eb',
    unread: true,
  },
];

export async function fetchGlowAlerts(params: FetchGlowAlertsParams): Promise<GlowAlertPageResponse> {
  const query = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
    status: params.status,
    category: params.category,
  });
  const keyword = params.q?.trim();
  if (keyword) {
    query.set('q', keyword);
  }

  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-alerts?${query.toString()}`, {
      cache: 'no-store',
      credentials: 'include',
      headers: withAuthHeaders(params.accessToken),
    });

    if (!response.ok) {
      throw new Error(`Glow alerts request failed: ${response.status}`);
    }

    return (await response.json()) as GlowAlertPageResponse;
  } catch {
    return createFallbackGlowAlertsPage(params);
  }
}

export async function fetchGlowAlertUnreadCount(accessToken?: string | null): Promise<number> {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-alerts/unread-count`, {
      cache: 'no-store',
      credentials: 'include',
      headers: withAuthHeaders(accessToken),
    });

    if (!response.ok) {
      throw new Error(`Glow unread count request failed: ${response.status}`);
    }

    const body = (await response.json()) as { count?: number };
    return Number.isFinite(body.count) ? Number(body.count) : 0;
  } catch {
    const readIds = readStoredAlertIds();
    return FALLBACK_ITEMS.filter(item => item.unread && !readIds.has(item.id) && isVisibleAlert(item, new Date())).length;
  }
}

export async function markGlowAlertRead(alertId: number, accessToken?: string | null): Promise<number> {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-alerts/${alertId}/read`, {
      method: 'POST',
      cache: 'no-store',
      credentials: 'include',
      headers: withAuthHeaders(accessToken),
    });

    if (!response.ok) {
      throw new Error(`Glow mark read request failed: ${response.status}`);
    }

    const body = (await response.json()) as { count?: number };
    const unreadCount = Number.isFinite(body.count) ? Number(body.count) : 0;
    emitGlowAlertsChanged(unreadCount);
    return unreadCount;
  } catch {
    const readIds = readStoredAlertIds();
    readIds.add(alertId);
    writeStoredAlertIds(readIds);
    const unreadCount = FALLBACK_ITEMS.filter(
      item => item.unread && !readIds.has(item.id) && isVisibleAlert(item, new Date())
    ).length;
    emitGlowAlertsChanged(unreadCount);
    return unreadCount;
  }
}

function createFallbackGlowAlertsPage(params: FetchGlowAlertsParams): GlowAlertPageResponse {
  const now = new Date();
  const readIds = readStoredAlertIds();
  const keyword = params.q?.trim().toLowerCase() ?? '';
  const tabItems = FALLBACK_ITEMS
    .filter(item => isVisibleAlert(item, now))
    .filter(item => matchesKeyword(item, keyword));
  const statusItems = tabItems.filter(item => matchesStatus(item, params.status, readIds));
  const filteredItems = statusItems.filter(item => params.category === 'all' || item.category === params.category);
  const page = Math.max(1, params.page);
  const size = Math.min(Math.max(1, params.size), 20);
  const fromIndex = Math.min((page - 1) * size, filteredItems.length);
  const toIndex = Math.min(fromIndex + size, filteredItems.length);
  const totalPages = filteredItems.length === 0 ? 0 : Math.ceil(filteredItems.length / size);

  return {
    items: filteredItems.slice(fromIndex, toIndex).map(item => ({
      ...item,
      receivedAtLabel: formatRelativeTime(item.receivedAt, now),
    })),
    categories: buildFallbackCategories(statusItems, readIds),
    page,
    size,
    totalElements: filteredItems.length,
    totalPages,
    hasNext: toIndex < filteredItems.length,
    tab: params.status,
    activeCategory: params.category,
  };
}

function emitGlowAlertsChanged(unreadCount: number): void {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(
    new CustomEvent(GLOW_ALERTS_CHANGED_EVENT, {
      detail: { unreadCount },
    })
  );
}

function buildFallbackCategories(items: GlowAlertItem[], readIds: Set<number>): GlowAlertCategory[] {
  const effectiveItems = items.map(item => ({ ...item, unread: item.unread && !readIds.has(item.id) }));
  const count = (category: GlowAlertCategoryId) =>
    category === 'all' ? effectiveItems.length : effectiveItems.filter(item => item.category === category).length;

  return [
    { id: 'all', label: '전체', count: count('all') },
    { id: 'verification', label: '회원인증', count: count('verification') },
    { id: 'activity', label: '활동소식', count: count('activity') },
    { id: 'finance', label: '금융·자산', count: count('finance') },
  ];
}

function matchesStatus(item: GlowAlertItem, status: GlowAlertStatus, readIds: Set<number>): boolean {
  const effectiveUnread = item.unread && !readIds.has(item.id);
  return status === 'unread' ? effectiveUnread : !effectiveUnread;
}

function isVisibleAlert(item: GlowAlertItem, now: Date): boolean {
  const receivedAt = new Date(item.receivedAt);
  const cutoff = new Date(now);
  cutoff.setMonth(cutoff.getMonth() - 1);
  return !Number.isNaN(receivedAt.getTime()) && receivedAt >= cutoff;
}

function matchesKeyword(item: GlowAlertItem, keyword: string): boolean {
  if (!keyword) return true;
  return [item.senderName, item.channelLabel, item.categoryLabel, item.message]
    .filter((value): value is string => Boolean(value))
    .some(value => value.toLowerCase().includes(keyword));
}

function formatRelativeTime(value: string, now: Date): string {
  const receivedAt = new Date(value);
  if (Number.isNaN(receivedAt.getTime())) return '';
  const diffMinutes = Math.max(0, Math.floor((now.getTime() - receivedAt.getTime()) / 60000));

  if (diffMinutes < 1) return '방금 전';
  if (diffMinutes < 60) return `${diffMinutes}분전`;
  if (diffMinutes < 60 * 24) return `${Math.floor(diffMinutes / 60)}시간전`;
  if (diffMinutes < 60 * 24 * 7) return `${Math.floor(diffMinutes / (60 * 24))}일전`;
  return `${Math.max(1, Math.floor(diffMinutes / (60 * 24 * 7)))}주전`;
}

function readStoredAlertIds(): Set<number> {
  if (typeof window === 'undefined') return new Set();
  try {
    const raw = window.localStorage.getItem(READ_STORAGE_KEY);
    if (!raw) return new Set();
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return new Set();
    return new Set(parsed.map(Number).filter(Number.isFinite));
  } catch {
    return new Set();
  }
}

function writeStoredAlertIds(ids: Set<number>): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(READ_STORAGE_KEY, JSON.stringify([...ids]));
}

function withAuthHeaders(accessToken?: string | null): HeadersInit | undefined {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined;
}
