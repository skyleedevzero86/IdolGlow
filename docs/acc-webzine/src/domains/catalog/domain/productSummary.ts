export type ProductRankingSummary = {
  readonly id: string;
  readonly name: string;
  readonly minPrice: string;
  readonly wishCount: number;
};

export type PopularPayload = {
  readonly popularProducts: ReadonlyArray<{
    readonly id: string;
    readonly name: string;
    readonly minPrice: string;
    readonly wishCount: number;
  }>;
};

export const takePopularFromPayload = (
  p: PopularPayload
): ReadonlyArray<ProductRankingSummary> =>
  p.popularProducts.map((x) => ({
    id: x.id,
    name: x.name,
    minPrice: x.minPrice,
    wishCount: x.wishCount,
  }));
