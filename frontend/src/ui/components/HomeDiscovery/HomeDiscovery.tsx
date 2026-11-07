import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  fetchLatestInKoreaRecommendations,
  type ProductRecommendationItem,
} from '../../../shared/data/productRecommendationApi';
import { fetchLatestPublicEventDocumentId } from '../../../shared/data/publicEventsApi';
import {
  fetchGlowBuzzPosts,
  type PublicNoticeSummary,
} from '../../../shared/data/publicNoticesApi';
import styles from './HomeDiscovery.module.css';

const ALL_LABEL = '모두';

type MenuGroup = 'travel' | 'beauty';

type MenuSelection = {
  readonly group: MenuGroup;
  readonly label: string;
};

const TRAVEL_ITEMS = [
  { label: '예약 판매', src: '/home-menu/travel-reserve.png' },
  { label: '경험', src: '/home-menu/travel-experience.png' },
  { label: '음식 및 음료', src: '/home-menu/travel-food.png' },
  { label: '케이팝', src: '/home-menu/travel-kpop.png' },
  { label: '미용실', src: '/home-menu/travel-salon.png' },
  { label: 'K-뷰티', src: '/home-menu/travel-kbeauty.png' },
  { label: '피부과', src: '/home-menu/travel-derma.png' },
  { label: ALL_LABEL, src: '/home-menu/travel-all.png' },
] as const;

const TREND_CHIPS = [
  '뜨거워',
  '색상 분석',
  '스파 및 미학',
  '뷰티 메이크업',
  '미적 요소',
  '헤드 스파 및 치료',
  '컬러 및 펌',
  '아웃핏 렌탈',
  '신분증 및 초상화',
  '프라이빗 스크럽',
] as const;

const BEAUTY_ITEMS = [
  { label: '스킨케어', src: '/home-menu/beauty-skincare.png' },
  { label: '의학', src: '/home-menu/beauty-medical.png' },
  { label: '네일 및 메이크업', src: '/home-menu/beauty-nail.png' },
  { label: '미용실', src: '/home-menu/beauty-salon.png' },
  { label: '색상 분석', src: '/home-menu/beauty-color.png' },
  { label: '배우고 빛나기', src: '/home-menu/beauty-learn.png' },
  { label: '반영구적', src: '/home-menu/beauty-semi.png' },
] as const;

export const HomeDiscovery = () => {
  const [selection, setSelection] = useState<MenuSelection | null>(null);
  const [notices, setNotices] = useState<readonly PublicNoticeSummary[]>([]);
  const [noticeMessage, setNoticeMessage] = useState('게시글을 불러오는 중입니다.');
  const [products, setProducts] = useState<readonly ProductRecommendationItem[]>([]);
  const [latestEventPath, setLatestEventPath] = useState('/events');

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const posts = await fetchGlowBuzzPosts();
        if (cancelled) {
          return;
        }
        setNotices(posts.slice(0, 3));
        setNoticeMessage(posts.length === 0 ? '게시글이 없습니다.' : '');
      } catch {
        if (!cancelled) {
          setNotices([]);
          setNoticeMessage('게시글을 불러오지 못했습니다.');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const documentId = await fetchLatestPublicEventDocumentId();
        if (!cancelled && documentId) {
          setLatestEventPath(`/events/${documentId}`);
        }
      } catch {
        if (!cancelled) {
          setLatestEventPath('/events');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const tag = selection === null || selection.label === ALL_LABEL ? null : selection.label;
        const next = await fetchLatestInKoreaRecommendations(9, tag);
        if (!cancelled) {
          setProducts(next);
        }
      } catch {
        if (!cancelled) {
          setProducts([]);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [selection]);

  return (
    <div className={styles.wrap}>
      <Link to={latestEventPath} className={styles.shotLink}>
        <img
          src="/home-kpop-style.png"
          alt="당신만의 K-POP 스타일을 발견하세요. 넘버원 테크의 포트라"
          className={styles.shot}
        />
      </Link>
      <Link to="/my-survey" className={styles.shotLink}>
        <img
          src="/home-trip-finder.png"
          alt="설문으로 완성하는 로컬 여행 추천. New Trip Finder"
          className={styles.shot}
        />
      </Link>
      <CategoryMenu selection={selection} onSelect={setSelection} />

      <section className={styles.buzz} id="idolglow-news" aria-labelledby="idolglow-news-title">
        <div className={styles.buzzHead}>
          <h2 id="idolglow-news-title" className={styles.buzzTitle}>
            ArchiveBuzz
          </h2>
          <Link to="/events" className={styles.moreLink}>
            더 보기
          </Link>
        </div>
        {noticeMessage ? <p className={styles.noticeEmpty}>{noticeMessage}</p> : null}
        {notices.length > 0 ? (
          <ul className={styles.noticeList}>
            {notices.map(notice => (
              <li key={notice.documentId}>
                <Link to={`/events/${notice.documentId}`} className={styles.noticeLink}>
                  {notice.title}
                </Link>
              </li>
            ))}
          </ul>
        ) : null}
      </section>

      {products.length > 0 ? <ProductGrid items={products} /> : null}
    </div>
  );
};

function chosen(selection: MenuSelection | null, group: MenuGroup, label: string): boolean {
  return selection?.group === group && selection.label === label;
}

function CategoryMenu({
  selection,
  onSelect,
}: {
  readonly selection: MenuSelection | null;
  readonly onSelect: (selection: MenuSelection) => void;
}) {
  return (
    <nav className={styles.menu} aria-label="홈 분류">
      <section className={styles.panel}>
        <Link to="/search?q=여행" className={styles.panelLink}>
          <h2 className={styles.panelTitle}>여행</h2>
          <Chevron />
        </Link>
        <ul className={styles.iconList}>
          {TRAVEL_ITEMS.map(item => (
            <li key={item.label}>
              <button
                type="button"
                className={styles.iconButton}
                aria-pressed={chosen(selection, 'travel', item.label)}
                onClick={() => onSelect({ group: 'travel', label: item.label })}
              >
                <img
                  src={item.src}
                  alt=""
                  className={`${styles.iconImage} ${chosen(selection, 'travel', item.label) ? styles.iconImageOn : ''}`}
                />
                <span className={styles.iconLabel}>{item.label}</span>
              </button>
            </li>
          ))}
        </ul>
        <ul className={styles.chipList}>
          {TREND_CHIPS.map(chip => (
            <li key={chip}>
              <button
                type="button"
                className={`${styles.chip} ${chosen(selection, 'travel', chip) ? styles.chipOn : ''}`}
                aria-pressed={chosen(selection, 'travel', chip)}
                onClick={() => onSelect({ group: 'travel', label: chip })}
              >
                {chip === '뜨거워' ? <img src="/home-menu/flame.png" alt="" className={styles.flame} /> : null}
                {chip}
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section className={styles.panel}>
        <Link to="/search?q=K뷰티" className={styles.panelLink}>
          <h2 className={styles.panelTitle}>K 뷰티 탐험</h2>
          <Chevron />
        </Link>
        <ul className={styles.beautyList}>
          {BEAUTY_ITEMS.map(item => (
            <li key={item.label}>
              <button
                type="button"
                className={`${styles.beautyButton} ${chosen(selection, 'beauty', item.label) ? styles.chipOn : ''}`}
                aria-pressed={chosen(selection, 'beauty', item.label)}
                onClick={() => onSelect({ group: 'beauty', label: item.label })}
              >
                <img src={item.src} alt="" className={styles.beautyImage} />
                <span>{item.label}</span>
              </button>
            </li>
          ))}
        </ul>
      </section>
    </nav>
  );
}

function Chevron() {
  return (
    <svg viewBox="0 0 24 24" className={styles.chevron} aria-hidden="true">
      <path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  );
}

function ProductGrid({ items }: { readonly items: readonly ProductRecommendationItem[] }) {
  return (
    <ul className={styles.productGrid}>
      {items.map(item => (
        <li key={item.id}>
          <Link to={`/products/${item.id}`} className={styles.productCard}>
            {item.thumbnailUrl ? (
              <img src={item.thumbnailUrl} alt="" className={styles.productImage} />
            ) : (
              <span className={styles.productFallback} />
            )}
            <span className={styles.productName}>{item.name}</span>
            <span className={styles.productMeta}>{formatPrice(item.minPrice)}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

function formatPrice(value: number): string {
  if (!Number.isFinite(value) || value <= 0) {
    return '가격 문의';
  }
  return `${Math.round(value).toLocaleString('ko-KR')}원`;
}
