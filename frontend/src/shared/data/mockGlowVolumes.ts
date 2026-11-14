import { mockArticles } from './mockArticles';

export interface GlowVolumeItem {
  readonly category: string;
  readonly title: string;
  readonly articleId: string;
}

export interface GlowVolume {
  readonly volume: number;
  readonly year: number;
  readonly month: number;
  readonly imageUrl: string;
  readonly items: readonly GlowVolumeItem[];
}

const LATEST_VOLUME = 108;
const VOLUME_COUNT = 40;

const FEATURED_ITEMS: Readonly<Record<number, readonly (readonly [string, string])[]>> = {
  108: [
    ['전시', '듣는 몸, 이동하는 소리: 우리는 어떻게 \'미래의 청취자\'가 되는가'],
    ['공연', '2026 차세대 아시아 예술인 커뮤니티 《우리는 위혀있다》'],
    ['전시', '우리는 기술시대에 어떻게 응답할 것인가'],
    ['전시', '빛이 머문 자리, 찬란한 순간을 걷다'],
    ['행사·교류', '<2026 책 읽는 GLOW> \'서(書)\'로 보고, 잇고, 나누다'],
    ['행사·교류', '아랍문화제가 보여준 아랍의 여러 얼굴'],
  ],
  107: [
    ['전시', '기계가 듣는 시대, \'인간의 듣기\'를 다시 생각하다'],
    ['전시', '2026 GLOW 미래상: 김영은 《미래의 청취자들에게: 이동하는 소리》'],
    ['칼럼', 'AI는 세계 만들기에 어떻게 관여하는가?'],
    ['행사·교류', 'GLOW 국제 콜로키엄 《코스모 아시아 피플: 지정학에서 행성성으로》'],
    ['전시', '삶을 짓는 그림, 내일을 향한 마음들'],
    ['교육·포럼', '<GLOW 아시아 예술체험-아시아 공예>'],
  ],
  106: [
    ['행사·교류', '시니어 세대를 위한 연극형 전시 관람 투어: 실크로드 인생유람'],
    ['전시', '2026 GLOW 크리에이터스 레지던시: 다가올 공동체를 향한 사변적 실험'],
    ['교육·포럼', '발효로 이어진 동북아시아 문화여행, GLOW <아시아 장르드>'],
    ['공연', '모두를 위한 극장 <아호아호 Echoing Dance>'],
    ['공연', '세 개의 감각을 잇는 2026 GLOW국립현대무용단 <코레오 커넥션>'],
    ['공연', '<2026 아리랑, 실크로드를 만나다>'],
  ],
  105: [
    ['전시', '낯선 세계에서 발견한 삶의 이야기'],
    ['전시', '지역 동시대 미술의 다원적 실험실'],
    ['공연', '뜨거운 여름, 무대에서 만나는 삶의 온기'],
    ['공연', '삶은 선물이 아니라, 잠시 빌린 것'],
    ['교육·포럼', '올여름, 제대로 된 진로 체험을 하고 싶다면 GLOW로!'],
    ['행사·교류', '"GLOW 다국어 해설사, 세계를 잇는 문화 안내자"'],
  ],
};

const CATEGORY_BY_TYPE: Readonly<Record<string, string>> = {
  exhibition: '전시',
  performance: '공연',
  exchange: '행사·교류',
  archive: '아카이브',
  video: '영상',
  event: '이벤트',
};

const buildItems = (volume: number): readonly GlowVolumeItem[] => {
  const featured = FEATURED_ITEMS[volume];
  if (featured) {
    return featured.map(([category, title], index) => ({
      category,
      title,
      articleId: mockArticles[index % mockArticles.length].id,
    }));
  }
  return Array.from({ length: 6 }, (_, index) => {
    const article = mockArticles[(volume + index) % mockArticles.length];
    return {
      category: CATEGORY_BY_TYPE[article.category] ?? 'GLOW',
      title: article.title,
      articleId: article.id,
    };
  });
};

export const mockGlowVolumes: readonly GlowVolume[] = Array.from(
  { length: VOLUME_COUNT },
  (_, offset) => {
    const volume = LATEST_VOLUME - offset;
    const monthIndex = 2026 * 12 + 9 - offset;
    return {
      volume,
      year: Math.floor(monthIndex / 12),
      month: (monthIndex % 12) + 1,
      imageUrl: mockArticles[offset % mockArticles.length].thumbnailUrl,
      items: buildItems(volume),
    };
  },
);
