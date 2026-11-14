import { getApiBaseUrl } from '../../auth/authConfig';

export type GlowTodayPhoto = {
  id: number;
  imageUrl: string;
  caption: string;
  placeName: string;
  linkUrl: string;
};

export type GlowTodayReview = {
  id: number;
  regionLabel: string;
  placeName: string;
  placeRating: number;
  placeReviewCount: number;
  placeImageUrl: string;
  rating: number;
  content: string;
  reviewerName: string;
  createdAt: string;
  linkUrl: string;
};

export type GlowPageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type FetchGlowPageParams = {
  page: number;
  size: number;
};

const image = (id: string, w = 480, h = 300) => `https://images.unsplash.com/${id}?w=${w}&h=${h}&fit=crop`;

const PHOTO_IDS = [
  'photo-1529156069898-49953e39b3ac',
  'photo-1531058020387-3be344556be6',
  'photo-1477959858617-67f85cf4f1df',
  'photo-1507525428034-b723cf961d3e',
  'photo-1512389142860-9c449e58a543',
  'photo-1543589077-47d81606c1bf',
  'photo-1501386761578-eac5c94b800a',
  'photo-1513151233558-d860c5398176',
  'photo-1493225457124-a3eb161ffa5f',
  'photo-1555529669-e69e7aa0ba9a',
  'photo-1556228578-8c89e6adf883',
  'photo-1558636508-e0db3814bd1d',
  'photo-1513889961551-628c1e5e2ee9',
  'photo-1528821128474-27f963b062bf',
  'photo-1482517967863-00e15c9b44be',
  'photo-1574629810360-7efbbe195018',
  'photo-1535378620166-273708d44e4c',
  'photo-1514533450685-4493e01d1fdc',
  'photo-1511632765486-a01980e01a18',
  'photo-1550745165-9bc0b252726f',
  'photo-1460661419201-fd4cecdf8a8b',
  'photo-1507838153414-b4b713384a76',
] as const;

const PHOTO_PLACES = ['서울 성수', '서울 명동', '북한산', '서울 홍대', '경복궁', '서울 용산'] as const;

const FALLBACK_PHOTOS: GlowTodayPhoto[] = Array.from({ length: 44 }, (_, index) => ({
  id: index + 1,
  imageUrl: image(PHOTO_IDS[index % PHOTO_IDS.length]),
  caption: `오늘의 사진 ${index + 1}`,
  placeName: PHOTO_PLACES[index % PHOTO_PLACES.length],
  linkUrl: '/glow',
}));

const FALLBACK_REVIEWS: GlowTodayReview[] = [
  {
    id: 1,
    regionLabel: '서울 성수',
    placeName: 'A_KNACK 성수 | 서울의 트렌디한 헤어 살롱에서 컷, 컬러, 펌',
    placeRating: 5,
    placeReviewCount: 167,
    placeImageUrl: image('photo-1529156069898-49953e39b3ac', 160, 160),
    rating: 5,
    content:
      '정말 멋진 경험이었어요! 머리가 자라면서 보기 좋게 보이는 레이어드 헤어스타일과 컬러를 원했어요. 영오는 내가 원하는 것을 주의 깊게 듣고 내 비전을 현실로 만들어 주었다. 10점 만점에 10점! 강력히 추천합니다!',
    reviewerName: '윙 톤 양',
    createdAt: '2026-11-04',
    linkUrl: '/glow',
  },
  {
    id: 2,
    regionLabel: '서울 명동',
    placeName: '리카자 머리 명동 | 서울의 영어권 미용실',
    placeRating: 4.9,
    placeReviewCount: 833,
    placeImageUrl: image('photo-1556228578-8c89e6adf883', 160, 160),
    rating: 5,
    content: '매우 아름답고 친근한 헤어스타일리스트 제니와 잘생긴 인턴 남성을 강력히 추천합니다.',
    reviewerName: '사용자1138580',
    createdAt: '2026-11-04',
    linkUrl: '/glow',
  },
  {
    id: 3,
    regionLabel: '서울 은평',
    placeName: 'K-트레킹 챌린지 | 북한산 국립공원(은평) | 무료 봉제인형 증정',
    placeRating: 5,
    placeReviewCount: 8,
    placeImageUrl: image('photo-1477959858617-67f85cf4f1df', 160, 160),
    rating: 5,
    content:
      '북한산 국립공원의 배군대봉까지 하이킹을 하며 숲길, 바위 지형, 가파른 구간, 계단을 지나 마지막 등반에서는 고정 케이블을 사용했습니다. 정상에 오르는 것은 신체적으로 힘들었지만, 산과 서울의 탁 트인 전망 덕분에 한 걸음 모두 가치 있었고, 저는 제 자신이 매우 자랑스러웠습니다. 또한 관리가 잘 된 등산로 덕분에 처음 오는 사람도 안심하고 오를 수 있었습니다.',
    reviewerName: '노르자히다 빈티 아 아지즈',
    createdAt: '2026-11-04',
    linkUrl: '/glow',
  },
  {
    id: 4,
    regionLabel: '서울 홍대',
    placeName: '🎄 겨울 예약 판매 🎄 닥터 쁘띠 클리닉 홍대 | 프리미엄 스킨케어',
    placeRating: 5,
    placeReviewCount: 208,
    placeImageUrl: image('photo-1558636508-e0db3814bd1d', 160, 160),
    rating: 5,
    content:
      '저는 Dr. Petit와의 첫 방문이 매우 편안했습니다. 듀크는 나의 영어 통역사였고 매 순간 함께했다. 다른 손님들을 돌봐야 할 때도 저에게 꼭 알려주었고, 제가 익숙한 다른 언어로 다른 통역사가 개입해 돌아올 때까지 기다렸습니다. 의사 선생님 본인은 매우 친절하고 유익했습니다. 장기 치료 계획도 차근차근 설명해 주셨습니다.',
    reviewerName: '스테파니',
    createdAt: '2026-11-04',
    linkUrl: '/glow',
  },
  {
    id: 5,
    regionLabel: '서울 종로',
    placeName: '경복궁 한복 체험 | 하루 대여와 사진 촬영 패키지',
    placeRating: 4.8,
    placeReviewCount: 412,
    placeImageUrl: image('photo-1511632765486-a01980e01a18', 160, 160),
    rating: 4,
    content:
      '한복 종류가 많아서 고르는 재미가 있었어요. 머리 장식까지 해 주셔서 궁 안에서 찍은 사진이 정말 예쁘게 나왔습니다. 주말에는 사람이 많으니 예약하고 가는 걸 추천해요.',
    reviewerName: '민지',
    createdAt: '2026-11-03',
    linkUrl: '/glow',
  },
  {
    id: 6,
    regionLabel: '서울 용산',
    placeName: '하이브 인사이트 투어 | K-POP 전시와 굿즈 숍',
    placeRating: 4.7,
    placeReviewCount: 529,
    placeImageUrl: image('photo-1493225457124-a3eb161ffa5f', 160, 160),
    rating: 5,
    content: '전시 구성이 알차고 체험 공간이 많아서 시간 가는 줄 몰랐어요. 굿즈 숍은 오픈 직후에 가야 원하는 물건을 살 수 있어요.',
    reviewerName: 'Hana',
    createdAt: '2026-11-03',
    linkUrl: '/glow',
  },
];

function createFallbackPage<T>(items: readonly T[], params: FetchGlowPageParams): GlowPageResponse<T> {
  const start = params.page * params.size;
  return {
    items: items.slice(start, start + params.size),
    page: params.page,
    size: params.size,
    totalElements: items.length,
    totalPages: Math.max(1, Math.ceil(items.length / params.size)),
    hasNext: start + params.size < items.length,
  };
}

async function fetchGlowPage<T>(
  path: string,
  params: FetchGlowPageParams,
  fallback: readonly T[],
): Promise<GlowPageResponse<T>> {
  const query = new URLSearchParams({ page: String(params.page), size: String(params.size) });

  try {
    const response = await fetch(`${getApiBaseUrl()}${path}?${query.toString()}`, {
      cache: 'no-store',
      credentials: 'include',
    });

    if (!response.ok) {
      throw new Error(`${path} request failed: ${response.status}`);
    }

    return (await response.json()) as GlowPageResponse<T>;
  } catch {
    return createFallbackPage(fallback, params);
  }
}

export function fetchGlowTodayPhotos(params: FetchGlowPageParams): Promise<GlowPageResponse<GlowTodayPhoto>> {
  return fetchGlowPage('/api/glow-reviews/photos', params, FALLBACK_PHOTOS);
}

export function fetchGlowTodayReviews(params: FetchGlowPageParams): Promise<GlowPageResponse<GlowTodayReview>> {
  return fetchGlowPage('/api/glow-reviews', params, FALLBACK_REVIEWS);
}
