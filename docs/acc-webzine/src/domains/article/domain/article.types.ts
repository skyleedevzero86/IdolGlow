export type CategoryType =
  | "exhibition"
  | "performance"
  | "exchange"
  | "archive"
  | "video"
  | "event";

export interface ArticleDTO {
  readonly id: string;
  readonly title: string;
  readonly excerpt: string;
  readonly content: string;
  readonly thumbnailUrl: string;
  readonly category: string;
  readonly volume: number;
  readonly publishDate: string;
  readonly tags: readonly string[];
  readonly viewCount: number;
  readonly likeCount: number;
  readonly authorName: string;
}

export interface Article {
  readonly id: string;
  readonly title: string;
  readonly excerpt: string;
  readonly content: string;
  readonly thumbnailUrl: string;
  readonly category: CategoryType;
  readonly volume: number;
  readonly publishDate: Date;
  readonly tags: readonly string[];
  readonly viewCount: number;
  readonly likeCount: number;
  readonly authorName: string;
}

export interface ArticleViewModel {
  readonly id: string;
  readonly title: string;
  readonly excerpt: string;
  readonly thumbnailUrl: string;
  readonly categoryLabel: string;
  readonly categoryType: CategoryType;
  readonly volumeLabel: string;
  readonly publishDateFormatted: string;
  readonly tags: readonly string[];
  readonly viewCountFormatted: string;
  readonly likeCountFormatted: string;
}

export interface ArticleDetailViewModel extends ArticleViewModel {
  readonly content: string;
  readonly authorName: string;
}

export type SortOption = "latest" | "popular" | "likes";

export interface ArticleFilterCriteria {
  readonly category?: CategoryType;
  readonly volume?: number;
  readonly sortBy: SortOption;
  readonly page: number;
  readonly pageSize: number;
}

export interface PaginatedResult<T> {
  readonly items: readonly T[];
  readonly totalCount: number;
  readonly currentPage: number;
  readonly totalPages: number;
  readonly hasNextPage: boolean;
  readonly hasPrevPage: boolean;
}
