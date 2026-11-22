import { memo } from 'react';
import type { ArticleViewModel } from "../../../domains/article/domain/article.types";
import { ArticleCard, type ArticleCardLikeTarget } from '../ArticleCard/ArticleCard';
import styles from './VolumeSection.module.css';

export interface VolumeSectionCard {
  readonly article: ArticleViewModel;
  readonly linkTo?: string;
  readonly likeTarget?: ArticleCardLikeTarget;
}

interface VolumeSectionProps {
  readonly volume: number | null;
  readonly date?: string;
  readonly cards: readonly VolumeSectionCard[];
}

export const VolumeSection = memo<VolumeSectionProps>(({ volume, date, cards }) => {
  const headingId = volume !== null ? `volume-${volume}` : undefined;
  return (
    <section className={styles.section} aria-labelledby={headingId} aria-label={headingId ? undefined : '트렌드 기사'}>
      {volume !== null ? (
        <header className={styles.header}>
          <h2 className={styles.volume} id={headingId}>
            Vol.<span className={styles.volumeNumber}>{volume}</span>
          </h2>
          {date ? <span className={styles.date}>{date}</span> : null}
        </header>
      ) : null}

      <div className={styles.grid}>
        {cards.map(card => (
          <ArticleCard key={card.article.id} article={card.article} linkTo={card.linkTo} likeTarget={card.likeTarget} />
        ))}
      </div>
    </section>
  );
});

VolumeSection.displayName = 'VolumeSection';

export default VolumeSection;
