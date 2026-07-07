export interface AdminNewsletterItem {
  readonly id: number;
  readonly slug: string;
  readonly title: string;
  readonly categoryLabel: string;
  readonly publishedAt: string;
  readonly imageUrl: string;
  readonly tags: readonly string[];
  readonly summary: string;
  readonly paragraphs: readonly string[];
}

export const mockNewsletters: readonly AdminNewsletterItem[] = [
  {
    id: 1,
    slug: 'acc-supporters-5th-launches-with-50-members',
    title: ' 5기 서포터즈...역대 최대 50명 활동 시작',
    categoryLabel: ' 소식',
    publishedAt: '2026.03.31',
    imageUrl:
      'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80',
    tags: ['서포터즈5기', '문화현장크리에이터', '대학생', '청년서포터즈'],
    summary:
      'IdolGlow가 제5기 서포터즈 발대식을 열고, 역대 최대 규모의 청년 크리에이터 활동을 시작했습니다.',
    paragraphs: [
      'IdolGlow는 3월 말 문화정보와 공간 경험을 함께 전달할 제5기 서포터즈 발대식을 개최하고 본격적인 활동을 시작했습니다. 이번 기수는 역대 최대 규모인 50명으로 구성돼 현장 취재, 콘텐츠 제작, 온라인 확산을 함께 맡습니다.',
      '서포터즈는 공연, 전시, 교육, 라이브러리, 아카이브 등  주요 프로그램을 직접 체험하고 SNS와 영상 콘텐츠로 소개합니다. 관람객의 시선과 이용 경험을 담아내는 현장형 기록 활동도 강화될 예정입니다.',
      '는 이번 운영을 통해 청년 참여 기반의 홍보 채널을 넓히고, 문화예술 경험이 일상 속 이야기로 확산되는 선순환 구조를 만들겠다는 계획입니다.',
    ],
  },
  {
    id: 2,
    slug: 'acc-launches-specialized-cultural-forum',
    title: ', 9개 전문분과로 평화포럼 첫 시작',
    categoryLabel: ' 소식',
    publishedAt: '2026.03.24',
    imageUrl:
      'https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&w=1200&q=80',
    tags: ['포럼', '문화정책', '전문분과', '아시아문화담론'],
    summary:
      '가 9개 전문분과 체제로 포럼을 재정비하고, 아시아 문화 의제를 다루는 신규 논의 구조를 출범했습니다.',
    paragraphs: [
      '이번 포럼은 문화정책, 공연예술, 지역문화, 디지털 전환, 아카이브, 교육, 국제협력 등 세분화된 전문분과로 구성됐습니다.',
      '참여자들은 각 분과별로 현안과 사례를 공유하며 가 향후 집중해야 할 문화 의제를 도출하는 데 의견을 모았습니다.',
      '는 포럼 결과를 연간 사업 운영과 신규 프로그램 기획 과정에 연결해 실질적인 실행 기반으로 활용할 계획입니다.',
    ],
  },
  {
    id: 3,
    slug: 'acc-stage-tour-urban-thrill',
    title: ' 공연장 투어, 도심 속 설렘을 만나는 순간',
    categoryLabel: ' 소식',
    publishedAt: '2026.03.18',
    imageUrl:
      'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1200&q=80',
    tags: ['공연장', '무대투어', '백스테이지', '관객프로그램'],
    summary:
      '무대 뒤편과 객석, 조명·음향 장비까지 공연장의 숨은 장면을 소개하는 투어 프로그램이 관람객 호응을 얻고 있습니다.',
    paragraphs: [
      ' 공연장 투어는 단순 관람을 넘어 무대가 준비되는 과정과 공간의 작동 방식을 함께 체험하는 프로그램으로 운영되고 있습니다.',
      '참가자들은 객석에서 보이지 않던 백스테이지 동선과 장비 구성을 살펴보며, 공연이 완성되기까지의 협업 과정을 이해하게 됩니다.',
      '는 향후 가족 단위 관람객과 청소년 대상 프로그램도 추가해 접근성을 넓힐 계획입니다.',
    ],
  },
  {
    id: 4,
    slug: 'acc-bridges-local-commerce-with-culture-market',
    title: ', 지역 상권과 문화로 잇는 플리마켓',
    categoryLabel: ' 소식',
    publishedAt: '2026.03.10',
    imageUrl:
      'https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=1200&q=80',
    tags: ['플리마켓', '지역상생', '문화장터', '커뮤니티'],
    summary:
      '지역 창작자와 상인, 방문객이 함께 만나는 문화 플리마켓이  야외 공간에서 열렸습니다.',
    paragraphs: [
      '이번 플리마켓은 지역 상권과  방문객을 연결하는 체류형 프로그램으로 운영됐습니다.',
      '수공예, 출판, 로컬 푸드, 체험 부스가 한데 모이며 문화 소비와 지역 상생의 접점을 넓혔다는 평가를 받았습니다.',
      '는 정기 프로그램화 가능성을 검토하며, 계절별 기획전과 연계한 확장 운영도 추진 중입니다.',
    ],
  },
  {
    id: 5,
    slug: 'acc-one-media-platform-renews-visual-identity',
    title: ' 1인 미디어...새로운 감각으로 개편',
    categoryLabel: ' 소식',
    publishedAt: '2026.03.05',
    imageUrl:
      'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1200&q=80',
    tags: ['1인미디어', '콘텐츠개편', '영상제작', '창작지원'],
    summary:
      ' 1인 미디어 지원 프로그램이 시각 정체성과 운영 구조를 새롭게 정비했습니다.',
    paragraphs: [
      '개편된 프로그램은 촬영 환경과 멘토링 체계를 동시에 보완해 창작자들이 결과물 제작에 집중할 수 있도록 구성됐습니다.',
      '특히 시각 아이덴티티를 정비하면서 젊은 창작자들의 참여 동기를 높이고, 온라인 노출 방식도 함께 재설계했습니다.',
      '는 후속 시즌에서 인터뷰, 숏폼, 다큐멘터리형 프로젝트를 병행 운영할 예정입니다.',
    ],
  },
  {
    id: 6,
    slug: 'acc-special-program-links-exhibitions-and-education',
    title: ' 특별해설 프로그램 10월 전시 교과 과정',
    categoryLabel: ' 소식',
    publishedAt: '2026.02.27',
    imageUrl:
      'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80',
    tags: ['특별해설', '전시교육', '학교연계', '문화예술교육'],
    summary:
      '전시 감상과 교육 과정을 연결하는 특별 해설 프로그램이 학교 현장과 협업해 운영됩니다.',
    paragraphs: [
      '이번 특별해설은 전시 내용을 교육 현장과 연결할 수 있도록 수업 연계형 콘텐츠 중심으로 구성됐습니다.',
      '교사와 학생이 함께 참여할 수 있는 질문지와 활동 가이드가 제공돼, 전시 감상이 일회성 체험에 머물지 않도록 지원합니다.',
      '는 연령대와 교과 특성에 맞춘 세분화 프로그램도 점차 확대할 예정입니다.',
    ],
  },
  {
    id: 7,
    slug: 'acc-jebodan-records-forty-design-cases',
    title: ', 제7기 제보단 소리샘  디자인사례 40건',
    categoryLabel: ' 소식',
    publishedAt: '2026.02.20',
    imageUrl:
      'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80',
    tags: ['제보단', '디자인사례', '기록', '사용자관점'],
    summary:
      '제7기 제보단이 공간과 안내 체계, 디자인 경험을 포함한  사례 40건을 기록했습니다.',
    paragraphs: [
      '제보단은 방문객의 동선과 시선, 공간 정보 전달 방식을 중심으로  내 디자인 사례를 조사했습니다.',
      '수집된 사례는 운영 개선 자료와 향후 브랜딩·사인 체계 보완을 위한 기초 자료로 활용됩니다.',
      '는 사용자의 경험 데이터를 정성 기록과 함께 축적해 실질적인 서비스 개선으로 연결할 계획입니다.',
    ],
  },
  {
    id: 8,
    slug: 'acc-builds-brand-knowledge-sharing-program',
    title: ', 브랜드  지식문화소통 프로그램 운영',
    categoryLabel: ' 소식',
    publishedAt: '2026.02.12',
    imageUrl:
      'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80',
    tags: ['브랜드', '지식문화소통', '네트워킹', '아시아문화'],
    summary:
      '브랜드 를 주제로 한 지식문화소통 프로그램이 전문가와 시민 참여형으로 진행됐습니다.',
    paragraphs: [
      '프로그램은  브랜드가 갖는 공공성과 개방성, 문화 플랫폼으로서의 역할을 다양한 관점에서 해석하는 자리로 구성됐습니다.',
      '참여자들은 강연과 대화, 네트워킹 세션을 통해 가 축적해 온 가치와 앞으로의 방향을 함께 논의했습니다.',
      '는 이러한 소통 프로그램을 통해 기관 정체성과 시민 접점을 동시에 강화할 계획입니다.',
    ],
  },
  {
    id: 9,
    slug: 'acc-opens-media-art-special-exhibition',
    title: '! 2026 호안전 미디어 특별전 전시',
    categoryLabel: ' 소식',
    publishedAt: '2026.02.04',
    imageUrl:
      'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=1200&q=80',
    tags: ['미디어특별전', '전시개막', '디지털아트', '아시아창작'],
    summary:
      '미디어 환경과 시각 경험을 주제로 한 특별전이  전시 공간에서 개막했습니다.',
    paragraphs: [
      '이번 전시는 디지털 화면과 감각 환경이 우리의 지각을 어떻게 바꾸는지 탐구하는 작품들로 구성됐습니다.',
      '관람객은 몰입형 설치와 영상, 인터랙티브 작업을 통해 동시대 시각 문화의 흐름을 경험할 수 있습니다.',
      '는 전시 기간 중 작품 해설과 연계 프로그램을 함께 운영해 이해도를 높일 예정입니다.',
    ],
  },
  {
    id: 10,
    slug: 'acc-move-forum-on-ai-and-empathy',
    title: ' 무브, 인공지능과 공감의 경기',
    categoryLabel: ' 소식',
    publishedAt: '2026.01.28',
    imageUrl:
      'https://images.unsplash.com/photo-1516321165247-4aa89a48be28?auto=format&fit=crop&w=1200&q=80',
    tags: ['무브', '인공지능', '공감', '포럼'],
    summary:
      '인공지능과 공감 능력을 주제로 한 대화형 세션이  무브 프로그램으로 열렸습니다.',
    paragraphs: [
      '세션은 기술이 감정과 관계를 이해하는 방식, 그리고 인간의 공감 능력이 어떤 의미를 갖는지에 대한 논의로 구성됐습니다.',
      '참여자들은 실제 사례와 창작 실험을 중심으로 기술과 예술의 협업 가능성을 탐색했습니다.',
      '는 향후 디지털 감수성과 문화기술 담론을 연결하는 프로그램을 이어갈 예정입니다.',
    ],
  },
  {
    id: 11,
    slug: 'acc-opens-spring-library-program',
    title: ' 라이브러리, 봄 시즌 큐레이션 공개',
    categoryLabel: ' 소식',
    publishedAt: '2026.01.22',
    imageUrl:
      'https://images.unsplash.com/photo-1521587760476-6c12a4b040da?auto=format&fit=crop&w=1200&q=80',
    tags: ['라이브러리', '큐레이션', '봄프로그램', '아카이브'],
    summary:
      '라이브러리 봄 시즌 큐레이션이 공개되며 자료 열람과 연계 프로그램이 시작됐습니다.',
    paragraphs: [
      '이번 큐레이션은 아시아 문화자료와 현대 예술 출판물을 함께 소개하며 탐색 동선을 강화했습니다.',
      '방문객은 주제별 진열과 함께 해설 카드, 추천 자료 맵을 통해 더 쉽게 자료를 만날 수 있습니다.',
      '는 계절별 큐레이션을 통해 라이브러리 접근성을 꾸준히 넓혀갈 계획입니다.',
    ],
  },
  {
    id: 12,
    slug: 'acc-education-week-wraps-up-with-showcase',
    title: ' 교육주간, 참여형 결과 공유회 마무리',
    categoryLabel: ' 소식',
    publishedAt: '2026.01.15',
    imageUrl:
      'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1200&q=80',
    tags: ['교육주간', '결과공유회', '참여형프로그램', '교육'],
    summary:
      '교육주간 프로그램이 참여자 결과 공유회와 함께 마무리되며 올해 운영 방향을 제시했습니다.',
    paragraphs: [
      '공유회에서는 참여자들이 직접 만든 결과물과 활동 기록이 공개돼 프로그램의 과정을 한눈에 볼 수 있었습니다.',
      '예술교육, 워크숍, 현장 체험을 묶은 운영 구조가 좋은 반응을 얻으며 향후 확장 가능성도 확인됐습니다.',
      '는 결과 공유회를 정례화해 교육 프로그램의 지속성과 성과를 더 분명히 축적할 계획입니다.',
    ],
  },
];

export const findMockNewsletterBySlug = (slug: string): AdminNewsletterItem | null =>
  mockNewsletters.find(newsletter => newsletter.slug === slug) ?? null;
