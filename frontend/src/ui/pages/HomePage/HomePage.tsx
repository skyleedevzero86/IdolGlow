import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import type { SortOption } from "../../../domains/article/domain/article.types";
import type { Banner } from "../../../domains/banner/domain/banner.types";
import {
  type HomeBannerCard,
  type SiteHomeContentResponse,
  fetchSiteHomeContent,
} from "../../../shared/data/siteContentApi";
import { HeroBanner } from "../../components/HeroBanner/HeroBanner";
import { HomeDiscovery } from "../../components/HomeDiscovery/HomeDiscovery";
import { HomePopupModal } from "../../components/HomePopupModal/HomePopupModal";
import { LoadingSpinner } from "../../components/LoadingSpinner/LoadingSpinner";
import { NewsletterSubscribeModal } from "../../components/NewsletterSubscribeModal/NewsletterSubscribeModal";
import { SortFilter } from "../../components/SortFilter/SortFilter";
import { VolumeSection } from "../../components/VolumeSection/VolumeSection";
import { useArticlesGroupedByVolume } from "../../hooks/useArticles";
import styles from "./HomePage.module.css";

const EMPTY_HOME_CONTENT: SiteHomeContentResponse = {
  heroSlides: [],
  banners: [],
  popups: [],
};

const BANNERS_VISIBLE_COUNT = 4;
const BANNER_AUTO_SLIDE_MS = 3000;

const DARK_INK = "#111827";
const LIGHT_INK = "#ffffff";
const isExternalLink = (value: string): boolean => /^https?:\/\//i.test(value);

export const HomePage = () => {
  const [sortBy, setSortBy] = useState<SortOption>("latest");
  const [subscribeOpen, setSubscribeOpen] = useState(false);
  const [homeContent, setHomeContent] =
    useState<SiteHomeContentResponse>(EMPTY_HOME_CONTENT);
  const [currentBannerIndex, setCurrentBannerIndex] = useState(0);
  const {
    data: volumeGroups,
    loading,
    error,
  } = useArticlesGroupedByVolume(sortBy);

  useEffect(() => {
    let cancelled = false;

    void (async () => {
      try {
        const nextContent = await fetchSiteHomeContent();
        if (!cancelled) {
          setHomeContent(nextContent);
        }
      } catch {
        if (!cancelled) {
          setHomeContent(EMPTY_HOME_CONTENT);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleSortChange = useCallback((newSort: SortOption) => {
    setSortBy(newSort);
  }, []);

  const heroSlides = useMemo<readonly Banner[]>(
    () =>
      homeContent.heroSlides.map((slide, index) => ({
        id: slide.imageId,
        title: slide.title,
        subtitle: slide.subtitle ?? "",
        imageUrl: slide.imageUrl,
        linkUrl: slide.linkUrl,
        category: slide.categoryLabel || `slide-${index + 1}`,
        isActive: true,
        order: index,
      })),
    [homeContent.heroSlides],
  );

  useEffect(() => {
    if (currentBannerIndex <= homeContent.banners.length - 1) {
      return;
    }

    setCurrentBannerIndex(0);
  }, [currentBannerIndex, homeContent.banners.length]);

  const canSlideBanners = homeContent.banners.length > BANNERS_VISIBLE_COUNT;

  useEffect(() => {
    if (!canSlideBanners) {
      return;
    }

    const timer = window.setInterval(() => {
      setCurrentBannerIndex(
        (previous) => (previous + 1) % homeContent.banners.length,
      );
    }, BANNER_AUTO_SLIDE_MS);

    return () => window.clearInterval(timer);
  }, [canSlideBanners, homeContent.banners.length]);

  const visibleBanners = useMemo(() => {
    if (homeContent.banners.length <= BANNERS_VISIBLE_COUNT) {
      return homeContent.banners;
    }

    return Array.from({ length: BANNERS_VISIBLE_COUNT }, (_, offset) => {
      const nextIndex =
        (currentBannerIndex + offset) % homeContent.banners.length;
      return homeContent.banners[nextIndex];
    });
  }, [currentBannerIndex, homeContent.banners]);

  return (
    <>
      <main className={styles.main} id="main-content">
        <HeroBanner banners={heroSlides} />

        <div className={styles.socialBar}>
          <div className={styles.socialBarInner}>
            <nav className={styles.webzineQuickNav} aria-label="웹진 바로가기">
              <ul className={`${styles.webzineWrap} ${styles.blue}`}>
                <li>
                  <Link to="/glow" className={styles.webzineLink}>
                    트렌드 보기
                  </Link>
                </li>
                <li>
                  <button
                    type="button"
                    className={styles.webzineActionButton}
                    onClick={() => setSubscribeOpen(true)}
                  >
                    구독 신청
                  </button>
                </li>
              </ul>
            </nav>
          </div>
        </div>

        <HomeDiscovery />

        <section className={styles.contentSection} id="volume-section">
          <SortFilter
            currentSort={sortBy}
            onSortChange={handleSortChange}
            title="한국의 최신 IdolGlow"
          />

          {loading ? (
            <LoadingSpinner text="권호 목록을 불러오는 중..." />
          ) : null}

          {error ? (
            <div role="alert">
              <p>
                권호 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
              </p>
            </div>
          ) : null}

          <div>
            {volumeGroups?.map((group) => (
              <VolumeSection key={group.volume} volumeGroup={group} />
            ))}
          </div>

          {homeContent.banners.length > 0 ? (
            <section className={styles.bannerSection} aria-label="패밀리사이트">
              <div className={styles.bannerHeader}>
                <h2 className={styles.bannerSectionTitle}>패밀리사이트</h2>
                {canSlideBanners ? (
                  <div className={styles.bannerControls}>
                    <button
                      type="button"
                      className={styles.bannerNavButton}
                      onClick={() =>
                        setCurrentBannerIndex((previous) =>
                          previous <= 0
                            ? homeContent.banners.length - 1
                            : previous - 1,
                        )
                      }
                      aria-label="이전 배너"
                    >
                      &#8249;
                    </button>
                    <span className={styles.bannerPageLabel}>
                      {currentBannerIndex + 1} / {homeContent.banners.length}
                    </span>
                    <button
                      type="button"
                      className={styles.bannerNavButton}
                      onClick={() =>
                        setCurrentBannerIndex(
                          (previous) =>
                            (previous + 1) % homeContent.banners.length,
                        )
                      }
                      aria-label="다음 배너"
                    >
                      &#8250;
                    </button>
                  </div>
                ) : null}
              </div>

              <div className={styles.bannerGrid}>
                {visibleBanners.map((banner, index) => (
                  <GlowBannerCard
                    key={`${banner.bannerId}-${index}`}
                    banner={banner}
                  />
                ))}
              </div>
            </section>
          ) : null}
        </section>
      </main>

      <HomePopupModal popups={homeContent.popups} />
      <NewsletterSubscribeModal
        open={subscribeOpen}
        onClose={() => setSubscribeOpen(false)}
      />
    </>
  );
};

function GlowBannerCard({ banner }: { readonly banner: HomeBannerCard }) {
  const ink = useBannerInk(banner.imageUrl);
  const onImage = Boolean(banner.imageUrl);

  return (
    <a
      href={banner.linkUrl}
      className={styles.bannerCard}
      target={isExternalLink(banner.linkUrl) ? "_blank" : undefined}
      rel={isExternalLink(banner.linkUrl) ? "noreferrer" : undefined}
    >
      {banner.imageUrl ? (
        <img
          src={banner.imageUrl}
          alt=""
          className={styles.bannerImage}
          crossOrigin="anonymous"
        />
      ) : null}
      <span
        className={styles.bannerCaption}
        style={
          onImage
            ? {
                color: ink,
                textShadow:
                  ink === LIGHT_INK
                    ? "0 0 8px rgba(0, 0, 0, 0.72)"
                    : "0 0 8px rgba(255, 255, 255, 0.9)",
              }
            : undefined
        }
      >
        {banner.title}
      </span>
    </a>
  );
}

function useBannerInk(imageUrl: string | null): string {
  const [ink, setInk] = useState(DARK_INK);

  useEffect(() => {
    if (!imageUrl) {
      setInk(DARK_INK);
      return;
    }

    let cancelled = false;
    const image = new Image();
    image.crossOrigin = "anonymous";
    image.onload = () => {
      if (!cancelled) {
        setInk(inkForImage(image));
      }
    };
    image.onerror = () => {
      if (!cancelled) {
        setInk(LIGHT_INK);
      }
    };
    image.src = imageUrl;

    return () => {
      cancelled = true;
    };
  }, [imageUrl]);

  return ink;
}

function inkForImage(image: HTMLImageElement): string {
  const luminance = averageLuminance(image);
  if (luminance == null) {
    return LIGHT_INK;
  }
  return luminance > 0.62 ? DARK_INK : LIGHT_INK;
}

function averageLuminance(image: HTMLImageElement): number | null {
  const canvas = document.createElement("canvas");
  canvas.width = 12;
  canvas.height = 12;
  const context = canvas.getContext("2d", { willReadFrequently: true });
  if (!context) {
    return null;
  }

  try {
    context.drawImage(image, 0, 0, 12, 12);
    return luminanceOf(context.getImageData(0, 0, 12, 12).data);
  } catch {
    return null;
  }
}

function luminanceOf(data: Uint8ClampedArray): number | null {
  let sum = 0;
  let count = 0;
  for (let index = 0; index < data.length; index += 4) {
    if (data[index + 3] < 16) {
      continue;
    }
    sum +=
      0.2126 * data[index] +
      0.7152 * data[index + 1] +
      0.0722 * data[index + 2];
    count += 1;
  }
  if (count === 0) {
    return null;
  }
  return sum / count / 255;
}

export default HomePage;
