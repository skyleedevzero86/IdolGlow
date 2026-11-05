import { useCallback, useEffect, useState } from "react";
import { createArticleUseCases, type VolumeGroup, type AdjacentArticles } from "../../domains/article/application/articleUseCases";
import { createMockArticleRepository } from "../../domains/article/infrastructure/mockArticleRepository";
import type { ArticleDetailViewModel, ArticleViewModel, CategoryType, SortOption } from "../../domains/article/domain/article.types";

const repository = createMockArticleRepository();
const useCases = createArticleUseCases(repository);

interface AsyncState<T> {
  readonly data: T | null;
  readonly loading: boolean;
  readonly error: Error | null;
}

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

export const useInfiniteArticles = (sortBy: SortOption = 'latest', pageSize = 6) => {
  const [articles, setArticles] = useState<ArticleViewModel[]>([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [initialLoad, setInitialLoad] = useState(true);

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

  // biome-ignore lint/correctness/useExhaustiveDependencies: sortBy change should trigger reset
  useEffect(() => {
    setArticles([]);
    setPage(1);
    setHasMore(true);
    setInitialLoad(true);
  }, [sortBy]);

  useEffect(() => {
    if (initialLoad) {
      fetchArticles(1, true);
      setInitialLoad(false);
    }
  }, [initialLoad, fetchArticles]);

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
      const allArticles = await useCases.getFilteredArticles({
        sortBy: 'latest',
        page: 1,
        pageSize: 100,
      });

      const lowerQuery = query.toLowerCase();

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
