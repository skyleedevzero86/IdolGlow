export interface Banner {
  readonly id: string;
  readonly title: string;
  readonly subtitle: string;
  readonly imageUrl: string;
  readonly linkUrl: string;
  readonly category: string;
  readonly isActive: boolean;
  readonly order: number;
}

export interface BannerViewModel {
  readonly id: string;
  readonly title: string;
  readonly subtitle: string;
  readonly imageUrl: string;
  readonly linkUrl: string;
  readonly categoryLabel: string;
}
