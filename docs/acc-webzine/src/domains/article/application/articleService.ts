import type { Article, CategoryType, PaginatedResult, SortOption } from "../domain/article.types";

const sortByLatest = (articles: readonly Article[]): Article[] =>
  [...articles].sort((a, b) => b.publishDate.getTime() - a.publishDate.getTime());

const sortByPopular = (articles: readonly Article[]): Article[] =>
  [...articles].sort((a, b) => b.viewCount - a.viewCount);

const sortByLikes = (articles: readonly Article[]): Article[] =>
  [...articles].sort((a, b) => b.likeCount - a.likeCount);

const getSortFunction = (sortBy: SortOption): ((articles: readonly Article[]) => Article[]) => {
  const sortFunctions: Record<SortOption, (articles: readonly Article[]) => Article[]> = {
    latest: sortByLatest,
    popular: sortByPopular,
    likes: sortByLikes,
  };
  return sortFunctions[sortBy];
};

export const filterByCategory = (articles: readonly Article[], category: CategoryType): Article[] =>
  articles.filter((article) => article.category === category);

export const filterByVolume = (articles: readonly Article[], volume: number): Article[] =>
  articles.filter((article) => article.volume === volume);

export const filterByTag = (articles: readonly Article[], tag: string): Article[] =>
  articles.filter((article) => article.tags.includes(tag));

export const paginate = <T>(
  items: readonly T[],
  page: number,
  pageSize: number
): PaginatedResult<T> => {
  const totalCount = items.length;
  const totalPages = Math.ceil(totalCount / pageSize) || 1;
  const currentPage = Math.max(1, Math.min(page, totalPages));
  const startIndex = (currentPage - 1) * pageSize;
  return {
    items: items.slice(startIndex, startIndex + pageSize),
    totalCount,
    currentPage,
    totalPages,
    hasNextPage: currentPage < totalPages,
    hasPrevPage: currentPage > 1,
  };
};

export const sortArticles = (articles: readonly Article[], sortBy: SortOption): Article[] =>
  getSortFunction(sortBy)(articles);

export const groupByVolume = (articles: readonly Article[]): Map<number, Article[]> => {
  return articles.reduce((groups, article) => {
    const volume = article.volume;
    const existing = groups.get(volume) || [];
    groups.set(volume, [...existing, article]);
    return groups;
  }, new Map<number, Article[]>());
};

export const getLatestVolume = (articles: readonly Article[]): number => {
  if (articles.length === 0) {
    return 0;
  }
  return Math.max(...articles.map((a) => a.volume));
};

export const findAdjacentArticles = (
  articles: readonly Article[],
  currentId: string
): { prev: Article | null; next: Article | null } => {
  const sortedArticles = sortByLatest(articles);
  const currentIndex = sortedArticles.findIndex((a) => a.id === currentId);
  if (currentIndex === -1) {
    return { prev: null, next: null };
  }
  return {
    prev: currentIndex > 0 ? sortedArticles[currentIndex - 1]! : null,
    next: currentIndex < sortedArticles.length - 1 ? sortedArticles[currentIndex + 1]! : null,
  };
};

export const findRelatedArticles = (
  articles: readonly Article[],
  targetArticle: Article,
  limit = 3
): Article[] => {
  return articles
    .filter(
      (article) =>
        article.id !== targetArticle.id &&
        (article.category === targetArticle.category ||
          article.tags.some((tag) => targetArticle.tags.includes(tag)))
    )
    .slice(0, limit);
};
