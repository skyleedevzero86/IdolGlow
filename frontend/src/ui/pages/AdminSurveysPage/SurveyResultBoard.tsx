import { useEffect, useState } from 'react';
import {
  downloadAdminSurveyStatistics,
  fetchAdminSurveyFormStatistics,
  visibleChoiceLabel,
  type SurveyFormStatistics,
} from '../../../shared/data/surveyFormAdminApi';
import styles from './AdminSurveysPage.module.css';

type SurveyResultBoardProps = {
  readonly accessToken: string;
  readonly formId: number;
  readonly onEdit: () => void;
  readonly onList: () => void;
};

export function SurveyResultBoard({ accessToken, formId, onEdit, onList }: SurveyResultBoardProps) {
  const [statistics, setStatistics] = useState<SurveyFormStatistics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);

  const downloadStatistics = async () => {
    if (!statistics) return;
    setDownloading(true);
    setError(null);
    try {
      await downloadAdminSurveyStatistics(accessToken, formId, statistics.title);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '설문 통계 엑셀을 내려받지 못했습니다.');
    } finally {
      setDownloading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setError(null);
      try {
        const next = await fetchAdminSurveyFormStatistics(accessToken, formId);
        if (!cancelled) setStatistics(next);
      } catch (cause) {
        if (!cancelled) setError(cause instanceof Error ? cause.message : '설문 통계를 불러오지 못했습니다.');
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [accessToken, formId]);

  return (
    <section className={styles.resultBoard}>
      <header className={styles.resultHeader}>
        <div>
          <p className={styles.resultKicker}>설문 결과</p>
          <h2 className={styles.resultHeading}>{statistics?.title ?? '설문 통계'}</h2>
          <p className={styles.resultParticipants}>참여자 {statistics?.participantCount ?? 0}명</p>
        </div>
        <div className={styles.resultActions}>
          <button type="button" className={styles.secondaryButton} onClick={onEdit}>문항 수정</button>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => void downloadStatistics()}
            disabled={!statistics || downloading}
          >
            {downloading ? '다운로드 중...' : '엑셀 다운로드'}
          </button>
          <button type="button" className={styles.listButton} onClick={onList}>목록</button>
        </div>
      </header>
      {error ? <p className={styles.resultError}>{error}</p> : null}
      {!statistics && !error ? <p className={styles.helper}>통계를 불러오는 중입니다.</p> : null}
      <div className={styles.resultQuestions}>
        {statistics?.questions.map(question => (
          <article key={question.questionId} className={styles.resultQuestion}>
            <h3 className={styles.resultQuestionTitle}>
              {String(question.order).padStart(2, '0')}. {question.title}
              {question.required ? <em className={styles.requiredMark}> [필수]</em> : null}
            </h3>
            <p className={styles.resultResponseCount}>
              응답 <strong>{question.responseCount}</strong>명
            </p>
            {question.written ? (
              <p className={styles.writtenNotice}>주관식 설문 결과는 표시되지 않습니다.</p>
            ) : (
              <ul className={styles.statList}>
                {question.options.map(option => (
                  <li key={option.label} className={styles.statRow}>
                    <span className={styles.statLabel}>{visibleChoiceLabel(option.label)}</span>
                    <span className={styles.statTrack} aria-hidden="true">
                      <span className={styles.statFill} style={{ width: `${Math.min(100, option.percent)}%` }} />
                    </span>
                    <span className={styles.statValue}>
                      <strong>{option.percent}</strong>%(<strong>{option.count}</strong>명)
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </article>
        ))}
      </div>
    </section>
  );
}
