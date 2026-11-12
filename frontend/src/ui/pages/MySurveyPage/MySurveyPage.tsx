import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchCurrentSurveyForm,
  fetchMyLatestSurveySubmission,
  submitCurrentSurveyForm,
  isWrittenSurveyQuestion,
  type SurveyFormResponse,
} from '../../../shared/data/surveyFormUserApi';
import {
  fetchUserSurvey,
  saveUserSurvey,
  USER_SURVEY_CONCEPT_OPTIONS,
  type UserSurveyConceptType,
} from '../../../shared/data/userSurveyApi';
import { MySurveyBoard, tripDatesIncomplete, type SurveyAnswerDraft } from './MySurveyBoard';
import styles from './MySurveyPage.module.css';

type AnswerDraft = SurveyAnswerDraft;

const emptyAnswers = (form: SurveyFormResponse): Record<number, AnswerDraft> => {
  const next: Record<number, AnswerDraft> = {};
  for (const q of form.questions) {
    next[q.id] = { text: '', selected: [] };
  }
  return next;
};

type SurveyDraftRecord = {
  readonly slideIndex: number;
  readonly answers: Record<string, AnswerDraft>;
};

const surveyDraftKey = (userId: number, formId: number): string => `idolglow-survey-draft:${userId}:${formId}`;

const readSurveyDraft = (userId: number, formId: number): SurveyDraftRecord | null => {
  try {
    const raw = window.localStorage.getItem(surveyDraftKey(userId, formId));
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as Partial<SurveyDraftRecord>;
    if (typeof parsed.slideIndex !== 'number' || !parsed.answers || typeof parsed.answers !== 'object') {
      return null;
    }
    return { slideIndex: parsed.slideIndex, answers: parsed.answers };
  } catch {
    return null;
  }
};

const writeSurveyDraft = (userId: number, formId: number, draft: SurveyDraftRecord): void => {
  try {
    window.localStorage.setItem(surveyDraftKey(userId, formId), JSON.stringify(draft));
  } catch {
    return;
  }
};

const clearSurveyDraft = (userId: number, formId: number): void => {
  try {
    window.localStorage.removeItem(surveyDraftKey(userId, formId));
  } catch {
    return;
  }
};

const answersFromDraft = (form: SurveyFormResponse, stored: SurveyDraftRecord | null): Record<number, AnswerDraft> => {
  const next = emptyAnswers(form);
  if (!stored) {
    return next;
  }
  for (const question of form.questions) {
    const saved = stored.answers[String(question.id)];
    if (!saved || typeof saved.text !== 'string' || !Array.isArray(saved.selected)) {
      continue;
    }
    next[question.id] = {
      text: saved.text,
      selected: saved.selected.filter((item): item is string => typeof item === 'string'),
    };
  }
  return next;
};

export function MySurveyPage() {
  const { accessToken, authReady, user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState<SurveyFormResponse | null>(null);
  const [answers, setAnswers] = useState<Record<number, AnswerDraft>>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [focusRequest, setFocusRequest] = useState<{ index: number; token: number } | null>(null);
  const [surveySubmitted] = useState(false);
  const [resumeIndex, setResumeIndex] = useState(0);
  const travelSectionRef = useRef<HTMLElement>(null);
  const draftHydrated = useRef(false);

  const [travelLoading, setTravelLoading] = useState(true);
  const [travelSaving, setTravelSaving] = useState(false);
  const [travelError, setTravelError] = useState<string | null>(null);
  const [travelMessage, setTravelMessage] = useState<string | null>(null);
  const [concept, setConcept] = useState<UserSurveyConceptType>('GIRL_CRUSH');
  const [idolName, setIdolName] = useState('');
  const [visitStartDate, setVisitStartDate] = useState('');
  const [visitEndDate, setVisitEndDate] = useState('');
  const [placeRows, setPlaceRows] = useState<string[]>(['']);

  const sortedQuestions = useMemo(() => {
    if (!form) {
      return [];
    }
    return [...form.questions].sort((a, b) => a.order - b.order);
  }, [form]);

  const loadTravel = useCallback(async () => {
    if (!accessToken) {
      return;
    }
    setTravelLoading(true);
    setTravelError(null);
    try {
      const s = await fetchUserSurvey(accessToken);
      if (s) {
        setConcept(s.concept);
        setIdolName(s.idolName);
        setVisitStartDate(s.visitStartDate);
        setVisitEndDate(s.visitEndDate);
        setPlaceRows(s.places.length > 0 ? [...s.places] : ['']);
      } else {
        setConcept('GIRL_CRUSH');
        setIdolName('');
        setVisitStartDate('');
        setVisitEndDate('');
        setPlaceRows(['']);
      }
    } catch (e) {
      setTravelError(e instanceof Error ? e.message : '취향·여행 정보를 불러오지 못했습니다.');
    } finally {
      setTravelLoading(false);
    }
  }, [accessToken]);

  const loadForm = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const next = await fetchCurrentSurveyForm(accessToken);
      setForm(next);
      if (next && user) {
        const stored = readSurveyDraft(user.id, next.id);
        const questionCount = next.questions.length;
        const slideIndex = stored ? Math.min(Math.max(stored.slideIndex, 0), Math.max(questionCount - 1, 0)) : 0;
        setAnswers(answersFromDraft(next, stored));
        setResumeIndex(slideIndex);
      } else if (next) {
        setAnswers(emptyAnswers(next));
        setResumeIndex(0);
      } else {
        setAnswers({});
        setResumeIndex(0);
      }
      draftHydrated.current = true;
    } catch (e) {
      setForm(null);
      setError(e instanceof Error ? e.message : '설문지를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [accessToken, user]);

  useEffect(() => {
    if (!authReady || !accessToken) {
      return;
    }
    void loadTravel();
  }, [accessToken, authReady, loadTravel]);

  useEffect(() => {
    if (!authReady || !accessToken) {
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const latest = await fetchMyLatestSurveySubmission(accessToken);
        if (cancelled) {
          return;
        }
        if (latest) {
          navigate('/wish', { replace: true });
          return;
        }
      } catch {
        if (cancelled) {
          return;
        }
      }
      if (!cancelled) {
        void loadForm();
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, loadForm, navigate]);

  useEffect(() => {
    if (!surveySubmitted) {
      return;
    }
    travelSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [surveySubmitted]);

  const rememberSlide = useCallback((index: number) => {
    setResumeIndex(current => (current === index ? current : index));
  }, []);

  useEffect(() => {
    if (!draftHydrated.current || !user || !form || surveySubmitted) {
      return;
    }
    const storedAnswers: Record<string, AnswerDraft> = {};
    for (const [questionId, draft] of Object.entries(answers)) {
      storedAnswers[questionId] = draft;
    }
    writeSurveyDraft(user.id, form.id, { slideIndex: resumeIndex, answers: storedAnswers });
  }, [answers, form, resumeIndex, surveySubmitted, user]);

  const setTextAnswer = (questionId: number, text: string) => {
    setAnswers(current => ({
      ...current,
      [questionId]: { ...current[questionId], text },
    }));
  };

  const toggleMulti = (questionId: number, option: string, checked: boolean) => {
    setAnswers(current => {
      const prev = current[questionId] ?? { text: '', selected: [] };
      const set = new Set(prev.selected);
      if (checked) {
        set.add(option);
      } else {
        set.delete(option);
      }
      return { ...current, [questionId]: { ...prev, selected: Array.from(set) } };
    });
  };

  const setSingle = (questionId: number, option: string) => {
    setAnswers(current => ({
      ...current,
      [questionId]: { ...(current[questionId] ?? { text: '', selected: [] }), selected: [option] },
    }));
  };

  const buildSubmitPayload = () => {
    if (!form) {
      return null;
    }
    const list = sortedQuestions.map(q => {
      const draft = answers[q.id] ?? { text: '', selected: [] };
      if (isWrittenSurveyQuestion(q.type)) {
        return { questionId: q.id, answerText: draft.text.trim(), selectedOptions: [] as string[] };
      }
      return { questionId: q.id, answerText: null, selectedOptions: draft.selected };
    });
    return { answers: list };
  };

  const handleSubmitTravel = async (e: FormEvent) => {
    e.preventDefault();
    if (!accessToken) {
      return;
    }
    const places = placeRows.map(p => p.trim()).filter(Boolean);
    if (!idolName.trim()) {
      setTravelError('아이돌(또는 그룹) 이름을 입력해 주세요.');
      return;
    }
    if (!visitStartDate || !visitEndDate) {
      setTravelError('여행 시작일·종료일을 선택해 주세요.');
      return;
    }
    if (visitEndDate < visitStartDate) {
      setTravelError('여행 종료일은 시작일 이후여야 합니다.');
      return;
    }
    if (places.length === 0) {
      setTravelError('여행·방문 지역(구)을 1곳 이상 입력해 주세요.');
      return;
    }
    setTravelSaving(true);
    setTravelError(null);
    setTravelMessage(null);
    try {
      await saveUserSurvey(accessToken, {
        concept,
        idolName: idolName.trim(),
        visitStartDate,
        visitEndDate,
        places,
      });
      setTravelMessage('취향·여행 정보를 저장했습니다.');
      navigate('/wish');
    } catch (err) {
      setTravelError(err instanceof Error ? err.message : '저장에 실패했습니다.');
    } finally {
      setTravelSaving(false);
    }
  };

  const handleSubmit = async () => {
    if (!accessToken || !form) {
      return;
    }
    const payload = buildSubmitPayload();
    if (!payload) {
      return;
    }
    const missingIndex = sortedQuestions.findIndex(q => {
      if (!q.required) {
        return false;
      }
      const d = answers[q.id] ?? { text: '', selected: [] };
      if (tripDatesIncomplete(q.title, d.text)) {
        return true;
      }
      if (isWrittenSurveyQuestion(q.type)) {
        return d.text.trim().length === 0;
      }
      return d.selected.length === 0;
    });
    if (missingIndex >= 0) {
      const missing = sortedQuestions[missingIndex];
      const draft = missing ? answers[missing.id] : undefined;
      const dateMissing = missing ? tripDatesIncomplete(missing.title, draft?.text ?? '') : false;
      const verb = missing && isWrittenSurveyQuestion(missing.type) ? '입력' : '선택';
      setError(dateMissing ? '여행 시작일과 종료일을 선택해 주세요.' : `필수 문항을 ${verb}해 주세요: ${missing?.title ?? ''}`);
      setFocusRequest({ index: missingIndex, token: Date.now() });
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await submitCurrentSurveyForm(accessToken, payload);
      if (user) {
        clearSurveyDraft(user.id, form.id);
      }
      navigate('/wish', { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : '제출에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!authReady) {
    return (
      <main className={styles.root}>
        <div className={styles.sheet}>
          <p className={styles.subtitle}>로그인 상태를 확인하는 중입니다.</p>
        </div>
      </main>
    );
  }

  if (!accessToken || !user) {
    return (
      <main className={styles.root}>
        <div className={styles.sheet}>
          <h1 className={styles.title}>설문</h1>
          <p className={styles.subtitle}>로그인한 회원만 설문에 참여할 수 있습니다.</p>
          <div className={styles.buttonRow}>
            <Link to="/mypage" className={styles.primaryButton}>
              마이페이지로 이동
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.root}>
      <div className={styles.page}>
        {error ? <p className={styles.error}>{error}</p> : null}
        {message ? <p className={styles.message}>{message}</p> : null}
        {loading ? (
          <p className={styles.subtitle}>불러오는 중…</p>
        ) : !form ? (
          <p className={styles.subtitle}>등록된 운영 설문이 없습니다. 관리자가 설문지를 등록하면 이 영역이 표시됩니다.</p>
        ) : (
          <MySurveyBoard
            form={form}
            answers={answers}
            submitting={submitting}
            onText={setTextAnswer}
            onSingle={setSingle}
            onToggle={toggleMulti}
            onSubmit={() => void handleSubmit()}
            focusRequest={focusRequest}
            initialSlideIndex={resumeIndex}
            onSlideIndex={rememberSlide}
          />
        )}

        {surveySubmitted ? (
          <section ref={travelSectionRef}>
            <hr className={styles.rule} />
            <details className={styles.travelDetails} open>
          <summary>취향·여행 정보 — 컨셉, 아이돌, 기간, 장소</summary>
          {travelError ? <p className={styles.error}>{travelError}</p> : null}
          {travelMessage ? <p className={styles.message}>{travelMessage}</p> : null}
          {travelLoading ? (
            <p className={styles.subtitle}>취향·여행 정보를 불러오는 중…</p>
          ) : (
            <form onSubmit={e => void handleSubmitTravel(e)}>
              <div className={styles.fieldRow}>
                <span className={styles.labelText}>선호 컨셉</span>
                <select
                  className={styles.select}
                  value={concept}
                  onChange={e => setConcept(e.target.value as UserSurveyConceptType)}
                  aria-label="선호 컨셉"
                >
                  {USER_SURVEY_CONCEPT_OPTIONS.map(o => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className={styles.fieldRow}>
                <span className={styles.labelText}>선호 아이돌 또는 그룹</span>
                <input
                  className={styles.input}
                  value={idolName}
                  onChange={e => setIdolName(e.target.value)}
                  placeholder="예: 에스파, 뉴진스"
                  autoComplete="off"
                />
              </div>
              <div className={`${styles.gridTwo} ${styles.fieldRow}`}>
                <div>
                  <span className={styles.labelText}>방문 시작일</span>
                  <input
                    className={styles.input}
                    type="date"
                    value={visitStartDate}
                    onChange={e => setVisitStartDate(e.target.value)}
                  />
                </div>
                <div>
                  <span className={styles.labelText}>방문 종료일</span>
                  <input
                    className={styles.input}
                    type="date"
                    value={visitEndDate}
                    onChange={e => setVisitEndDate(e.target.value)}
                  />
                </div>
              </div>
              <div className={styles.fieldRow}>
                <span className={styles.labelText}>여행·방문 지역 (복수)</span>
                {placeRows.map((row, idx) => (
                  <div key={idx} className={styles.placeRow}>
                    <input
                      className={styles.input}
                      value={row}
                      onChange={e => setPlaceRows(current => current.map((v, i) => (i === idx ? e.target.value : v)))}
                      placeholder="예: 용산구, 홍대입구 일대"
                    />
                    <button
                      type="button"
                      className={styles.iconButton}
                      onClick={() => setPlaceRows(current => current.filter((_, i) => i !== idx))}
                      disabled={placeRows.length <= 1}
                    >
                      제거
                    </button>
                  </div>
                ))}
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => setPlaceRows(current => [...current, ''])}
                >
                  지역 추가
                </button>
              </div>
              <div className={styles.buttonRow}>
                <button type="submit" className={styles.primaryButton} disabled={travelSaving}>
                  {travelSaving ? '저장 중…' : '취향·여행 정보 저장'}
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => void loadTravel()}
                  disabled={travelSaving}
                >
                  다시 불러오기
                </button>
              </div>
            </form>
          )}
            </details>
          </section>
        ) : null}
      </div>
    </main>
  );
}

export default MySurveyPage;
