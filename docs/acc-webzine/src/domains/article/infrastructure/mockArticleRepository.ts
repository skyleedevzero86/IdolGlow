import type { ArticleRepository } from "../application/ports/articleRepositoryPort";
import type { Article, ArticleFilterCriteria, CategoryType, PaginatedResult } from "../domain/article.types";
import { mapDTOToEntity } from "./articleMapper";
import { filterByCategory, filterByVolume, paginate, sortArticles } from "../application/articleService";
import { mockArticles } from "../../../shared/data/mockArticles";

const getArticleEntities = (): readonly Article[] => mockArticles.map(mapDTOToEntity);

const delay = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

export const createMockArticleRepository = (): ArticleRepository => ({
  getAll: async () => {
    await delay(100);
    return getArticleEntities();
  },
  getById: async (id: string) => {
    await delay(50);
    return getArticleEntities().find((a) => a.id === id) || null;
  },
  getFiltered: async (criteria: ArticleFilterCriteria) => {
    await delay(100);
    let articles: Article[] = [...getArticleEntities()];
    if (criteria.category) {
      articles = filterByCategory(articles, criteria.category);
    }
    if (criteria.volume) {
      articles = filterByVolume(articles, criteria.volume);
    }
    articles = sortArticles(articles, criteria.sortBy);
    return paginate(articles, criteria.page, criteria.pageSize);
  },
  getByCategory: async (category: CategoryType) => {
    await delay(50);
    return filterByCategory([...getArticleEntities()], category);
  },
  getByVolume: async (volume: number) => {
    await delay(50);
    return filterByVolume([...getArticleEntities()], volume);
  },
  getLatest: async (limit: number) => {
    await delay(50);
    return sortArticles([...getArticleEntities()], "latest").slice(0, limit);
  },
  getPopular: async (limit: number) => {
    await delay(50);
    return sortArticles([...getArticleEntities()], "popular").slice(0, limit);
  },
});
