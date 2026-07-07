import { requestGraphql } from "../../../shared/infrastructure/graphql/gqlRequest";
import { POPULAR_PRODUCTS_QUERY } from "../infrastructure/popularProductsQuery";
import {
  takePopularFromPayload,
  type PopularPayload,
  type ProductRankingSummary,
} from "../domain/productSummary";

const DEFAULT_SIZE = 5;

export const fetchPopularProductSummaries = async (
  size: number = DEFAULT_SIZE
): Promise<ReadonlyArray<ProductRankingSummary>> => {
  const data = await requestGraphql<PopularPayload>(POPULAR_PRODUCTS_QUERY, {
    size,
  });
  return takePopularFromPayload(data);
};
