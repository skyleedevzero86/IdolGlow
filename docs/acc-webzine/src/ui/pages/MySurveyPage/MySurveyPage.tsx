import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchCurrentSurveyForm,
  fetchMyLatestSurveySubmission,
  generateSurveyRecommendation,
  submitCurrentSurveyForm,
  type SurveyFormResponse,
  type SurveyQuestionType,
  type SurveyRecommendationResponse,
  type SurveySubmissionResponse,
} from '../../../shared/data/surveyFormUserApi';
import {
  fetchUserSurvey,
  saveUserSurvey,
  USER_SURVEY_CONCEPT_OPTIONS,
  type UserSurveyConceptType,
} from '../../../shared/data/userSurveyApi';
import styles from './MySurveyPage.module.css';

type AnswerDraft = {
  readonly text: string;
  readonly selected: string[];
};

const emptyAnswers = (form: SurveyFormResponse): Record<number, AnswerDraft> => {
  const next: Record<number, AnswerDraft> = {};
  for (const q of form.questions) {
    next[q.id] = { text: '', selected: [] };
  }
  return next;
};

const formatMoney = (n: number) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(n);

export function MySurveyPage() {
  const { accessToken, authReady, user } = useAuth();
  const [form, setForm] = useState<SurveyFormResponse | null>(null);
  const [answers, setAnswers] = useState<Record<number, AnswerDraft>>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [submission, setSubmission] = useState<SurveySubmissionResponse | null>(null);
  const [recommendation, setRecommendation] = useState<SurveyRecommendationResponse | null>(null);
  const [useLlm, setUseLlm] = useState(true);
  const [recLoading, setRecLoading] = useState(false);

  const [travelLoading, setTravelLoading] = useState(true);
  const [travelSaving, setTravelSaving] = useState(false);
  const [travelError, setTravelError] = useState<string | null>(null);
  const [travelMessage, setTravelMessage] = useState<string | null>(null);
  const [travelId, setTravelId] = useState<number | null>(null);
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
        setTravelId(s.id);
        setConcept(s.concept);
        setIdolName(s.idolName);
        setVisitStartDate(s.visitStartDate);
        setVisitEndDate(s.visitEndDate);
        setPlaceRows(s.places.length > 0 ? [...s.places] : ['']);
      } else {
        setTravelId(null);
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
      if (next) {
        setAnswers(emptyAnswers(next));
      } else {
        setAnswers({});
      }
    } catch (e) {
      setForm(null);
      setError(e instanceof Error ? e.message : '설문지를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

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
    void loadForm();
  }, [accessToken, authReady, loadForm]);

  useEffect(() => {
    if (!authReady || !accessToken) {
      return;
    }
    let cancelled = false;
    const loadLatest = async () => {
      try {
        const latest = await fetchMyLatestSurveySubmission(accessToken);
        if (!cancelled) {
          setSubmission(latest);
        }
      } catch {
        if (!cancelled) {
          setSubmission(null);
        }
      }
    };
    void loadLatest();
    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady]);

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
      if (q.type === 'TEXT') {
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
      const res = await saveUserSurvey(accessToken, {
        concept,
        idolName: idolName.trim(),
        visitStartDate,
        visitEndDate,
        places,
      });
      setTravelId(res.id);
      setTravelMessage('취향·여행 정보를 저장했습니다.');
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
    for (const q of sortedQuestions) {
      if (!q.required) {
        continue;
      }
      const d = answers[q.id] ?? { text: '', selected: [] };
      if (q.type === 'TEXT' && !d.text.trim()) {
        setError(`필수 문항을 입력해 주세요: ${q.title}`);
        return;
      }
      if (q.type !== 'TEXT' && d.selected.length === 0) {
        setError(`필수 문항을 선택해 주세요: ${q.title}`);
        return;
      }
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const created = await submitCurrentSurveyForm(accessToken, payload);
      setSubmission(created);
      setMessage('설문을 제출했습니다. 아래에서 추천을 생성할 수 있습니다.');
      setRecommendation(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : '제출에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRecommend = async () => {
    if (!accessToken || !submission) {
      setError('먼저 설문을 제출해 주세요.');
      return;
    }
    setRecLoading(true);
    setError(null);
    setMessage(null);
    try {
      const res = await generateSurveyRecommendation(accessToken, submission.id, useLlm);
      setRecommendation(res);
    } catch (e) {
      setError(e instanceof Error ? e.message : '추천 생성에 실패했습니다.');
    } finally {
      setRecLoading(false);
    }
  };

  const renderQuestionControl = (q: {
    id: number;
    title: string;
    description: string | null;
    type: SurveyQuestionType;
    options: readonly string[];
  }) => {
    const draft = answers[q.id] ?? { text: '', selected: [] };
    if (q.type === 'TEXT') {
      return (
        <textarea
          className={styles.textarea}
          value={draft.text}
          onChange={e => setTextAnswer(q.id, e.target.value)}
          placeholder="답변을 입력해 주세요."
        />
      );
    }
    if (q.type === 'SINGLE_CHOICE') {
      return (
        <div>
          {q.options.map(opt => (
            <label key={opt} className={styles.label}>
              <input type="radio" name={`q-${q.id}`} checked={draft.selected[0] === opt} onChange={() => setSingle(q.id, opt)} />
              <span>{opt}</span>
            </label>
          ))}
        </div>
      );
    }
    return (
      <div>
        {q.options.map(opt => (
          <label key={opt} className={styles.label}>
            <input
              type="checkbox"
              checked={draft.selected.includes(opt)}
              onChange={e => toggleMulti(q.id, opt, e.target.checked)}
            />
            <span>{opt}</span>
          </label>
        ))}
      </div>
    );
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
          <p className={styles.subtitle}>로그인 후 이용할 수 있습니다.</p>
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
      <div className={styles.sheet}>
        <h1 className={styles.title}>설문</h1>
        <p className={styles.subtitle}>
          현재 운영 중인 <strong>운영 설문지</strong>에 응답하고, 제출 후 맞춤 추천을 받을 수 있습니다. (관리자는 <strong>설문관리</strong>{' '}
          메뉴에서 문항을 등록·수정합니다.) 취향·아이돌·여행 기간·장소는 맨 아래 <strong>취향·여행 정보</strong>에서 함께 저장할 수 있습니다.
        </p>
        {error ? <p className={styles.error}>{error}</p> : null}
        {message ? <p className={styles.message}>{message}</p> : null}

        <h2 className={styles.sectionHeading}>운영 설문</h2>
        {loading ? (
          <p className={styles.subtitle}>불러오는 중…</p>
        ) : !form ? (
          <p className={styles.subtitle}>등록된 운영 설문이 없습니다. 관리자가 설문지를 등록하면 이 영역이 표시됩니다.</p>
        ) : (
          <form
            onSubmit={e => {
              e.preventDefault();
              void handleSubmit();
            }}
          >
            {sortedQuestions.map(q => (
              <section key={q.id} className={styles.questionBlock}>
                <h2 className={styles.questionTitle}>
                  {q.title}
                  {q.required ? <span className={styles.badge}>필수</span> : null}
                </h2>
                {q.description ? <p className={styles.questionDesc}>{q.description}</p> : null}
                {renderQuestionControl(q)}
              </section>
            ))}
            <div className={styles.buttonRow}>
              <button type="submit" className={styles.primaryButton} disabled={submitting}>
                {submitting ? '제출 중…' : '설문 제출'}
              </button>
              <button type="button" className={styles.secondaryButton} onClick={() => void loadForm()} disabled={submitting}>
                설문 새로고침
              </button>
            </div>
          </form>
        )}

        <hr className={styles.rule} />

        <section>
          <h2 className={styles.title}>추천 생성</h2>
          <p className={styles.subtitle}>
            최근 제출 기준으로 추천을 만듭니다. 제출 직후에는 위에서 방금 제출한 결과가 사용되고, 이후에는 서버에 저장된 최신 제출을
            사용합니다.
          </p>
          {submission ? (
            <p className={styles.questionDesc}>
              최신 제출 ID: <strong>{submission.id}</strong> (폼 #{submission.formId})
            </p>
          ) : (
            <p className={styles.questionDesc}>아직 제출 이력이 없습니다. 위 설문을 먼저 제출해 주세요.</p>
          )}
          <div className={styles.toggleRow}>
            <label className={styles.label}>
              <input type="checkbox" checked={useLlm} onChange={e => setUseLlm(e.target.checked)} />
              <span>OpenAI(LLM) 문구 보강 사용</span>
            </label>
          </div>
          <div className={styles.buttonRow}>
            <button type="button" className={styles.primaryButton} onClick={() => void handleRecommend()} disabled={recLoading || !submission}>
              {recLoading ? '생성 중…' : '추천 생성'}
            </button>
          </div>
        </section>

        {recommendation ? (
          <div className={styles.resultBox}>
            <h3 className={styles.resultTitle}>{recommendation.title}</h3>
            <p className={styles.resultSub}>
              {recommendation.subtitle}
              <span className={styles.badge}>{recommendation.llmEnhanced ? 'LLM 보강' : '룰 기반'}</span>
            </p>
            <p className={styles.resultNarrative}>{recommendation.narrative}</p>
            <h4 className={styles.questionTitle}>추천 관광지</h4>
            <ul className={styles.list}>
              {recommendation.attractions.map(a => (
                <li key={a.attractionCode}>
                  <strong>{a.name}</strong> — {a.reason}
                </li>
              ))}
            </ul>
            <h4 className={styles.questionTitle}>추천 상품</h4>
            <div className={styles.productGrid}>
              {recommendation.recommendedProducts.map(p => (
                <article key={p.id} className={styles.productCard}>
                  <div className={styles.productName}>
                    <Link to={`/products/${p.id}`}>#{p.id} {p.name}</Link>
                  </div>
                  <div>{formatMoney(p.totalPrice)}</div>
                </article>
              ))}
            </div>
          </div>
        ) : null}

        <hr className={styles.rule} />

        <details className={styles.travelDetails}>
          <summary>취향·여행 정보 (UserSurvey) — 컨셉, 아이돌, 기간, 장소</summary>
          <p className={styles.questionDesc}>
            회원 API <code>GET/POST /surveys</code>와 연결됩니다.
            {travelId != null ? (
              <>
                {' '}
                <strong>등록 ID: {travelId}</strong>
              </>
            ) : null}
          </p>
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
      </div>
    </main>
  );
}

export default MySurveyPage;
