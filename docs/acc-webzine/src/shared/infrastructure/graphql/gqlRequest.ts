import { getGraphqlUrl } from "../../../auth/authConfig";

export type GraphqlPayload<T> = {
  readonly data?: T;
  readonly errors?: ReadonlyArray<{ readonly message: string }>;
};

export const requestGraphql = async <T>(
  query: string,
  variables?: Record<string, unknown>,
  init?: RequestInit
): Promise<T> => {
  const res = await fetch(getGraphqlUrl(), {
    ...init,
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...init?.headers,
    },
    credentials: init?.credentials ?? "include",
    body: JSON.stringify({ query, variables }),
  });
  const json = (await res.json()) as GraphqlPayload<T>;
  if (!res.ok) {
    throw new Error(`GraphQL HTTP ${res.status}: ${res.statusText}`);
  }
  if (json.errors?.length) {
    const msg = json.errors[0]?.message ?? "GraphQL error";
    throw new Error(msg);
  }
  if (json.data === undefined) {
    throw new Error("GraphQL: 응답에 data가 없습니다.");
  }
  return json.data;
};
