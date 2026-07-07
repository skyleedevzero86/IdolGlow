/**
 * useTheme Hook
 *
 * 다크 모드 테마 관리 훅
 * localStorage에 테마 설정을 저장하여 유지
 */

import { useCallback, useEffect, useState } from 'react';

type Theme = 'light' | 'dark';

const STORAGE_KEY = 'culture-webzine-theme';

/**
 * 초기 테마 결정 - 순수 함수
 * 1. localStorage 확인
 * 2. 시스템 설정 확인
 * 3. 기본값 light
 */
const getInitialTheme = (): Theme => {
  // 브라우저 환경 체크
  if (typeof window === 'undefined') {
    return 'light';
  }

  // localStorage에서 저장된 테마 확인
  const savedTheme = localStorage.getItem(STORAGE_KEY) as Theme | null;
  if (savedTheme === 'light' || savedTheme === 'dark') {
    return savedTheme;
  }

  // 시스템 다크 모드 설정 확인
  if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
    return 'dark';
  }

  /* 웹진 레퍼런스는 다크 베이스 */
  return 'dark';
};

/**
 * DOM에 테마 적용 - 사이드 이펙트
 */
const applyTheme = (theme: Theme): void => {
  const root = document.documentElement;

  if (theme === 'dark') {
    root.setAttribute('data-theme', 'dark');
  } else {
    root.setAttribute('data-theme', 'light');
  }

  // localStorage에 저장
  localStorage.setItem(STORAGE_KEY, theme);
};

/**
 * useTheme 훅
 */
export const useTheme = () => {
  const [theme, setTheme] = useState<Theme>(getInitialTheme);

  // 초기 테마 적용
  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  // 시스템 테마 변경 감지
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const handleChange = (e: MediaQueryListEvent) => {
      // localStorage에 저장된 값이 없을 때만 시스템 설정 따르기
      const savedTheme = localStorage.getItem(STORAGE_KEY);
      if (!savedTheme) {
        setTheme(e.matches ? 'dark' : 'light');
      }
    };

    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  // 테마 토글
  const toggleTheme = useCallback(() => {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  }, []);

  // 특정 테마로 설정
  const setSpecificTheme = useCallback((newTheme: Theme) => {
    setTheme(newTheme);
  }, []);

  return {
    theme,
    toggleTheme,
    setTheme: setSpecificTheme,
    isDark: theme === 'dark',
  };
};

export default useTheme;
