import { memo } from 'react';
import type { VolumeGroup } from "../../../domains/article/application/articleUseCases";
import { ArticleCard } from '../ArticleCard/ArticleCard';
import styles from './VolumeSection.module.css';

interface VolumeSectionProps {
  readonly volumeGroup: VolumeGroup;
}

export const VolumeSection = memo<VolumeSectionProps>(({ volumeGroup }) => {
  return (
    <section className={styles.section} aria-labelledby={`volume-${volumeGroup.volume}`}>
      <header className={styles.header}>
        <h2 className={styles.volume} id={`volume-${volumeGroup.volume}`}>
          Vol.<span className={styles.volumeNumber}>{volumeGroup.volume}</span>
        </h2>
        <span className={styles.date}>{volumeGroup.date}</span>
      </header>

      <div className={styles.grid}>
        {volumeGroup.articles.map(article => (
          <ArticleCard key={article.id} article={article} />
        ))}
      </div>
    </section>
  );
});

VolumeSection.displayName = 'VolumeSection';

export default VolumeSection;
