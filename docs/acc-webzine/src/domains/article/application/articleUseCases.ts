import type { ArticleRepository } from "./ports/articleRepositoryPort";
import type {
  ArticleDetailViewModel,
  ArticleFilterCriteria,
  ArticleViewModel,
  CategoryType,
  PaginatedResult,
  SortOption,
} from "../domain/article.types";
import { mapEntityToDetailViewModel, mapEntityToViewModel } from "../infrastructure/articleMapper";
import { findAdjacentArticles, groupByVolume } from "./articleService";

export interface VolumeGroup {
  readonly volume: number;
  readonly date: string;
  readonly articles: readonly ArticleViewModel[];
}

export interface AdjacentArticles {
  readonly prev: ArticleViewModel | null;
  readonly next: ArticleViewModel | null;
}

export const createArticleUseCases = (repository: ArticleRepository) => ({
  getFilteredArticles: async (criteria: ArticleFilterCriteria): Promise<PaginatedResult<ArticleViewModel>> => {
    const result = await repository.getFiltered(criteria);
    return {
      ...result,
      items: result.items.map(mapEntityToViewModel),
    };
  },

  getArticlesGroupedByVolume: async (sortBy: SortOption = "latest"): Promise<readonly VolumeGroup[]> => {
    const articles = await repository.getFiltered({
      sortBy,
      page: 1,
      pageSize: 100,
    });
    const grouped = groupByVolume(articles.items);
    return Array.from(grouped.entries())
      .sort(([volA], [volB]) => volB - volA)
      .map(([volume, items]) => ({
        volume,
        date: getVolumeDate(volume),
        articles: items.map(mapEntityToViewModel),
      }));
  },

  getArticleDetail: async (id: string): Promise<ArticleDetailViewModel | null> => {
    const article = await repository.getById(id);
    if (!article) {
      return null;
    }
    return mapEntityToDetailViewModel(article);
  },

  getAdjacentArticles: async (currentId: string): Promise<AdjacentArticles> => {
    const allArticles = await repository.getAll();
    const { prev, next } = findAdjacentArticles(allArticles, currentId);
    return {
      prev: prev ? mapEntityToViewModel(prev) : null,
      next: next ? mapEntityToViewModel(next) : null,
    };
  },

  getArticlesByCategory: async (category: CategoryType, limit = 6): Promise<readonly ArticleViewModel[]> => {
    const articles = await repository.getByCategory(category);
    return articles.slice(0, limit).map(mapEntityToViewModel);
  },

  getLatestArticles: async (limit = 6): Promise<readonly ArticleViewModel[]> => {
    const articles = await repository.getLatest(limit);
    return articles.map(mapEntityToViewModel);
  },

  getPopularArticles: async (limit = 6): Promise<readonly ArticleViewModel[]> => {
    const articles = await repository.getPopular(limit);
    return articles.map(mapEntityToViewModel);
  },
});

const getVolumeDate = (volume: number): string => {
  const baseYear = 2026;
  const baseMonth = 1;
  const baseVolume = 100;
  const monthDiff = baseVolume - volume;
  const year = baseYear - Math.floor((baseMonth - 1 + monthDiff) / 12);
  const month = (((baseMonth - 1 - (monthDiff % 12)) + 12) % 12) + 1;
  return `${year}.${String(month).padStart(2, "0")}.`;
};

export type ArticleUseCases = ReturnType<typeof createArticleUseCases>;
