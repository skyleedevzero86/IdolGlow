import type { Article, ArticleFilterCriteria, CategoryType, PaginatedResult } from "../../domain/article.types";

export interface ArticleRepository {
  getAll(): Promise<readonly Article[]>;
  getById(id: string): Promise<Article | null>;
  getFiltered(criteria: ArticleFilterCriteria): Promise<PaginatedResult<Article>>;
  getByCategory(category: CategoryType): Promise<readonly Article[]>;
  getByVolume(volume: number): Promise<readonly Article[]>;
  getLatest(limit: number): Promise<readonly Article[]>;
  getPopular(limit: number): Promise<readonly Article[]>;
}
