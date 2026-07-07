export const POPULAR_PRODUCTS_QUERY = /* GraphQL */ `
  query PopularProducts($size: Int) {
    popularProducts(size: $size) {
      id
      name
      minPrice
      wishCount
    }
  }
`;
