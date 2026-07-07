export interface Category {
  readonly id: string;
  readonly name: string;
  readonly slug: string;
  readonly description: string;
  readonly iconUrl?: string;
  readonly articleCount: number;
}

export interface CategoryViewModel {
  readonly id: string;
  readonly name: string;
  readonly slug: string;
  readonly description: string;
  readonly iconUrl?: string;
  readonly articleCountLabel: string;
}
