import type { Banner } from "../../domain/banner.types";

export interface BannerRepository {
  getActiveBanners(): Promise<readonly Banner[]>;
}
