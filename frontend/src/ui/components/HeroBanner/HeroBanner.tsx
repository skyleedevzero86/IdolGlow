import { memo, useCallback, useEffect, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import type { Banner } from "../../../domains/banner/domain/banner.types";
import styles from './HeroBanner.module.css';

const isExternalLink = (value: string): boolean => /^https?:\/\//i.test(value);

const ChevronLeftIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <polyline points="15 18 9 12 15 6" />
  </svg>
);

const ChevronRightIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <polyline points="9 18 15 12 9 6" />
  </svg>
);

function SlideLink({
  href,
  className,
  children,
}: {
  readonly href: string;
  readonly className: string;
  readonly children: ReactNode;
}) {
  if (isExternalLink(href)) {
    return (
      <a href={href} className={className} target="_blank" rel="noreferrer">
        {children}
      </a>
    );
  }

  return (
    <Link to={href} className={className}>
      {children}
    </Link>
  );
}

export interface HeroBannerProps {
  readonly banners: readonly Banner[];
}

export const HeroBanner = memo(({ banners }: HeroBannerProps) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  useEffect(() => {
    if (currentIndex < banners.length) {
      return;
    }
    setCurrentIndex(0);
  }, [banners.length, currentIndex]);

  const goToNext = useCallback(() => {
    setCurrentIndex(prev => (prev + 1) % banners.length);
  }, [banners.length]);

  const goToPrev = useCallback(() => {
    setCurrentIndex(prev => (prev - 1 + banners.length) % banners.length);
  }, [banners.length]);

  const goToSlide = useCallback((index: number) => {
    setCurrentIndex(index);
  }, []);

  useEffect(() => {
    if (isPaused || banners.length <= 1) {
      return;
    }

    const timer = window.setInterval(goToNext, 5000);
    return () => window.clearInterval(timer);
  }, [banners.length, goToNext, isPaused]);

  const handleMouseEnter = useCallback(() => setIsPaused(true), []);
  const handleMouseLeave = useCallback(() => setIsPaused(false), []);

  if (banners.length === 0) {
    return null;
  }

  return (
    <section
      className={styles.hero}
      aria-label="주요 콘텐츠 슬라이더"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {banners.map((banner, index) => (
        <div
          key={banner.id}
          className={`${styles.slide} ${index === currentIndex ? styles.slideActive : ''}`}
          aria-hidden={index !== currentIndex}
        >
          <img
            src={banner.imageUrl}
            alt={index === currentIndex ? banner.title : ''}
            className={styles.slideImage}
            loading={index === 0 ? 'eager' : 'lazy'}
          />
          <div className={styles.overlay} aria-hidden="true" />

          <div className={styles.content}>
            <span className={styles.category}>{banner.category}</span>
            <h2 className={styles.title}>
              <SlideLink href={banner.linkUrl} className={styles.titleLink}>
                {banner.title}
              </SlideLink>
            </h2>
            <p className={styles.subtitle}>{banner.subtitle}</p>
          </div>
        </div>
      ))}

      <div className={styles.controls}>
        <button
          type="button"
          className={styles.navButton}
          onClick={goToPrev}
          aria-label="이전 슬라이드"
        >
          <ChevronLeftIcon />
        </button>

        <div className={styles.indicators} role="tablist" aria-label="슬라이드 선택">
          {banners.map((banner, index) => (
            <button
              key={banner.id}
              type="button"
              className={`${styles.indicator} ${index === currentIndex ? styles.indicatorActive : ''}`}
              onClick={() => goToSlide(index)}
              aria-label={`슬라이드 ${index + 1}로 이동`}
              aria-selected={index === currentIndex}
              role="tab"
            />
          ))}
        </div>

        <button
          type="button"
          className={styles.navButton}
          onClick={goToNext}
          aria-label="다음 슬라이드"
        >
          <ChevronRightIcon />
        </button>
      </div>
    </section>
  );
});

HeroBanner.displayName = 'HeroBanner';

export default HeroBanner;
