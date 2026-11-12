import { useEffect, useMemo, useState } from 'react';
import {
  isWrittenSurveyQuestion,
  visibleChoiceLabel,
  type SurveyFormResponse,
} from '../../../shared/data/surveyFormUserApi';
import styles from './MySurveyPage.module.css';

type SurveyQuestion = SurveyFormResponse['questions'][number];

export type SurveyAnswerDraft = {
  readonly text: string;
  readonly selected: string[];
};

type MySurveyBoardProps = {
  readonly form: SurveyFormResponse;
  readonly answers: Record<number, SurveyAnswerDraft>;
  readonly submitting: boolean;
  readonly onText: (questionId: number, text: string) => void;
  readonly onSingle: (questionId: number, option: string) => void;
  readonly onToggle: (questionId: number, option: string, checked: boolean) => void;
  readonly onSubmit: () => void;
  readonly focusRequest: { readonly index: number; readonly token: number } | null;
  readonly initialSlideIndex: number;
  readonly onSlideIndex: (index: number) => void;
};

const TRIP_SEPARATOR = ' / ';
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function formatWrittenDate(value: string | null | undefined): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function plainDescription(value: string | null): string {
  if (!value) return '';
  return value
    .replace(/!\[[^\]]*]\([^)]*\)/g, '')
    .replace(/\[([^\]]+)]\([^)]*\)/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/[*_`]/g, '')
    .trim();
}

function textLimit(description: string | null): number | null {
  const matched = description?.match(/(\d+)\s*자/);
  if (!matched) return null;
  const value = Number(matched[1]);
  return Number.isInteger(value) && value > 0 ? value : null;
}

function isTripQuestion(title: string): boolean {
  return title.includes('지역') && (title.includes('인원') || title.includes('예산'));
}

function splitTrip(text: string): readonly [string, string, string, string] {
  const parts = text.split(TRIP_SEPARATOR);
  if (parts.length >= 4) {
    return [parts[0] ?? '', parts[1] ?? '', parts[2] ?? '', parts.slice(3).join(TRIP_SEPARATOR)];
  }
  return [text, '', '', ''];
}

function joinTrip(parts: readonly string[]): string {
  const trimmed = parts.map(part => part.trim());
  if (trimmed.every(part => part.length === 0)) {
    return '';
  }
  return trimmed.join(TRIP_SEPARATOR);
}

function splitDateRange(value: string): readonly [string, string] {
  const [start, end] = value.split('~');
  const startDate = start?.trim() ?? '';
  const endDate = end?.trim() ?? '';
  return [ISO_DATE.test(startDate) ? startDate : '', ISO_DATE.test(endDate) ? endDate : ''];
}

function joinDateRange(start: string, end: string): string {
  if (!start && !end) {
    return '';
  }
  return `${start}~${end}`;
}

export function tripDatesIncomplete(title: string, text: string): boolean {
  if (!isTripQuestion(title)) {
    return false;
  }
  const [start, end] = splitDateRange(splitTrip(text)[1]);
  return !start || !end || end < start;
}

export function MySurveyBoard({
  form,
  answers,
  submitting,
  onText,
  onSingle,
  onToggle,
  onSubmit,
  focusRequest,
  initialSlideIndex,
  onSlideIndex,
}: MySurveyBoardProps) {
  const questions = useMemo(
    () => [...form.questions].sort((a, b) => a.order - b.order),
    [form.questions],
  );
  const [slideIndex, setSlideIndex] = useState(initialSlideIndex);
  useEffect(() => {
    if (focusRequest) {
      setSlideIndex(focusRequest.index);
    }
  }, [focusRequest]);
  const activeIndex = questions.length === 0 ? 0 : Math.min(slideIndex, questions.length - 1);
  useEffect(() => {
    onSlideIndex(activeIndex);
  }, [activeIndex, onSlideIndex]);
  const activeQuestion = questions[activeIndex];
  const writtenDate = formatWrittenDate(form.createdAt);
  const description = plainDescription(form.description);
  const progress = questions.length === 0 ? 0 : ((activeIndex + 1) / questions.length) * 100;

  return (
    <section className={styles.board}>
      <header className={styles.boardHeader}>
        <p className={styles.boardKicker}>설문 참여</p>
        <h1 className={styles.boardTitle}>{form.title}</h1>
        {writtenDate ? <p className={styles.boardMeta}>작성일: {writtenDate}</p> : null}
        {description ? <p className={styles.boardDescription}>{description}</p> : null}
        <div className={styles.slideTrack} aria-hidden="true">
          <span style={{ width: `${progress}%` }} />
        </div>
      </header>
      {activeQuestion ? (
        <QuestionAnswer
          question={activeQuestion}
          index={activeIndex}
          total={questions.length}
          draft={answers[activeQuestion.id] ?? { text: '', selected: [] }}
          submitting={submitting}
          onText={onText}
          onSingle={onSingle}
          onToggle={onToggle}
          onPrevious={activeIndex > 0 ? () => setSlideIndex(activeIndex - 1) : null}
          onNext={activeIndex < questions.length - 1 ? () => setSlideIndex(activeIndex + 1) : null}
          onSubmit={onSubmit}
        />
      ) : (
        <p className={styles.questionDesc}>등록된 문항이 없습니다.</p>
      )}
    </section>
  );
}

function QuestionAnswer({
  question,
  index,
  total,
  draft,
  onText,
  onSingle,
  onToggle,
  submitting,
  onPrevious,
  onNext,
  onSubmit,
}: {
  readonly question: SurveyQuestion;
  readonly index: number;
  readonly total: number;
  readonly draft: SurveyAnswerDraft;
  readonly submitting: boolean;
  readonly onText: (questionId: number, text: string) => void;
  readonly onSingle: (questionId: number, option: string) => void;
  readonly onToggle: (questionId: number, option: string, checked: boolean) => void;
  readonly onPrevious: (() => void) | null;
  readonly onNext: (() => void) | null;
  readonly onSubmit: () => void;
}) {
  const description = plainDescription(question.description);
  const limit = isTripQuestion(question.title) ? null : textLimit(question.description);

  return (
    <div className={styles.answerPane}>
      <h2 className={styles.answerTitle}>
        <span className={styles.questionNo}>{index + 1}</span>
        <span>
          {question.title}
          {question.required ? <em className={styles.requiredMark}> [필수]</em> : null}
        </span>
      </h2>
      <p className={styles.answerProgress}>{index + 1} / {total}</p>
      {description ? <p className={styles.questionDesc}>{description}</p> : null}
      <AnswerControl question={question} draft={draft} limit={limit} onText={onText} onSingle={onSingle} onToggle={onToggle} />
      <div className={styles.answerFooter}>
        <div className={styles.buttonRow}>
          {onPrevious ? <button type="button" className={styles.secondaryButton} onClick={onPrevious}>이전</button> : null}
          {onNext ? <button type="button" className={styles.secondaryButton} onClick={onNext}>다음</button> : null}
        </div>
        {onNext ? null : (
          <button type="button" className={styles.listButton} onClick={onSubmit} disabled={submitting}>
            {submitting ? '제출 중…' : '설문 제출'}
          </button>
        )}
      </div>
    </div>
  );
}

function AnswerControl({
  question,
  draft,
  limit,
  onText,
  onSingle,
  onToggle,
}: {
  readonly question: SurveyQuestion;
  readonly draft: SurveyAnswerDraft;
  readonly limit: number | null;
  readonly onText: (questionId: number, text: string) => void;
  readonly onSingle: (questionId: number, option: string) => void;
  readonly onToggle: (questionId: number, option: string, checked: boolean) => void;
}) {
  if (question.type === 'SHORT_TEXT' && isTripQuestion(question.title)) {
    return <TripFields questionId={question.id} text={draft.text} onText={onText} />;
  }
  if (question.type === 'SHORT_TEXT') {
    return (
      <label className={styles.answerField}>
        <input
          className={styles.input}
          type="text"
          value={draft.text}
          maxLength={limit ?? undefined}
          onChange={event => onText(question.id, event.target.value)}
          placeholder="한 줄로 입력해 주세요."
        />
        {limit ? <span className={styles.charCount}>{draft.text.length} / {limit}</span> : null}
      </label>
    );
  }
  if (question.type === 'LONG_TEXT' || question.type === 'TEXT') {
    return (
      <label className={styles.answerField}>
        <textarea
          className={styles.textarea}
          value={draft.text}
          maxLength={limit ?? undefined}
          onChange={event => onText(question.id, event.target.value)}
          placeholder="답변을 입력해 주세요."
        />
        {limit ? <span className={styles.charCount}>{draft.text.length} / {limit}</span> : null}
      </label>
    );
  }
  if (question.type === 'SINGLE_CHOICE') {
    return (
      <div className={styles.choiceList}>
        {question.options.map(option => (
          <label key={option} className={styles.choiceRow}>
            <input type="radio" name={`q-${question.id}`} checked={draft.selected[0] === option} onChange={() => onSingle(question.id, option)} />
            <span>{visibleChoiceLabel(option)}</span>
          </label>
        ))}
      </div>
    );
  }
  return (
    <div className={styles.choiceList}>
      {question.options.map(option => (
        <label key={option} className={styles.choiceRow}>
          <input
            type="checkbox"
            checked={draft.selected.includes(option)}
            onChange={event => onToggle(question.id, option, event.target.checked)}
          />
          <span>{visibleChoiceLabel(option)}</span>
        </label>
      ))}
    </div>
  );
}

function TripFields({
  questionId,
  text,
  onText,
}: {
  readonly questionId: number;
  readonly text: string;
  readonly onText: (questionId: number, text: string) => void;
}) {
  const [region, dates, people, budget] = splitTrip(text);
  const [startDate, endDate] = splitDateRange(dates);
  const update = (next: readonly [string, string, string, string]) => onText(questionId, joinTrip(next));
  const updateDates = (start: string, end: string) => update([region, joinDateRange(start, end), people, budget]);

  return (
    <div className={styles.tripGrid}>
      <label className={styles.answerField}>
        <span className={styles.labelText}>지역</span>
        <input className={styles.input} value={region} onChange={event => update([event.target.value, dates, people, budget])} placeholder="예: 서울" />
      </label>
      <label className={styles.answerField}>
        <span className={styles.labelText}>여행 시작일</span>
        <input className={styles.input} type="date" value={startDate} onChange={event => updateDates(event.target.value, endDate)} />
      </label>
      <label className={styles.answerField}>
        <span className={styles.labelText}>여행 종료일</span>
        <input className={styles.input} type="date" value={endDate} min={startDate || undefined} onChange={event => updateDates(startDate, event.target.value)} />
      </label>
      <label className={styles.answerField}>
        <span className={styles.labelText}>인원</span>
        <input className={styles.input} value={people} onChange={event => update([region, dates, event.target.value, budget])} placeholder="예: 2명" />
      </label>
      <label className={styles.answerField}>
        <span className={styles.labelText}>1인당 체험 예산</span>
        <input className={styles.input} value={budget} onChange={event => update([region, dates, people, event.target.value])} placeholder="예: 1인 15만 원" />
      </label>
    </div>
  );
}
