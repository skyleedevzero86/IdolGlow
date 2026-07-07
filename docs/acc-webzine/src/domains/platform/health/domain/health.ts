export type HealthState = "ok" | "error" | "unknown";

export type HealthSnapshot = {
  readonly state: HealthState;
  readonly message: string;
};
