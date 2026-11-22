import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import type { Banner } from '../../../domains/banner/domain/banner.types';
import { fetchSiteHomeContent } from '../../../shared/data/siteContentApi';
import { EventInfoBrowser } from '../../components/EventInfoBrowser/EventInfoBrowser';
import { HeroBanner } from '../../components/HeroBanner/HeroBanner';
import { NewsletterSubscribeModal } from '../../components/NewsletterSubscribeModal/NewsletterSubscribeModal';
import homeStyles from '../HomePage/HomePage.module.css';
import { GlowAllFeed } from './GlowAllFeed';
import styles from './GlowPage.module.css';
import { GlowCommunity } from './GlowCommunity';
import { GlowRecentReviews } from './GlowRecentReviews';
import { GlowTrendVolumes } from './GlowTrendVolumes';

type GlowFilter = 'all' | 'trend' | 'news' | 'recent' | 'local';

const FILTERS: readonly { readonly id: GlowFilter; readonly label: string }[] = [
  { id: 'all', label: '전체' },
  { id: 'trend', label: '트렌드' },
  { id: 'news', label: 'glow정보' },
  { id: 'recent', label: '최근리뷰' },
  { id: 'local', label: '지역사회' },
];

const parseGlowFilter = (raw: string | null): GlowFilter =>
  raw === 'trend' || raw === 'news' || raw === 'recent' || raw === 'local' ? raw : 'all';

export const GlowPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeFilter = parseGlowFilter(searchParams.get('tab'));
  const contentSectionRef = useRef<HTMLElement>(null);
  const skipTabScrollRef = useRef(false);
  const [subscribeOpen, setSubscribeOpen] = useState(false);
  const [heroSlides, setHeroSlides] = useState<readonly Banner[]>([]);

  const setActiveFilter = (filter: GlowFilter) => {
    skipTabScrollRef.current = true;
    const next = new URLSearchParams();
    if (filter !== 'all') next.set('tab', filter);
    setSearchParams(next, { replace: true });
  };

  // biome-ignore lint/correctness/useExhaustiveDependencies: location.key is an intentional re-run trigger
  useEffect(() => {
    if (skipTabScrollRef.current) {
      skipTabScrollRef.current = false;
      return;
    }
    const params = new URLSearchParams(location.search);
    if (!params.get('tab') || params.has('post') || location.hash) return;
    const frame = window.requestAnimationFrame(() => {
      contentSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [location.key, location.search, location.hash]);

  useEffect(() => {
    if (location.hash !== '#exchange-rate') return;
    navigate('/exchange-rate', { replace: true });
  }, [location.hash, navigate]);

  useEffect(() => {
    let cancelled = false;

    void (async () => {
      try {
        const content = await fetchSiteHomeContent();
        if (cancelled) return;
        setHeroSlides(
          content.heroSlides.map((slide, index) => ({
            id: slide.imageId,
            title: slide.title,
            subtitle: slide.subtitle ?? '',
            imageUrl: slide.imageUrl,
            linkUrl: slide.linkUrl,
            category: slide.categoryLabel || `slide-${index + 1}`,
            isActive: true,
            order: index,
          })),
        );
      } catch {
        if (!cancelled) setHeroSlides([]);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const filterBar = (
    <div className={styles.filterBar} aria-label="콘텐츠 카테고리">
              {FILTERS.map((filter) => (
                <button
                  type="button"
                  key={filter.id}
                  className={activeFilter === filter.id ? styles.activeFilter : undefined}
                  onClick={() => setActiveFilter(filter.id)}
                >
                  {filter.label}
                </button>
              ))}
            </div>
  );

  return (
    <>
    <main className={styles.glowPage} id="main-content">
      <HeroBanner banners={heroSlides} />

      <div className={homeStyles.socialBar}>
        <div className={homeStyles.socialBarInner}>
          <nav className={homeStyles.idolglowQuickNav} aria-label="웹진 바로가기">
            <ul className={`${homeStyles.idolglowWrap} ${homeStyles.blue}`}>
              <li>
                <Link to="/glow?tab=trend" className={homeStyles.idolglowLink}>
                  트렌드 보기
                </Link>
              </li>
              <li>
                <button
                  type="button"
                  className={homeStyles.idolglowActionButton}
                  onClick={() => setSubscribeOpen(true)}
                >
                  구독 신청
                </button>
              </li>
            </ul>
          </nav>
        </div>
      </div>

      <section ref={contentSectionRef} className={styles.contentSection} aria-label="GLOW 콘텐츠 목록">
        <div className={styles.contentInner}>
          {activeFilter === 'trend' ? (
            <GlowTrendVolumes filterBar={filterBar} />
          ) : activeFilter === 'news' ? (
            <EventInfoBrowser variant="embedded" heading="Glow 행사정보" filterBar={filterBar} />
          ) : activeFilter === 'recent' ? (
            <GlowRecentReviews filterBar={filterBar} />
          ) : activeFilter === 'local' ? (
            <GlowCommunity filterBar={filterBar} />
          ) : (
          <GlowAllFeed filterBar={filterBar} />
          )}
        </div>
      </section>
    </main>
    <NewsletterSubscribeModal
      open={subscribeOpen}
      onClose={() => setSubscribeOpen(false)}
    />
    </>
  );
};

export default GlowPage;
