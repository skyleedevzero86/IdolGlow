export const ADMIN_SNB_GROUPS: ReadonlyArray<{
  readonly id: string;
  readonly title: string;
  readonly items: ReadonlyArray<{ readonly label: string; readonly href: string }>;
}> = [
  {
    id: 'admin-product',
    title: '상품·예약',
    items: [
      { label: '상품관리', href: '/admin/products' },
      { label: '룸관리', href: '/admin/slots' },
      { label: '옵션관리', href: '/admin/options' },
      { label: '예약관리', href: '/admin/reservations' },
    ],
  },
  {
    id: 'admin-content',
    title: '콘텐츠 관리',
    items: [
      { label: '공지사항', href: '/admin/notices' },
      { label: '뉴스레터 관리', href: '/admin/newsletters' },
      { label: '트렌드관리', href: '/admin/issues' },
      { label: '설문관리', href: '/admin/surveys' },
      { label: '피부관리', href: '/admin/products?category=skin' },
      { label: '의료관리', href: '/admin/products?category=medical' },
      { label: '경험관리', href: '/admin/products?category=experience' },
      { label: '이벤트관리', href: '/admin/events' },
      { label: '배너 관리', href: '/admin/banners' },
      { label: '팝업 관리', href: '/admin/popups' },
      { label: '광고 관리', href: '/admin/ads' },
    ],
  },
  {
    id: 'admin-insight',
    title: '운영지표',
    items: [
      { label: '회원관리', href: '/admin/users' },
      { label: '인증관리', href: '/admin/auth-verifications' },
      { label: '리뷰관리', href: '/admin/reviews' },
      { label: '결제관리', href: '/admin/payments' },
      { label: '구독관리', href: '/admin/subscriptions' },
      { label: '서버상태', href: '/admin/server-status' },
    ],
  },
  {
    id: 'admin-archive',
    title: 'Glow 아티클',
    items: [
      { label: 'Glow 지도', href: '/glow_map' },
      { label: 'Glow 지하철', href: '/subway' },
      { label: 'Glow 공항인파', href: '/airport-crowd' },
      { label: 'Glow 행사정보', href: '/event-info' },
      { label: 'Glow 알림', href: '/glow-alerts' },
      { label: 'Glow 날씨', href: '/glow-weather' },
      { label: '오늘의 환율', href: '/exchange-rate' },
    ],
  },
  {
    id: 'admin-glow-picks',
    title: '나의 정보',
    items: [
      { label: 'Glow 추천', href: '/admin/glow-recommendation' },
      { label: 'Glow 여행계획', href: '/admin/schedules' },
      { label: '나의 예약', href: '/my-payments' },
      { label: '나의 결제', href: '/my-payments' },
      { label: '나의 이야기', href: '/my-archive' },
      { label: '나의 리뷰', href: '/myreviewsfh' },
      { label: '개인정보 변경', href: '/mypage/userInfo' },
    ],
  },
];

export const avatarInitial = (nickname?: string | null, email?: string | null): string => {
  const display = nickname?.trim() || email?.trim() || 'A';
  return display.slice(0, 1).toUpperCase();
};
