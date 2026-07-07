/**
 * Mock Article Data
 *
 * 실제 API 대신 사용할 목 데이터입니다.
 * ArticleDTO 형태로 정의되어 있으며, 도메인 레이어에서 Entity로 변환됩니다.
 */

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
  // Vol. 99 Articles
  {
    id: 'article-007',
    title: '사라져가는 것들에 대한 기록: 아카이브 프로젝트',
    excerpt: '무형문화재와 전통 기술을 디지털로 보존하는 아카이브 프로젝트를 소개합니다.',
    content: `
      <h2>기억을 보존하다</h2>
      <p>시간이 흐르면서 사라져가는 전통 기술과 문화유산을 디지털 기술로 보존하는 프로젝트입니다. 3D 스캔, VR 촬영, 고해상도 기록 등을 통해 미래 세대를 위한 문화 아카이브를 구축합니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600&h=400&fit=crop',
    category: 'archive',
    volume: 99,
    publishDate: '2025-12-20',
    tags: ['아카이브', '보존', '디지털', '문화유산'],
    viewCount: 567,
    likeCount: 41,
    authorName: '송기록',
  },
  {
    id: 'article-008',
    title: '실험 연극의 새로운 물결',
    excerpt: '기존 연극의 틀을 깨는 실험적 공연들이 관객과 새로운 소통을 시도합니다.',
    content: `
      <h2>무대의 경계를 허물다</h2>
      <p>관객과 배우의 경계, 무대와 객석의 경계를 허무는 실험 연극이 새로운 트렌드로 부상하고 있습니다. 이머시브 씨어터, 사이트 스페시픽 퍼포먼스 등 다양한 형식의 실험이 진행 중입니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1503095396549-807759245b35?w=600&h=400&fit=crop',
    category: 'performance',
    volume: 99,
    publishDate: '2025-12-18',
    tags: ['실험연극', '이머시브', '공연예술', '혁신'],
    viewCount: 823,
    likeCount: 56,
    authorName: '유연극',
  },
  {
    id: 'article-009',
    title: '아시아 현대미술의 동향',
    excerpt: '아시아 각국 현대미술의 최신 트렌드와 주목할 작가들을 소개합니다.',
    content: `
      <h2>아시아 미술의 새로운 흐름</h2>
      <p>아시아 현대미술은 지역적 정체성과 글로벌 담론 사이에서 독자적인 목소리를 내고 있습니다. 각국의 역사적, 사회적 맥락을 반영한 작품들이 국제 미술계에서 주목받고 있습니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1561214115-f2f134cc4912?w=600&h=400&fit=crop',
    category: 'exhibition',
    volume: 99,
    publishDate: '2025-12-15',
    tags: ['현대미술', '아시아', '트렌드', '작가'],
    viewCount: 945,
    likeCount: 73,
    authorName: '임미술',
  },
  {
    id: 'article-010',
    title: '문화예술 영상 콘텐츠의 진화',
    excerpt: '스트리밍 시대에 문화예술 영상 콘텐츠는 어떻게 변화하고 있을까요?',
    content: `
      <h2>디지털 플랫폼과 문화예술</h2>
      <p>코로나19 이후 문화예술의 디지털 전환이 가속화되었습니다. 온라인 공연, 버추얼 갤러리, 다큐멘터리 시리즈 등 다양한 형태의 영상 콘텐츠가 새로운 관객을 만나고 있습니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600&h=400&fit=crop',
    category: 'video',
    volume: 99,
    publishDate: '2025-12-12',
    tags: ['영상', '스트리밍', '디지털', '콘텐츠'],
    viewCount: 1456,
    likeCount: 98,
    authorName: '강영상',
  },
  {
    id: 'article-011',
    title: '참여형 문화 이벤트의 성공 비결',
    excerpt: '관객 참여를 이끌어내는 문화 이벤트 기획의 노하우를 공유합니다.',
    content: `
      <h2>함께 만드는 문화</h2>
      <p>일방적인 관람에서 벗어나 관객이 직접 참여하고 체험하는 문화 이벤트가 인기를 끌고 있습니다. 성공적인 참여형 이벤트의 기획 요소와 사례를 분석합니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=600&h=400&fit=crop',
    category: 'event',
    volume: 99,
    publishDate: '2025-12-10',
    tags: ['이벤트', '참여', '기획', '체험'],
    viewCount: 678,
    likeCount: 52,
    authorName: '윤이벤트',
  },
  {
    id: 'article-012',
    title: '세계 각국의 축제 문화 탐방',
    excerpt: '아시아와 세계 각국의 독특한 축제 문화를 비교 분석합니다.',
    content: `
      <h2>축제로 보는 세계 문화</h2>
      <p>축제는 각 문화권의 역사, 가치관, 예술적 표현이 집약된 문화 현상입니다. 아시아의 전통 축제부터 현대적 페스티벌까지, 다양한 축제 문화를 탐방합니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=600&h=400&fit=crop',
    category: 'event',
    volume: 99,
    publishDate: '2025-12-08',
    tags: ['축제', '문화', '세계', '탐방'],
    viewCount: 892,
    likeCount: 61,
    authorName: '조축제',
  },
  // Vol. 98 Articles
  {
    id: 'article-013',
    title: '소리의 건축: 사운드 인스톨레이션',
    excerpt: '공간과 소리가 만나 새로운 예술적 경험을 창조하는 사운드 설치미술의 세계.',
    content: `
      <h2>청각적 공간의 창조</h2>
      <p>사운드 인스톨레이션은 시각 중심의 미술에서 벗어나 청각적 경험을 전면에 내세우는 예술 형식입니다. 공간의 음향적 특성을 활용하여 관객에게 몰입적인 경험을 제공합니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=600&h=400&fit=crop',
    category: 'exhibition',
    volume: 98,
    publishDate: '2025-11-25',
    tags: ['사운드', '설치미술', '공간', '청각'],
    viewCount: 534,
    likeCount: 39,
    authorName: '배사운드',
  },
  {
    id: 'article-014',
    title: '무용수의 몸, 안무가의 언어',
    excerpt: '현대무용에서 몸의 움직임이 어떻게 언어가 되는지 탐구합니다.',
    content: `
      <h2>몸으로 말하다</h2>
      <p>무용은 인류 가장 오래된 예술 형식 중 하나입니다. 현대무용에서 안무가들은 전통적인 동작 어휘를 넘어 새로운 움직임 언어를 창조하고 있습니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1508700929628-666bc8bd84ea?w=600&h=400&fit=crop',
    category: 'performance',
    volume: 98,
    publishDate: '2025-11-22',
    tags: ['현대무용', '안무', '몸', '표현'],
    viewCount: 712,
    likeCount: 48,
    authorName: '문무용',
  },
  {
    id: 'article-015',
    title: '디지털 아카이빙의 미래',
    excerpt: '문화유산의 디지털 보존과 활용에 대한 최신 기술과 동향을 소개합니다.',
    content: `
      <h2>기술로 보존하는 문화</h2>
      <p>디지털 아카이빙은 물리적 한계를 넘어 문화유산을 영구적으로 보존하고 전 세계와 공유할 수 있게 합니다. AI 기반 복원, 블록체인 인증 등 최신 기술의 적용 사례를 살펴봅니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&h=400&fit=crop',
    category: 'archive',
    volume: 98,
    publishDate: '2025-11-20',
    tags: ['디지털', '아카이빙', '보존', '기술'],
    viewCount: 623,
    likeCount: 44,
    authorName: '신디지털',
  },
  {
    id: 'article-016',
    title: '예술과 기술의 융합: XR 콘텐츠',
    excerpt: 'VR, AR, MR을 활용한 확장현실 예술 콘텐츠의 현재와 미래.',
    content: `
      <h2>현실을 넘어서</h2>
      <p>XR(확장현실) 기술은 예술 경험의 새로운 지평을 열고 있습니다. 가상현실 속 전시관, 증강현실 공연, 혼합현실 설치작품 등 다양한 형태의 XR 콘텐츠가 등장하고 있습니다.</p>
    `,
    thumbnailUrl: 'https://images.unsplash.com/photo-1617802690992-15d93263d3a9?w=600&h=400&fit=crop',
    category: 'video',
    volume: 98,
    publishDate: '2025-11-18',
    tags: ['XR', 'VR', 'AR', '확장현실'],
    viewCount: 1234,
    likeCount: 87,
    authorName: '권테크',
  },
];
