import { getApiBaseUrl } from '../../auth/authConfig';

export type GlowPopupItem = {
  id: number;
  title: string;
  emoji: string | null;
  startDate: string;
  endDate: string;
  regionLabel: string;
  imageUrl: string;
  linkUrl: string;
};

export type GlowPopupPageResponse = {
  items: GlowPopupItem[];
  date: string;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type FetchGlowPopupsParams = {
  date: string;
  page: number;
  size: number;
};

const image = (id: string) => `https://images.unsplash.com/${id}?w=400&h=520&fit=crop`;

const FALLBACK_ITEMS: GlowPopupItem[] = [
  { id: 1, title: "TWS 2싱글 앨범 'TO US' 팝업", emoji: null, startDate: '2026-11-02', endDate: '2026-11-07', regionLabel: '서울 용산', imageUrl: image('photo-1501386761578-eac5c94b800a'), linkUrl: '/event-info' },
  { id: 2, title: '갤러리아 백화점 무역센터 지점 크리스마스 테마 오픈', emoji: '🎄', startDate: '2026-10-27', endDate: '2031-01-01', regionLabel: '서울 강남', imageUrl: image('photo-1512389142860-9c449e58a543'), linkUrl: '/event-info' },
  { id: 3, title: '명동의 크리스마스', emoji: '🎄', startDate: '2026-10-30', endDate: '2026-12-31', regionLabel: '서울 남대문', imageUrl: image('photo-1543589077-47d81606c1bf'), linkUrl: '/event-info' },
  { id: 4, title: '2026 신세계연말 시장', emoji: '🎁', startDate: '2026-10-30', endDate: '2026-12-28', regionLabel: '서울 서초', imageUrl: image('photo-1482517967863-00e15c9b44be'), linkUrl: '/event-info' },
  { id: 5, title: '이상하고 신선한 스트레이 키즈 팝업', emoji: '🛒', startDate: '2026-10-22', endDate: '2026-11-08', regionLabel: '서울 강남', imageUrl: image('photo-1513151233558-d860c5398176'), linkUrl: '/event-info' },
  { id: 6, title: "D'FESTA 기원 전시", emoji: null, startDate: '2026-10-23', endDate: '2026-11-30', regionLabel: '서울 영등포', imageUrl: image('photo-1531058020387-3be344556be6'), linkUrl: '/event-info' },
  { id: 7, title: '메디큐브 x 포에버 체리 팝업', emoji: null, startDate: '2026-07-24', endDate: '2026-11-30', regionLabel: '서울 명동', imageUrl: image('photo-1528821128474-27f963b062bf'), linkUrl: '/event-info' },
  { id: 8, title: '동물의 숲: 뉴 호라이즌 타누키 샵 오픈', emoji: null, startDate: '2026-10-02', endDate: '2031-01-01', regionLabel: '서울 용산', imageUrl: image('photo-1555529669-e69e7aa0ba9a'), linkUrl: '/event-info' },
  { id: 9, title: '로미엔드 10번째 생일 파티', emoji: '💗', startDate: '2026-10-12', endDate: '2026-11-10', regionLabel: '서울 성동', imageUrl: image('photo-1558636508-e0db3814bd1d'), linkUrl: '/event-info' },
  { id: 10, title: '센녹 목욕탕 성수 개방', emoji: null, startDate: '2026-10-07', endDate: '2031-01-01', regionLabel: '서울 성동', imageUrl: image('photo-1556228578-8c89e6adf883'), linkUrl: '/event-info' },
  { id: 11, title: '2026 BTS 달마정 팝업', emoji: null, startDate: '2026-09-29', endDate: '2026-11-30', regionLabel: '서울 용산', imageUrl: image('photo-1493225457124-a3eb161ffa5f'), linkUrl: '/event-info' },
  { id: 12, title: '롯데 월드 어드벤처 미스터리 파티', emoji: null, startDate: '2026-09-12', endDate: '2026-11-15', regionLabel: '서울 송파', imageUrl: image('photo-1513889961551-628c1e5e2ee9'), linkUrl: '/event-info' },
  { id: 13, title: '토리든 커넥트 X 코르티스', emoji: null, startDate: '2026-07-15', endDate: '2026-12-31', regionLabel: '서울 성동', imageUrl: image('photo-1529156069898-49953e39b3ac'), linkUrl: '/event-info' },
  { id: 14, title: '스카이 전망대 오픈', emoji: null, startDate: '2026-07-09', endDate: '2031-01-01', regionLabel: '서울 광화문', imageUrl: image('photo-1477959858617-67f85cf4f1df'), linkUrl: '/event-info' },
  { id: 15, title: '신라면 40주년 팝업', emoji: null, startDate: '2026-06-16', endDate: '2026-11-30', regionLabel: '서울 성동', imageUrl: image('photo-1507525428034-b723cf961d3e'), linkUrl: '/event-info' },
  { id: 16, title: '팝 마트 월드컵', emoji: '⚽', startDate: '2026-05-29', endDate: '2031-01-01', regionLabel: '서울 마포', imageUrl: image('photo-1574629810360-7efbbe195018'), linkUrl: '/event-info' },
  { id: 17, title: '한강 야간 미디어 아트 산책', emoji: '✨', startDate: '2026-11-05', endDate: '2026-11-20', regionLabel: '서울 여의도', imageUrl: image('photo-1550745165-9bc0b252726f'), linkUrl: '/event-info' },
  { id: 18, title: '성수 빈티지 레코드 페어', emoji: '🎵', startDate: '2026-11-06', endDate: '2026-11-16', regionLabel: '서울 성동', imageUrl: image('photo-1514533450685-4493e01d1fdc'), linkUrl: '/event-info' },
];

function createFallbackGlowPopupsPage(params: FetchGlowPopupsParams): GlowPopupPageResponse {
  const running = FALLBACK_ITEMS.filter(
    (item) => item.startDate <= params.date && params.date <= item.endDate,
  );
  const start = params.page * params.size;
  const items = running.slice(start, start + params.size);
  const totalPages = Math.max(1, Math.ceil(running.length / params.size));

  return {
    items,
    date: params.date,
    page: params.page,
    size: params.size,
    totalElements: running.length,
    totalPages,
    hasNext: start + params.size < running.length,
  };
}

export async function fetchGlowPopups(params: FetchGlowPopupsParams): Promise<GlowPopupPageResponse> {
  const query = new URLSearchParams({
    date: params.date,
    page: String(params.page),
    size: String(params.size),
  });

  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-popups?${query.toString()}`, {
      cache: 'no-store',
      credentials: 'include',
    });

    if (!response.ok) {
      throw new Error(`Glow popups request failed: ${response.status}`);
    }

    return (await response.json()) as GlowPopupPageResponse;
  } catch {
    return createFallbackGlowPopupsPage(params);
  }
}
