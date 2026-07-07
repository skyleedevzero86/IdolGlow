/**
 * Mock Banner Data
 */

import type { Banner } from "../../domains/banner/domain/banner.types";

export const mockBanners: readonly Banner[] = [
  {
    id: 'banner-001',
    title: '디지털 시대의 문화예술',
    subtitle: '새로운 창작의 패러다임을 탐험하다',
    imageUrl: 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1920&h=800&fit=crop',
    linkUrl: '/articles/article-001',
    category: 'exhibition',
    isActive: true,
    order: 1,
  },
  {
    id: 'banner-002',
    title: '아시아 공연예술의 오늘',
    subtitle: '전통과 현대가 만나는 무대',
    imageUrl: 'https://images.unsplash.com/photo-1514533450685-4493e01d1fdc?w=1920&h=800&fit=crop',
    linkUrl: '/articles/article-002',
    category: 'performance',
    isActive: true,
    order: 2,
  },
  {
    id: 'banner-003',
    title: '국제 문화교류 프로젝트',
    subtitle: '경계를 넘는 협력의 순간들',
    imageUrl: 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=1920&h=800&fit=crop',
    linkUrl: '/articles/article-003',
    category: 'exchange',
    isActive: true,
    order: 3,
  },
];
