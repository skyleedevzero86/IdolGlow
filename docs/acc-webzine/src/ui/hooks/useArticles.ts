/**
 * useArticles Hook
 *
 * 아티클 데이터를 관리하는 커스텀 훅입니다.
 * UI 컴포넌트에서 도메인 로직을 직접 호출하지 않고,
 * 이 훅을 통해 데이터를 조회합니다.
 */

import { useCallback, useEffect, useState } from "react";
import { createArticleUseCases, type VolumeGroup, type AdjacentArticles } from "../../domains/article/application/articleUseCases";
import { createMockArticleRepository } from "../../domains/article/infrastructure/mockArticleRepository";
import type { ArticleDetailViewModel, ArticleViewModel, CategoryType, SortOption } from "../../domains/article/domain/article.types";

// Repository와 UseCase 인스턴스 생성 (싱글톤)
const repository = createMockArticleRepository();
const useCases = createArticleUseCases(repository);

// 공통 상태 타입
interface AsyncState<T> {
  readonly data: T | null;
  readonly loading: boolean;
  readonly error: Error | null;
}

/**
 * Volume별 그룹화된 아티클 목록을 조회하는 훅
 */
export const useArticlesGroupedByVolume = (sortBy: SortOption = 'latest') => {
  const [state, setState] = useState<AsyncState<readonly VolumeGroup[]>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      try {
        setState(prev => ({ ...prev, loading: true }));
        const data = await useCases.getArticlesGroupedByVolume(sortBy);

        if (isMounted) {
          setState({ data, loading: false, error: null });
        }
      } catch (err) {
        if (isMounted) {
          setState({ data: null, loading: false, error: err as Error });
        }
      }
    };

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [sortBy]);

  return state;
};

/**
 * 무한 스크롤을 위한 아티클 목록 훅
 */
export const useInfiniteArticles = (sortBy: SortOption = 'latest', pageSize = 6) => {
  const [articles, setArticles] = useState<ArticleViewModel[]>([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [initialLoad, setInitialLoad] = useState(true);

  // 데이터 가져오기
  const fetchArticles = useCallback(async (pageNum: number, reset = false) => {
    setLoading(true);
    setError(null);

    try {
      const result = await useCases.getFilteredArticles({
        sortBy,
        page: pageNum,
        pageSize,
      });

      setArticles(prev => reset ? [...result.items] : [...prev, ...result.items]);
      setHasMore(result.hasNextPage);
      setPage(pageNum);
    } catch (err) {
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  }, [sortBy, pageSize]);

  // 초기 로드 및 sortBy 변경 시 리셋
  // biome-ignore lint/correctness/useExhaustiveDependencies: sortBy change should trigger reset
  useEffect(() => {
    setArticles([]);
    setPage(1);
    setHasMore(true);
    setInitialLoad(true);
  }, [sortBy]);

  // 초기 로드 실행
  useEffect(() => {
    if (initialLoad) {
      fetchArticles(1, true);
      setInitialLoad(false);
    }
  }, [initialLoad, fetchArticles]);

  // 다음 페이지 로드
  const loadMore = useCallback(() => {
    if (!loading && hasMore) {
      fetchArticles(page + 1);
    }
  }, [loading, hasMore, page, fetchArticles]);

  return {
    articles,
    loading,
    hasMore,
    error,
    loadMore,
  };
};

/**
 * 검색 기능이 있는 아티클 훅
 */
export const useSearchArticles = () => {
  const [results, setResults] = useState<ArticleViewModel[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const search = useCallback(async (query: string) => {
    if (!query.trim()) {
      setResults([]);
      setSearchTerm('');
      return;
    }

    setLoading(true);
    setSearchTerm(query);

    try {
      // 전체 아티클 조회 후 필터링 (실제 API에서는 서버 사이드 검색)
      const allArticles = await useCases.getFilteredArticles({
        sortBy: 'latest',
        page: 1,
        pageSize: 100,
      });

      const lowerQuery = query.toLowerCase();

      // 제목, 태그, 카테고리 검색 - 함수형 필터링
      const filtered = allArticles.items.filter(article =>
        article.title.toLowerCase().includes(lowerQuery) ||
        article.categoryLabel.toLowerCase().includes(lowerQuery) ||
        article.tags.some(tag => tag.toLowerCase().includes(lowerQuery))
      );

      setResults(filtered);
    } catch (err) {
      console.error('Search error:', err);
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const clearSearch = useCallback(() => {
    setResults([]);
    setSearchTerm('');
  }, []);

  return {
    results,
    loading,
    searchTerm,
    search,
    clearSearch,
  };
};

/**
 * 아티클 상세 정보를 조회하는 훅
 */
export const useArticleDetail = (id: string) => {
  const [state, setState] = useState<AsyncState<ArticleDetailViewModel>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      try {
        setState(prev => ({ ...prev, loading: true }));
        const data = await useCases.getArticleDetail(id);

        if (isMounted) {
          if (data) {
            setState({ data, loading: false, error: null });
          } else {
            setState({ data: null, loading: false, error: new Error('Article not found') });
          }
        }
      } catch (err) {
        if (isMounted) {
          setState({ data: null, loading: false, error: err as Error });
        }
      }
    };

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [id]);

  return state;
};

/**
 * 이전/다음 아티클을 조회하는 훅
 */
export const useAdjacentArticles = (currentId: string) => {
  const [state, setState] = useState<AsyncState<AdjacentArticles>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      try {
        setState(prev => ({ ...prev, loading: true }));
        const data = await useCases.getAdjacentArticles(currentId);

        if (isMounted) {
          setState({ data, loading: false, error: null });
        }
      } catch (err) {
        if (isMounted) {
          setState({ data: null, loading: false, error: err as Error });
        }
      }
    };

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [currentId]);

  return state;
};

/**
 * 카테고리별 아티클을 조회하는 훅
 */
export const useArticlesByCategory = (category: CategoryType, limit = 6) => {
  const [state, setState] = useState<AsyncState<readonly ArticleViewModel[]>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      try {
        setState(prev => ({ ...prev, loading: true }));
        const data = await useCases.getArticlesByCategory(category, limit);

        if (isMounted) {
          setState({ data, loading: false, error: null });
        }
      } catch (err) {
        if (isMounted) {
          setState({ data: null, loading: false, error: err as Error });
        }
      }
    };

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [category, limit]);

  return state;
};

/**
 * 최신 아티클을 조회하는 훅
 */
export const useLatestArticles = (limit = 6) => {
  const [state, setState] = useState<AsyncState<readonly ArticleViewModel[]>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      try {
        setState(prev => ({ ...prev, loading: true }));
        const data = await useCases.getLatestArticles(limit);

        if (isMounted) {
          setState({ data, loading: false, error: null });
        }
      } catch (err) {
        if (isMounted) {
          setState({ data: null, loading: false, error: err as Error });
        }
      }
    };

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [limit]);

  return state;
};

/**
 * 인기 아티클을 조회하는 훅
 */
export const usePopularArticles = (limit = 6) => {
  const [state, setState] = useState<AsyncState<readonly ArticleViewModel[]>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      try {
        setState(prev => ({ ...prev, loading: true }));
        const data = await useCases.getPopularArticles(limit);

        if (isMounted) {
          setState({ data, loading: false, error: null });
        }
      } catch (err) {
        if (isMounted) {
          setState({ data: null, loading: false, error: err as Error });
        }
      }
    };

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [limit]);

  return state;
};
