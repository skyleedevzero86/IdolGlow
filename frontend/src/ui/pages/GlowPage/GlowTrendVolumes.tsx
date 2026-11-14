import { type ReactNode, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { type GlowVolume, mockGlowVolumes } from '../../../shared/data/mockGlowVolumes';
import styles from './GlowTrendVolumes.module.css';

const VOLUMES_PER_PAGE = 4;
const PAGE_GROUP_SIZE = 10;

const pad = (value: number): string => String(value).padStart(2, '0');

const badgeClass = (category: string, index: number): string => {
  if (category === '칼럼') return styles.badgeMuted;
  if (index === 0) return styles.badgeDark;
  return styles.badgeBlue;
};

const VolumeCard = ({ volume }: { readonly volume: GlowVolume }) => (
  <article className={styles.volumeCard}>
    <Link to={`/articles/${volume.items[0]?.articleId ?? ''}`} className={styles.volumeImage} tabIndex={-1} aria-hidden="true">
      <img src={volume.imageUrl} alt="" loading="lazy" />
    </Link>
    <div className={styles.volumeBody}>
      <div className={styles.volumeHead}>
        <h3>Vol.{volume.volume}</h3>
        <span>{volume.year}.{pad(volume.month)}.</span>
      </div>
      <ul className={styles.volumeItems}>
        {volume.items.map((item, index) => (
          <li key={`${volume.volume}-${item.title}`}>
            <span className={`${styles.badge} ${badgeClass(item.category, index)}`}>{item.category}</span>
            <Link to={`/articles/${item.articleId}`}>{item.title}</Link>
          </li>
        ))}
      </ul>
    </div>
  </article>
);

const SelectField = ({
  label,
  value,
  options,
  onChange,
}: {
  readonly label: string;
  readonly value: string;
  readonly options: readonly { readonly value: string; readonly label: string }[];
  readonly onChange: (value: string) => void;
}) => (
  <label className={styles.selectField}>
    <span className={styles.srOnly}>{label}</span>
    <select value={value} onChange={(event) => onChange(event.target.value)}>
      <option value="">{label}</option>
      {options.map((option) => (
        <option key={option.value} value={option.value}>{option.label}</option>
      ))}
    </select>
    <span className={styles.selectIcon} aria-hidden="true" />
  </label>
);

export const GlowTrendVolumes = ({ filterBar }: { readonly filterBar: ReactNode }) => {
  const [year, setYear] = useState('');
  const [month, setMonth] = useState('');
  const [volumeNo, setVolumeNo] = useState('');
  const [page, setPage] = useState(1);

  const latestVolume = mockGlowVolumes[0]?.volume ?? 0;

  const yearOptions = useMemo(
    () => [...new Set(mockGlowVolumes.map((v) => v.year))].map((y) => ({ value: String(y), label: `${y}년` })),
    [],
  );
  const monthOptions = useMemo(
    () =>
      [...new Set(mockGlowVolumes.filter((v) => !year || String(v.year) === year).map((v) => v.month))]
        .sort((a, b) => a - b)
        .map((m) => ({ value: String(m), label: `${m}월` })),
    [year],
  );
  const volumeOptions = useMemo(
    () =>
      mockGlowVolumes
        .filter((v) => (!year || String(v.year) === year) && (!month || String(v.month) === month))
        .map((v) => ({ value: String(v.volume), label: `Vol.${v.volume}` })),
    [year, month],
  );

  const filtered = useMemo(
    () =>
      mockGlowVolumes.filter(
        (v) =>
          (!year || String(v.year) === year) &&
          (!month || String(v.month) === month) &&
          (!volumeNo || String(v.volume) === volumeNo),
      ),
    [year, month, volumeNo],
  );

  useEffect(() => {
    setPage(1);
  }, [year, month, volumeNo]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / VOLUMES_PER_PAGE));
  const pageVolumes = filtered.slice((page - 1) * VOLUMES_PER_PAGE, page * VOLUMES_PER_PAGE);
  const groupStart = Math.floor((page - 1) / PAGE_GROUP_SIZE) * PAGE_GROUP_SIZE + 1;
  const groupEnd = Math.min(totalPages, groupStart + PAGE_GROUP_SIZE - 1);
  const pageNumbers = Array.from({ length: groupEnd - groupStart + 1 }, (_, i) => groupStart + i);

  const goToPage = (next: number) => {
    setPage(Math.min(totalPages, Math.max(1, next)));
  };

  return (
    <div className={styles.trendLayout}>
      <div className={styles.volumeColumn}>
        {filterBar}

        <h2 className={styles.title}>트렌드 별 보기</h2>

        <div className={styles.volumeList}>
          {pageVolumes.map((volume) => (
            <VolumeCard key={volume.volume} volume={volume} />
          ))}
          {pageVolumes.length === 0 ? (
            <p className={styles.empty} role="status">조건에 맞는 호가 없습니다.</p>
          ) : null}
        </div>

        {totalPages > 1 ? (
          <nav className={styles.pagination} aria-label="호별보기 페이지">
            {page > 1 ? (
              <button type="button" className={styles.pageArrow} aria-label="이전 페이지" onClick={() => goToPage(page - 1)}>
                ‹
              </button>
            ) : null}
            {pageNumbers.map((number) => (
              <button
                key={number}
                type="button"
                className={number === page ? styles.activePage : undefined}
                aria-current={number === page ? 'page' : undefined}
                onClick={() => goToPage(number)}
              >
                {number}
              </button>
            ))}
            {page < totalPages ? (
              <button
                type="button"
                className={styles.pageArrow}
                aria-label="다음 페이지"
                onClick={() => goToPage(page + 1)}
              >
                ›
              </button>
            ) : null}
          </nav>
        ) : null}
      </div>

      <aside className={styles.volumeSidebar} aria-label="호 찾기">
        <p className={styles.latest}>
          트렌드 별 Vol. <strong>{latestVolume}</strong>
        </p>
        <div className={styles.selects}>
          <SelectField
            label="발행 연도"
            value={year}
            options={yearOptions}
            onChange={(value) => {
              setYear(value);
              setMonth('');
              setVolumeNo('');
            }}
          />
          <SelectField
            label="발행 월"
            value={month}
            options={monthOptions}
            onChange={(value) => {
              setMonth(value);
              setVolumeNo('');
            }}
          />
          <SelectField label="발행 호" value={volumeNo} options={volumeOptions} onChange={setVolumeNo} />
        </div>
      </aside>
    </div>
  );
};

export default GlowTrendVolumes;
