import { useCallback, useEffect, useRef, useState } from 'react';

interface UseInfiniteScrollOptions {
  readonly threshold?: number;
  readonly rootMargin?: string;
}

interface UseInfiniteScrollReturn {
  readonly observerRef: React.RefObject<HTMLDivElement>;
  readonly isIntersecting: boolean;
}

export const useInfiniteScroll = (
  onIntersect: () => void,
  options: UseInfiniteScrollOptions = {}
): UseInfiniteScrollReturn => {
  const { threshold = 0.1, rootMargin = '100px' } = options;
  const [isIntersecting, setIsIntersecting] = useState(false);
  const observerRef = useRef<HTMLDivElement>(null);

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
