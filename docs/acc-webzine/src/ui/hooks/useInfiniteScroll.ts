/**
 * useInfiniteScroll Hook
 *
 * 무한 스크롤 기능을 제공하는 커스텀 훅
 * Intersection Observer API를 사용하여 스크롤 감지
 */

import { useCallback, useEffect, useRef, useState } from 'react';

interface UseInfiniteScrollOptions {
  readonly threshold?: number;
  readonly rootMargin?: string;
}

interface UseInfiniteScrollReturn {
  readonly observerRef: React.RefObject<HTMLDivElement>;
  readonly isIntersecting: boolean;
}

/**
 * 무한 스크롤 훅
 * @param onIntersect - 요소가 뷰포트에 진입했을 때 호출되는 콜백
 * @param options - Intersection Observer 옵션
 */
export const useInfiniteScroll = (
  onIntersect: () => void,
  options: UseInfiniteScrollOptions = {}
): UseInfiniteScrollReturn => {
  const { threshold = 0.1, rootMargin = '100px' } = options;
  const [isIntersecting, setIsIntersecting] = useState(false);
  const observerRef = useRef<HTMLDivElement>(null);

  // 콜백 메모이제이션
  const handleIntersect = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const [entry] = entries;
      setIsIntersecting(entry.isIntersecting);

      if (entry.isIntersecting) {
        onIntersect();
      }
    },
    [onIntersect]
  );

  useEffect(() => {
    const element = observerRef.current;
    if (!element) return;

    const observer = new IntersectionObserver(handleIntersect, {
      threshold,
      rootMargin,
    });

    observer.observe(element);

    return () => {
      observer.disconnect();
    };
  }, [handleIntersect, threshold, rootMargin]);

  return { observerRef, isIntersecting };
};

export default useInfiniteScroll;
