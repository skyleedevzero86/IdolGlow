export type IssueCategoryKey =
  | 'exhibition'
  | 'performance'
  | 'forum'
  | 'event'
  | 'article'
  | 'video';

export interface RelatedIssueContent {
  readonly id: string;
  readonly title: string;
  readonly category: IssueCategoryKey;
  readonly imageUrl: string;
}

export interface IssueArticleSection {
  readonly id: string;
  readonly heading?: string;
  readonly paragraphs: readonly string[];
  readonly note?: string;
}

export interface IssueArticle {
  readonly id: string;
  readonly slug: string;
  readonly issueSlug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly title: string;
  readonly kicker: string;
  readonly summary: string;
  readonly heroImageUrl: string;
  readonly cardImageUrl: string;
  readonly category: IssueCategoryKey;
  readonly formatLabel: string;
  readonly tags: readonly string[];
  readonly authorName: string;
  readonly authorEmail: string;
  readonly creditLine: string;
  readonly highlightQuote?: string;
  readonly sections: readonly IssueArticleSection[];
  readonly relatedContents: readonly RelatedIssueContent[];
}

export interface IssueVolume {
  readonly id: string;
  readonly slug: string;
  readonly volume: number;
  readonly issueDate: string;
  readonly coverImageUrl: string;
  readonly teaser: string;
  readonly articles: readonly IssueArticle[];
}

export const ISSUE_CATEGORY_LABELS: Record<IssueCategoryKey, string> = {
  exhibition: '전시',
  performance: '공연',
  forum: '교육·포럼',
  event: '행사·교류',
  article: '아티클',
  video: '비디오',
};

const relatedShowcase: readonly RelatedIssueContent[] = [
  {
    id: 'related-adhd',
    title: 'ADHD, 상상력의 분출과 경계 없는 도전',
    category: 'article',
    imageUrl:
      'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80',
  },
  {
    id: 'related-songo',
    title: '끊임없이 생성하는 신비감과 송고미',
    category: 'article',
    imageUrl:
      'https://images.unsplash.com/photo-1518991791750-749a3f0d8e2f?auto=format&fit=crop&w=900&q=80',
  },
  {
    id: 'related-sculpture',
    title: '공잔안 : 현대조각과 공예 사이',
    category: 'article',
    imageUrl:
      'https://images.unsplash.com/photo-1459908676235-d5f02a50184b?auto=format&fit=crop&w=900&q=80',
  },
];

const createGenericSections = (topic: string): readonly IssueArticleSection[] => [
  {
    id: `${topic}-section-1`,
    heading: '기획 포인트',
    paragraphs: [
      `${topic}는 가 지금 다루고 있는 전시·공연·교육 콘텐츠를 한 장면으로 묶어 보여주기 위한 더미 기사입니다.`,
      '관리자에서 이 영역은 에디터 본문, 요약문, 이미지 배치, 태그 노출이 실제로 어떻게 보일지 검수하는 용도로 활용할 수 있습니다.',
    ],
  },
  {
    id: `${topic}-section-2`,
    heading: '운영 메모',
    paragraphs: [
      '호별 기사 구조는 “호(issue) - 기사(article) - 본문 섹션(section)”으로 나누면 목록, 카드, 상세페이지를 모두 같은 데이터로 조립할 수 있습니다.',
      '추후 CMS를 붙일 때는 이 영역을 리치텍스트 에디터나 블록 에디터로 전환하고, 공개 여부와 정렬 순서만 관리하면 됩니다.',
    ],
  },
];

const volume101Articles: readonly IssueArticle[] = [
  {
    id: 'vol101-article-01',
    slug: 'language-of-being-beyond-fragments',
    issueSlug: 'vol-101',
    volume: 101,
    issueDate: '2026.03.',
    title: '파편, 결핍이 아닌 존재의 언어로',
    kicker: '《파편의 파편: 박치호·정광희》전',
    summary:
      '우리는 대개 부서지고 깨진 것을 쓸모없다고 여긴다. 하지만 완전히 산산조각이 나고 나서야 비로소 숨겨진 본연의 모습을 발견할 수 있다면 어떨까? 이번 더미 페이지는 전시 상세와 본문, 태그, 공유, 관련 콘텐츠가 한 흐름으로 어떻게 연결되는지 보여주기 위한 예시다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=900&q=80',
    category: 'exhibition',
    formatLabel: '비디오',
    tags: ['설치미술', '파편의파편', '박치호', '정광희', '지역작가협력전시', '동시대미술'],
    authorName: '노시영',
    authorEmail: 'nayeongso@daum.net',
    creditLine: 'Video 디자인아이엠 촬영감독 왕채영',
    highlightQuote: '"당신은 어떤 문장을 적고 싶나요?" 한 권의 책이 된 전시, 《파편의 파편》',
    sections: [
      {
        id: 'fragment-section-1',
        heading: '깨진 것의 언어를 다시 읽기',
        paragraphs: [
          '반면, 정광희 작가에게 “파편”은 고정관념의 틀을 벗어나기 위한 의도적인 행위의 산물이다. 작품은 부서짐을 결핍으로 보지 않고, 새로운 감각과 관계가 시작되는 틈으로 해석한다.',
          '먹물을 가득 채운 달항아리를 공중에서 낙하시킨 퍼포먼스는 파괴처럼 보이지만, 작가에게 이는 오히려 새로운 생성의 시작이다. 흘러나간 먹물과 깨진 조각들은 예측할 수 없는 방향으로 번져가며 새 의미의 자리를 만든다.',
        ],
      },
      {
        id: 'fragment-section-2',
        heading: '전시 동선과 체험 설계',
        paragraphs: [
          '이번 상세페이지 더미에서는 큰 대표 이미지, 요약정보, 카테고리 태그, 작성자 정보, 관련 콘텐츠 영역이 한 페이지에서 자연스럽게 이어지도록 설계했다.',
          '사용자는 상단에서 전시 맥락을 빠르게 이해하고, 중간 본문에서 기획 의도와 비평을 읽고, 마지막에 관련 기사나 목록보기 버튼으로 다시 다른 글로 이동하게 된다.',
        ],
        note: '* 파편의 의미를 “깨뜨릴 파(破), 곧을 직(直)”처럼 새로 정의하는 식의 주석 영역도 본문 중간에 박스로 삽입할 수 있다.',
      },
      {
        id: 'fragment-section-3',
        heading: 'CMS로 옮길 때 고려할 점',
        paragraphs: [
          '상세 본문은 문자열 하나보다 “본문 블록 목록”으로 설계하는 것이 낫다. 문단, 이미지, 인용문, 주석, 영상 임베드 같은 요소를 별도 블록으로 분리해두면 편집과 재배치가 훨씬 쉬워진다.',
          '관리자에서는 발행 상태, 대표 이미지, 관련 기사, 태그, SEO 메타값까지 한 화면에서 검수할 수 있게 묶는 편이 운영 효율이 높다.',
        ],
      },
    ],
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol101-article-02',
    slug: 'wednesday-theater-cultural-prescription',
    issueSlug: 'vol-101',
    volume: 101,
    issueDate: '2026.03.',
    title: '한 주의 중심에서 만난 문화 처방전,  수요극장',
    kicker: '상영 프로그램 큐레이션',
    summary:
      '수요극장은 관객의 한 주에 리듬을 만들어주는 반복 프로그램이다. 영화 상영과 짧은 해설, 후속 콘텐츠를 연결하는 방식이 화면에서 어떻게 정리될지 살펴보기 위한 더미 데이터다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=900&q=80',
    category: 'performance',
    formatLabel: '아티클',
    tags: ['수요극장', '라파치니의정원', '문화처방', '상영프로그램'],
    authorName: ' 편집부',
    authorEmail: 'webzine@',
    creditLine: 'Photo  아카이브',
    sections: createGenericSections(' 수요극장'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol101-article-03',
    slug: 'asia-garden-culture-program',
    issueSlug: 'vol-101',
    volume: 101,
    issueDate: '2026.03.',
    title: ' 아시아 예술체험 <아시아의 정원문화>',
    kicker: '교육·포럼 프로그램',
    summary:
      '교육 체험형 프로그램은 카드 목록에서 밝고 직관적으로 보여야 한다. 사진, 카테고리, 해시태그만 봐도 성격이 드러나는 레이아웃을 가정했다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=900&q=80',
    category: 'forum',
    formatLabel: '아티클',
    tags: ['아시아예술체험', '아시아정원문화', '교육프로그램', '체험학습'],
    authorName: '이수민',
    authorEmail: 'academy@',
    creditLine: 'Photo 프로그램 운영팀',
    sections: createGenericSections('아시아의 정원문화'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol101-article-04',
    slug: 'what-era-of-asia-are-we-living',
    issueSlug: 'vol-101',
    volume: 101,
    issueDate: '2026.03.',
    title: '아시아, 우리는 지금 어떤 시대를 살고 있는가? : 《2026  NEXT 아시아 신진작가전》',
    kicker: '신진작가전 미리보기',
    summary:
      '전시 미리보기형 기사에서는 제목 길이가 길어질 수 있으므로 카드와 상세 상단에서 줄바꿈 제어가 중요하다. 이 더미는 그 가독성을 검토하려는 목적을 갖는다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80',
    category: 'exhibition',
    formatLabel: '아티클',
    tags: ['NEXT', '아시아신진작가전', '기획전시', '동시대담론'],
    authorName: '조해원',
    authorEmail: 'curation@',
    creditLine: 'Photo 전시기획팀',
    sections: createGenericSections(' NEXT 아시아 신진작가전'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol101-article-05',
    slug: 'bridge-market-linking-people-and-culture',
    issueSlug: 'vol-101',
    volume: 101,
    issueDate: '2026.03.',
    title: '사람과 문화를 잇는 다리,  ‘별별브릿지마켓’',
    kicker: '행사·교류 현장 스케치',
    summary:
      '행사성 기사에서는 사진 비중이 높고, 카드 리스트에서도 현장감이 먼저 느껴지는 구성이 중요하다. 대표 이미지와 짧은 설명만으로도 콘텐츠 성격이 드러나게 설계했다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1489515217757-5fd1be406fef?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1489515217757-5fd1be406fef?auto=format&fit=crop&w=900&q=80',
    category: 'event',
    formatLabel: '아티클',
    tags: ['별별브릿지마켓', '플리마켓', '문화교류', '현장스케치'],
    authorName: '김보라',
    authorEmail: 'event@',
    creditLine: 'Photo 행사운영팀',
    sections: createGenericSections('별별브릿지마켓'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol101-article-06',
    slug: 'spring-library-at-acc',
    issueSlug: 'vol-101',
    volume: 101,
    issueDate: '2026.03.',
    title: '봄에 만나는  도서관',
    kicker: '행사·교류 큐레이션',
    summary:
      '도서관과 아카이브성 콘텐츠는 정보성 문구가 많기 때문에 카드에서는 제목과 키워드를 간결하게 보여주고, 상세에서는 안내 문구를 섹션형 본문으로 풀어내는 방식이 적합하다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80',
    category: 'event',
    formatLabel: '아티클',
    tags: ['도서관', '큐레이션', '문화정보원', '북큐레이션'],
    authorName: '정다은',
    authorEmail: 'library@',
    creditLine: 'Photo 문화정보원',
    sections: createGenericSections(' 도서관'),
    relatedContents: relatedShowcase,
  },
];

const volume100Articles: readonly IssueArticle[] = [
  {
    id: 'vol100-article-01',
    slug: 'artificial-you-exhibition',
    issueSlug: 'vol-100',
    volume: 100,
    issueDate: '2026.01.',
    title: '인공지능이라는 거울 앞에 선 당신(Artificial You)',
    kicker: 'Vol.100 특집',
    summary:
      '100호 특집은 키비주얼이 강하게 드러나는 볼륨으로 가정했다. 카드, 리스트, 상세 상단에서 모두 같은 비주얼 정체성이 유지되는지를 보려는 샘플 데이터다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80',
    category: 'exhibition',
    formatLabel: '비디오',
    tags: ['ArtificialYou', 'AI전시', '키비주얼', '100호특집'],
    authorName: ' 편집부',
    authorEmail: 'webzine@',
    creditLine: 'Design  크리에이티브팀',
    sections: createGenericSections('Artificial You'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol100-article-02',
    slug: 'saturday-stage',
    issueSlug: 'vol-100',
    volume: 100,
    issueDate: '2026.01.',
    title: '아시아의 영웅, 오늘의 무대에 서다 –  국제협력공연 <세메테이>',
    kicker: '공연 리뷰',
    summary:
      '공연 리뷰형 콘텐츠는 긴 제목과 공연 사진이 같이 노출되므로 카드의 정보 밀도와 상세 상단의 타이포가 중요하다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=900&q=80',
    category: 'performance',
    formatLabel: '아티클',
    tags: ['국제협력공연', '세메테이', '공연리뷰', '영웅서사'],
    authorName: '김지원',
    authorEmail: 'theater@',
    creditLine: 'Photo 공연예술팀',
    sections: createGenericSections('국제협력공연 세메테이'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol100-article-03',
    slug: 'little-heroes-of-light',
    issueSlug: 'vol-100',
    volume: 100,
    issueDate: '2026.01.',
    title: '“2025년 를 빛낸 작은 영웅들”',
    kicker: '행사·교류 스토리',
    summary:
      '연말결산형 기사에서는 감성적인 제목과 요약문, 여러 장의 사진이 어우러진다. 현재는 카드/상세 구조 검토를 위한 더미로 구성했다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1489515217757-5fd1be406fef?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1489515217757-5fd1be406fef?auto=format&fit=crop&w=900&q=80',
    category: 'event',
    formatLabel: '아티클',
    tags: ['연말결산', '작은영웅들', '운영기록', '현장'],
    authorName: '서윤아',
    authorEmail: 'event@',
    creditLine: 'Photo 행사운영팀',
    sections: createGenericSections('를 빛낸 작은 영웅들'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol100-article-04',
    slug: 'osongsoo-korean-colors',
    issueSlug: 'vol-100',
    volume: 100,
    issueDate: '2026.01.',
    title: '오송은 – 가장 한국적인 색으로 자연과의 조화를 노래하다',
    kicker: '전시 인터뷰',
    summary:
      '작가 인터뷰형 상세는 본문 섹션, 인용문, 이미지 캡션이 반복되기 때문에 기사 블록 설계를 먼저 정리해두는 편이 좋다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1459908676235-d5f02a50184b?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1459908676235-d5f02a50184b?auto=format&fit=crop&w=900&q=80',
    category: 'exhibition',
    formatLabel: '아티클',
    tags: ['작가인터뷰', '한국색채', '자연', '전시기획'],
    authorName: '최현지',
    authorEmail: 'curation@',
    creditLine: 'Photo 전시기획팀',
    sections: createGenericSections('오송은 인터뷰'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol100-article-05',
    slug: 'residency-results-and-recovery',
    issueSlug: 'vol-100',
    volume: 100,
    issueDate: '2026.01.',
    title: '<2025  공연 레지던시 결과발표회> - 예술과 회복력',
    kicker: '공연 레지던시',
    summary:
      '프로그램형 콘텐츠에서는 기간, 장소, 참여자 정보가 중요하므로 백엔드에서도 별도 메타 컬럼을 갖는 편이 운영에 유리하다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=900&q=80',
    category: 'performance',
    formatLabel: '아티클',
    tags: ['공연레지던시', '결과발표회', '회복력', '프로그램기록'],
    authorName: '문예진',
    authorEmail: 'theater@',
    creditLine: 'Photo 공연예술팀',
    sections: createGenericSections(' 공연 레지던시'),
    relatedContents: relatedShowcase,
  },
  {
    id: 'vol100-article-06',
    slug: 'our-home-asia',
    issueSlug: 'vol-100',
    volume: 100,
    issueDate: '2026.01.',
    title: '<우리 모두의 집, 아시아>',
    kicker: '행사·교류 리뷰',
    summary:
      '교류형 콘텐츠는 행사 기록과 메시지 전달이 함께 중요하다. 카드에서 제목이 돋보이면서도 해시태그가 과하지 않게 보이는 구성을 확인할 수 있다.',
    heroImageUrl:
      'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=1400&q=80',
    cardImageUrl:
      'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80',
    category: 'event',
    formatLabel: '아티클',
    tags: ['우리모두의집', '아시아', '문화교류', '프로그램리뷰'],
    authorName: '박수연',
    authorEmail: 'event@',
    creditLine: 'Photo 문화교류팀',
    sections: createGenericSections('우리 모두의 집, 아시아'),
    relatedContents: relatedShowcase,
  },
];

export const mockIssues: readonly IssueVolume[] = [
  {
    id: 'issue-vol-101',
    slug: 'vol-101',
    volume: 101,
    issueDate: '2026.03.',
    coverImageUrl:
      'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1200&q=80',
    teaser:
      '전시, 공연, 교육, 행사 콘텐츠가 한 권의 웹진 안에서 어떻게 묶이는지 미리 보는 101호 더미 데이터입니다.',
    articles: volume101Articles,
  },
  {
    id: 'issue-vol-100',
    slug: 'vol-100',
    volume: 100,
    issueDate: '2026.01.',
    coverImageUrl:
      'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80',
    teaser:
      '100호 특집처럼 키비주얼이 강한 호에서는 볼륨 카드와 카드 리스트, 상세 상단의 시각적 연결감이 특히 중요합니다.',
    articles: volume100Articles,
  },
];

const cloneIssue = (baseIssue: IssueVolume, volume: number, issueDate: string): IssueVolume => ({
  ...baseIssue,
  id: `issue-vol-${volume}`,
  slug: `vol-${volume}`,
  volume,
  issueDate,
  teaser: `Vol.${volume} 더미 데이터입니다. 실제 운영에서는 CMS에서 호 설명과 대표 이미지를 관리하고, 이 화면은 무한 스크롤로 다음 호를 이어서 보여줄 수 있습니다.`,
  articles: baseIssue.articles.map((article, index) => ({
    ...article,
    id: `vol${volume}-article-${String(index + 1).padStart(2, '0')}`,
    slug: `${article.slug}-vol-${volume}`,
    issueSlug: `vol-${volume}`,
    volume,
    issueDate,
  })),
});

export const mockIssueFeed: readonly IssueVolume[] = [
  ...mockIssues,
  cloneIssue(mockIssues[0], 99, '2025.12.'),
  cloneIssue(mockIssues[1], 98, '2025.11.'),
  cloneIssue(mockIssues[0], 97, '2025.10.'),
  cloneIssue(mockIssues[1], 96, '2025.09.'),
];

export const getIssueBySlug = (issueSlug: string): IssueVolume | undefined =>
  mockIssueFeed.find(issue => issue.slug === issueSlug);

export const getIssueArticleBySlug = (
  issueSlug: string,
  articleSlug: string
): IssueArticle | undefined =>
  getIssueBySlug(issueSlug)?.articles.find(article => article.slug === articleSlug);
