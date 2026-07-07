import { requestGraphql } from "../../../../shared/infrastructure/graphql/gqlRequest";
import { HEALTH_QUERY } from "../infrastructure/healthQuery";

type HealthData = { readonly health: string };

export const fetchHealthMessage = async (): Promise<string> => {
  const data = await requestGraphql<HealthData>(HEALTH_QUERY, {});
  return data.health;
};
