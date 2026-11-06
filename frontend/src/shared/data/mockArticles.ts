import type { ArticleDTO } from "../../domains/article/domain/article.types";

export const mockArticles: readonly ArticleDTO[] = [
  {
    id: 'article-001',
    title: '디지털 시대의 문화예술: 새로운 패러다임',
    excerpt: '인공지능과 예술이 만나는 접점에서 우리는 어떤 창작의 미래를 그릴 수 있을까요? 디지털 기술이 예술 창작에 미치는 영향을 탐구합니다.',
    content: `
      <h2>디지털 혁명과 예술의 융합</h2>
      <p>21세기 들어 디지털 기술은 예술 창작의 모든 영역을 변화시키고 있습니다. 인공지능, 가상현실, 증강현실 등 새로운 기술들이 예술가들에게 전례 없는 도구를 제공하고 있으며, 이는 창작의 개념 자체를 재정의하고 있습니다.</p>

      <h3>AI와 창작의 경계</h3>
      <p>최근 AI 생성 이미지와 음악이 주목받으면서, '창작'의 정의에 대한 논쟁이 활발해지고 있습니다. AI는 도구인가, 협력자인가, 아니면 새로운 형태의 예술가인가? 이 질문은 앞으로 예술계가 풀어야 할 중요한 과제입니다.</p>

      <h3>아시아 문화의 디지털 재해석</h3>
      <p>아시아의 전통 문화와 예술이 디지털 기술을 통해 새롭게 해석되고 있습니다. 전통 회화를 인터랙티브 아트로, 전통 음악을 디지털 사운드스케이프로 변환하는 시도들이 활발히 이루어지고 있습니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1535378620166-273708d44e4c?w=600&h=400&fit=crop',
    category: 'exhibition',
    volume: 100,
    publishDate: '2026-01-15',
    tags: ['디지털아트', 'AI', '미래예술', '창작'],
    viewCount: 1250,
    likeCount: 89,
    authorName: '김예술',
  },
  {
    id: 'article-002',
    title: '아시아 공연예술의 오늘: 전통과 현대의 대화',
    excerpt: '아시아 각국의 전통 공연예술이 현대적 감각으로 재해석되는 현장을 취재했습니다. 경계를 넘는 협업의 순간들.',
    content: `
      <h2>전통의 현대적 재창조</h2>
      <p>아시아 공연예술은 오랜 역사와 깊은 전통을 가지고 있습니다. 한국의 판소리, 일본의 노(能), 중국의 경극, 인도의 카타칼리 등 각국의 전통 공연은 독자적인 미학과 철학을 담고 있습니다.</p>

      <h3>경계를 넘는 협업</h3>
      <p>오늘날 아시아의 공연예술가들은 국경과 장르의 경계를 넘어 활발히 협업하고 있습니다. 전통 악기와 전자음악의 결합, 동양 무용과 서양 무용의 융합 등 새로운 시도들이 관객들에게 신선한 경험을 선사합니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1514533450685-4493e01d1fdc?w=600&h=400&fit=crop',
    category: 'performance',
    volume: 100,
    publishDate: '2026-01-12',
    tags: ['공연', '전통예술', '아시아', '협업'],
    viewCount: 980,
    likeCount: 67,
    authorName: '이문화',
  },
  {
    id: 'article-003',
    title: '문화교류의 새 지평: 국제 협력 프로젝트',
    excerpt: 'IdolGlow가 주도하는 국제 문화교류 프로젝트의 성과와 미래 비전을 살펴봅니다.',
    content: `
      <h2>글로벌 문화 네트워크 구축</h2>
      <p>문화교류는 단순한 이벤트를 넘어 지속가능한 네트워크 구축을 목표로 합니다. 아시아 각국의 문화기관들과의 협력을 통해 장기적인 파트너십을 형성하고 있습니다.</p>

      <h3>교류의 성과</h3>
      <p>지난 해 진행된 국제 교류 프로젝트를 통해 20개국 이상의 예술가들이 참여했으며, 공동 창작 작품 10편이 발표되었습니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=600&h=400&fit=crop',
    category: 'exchange',
    volume: 100,
    publishDate: '2026-01-10',
    tags: ['국제교류', '협력', '네트워크', '글로벌'],
    viewCount: 756,
    likeCount: 45,
    authorName: '박협력',
  },
  {
    id: 'article-004',
    title: '빛과 색의 향연: 미디어아트 특별전',
    excerpt: '빛을 소재로 한 미디어아트 작품들이 전시공간을 새로운 경험의 장으로 변모시킵니다.',
    content: `
      <h2>빛으로 그리는 새로운 세계</h2>
      <p>이번 특별전은 빛과 색을 주제로 한 미디어아트 작품들을 선보입니다. LED, 프로젝션 매핑, 레이저 등 다양한 기술을 활용한 작품들이 관람객들에게 몰입형 경험을 제공합니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&h=400&fit=crop',
    category: 'exhibition',
    volume: 100,
    publishDate: '2026-01-08',
    tags: ['미디어아트', '전시', '빛', '몰입형'],
    viewCount: 1890,
    likeCount: 134,
    authorName: '최미디어',
  },
  {
    id: 'article-005',
    title: '청년 예술가 육성 프로그램 성과 발표',
    excerpt: '차세대 문화예술 인재를 발굴하고 육성하는 프로그램의 1년간 성과를 정리했습니다.',
    content: `
      <h2>미래의 예술가를 키우다</h2>
      <p>청년 예술가 육성 프로그램은 재능 있는 젊은 예술가들에게 창작 공간, 멘토링, 발표 기회를 제공합니다. 올해 50명의 참가자 중 10명이 개인전을 개최하는 성과를 거두었습니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=600&h=400&fit=crop',
    category: 'exchange',
    volume: 100,
    publishDate: '2026-01-05',
    tags: ['청년예술가', '육성', '프로그램', '인재발굴'],
    viewCount: 654,
    likeCount: 38,
    authorName: '정육성',
  },
  {
    id: 'article-006',
    title: '전통 음악의 재발견: 국악 오케스트라 공연',
    excerpt: '전통 국악기와 현대 오케스트라가 만나 새로운 사운드를 창조합니다.',
    content: `
      <h2>동서양의 하모니</h2>
      <p>가야금, 거문고, 대금 등 전통 국악기와 바이올린, 첼로 등 서양 악기가 어우러진 특별한 공연입니다. 전통과 현대, 동양과 서양의 경계를 허무는 새로운 시도입니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=600&h=400&fit=crop',
    category: 'performance',
    volume: 100,
    publishDate: '2026-01-03',
    tags: ['국악', '오케스트라', '전통음악', '융합'],
    viewCount: 1120,
    likeCount: 92,
    authorName: '한음악',
  },
];
