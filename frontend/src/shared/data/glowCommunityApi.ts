import { getApiBaseUrl } from '../../auth/authConfig';

export type GlowCommunityTag = 'talk' | 'question' | 'travel';
export type GlowCommunityTagFilter = 'all' | GlowCommunityTag;
export type GlowCommunitySort = 'latest' | 'popular';

export const GLOW_COMMUNITY_TAG_LABELS: Record<GlowCommunityTag, string> = {
  talk: '토크',
  question: '질문',
  travel: '나의 여행 계획',
};

export type GlowCommunityPost = {
  id: number;
  tag: GlowCommunityTag;
  title: string;
  content: string;
  authorName: string;
  authorId: number | null;
  createdAt: string;
  likeCount: number;
  commentCount: number;
  liked: boolean;
};

export type GlowCommunityPageResponse = {
  items: GlowCommunityPost[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type FetchGlowCommunityPostsParams = {
  tag: GlowCommunityTagFilter;
  q: string;
  sort: GlowCommunitySort;
  page: number;
  size: number;
  accessToken?: string | null;
};

export type CreateGlowCommunityPostParams = {
  tag: GlowCommunityTag;
  title: string;
  content: string;
  authorName: string;
  authorId: number;
  accessToken?: string | null;
};

export const GLOW_COMMUNITY_SUGGESTED_KEYWORDS = [
  '공항 익스프레스 열차(AREX) 할인 티켓 | 인천공항 서울행 11',
  'KTX 기차 티켓 | 온라인으로 티켓 예약 5',
  '서울 🚌 청주공항 익스프레스버스',
  '명동 맛집',
  '한복 대여',
  '성수 팝업',
  '남산타워 야경',
] as const;

const SEED_POSTS: Omit<GlowCommunityPost, 'id' | 'liked'>[] = [
  { tag: 'talk', title: '이번 11월에 서울을 방문할 예정인가요? 명동 광장 미션 페스타에 참여하세요!', content: '명동 광장에서 열리는 미션 페스타 일정과 참여 방법을 공유합니다.', authorName: '익명', authorId: null, createdAt: '2026-11-02', likeCount: 0, commentCount: 0 },
  { tag: 'question', title: '배달 서비스', content: '외국인도 쓸 수 있는 배달 앱이 있을까요?', authorName: '사용자1444148', authorId: null, createdAt: '2026-11-01', likeCount: 0, commentCount: 1 },
  { tag: 'talk', title: '어머니의 장부를 통해 한국 역사를 다룬 감동적인 다큐멘터리', content: '최근 본 다큐멘터리 추천합니다.', authorName: '게스트 사용자', authorId: null, createdAt: '2026-10-31', likeCount: 0, commentCount: 0 },
  { tag: 'talk', title: '엄기준의 위엄: 대행로에서 꼭 봐야 할 뮤지컬', content: '대학로 뮤지컬 관람 후기입니다.', authorName: '게스트 사용자', authorId: null, createdAt: '2026-10-30', likeCount: 0, commentCount: 0 },
  { tag: 'travel', title: '한국 스킨케어가 탐구✨할 가치가 있는 이유', content: '스킨케어 투어 계획을 세우고 있어요.', authorName: '익명', authorId: null, createdAt: '2026-10-24', likeCount: 1, commentCount: 0 },
  { tag: 'talk', title: '서울에서 안전하게 사람들을 만날 방법을 찾고 계신가요?', content: '언어 교환 모임 정보를 나눠요.', authorName: '게스트 사용자', authorId: null, createdAt: '2026-08-06', likeCount: 1, commentCount: 1 },
  { tag: 'question', title: '응원봉은 어디서 받을 수 있나요?', content: '콘서트 응원봉 수령 장소가 궁금합니다.', authorName: '익명', authorId: null, createdAt: '2026-08-06', likeCount: 1, commentCount: 1 },
  { tag: 'travel', title: '홍대, 한국 단독 여행: 3일 이박 | 혼자서도 즐길 수 있는 의학 미학과', content: '홍대 중심 2박 3일 혼자 여행 일정입니다.', authorName: '마이코', authorId: null, createdAt: '2026-08-06', likeCount: 1, commentCount: 0 },
  { tag: 'question', title: '추천하는 유명 카페', content: '성수나 연남 쪽 카페 추천 부탁드려요.', authorName: '익명', authorId: null, createdAt: '2026-07-30', likeCount: 81, commentCount: 3 },
  { tag: 'question', title: '스파 온 에어.', content: '스파 예약해 보신 분 계신가요?', authorName: '게스트 사용자', authorId: null, createdAt: '2026-07-21', likeCount: 44, commentCount: 5 },
  { tag: 'question', title: '유명한 한국 바비큐 레스토랑 추천해 주세요', content: '고기 맛집 리스트 공유 부탁드립니다.', authorName: '익명', authorId: null, createdAt: '2026-07-18', likeCount: 40, commentCount: 4 },
  { tag: 'talk', title: '한강 피크닉 준비물 체크리스트', content: '돗자리, 텐트 대여 위치 정리했어요.', authorName: '리나', authorId: null, createdAt: '2026-07-10', likeCount: 27, commentCount: 2 },
  { tag: 'travel', title: '부산 당일치기 KTX 일정 짜 봤어요', content: '해운대와 감천마을 중심 일정입니다.', authorName: '여행자K', authorId: null, createdAt: '2026-07-02', likeCount: 19, commentCount: 6 },
  { tag: 'question', title: '외국인 관광객도 T-money 카드 충전이 되나요?', content: '편의점 충전 방법이 궁금합니다.', authorName: 'Leo', authorId: null, createdAt: '2026-06-28', likeCount: 12, commentCount: 4 },
  { tag: 'travel', title: '제주 3박 4일, 렌터카 없이 다녀온 루트', content: '버스와 택시로만 다닌 일정 공유합니다.', authorName: '하늘', authorId: null, createdAt: '2026-06-20', likeCount: 33, commentCount: 7 },
  { tag: 'talk', title: '광장시장 빈대떡, 어디가 제일 맛있었나요?', content: '다들 단골집 하나씩 알려 주세요.', authorName: '익명', authorId: null, createdAt: '2026-06-15', likeCount: 21, commentCount: 9 },
  { tag: 'question', title: '콘서트 티켓 양도는 어디서 안전하게 하나요?', content: '사기 걱정 없는 방법이 있을까요?', authorName: '게스트 사용자', authorId: null, createdAt: '2026-06-09', likeCount: 15, commentCount: 6 },
  { tag: 'travel', title: '경주 한복 입고 하루 코스 추천', content: '대릉원, 첨성대, 황리단길 순서로 다녀왔어요.', authorName: '민지', authorId: null, createdAt: '2026-05-30', likeCount: 26, commentCount: 2 },
  { tag: 'talk', title: '비 오는 날 서울에서 가기 좋은 실내 전시', content: '최근 다녀온 전시 세 곳을 정리했습니다.', authorName: '아트러버', authorId: null, createdAt: '2026-05-24', likeCount: 18, commentCount: 3 },
  { tag: 'question', title: '공항에서 시내까지 심야에 이동하는 방법', content: '밤 12시 도착인데 교통편이 있을까요?', authorName: '익명', authorId: null, createdAt: '2026-05-18', likeCount: 9, commentCount: 5 },
  { tag: 'travel', title: '강릉 바다 보고 오는 당일 여행 후기', content: 'KTX로 다녀온 당일치기 코스입니다.', authorName: '여행자K', authorId: null, createdAt: '2026-05-11', likeCount: 22, commentCount: 1 },
  { tag: 'talk', title: '혼자 여행할 때 꼭 챙기는 앱 정리', content: '지도, 번역, 교통 앱을 모아 봤어요.', authorName: '리나', authorId: null, createdAt: '2026-05-03', likeCount: 30, commentCount: 8 },
  { tag: 'question', title: '남산타워 야경, 케이블카 줄 오래 서나요?', content: '주말 저녁 기준으로 궁금합니다.', authorName: 'Hana', authorId: null, createdAt: '2026-04-27', likeCount: 7, commentCount: 2 },
  { tag: 'talk', title: '성수동 팝업 오픈런 꿀팁 공유', content: '주말 팝업은 오픈 30분 전 도착을 추천해요.', authorName: '익명', authorId: null, createdAt: '2026-04-19', likeCount: 41, commentCount: 11 },
];

let fallbackPosts: GlowCommunityPost[] = SEED_POSTS.map((seed, index) => ({ ...seed, id: index + 1, liked: false }));

function withAuthHeaders(accessToken?: string | null): Record<string, string> {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}

function createFallbackPage(params: FetchGlowCommunityPostsParams): GlowCommunityPageResponse {
  const keyword = params.q.trim().toLowerCase();
  const filtered = fallbackPosts
    .filter((post) => params.tag === 'all' || post.tag === params.tag)
    .filter(
      (post) =>
        !keyword ||
        post.title.toLowerCase().includes(keyword) ||
        post.content.toLowerCase().includes(keyword),
    )
    .sort((a, b) =>
      params.sort === 'popular'
        ? b.likeCount - a.likeCount || b.createdAt.localeCompare(a.createdAt)
        : b.createdAt.localeCompare(a.createdAt) || b.id - a.id,
    );
  const start = params.page * params.size;

  return {
    items: filtered.slice(start, start + params.size),
    page: params.page,
    size: params.size,
    totalElements: filtered.length,
    totalPages: Math.max(1, Math.ceil(filtered.length / params.size)),
    hasNext: start + params.size < filtered.length,
  };
}

export async function fetchGlowCommunityPosts(
  params: FetchGlowCommunityPostsParams,
): Promise<GlowCommunityPageResponse> {
  const query = new URLSearchParams({
    tag: params.tag,
    sort: params.sort,
    page: String(params.page),
    size: String(params.size),
  });
  const keyword = params.q.trim();
  if (keyword) query.set('q', keyword);

  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-community/posts?${query.toString()}`, {
      cache: 'no-store',
      credentials: 'include',
      headers: withAuthHeaders(params.accessToken),
    });
    if (!response.ok) throw new Error(`Glow community request failed: ${response.status}`);
    return (await response.json()) as GlowCommunityPageResponse;
  } catch {
    return createFallbackPage(params);
  }
}

export async function fetchGlowCommunityHighlights(): Promise<GlowCommunityPost[]> {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-community/highlights`, {
      cache: 'no-store',
      credentials: 'include',
    });
    if (!response.ok) throw new Error(`Glow community highlights failed: ${response.status}`);
    return (await response.json()) as GlowCommunityPost[];
  } catch {
    return [...fallbackPosts].sort((a, b) => b.likeCount - a.likeCount).slice(0, 8);
  }
}

export async function createGlowCommunityPost(
  params: CreateGlowCommunityPostParams,
): Promise<GlowCommunityPost> {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-community/posts`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', ...withAuthHeaders(params.accessToken) },
      body: JSON.stringify({ tag: params.tag, title: params.title, content: params.content }),
    });
    if (!response.ok) throw new Error(`Glow community create failed: ${response.status}`);
    return (await response.json()) as GlowCommunityPost;
  } catch {
    const now = new Date();
    const pad = (value: number) => String(value).padStart(2, '0');
    const post: GlowCommunityPost = {
      id: Math.max(0, ...fallbackPosts.map((item) => item.id)) + 1,
      tag: params.tag,
      title: params.title,
      content: params.content,
      authorName: params.authorName,
      authorId: params.authorId,
      createdAt: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`,
      likeCount: 0,
      commentCount: 0,
      liked: false,
    };
    fallbackPosts = [post, ...fallbackPosts];
    return post;
  }
}

export async function toggleGlowCommunityLike(
  postId: number,
  accessToken?: string | null,
): Promise<{ liked: boolean; likeCount: number }> {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-community/posts/${postId}/like`, {
      method: 'POST',
      credentials: 'include',
      headers: withAuthHeaders(accessToken),
    });
    if (!response.ok) throw new Error(`Glow community like failed: ${response.status}`);
    return (await response.json()) as { liked: boolean; likeCount: number };
  } catch {
    let result = { liked: false, likeCount: 0 };
    fallbackPosts = fallbackPosts.map((post) => {
      if (post.id !== postId) return post;
      const liked = !post.liked;
      const likeCount = Math.max(0, post.likeCount + (liked ? 1 : -1));
      result = { liked, likeCount };
      return { ...post, liked, likeCount };
    });
    return result;
  }
}

export async function deleteGlowCommunityPost(postId: number, accessToken?: string | null): Promise<void> {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-community/posts/${postId}`, {
      method: 'DELETE',
      credentials: 'include',
      headers: withAuthHeaders(accessToken),
    });
    if (!response.ok) throw new Error(`Glow community delete failed: ${response.status}`);
  } catch {
    fallbackPosts = fallbackPosts.filter((post) => post.id !== postId);
  }
}

export async function reportGlowCommunityPost(postId: number, accessToken?: string | null): Promise<void> {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/glow-community/posts/${postId}/report`, {
      method: 'POST',
      credentials: 'include',
      headers: withAuthHeaders(accessToken),
    });
    if (!response.ok) throw new Error(`Glow community report failed: ${response.status}`);
  } catch {
    return;
  }
}
