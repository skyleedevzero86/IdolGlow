import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { Banner } from "../../../domains/banner/domain/banner.types";
import type { SortOption } from "../../../domains/article/domain/article.types";
import { fetchSiteHomeContent, type SiteHomeContentResponse } from '../../../shared/data/siteContentApi';
import { HeroBanner } from '../../components/HeroBanner/HeroBanner';
import { HomePopupModal } from '../../components/HomePopupModal/HomePopupModal';
import { LoadingSpinner } from '../../components/LoadingSpinner/LoadingSpinner';
import { NewsletterSubscribeModal } from '../../components/NewsletterSubscribeModal/NewsletterSubscribeModal';
import { SortFilter } from '../../components/SortFilter/SortFilter';
import { VolumeSection } from '../../components/VolumeSection/VolumeSection';
import { useArticlesGroupedByVolume } from '../../hooks/useArticles';
import styles from './HomePage.module.css';

const EMPTY_HOME_CONTENT: SiteHomeContentResponse = {
  heroSlides: [],
  banners: [],
  popups: [],
};

const BANNERS_VISIBLE_COUNT = 4;
const BANNER_AUTO_SLIDE_MS = 3000;

const isExternalLink = (value: string): boolean => /^https?:\/\//i.test(value);

export const HomePage = () => {
  const location = useLocation();
  const [sortBy, setSortBy] = useState<SortOption>('latest');
  const [subscribeOpen, setSubscribeOpen] = useState(false);
  const [homeContent, setHomeContent] = useState<SiteHomeContentResponse>(EMPTY_HOME_CONTENT);
  const [currentBannerIndex, setCurrentBannerIndex] = useState(0);
  const { data: volumeGroups, loading, error } = useArticlesGroupedByVolume(sortBy);

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

  const scrollToAnchor = useCallback((id: string) => {
    const target = document.getElementById(id);
    const instant = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    target?.scrollIntoView({ behavior: instant ? 'auto' : 'smooth', block: 'start' });
  }, []);

  const handleVolumeBarClick = useCallback(
    (event: React.MouseEvent<HTMLAnchorElement>) => {
      if (location.pathname !== '/') {
        return;
      }
      event.preventDefault();
      scrollToAnchor('volume-section');
    },
    [location.pathname, scrollToAnchor]
  );

  const heroSlides = useMemo<readonly Banner[]>(
    () =>
      homeContent.heroSlides.map((slide, index) => ({
        id: slide.imageId,
        title: slide.title,
        subtitle: slide.subtitle ?? '',
        imageUrl: slide.imageUrl,
        linkUrl: slide.linkUrl,
        category: slide.categoryLabel || `slide-${index + 1}`,
        isActive: true,
        order: index,
      })),
    [homeContent.heroSlides]
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
      setCurrentBannerIndex(previous => (previous + 1) % homeContent.banners.length);
    }, BANNER_AUTO_SLIDE_MS);

    return () => window.clearInterval(timer);
  }, [canSlideBanners, homeContent.banners.length]);

  const visibleBanners = useMemo(() => {
    if (homeContent.banners.length <= BANNERS_VISIBLE_COUNT) {
      return homeContent.banners;
    }

    return Array.from({ length: BANNERS_VISIBLE_COUNT }, (_, offset) => {
      const nextIndex = (currentBannerIndex + offset) % homeContent.banners.length;
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
                  <Link
                    to={{ pathname: '/', hash: 'volume-section' }}
                    className={styles.webzineLink}
                    onClick={handleVolumeBarClick}
                  >
                    호별보기
                  </Link>
                </li>
                <li>
                  <Link to="/" className={styles.webzineLink}>
                    Idol Glow 소식
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

        <section className={styles.contentSection} id="volume-section">
          <SortFilter currentSort={sortBy} onSortChange={handleSortChange} />

          {loading ? <LoadingSpinner text="권호 목록을 불러오는 중..." /> : null}

          {error ? (
            <div role="alert">
              <p>권호 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
            </div>
          ) : null}

          {volumeGroups?.map(group => (
            <VolumeSection key={group.volume} volumeGroup={group} />
          ))}

          {homeContent.banners.length > 0 ? (
            <section className={styles.bannerSection} aria-label="Glow 배너">
              <div className={styles.bannerHeader}>
                <h2 className={styles.bannerSectionTitle}>Glow 배너</h2>
                {canSlideBanners ? (
                  <div className={styles.bannerControls}>
                    <button
                      type="button"
                      className={styles.bannerNavButton}
                      onClick={() =>
                        setCurrentBannerIndex(previous =>
                          previous <= 0 ? homeContent.banners.length - 1 : previous - 1
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
                        setCurrentBannerIndex(previous => (previous + 1) % homeContent.banners.length)
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
                  <a
                    key={`${banner.bannerId}-${index}`}
                    href={banner.linkUrl}
                    className={styles.bannerCard}
                    target={isExternalLink(banner.linkUrl) ? '_blank' : undefined}
                    rel={isExternalLink(banner.linkUrl) ? 'noreferrer' : undefined}
                  >
                    <img src={banner.imageUrl} alt={banner.title} className={styles.bannerImage} />
                    <span className={styles.bannerCaption}>{banner.title}</span>
                  </a>
                ))}
              </div>
            </section>
          ) : null}
        </section>
      </main>

      <HomePopupModal popups={homeContent.popups} />
      <NewsletterSubscribeModal open={subscribeOpen} onClose={() => setSubscribeOpen(false)} />
    </>
  );
};

export default HomePage;

