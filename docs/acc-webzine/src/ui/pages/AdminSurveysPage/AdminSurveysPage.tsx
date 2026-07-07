import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  createAdminSurveyForm,
  fetchAdminSurveyForm,
  fetchAdminSurveyFormPage,
  updateAdminSurveyForm,
  type SurveyFormPrimaryCategory,
  type SurveyFormQuestionInput,
  type SurveyFormResponse,
  type SurveyFormSecondaryCategory,
  type SurveyFormStatus,
  type SurveyFormSummaryResponse,
  type SurveyQuestionType,
} from '../../../shared/data/surveyFormAdminApi';
import MarkdownEditorField from '../../components/MarkdownEditorField/MarkdownEditorField';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminSurveysPage.module.css';

type SurveyQuestionDraft = SurveyFormQuestionInput;
type SurveyFormDraft = {
  readonly title: string;
  readonly description: string;
  readonly descriptionTagText: string;
  readonly status: SurveyFormStatus;
  readonly primaryCategory: SurveyFormPrimaryCategory;
  readonly secondaryCategory: SurveyFormSecondaryCategory | null;
  readonly questions: readonly SurveyQuestionDraft[];
};
type SurveyPageMeta = {
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
};
type EditorRouteState = {
  readonly surveyMessage?: string;
};
type CategoryOption<T extends string> = {
  readonly value: T;
  readonly label: string;
};

const MAX_OPERATIONAL_QUESTIONS = 5;
const PAGE_SIZE = 10;
const PAGE_WINDOW = 10;
const QUESTION_TYPE_OPTIONS: ReadonlyArray<CategoryOption<SurveyQuestionType>> = [
  { value: 'TEXT', label: '주관식' },
  { value: 'SINGLE_CHOICE', label: '단일 선택' },
  { value: 'MULTIPLE_CHOICE', label: '다중 선택' },
];
const STATUS_OPTIONS: ReadonlyArray<CategoryOption<SurveyFormStatus>> = [
  { value: 'IN_PROGRESS', label: '진행' },
  { value: 'SCHEDULED', label: '예정' },
  { value: 'COMPLETED', label: '완료' },
];
const PRIMARY_CATEGORY_OPTIONS: ReadonlyArray<CategoryOption<SurveyFormPrimaryCategory>> = [
  { value: 'ALL', label: '전체' },
  { value: 'TOUR_EXPERIENCE', label: '투어&체험' },
  { value: 'TRAVEL_ESSENTIALS', label: '여행필수품' },
  { value: 'BEAUTY', label: '뷰티' },
  { value: 'MEDICAL', label: '의료' },
  { value: 'ETC', label: '기타' },
];
const SECONDARY_CATEGORY_OPTIONS: Record<SurveyFormPrimaryCategory, ReadonlyArray<CategoryOption<SurveyFormSecondaryCategory>>> = {
  ALL: [],
  TOUR_EXPERIENCE: [
    { value: 'ACTIVITY', label: '활동' },
    { value: 'FOOD', label: '미식' },
    { value: 'K_POP', label: 'K-pop' },
    { value: 'ATTRACTIONS_TICKETS', label: '흥미거리&티켓' },
    { value: 'PHOTO', label: '포토' },
    { value: 'TOUR', label: '투어' },
  ],
  TRAVEL_ESSENTIALS: [
    { value: 'WIFI_SIM', label: 'wifi&sim' },
    { value: 'TRANSPORTATION', label: '교통' },
    { value: 'TRAVEL_SERVICE', label: '여행서비스' },
    { value: 'EXCHANGE', label: '환전' },
    { value: 'INSURANCE', label: '보험' },
  ],
  BEAUTY: [
    { value: 'HAIR_SALON', label: '헤어살롱' },
    { value: 'K_BEAUTY', label: 'k-뷰티' },
    { value: 'SKIN_CARE', label: '피부미용과' },
  ],
  MEDICAL: [
    { value: 'CLINIC', label: '클리닉' },
    { value: 'PHARMACY', label: '약국' },
    { value: 'VISION_CORRECTION', label: '시력교정' },
    { value: 'HEALTH_CHECKUP', label: '건강진단' },
    { value: 'KOREAN_MEDICINE', label: '한의원' },
  ],
  ETC: [
    { value: 'SPA_HEALING', label: '스파&치유' },
    { value: 'COUPON', label: '쿠폰' },
  ],
};
const ALL_SECONDARY_CATEGORY_OPTIONS = Object.values(SECONDARY_CATEGORY_OPTIONS).flat();
const EMPTY_FORM: SurveyFormDraft = {
  title: '',
  description: '',
  descriptionTagText: '',
  status: 'SCHEDULED',
  primaryCategory: 'ALL',
  secondaryCategory: null,
  questions: [],
};
const EMPTY_PAGE_META: SurveyPageMeta = {
  page: 1,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 1,
  hasNext: false,
};

const SurveyStatCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>건</span>
  </>
);

const SurveyPageStat = ({ current, total }: { readonly current: number; readonly total: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{current}</span>
    <span className={shellToolbar.statUnit}>/ {total}</span>
  </>
);

const toDraft = (surveyForm: SurveyFormResponse): SurveyFormDraft => ({
  title: surveyForm.title,
  description: surveyForm.description ?? '',
  descriptionTagText: (surveyForm.descriptionTags ?? []).join(' '),
  status: surveyForm.status === 'PLANNED' ? 'SCHEDULED' : surveyForm.status,
  primaryCategory: surveyForm.primaryCategory,
  secondaryCategory: surveyForm.secondaryCategory,
  questions: surveyForm.questions.map(q => ({
    order: q.order,
    title: q.title,
    description: q.description ?? '',
    type: q.type,
    required: q.required,
    options: [...q.options],
  })),
});

function parsePositivePage(value: string | null): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1;
}

function parseOption<T extends string>(
  value: string | null,
  options: ReadonlyArray<CategoryOption<T>>,
): T | '' {
  if (!value) return '';
  return options.some(option => option.value === value) ? value as T : '';
}

function parseSecondaryCategoryParam(
  value: string | null,
  primaryCategory: SurveyFormPrimaryCategory | '',
): SurveyFormSecondaryCategory | '' {
  const parsed = parseOption(value, ALL_SECONDARY_CATEGORY_OPTIONS);
  if (!parsed) return '';
  if (primaryCategory && primaryCategory !== 'ALL') {
    return SECONDARY_CATEGORY_OPTIONS[primaryCategory].some(option => option.value === parsed) ? parsed : '';
  }
  return parsed;
}

function buildListSearchParams(params: {
  readonly page: number;
  readonly keyword: string;
  readonly status: SurveyFormStatus | '';
  readonly primaryCategory: SurveyFormPrimaryCategory | '';
  readonly secondaryCategory: SurveyFormSecondaryCategory | '';
}): URLSearchParams {
  const search = new URLSearchParams();
  const keyword = params.keyword.trim();
  if (keyword) {
    search.set('keyword', keyword);
  }
  if (params.status) {
    search.set('status', params.status);
  }
  if (params.primaryCategory) {
    search.set('primaryCategory', params.primaryCategory);
  }
  if (params.secondaryCategory) {
    search.set('secondaryCategory', params.secondaryCategory);
  }
  if (params.page > 1) {
    search.set('page', String(params.page));
  }
  return search;
}

function formatDateTime(value: string | null): string {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  const yy = String(date.getFullYear()).slice(-2);
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${yy}.${mm}.${dd} ${hh}:${min}`;
}

function normalizeQuestions(questions: readonly SurveyQuestionDraft[]): SurveyQuestionDraft[] {
  return questions
    .map(q => ({
      ...q,
      title: q.title.trim(),
      description: q.description?.trim() ?? '',
      options: q.options.map(o => o.trim()).filter(Boolean),
    }))
    .filter(q => q.title.length > 0)
    .map((q, index) => ({ ...q, order: index + 1 }));
}

function toDescriptionTags(value: string): string[] {
  return Array.from(
    new Set(
      value
        .split(/[\s,]+/)
        .map(tag => tag.trim().replace(/^#/, ''))
        .filter(Boolean),
    ),
  );
}

function makePageNumbers(currentPage: number, totalPages: number): number[] {
  const start = Math.floor((currentPage - 1) / PAGE_WINDOW) * PAGE_WINDOW + 1;
  const end = Math.min(totalPages, start + PAGE_WINDOW - 1);
  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
}

function formatCategory(item: Pick<SurveyFormSummaryResponse, 'primaryCategoryLabel' | 'secondaryCategoryLabel'>): string {
  return item.secondaryCategoryLabel ? `${item.primaryCategoryLabel} > ${item.secondaryCategoryLabel}` : item.primaryCategoryLabel;
}

function getStatusClassName(status: SurveyFormStatus): string {
  if (status === 'COMPLETED') return `${styles.statusBadge} ${styles.statusCompleted}`;
  if (status === 'IN_PROGRESS') return `${styles.statusBadge} ${styles.statusInProgress}`;
  if (status === 'SCHEDULED') return `${styles.statusBadge} ${styles.statusScheduled}`;
  return `${styles.statusBadge} ${styles.statusScheduled}`;
}

export function AdminSurveysPage() {
  const { accessToken, authReady, user } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const currentPage = parsePositivePage(searchParams.get('page'));
  const keyword = searchParams.get('keyword')?.trim() ?? '';
  const statusFilter = parseOption(searchParams.get('status'), STATUS_OPTIONS);
  const primaryCategoryFilter = parseOption(searchParams.get('primaryCategory'), PRIMARY_CATEGORY_OPTIONS);
  const secondaryCategoryFilter = parseSecondaryCategoryParam(searchParams.get('secondaryCategory'), primaryCategoryFilter);
  const [items, setItems] = useState<readonly SurveyFormSummaryResponse[]>([]);
  const [pageMeta, setPageMeta] = useState<SurveyPageMeta>(EMPTY_PAGE_META);
  const [loadingList, setLoadingList] = useState(false);
  const [searchInput, setSearchInput] = useState(keyword);
  const [statusInput, setStatusInput] = useState<SurveyFormStatus | ''>(statusFilter);
  const [primaryCategoryInput, setPrimaryCategoryInput] = useState<SurveyFormPrimaryCategory | ''>(primaryCategoryFilter);
  const [secondaryCategoryInput, setSecondaryCategoryInput] = useState<SurveyFormSecondaryCategory | ''>(secondaryCategoryFilter);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setSearchInput(keyword);
    setStatusInput(statusFilter);
    setPrimaryCategoryInput(primaryCategoryFilter);
    setSecondaryCategoryInput(secondaryCategoryFilter);
  }, [keyword, primaryCategoryFilter, secondaryCategoryFilter, statusFilter]);

  const listSecondaryOptions = useMemo(
    () => primaryCategoryInput ? SECONDARY_CATEGORY_OPTIONS[primaryCategoryInput] : [],
    [primaryCategoryInput],
  );

  const updateListQuery = useCallback((next: {
    readonly page?: number;
    readonly keyword?: string;
    readonly status?: SurveyFormStatus | '';
    readonly primaryCategory?: SurveyFormPrimaryCategory | '';
    readonly secondaryCategory?: SurveyFormSecondaryCategory | '';
  }) => {
    setSearchParams(buildListSearchParams({
      page: next.page ?? currentPage,
      keyword: next.keyword ?? keyword,
      status: next.status ?? statusFilter,
      primaryCategory: next.primaryCategory ?? primaryCategoryFilter,
      secondaryCategory: next.secondaryCategory ?? secondaryCategoryFilter,
    }));
  }, [currentPage, keyword, primaryCategoryFilter, secondaryCategoryFilter, setSearchParams, statusFilter]);

  const loadPage = useCallback(async () => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') return;
    setLoadingList(true);
    setError(null);
    try {
      const response = await fetchAdminSurveyFormPage(accessToken, {
        page: currentPage,
        size: PAGE_SIZE,
        keyword,
        status: statusFilter,
        primaryCategory: primaryCategoryFilter,
        secondaryCategory: secondaryCategoryFilter,
      });
      const totalPages = Math.max(1, response.totalPages || 1);
      setItems(response.content);
      setPageMeta({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages,
        hasNext: response.hasNext,
      });

      if (response.page !== currentPage) {
        setSearchParams(buildListSearchParams({
          page: response.page,
          keyword,
          status: statusFilter,
          primaryCategory: primaryCategoryFilter,
          secondaryCategory: secondaryCategoryFilter,
        }), { replace: true });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : '설문 목록을 불러오지 못했습니다.');
      setItems([]);
      setPageMeta(EMPTY_PAGE_META);
    } finally {
      setLoadingList(false);
    }
  }, [
    accessToken,
    authReady,
    currentPage,
    keyword,
    primaryCategoryFilter,
    secondaryCategoryFilter,
    setSearchParams,
    statusFilter,
    user?.role,
  ]);

  useEffect(() => {
    void loadPage();
  }, [loadPage]);

  const totalPages = Math.max(1, pageMeta.totalPages);
  const pageNumbers = useMemo(() => makePageNumbers(pageMeta.page, totalPages), [pageMeta.page, totalPages]);
  const pageStartRow = pageMeta.totalElements === 0 ? 0 : (pageMeta.page - 1) * pageMeta.size + 1;
  const pageEndRow = Math.min(pageMeta.page * pageMeta.size, pageMeta.totalElements);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    updateListQuery({
      page: 1,
      keyword: searchInput,
      status: statusInput,
      primaryCategory: primaryCategoryInput,
      secondaryCategory: secondaryCategoryInput,
    });
  };

  const handleResetSearch = () => {
    setSearchInput('');
    setStatusInput('');
    setPrimaryCategoryInput('');
    setSecondaryCategoryInput('');
    setSearchParams(new URLSearchParams());
  };

  const goToPage = (pageNumber: number) => {
    updateListQuery({ page: Math.min(Math.max(1, pageNumber), totalPages) });
  };

  if (!authReady) return <main className={shellStyles.page}><div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div></main>;
  if (!accessToken || user?.role !== 'ADMIN') return <main className={shellStyles.page}><div className={shellStyles.denied}>관리자만 설문관리 화면을 사용할 수 있습니다.</div></main>;

  return (
    <AdminMarketingShell
      currentPath="/admin/surveys"
      title="설문관리"
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
        stats: shellToolbar.stats,
        statCard: shellToolbar.statCard,
        statLabel: shellToolbar.statLabel,
        statValue: shellToolbar.statValue,
      }}
      statusText={error ? error : loadingList ? '설문 목록을 불러오는 중입니다.' : null}
      stats={[
        { label: '등록 설문 수', value: <SurveyStatCount n={pageMeta.totalElements} /> },
        { label: '현재 페이지', value: <SurveyPageStat current={pageMeta.page} total={totalPages} /> },
        { label: '페이지당', value: <SurveyStatCount n={pageMeta.size} /> },
      ]}
    >
      <div className={styles.container}>
        <section className={styles.sheet}>
          <div className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2 className={styles.panelTitle}>설문 목록</h2>
              <button type="button" className={styles.submitButton} onClick={() => navigate('/admin/surveys/new')}>
                설문 등록
              </button>
            </div>
            <form className={styles.listToolbar} onSubmit={handleSearch}>
              <p className={styles.summary}>
                전체 <strong>{pageMeta.totalElements}</strong>건 · 페이지당 <strong>{pageMeta.size}</strong>건 ·{' '}
                <strong>{pageStartRow}</strong>-<strong>{pageEndRow}</strong>번째 표시
              </p>
              <div className={styles.inlineSearch}>
                <input
                  className={styles.searchInput}
                  value={searchInput}
                  onChange={event => setSearchInput(event.target.value)}
                  placeholder="제목 또는 설명 검색"
                  aria-label="설문 검색어"
                />
                <select
                  className={styles.filterSelect}
                  value={statusInput}
                  onChange={event => setStatusInput(event.target.value as SurveyFormStatus | '')}
                  aria-label="진행상태 필터"
                >
                  <option value="">전체</option>
                  {STATUS_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
                <select
                  className={styles.filterSelect}
                  value={primaryCategoryInput}
                  onChange={event => {
                    setPrimaryCategoryInput(event.target.value as SurveyFormPrimaryCategory | '');
                    setSecondaryCategoryInput('');
                  }}
                  aria-label="대분류 필터"
                >
                  <option value="">전체 대분류</option>
                  {PRIMARY_CATEGORY_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
                <select
                  className={styles.filterSelect}
                  value={secondaryCategoryInput}
                  onChange={event => setSecondaryCategoryInput(event.target.value as SurveyFormSecondaryCategory | '')}
                  disabled={listSecondaryOptions.length === 0}
                  aria-label="소분류 필터"
                >
                  <option value="">전체 소분류</option>
                  {listSecondaryOptions.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
                <button type="submit" className={styles.searchButton}>검색</button>
                {keyword || statusFilter || primaryCategoryFilter || secondaryCategoryFilter ? (
                  <button type="button" className={styles.secondaryButton} onClick={handleResetSearch}>초기화</button>
                ) : null}
              </div>
            </form>

            {loadingList ? (
              <div className={styles.empty}>설문 목록을 불러오는 중입니다.</div>
            ) : items.length === 0 ? (
              <div className={styles.empty}>등록된 설문지가 없습니다.</div>
            ) : (
              <div className={styles.tableWrap}>
                <table className={styles.listTable}>
                  <thead>
                    <tr>
                      <th scope="col" className={styles.colNumber}>번호</th>
                      <th scope="col" className={styles.colStatus}>상태</th>
                      <th scope="col">제목</th>
                      <th scope="col" className={styles.colCategory}>분류</th>
                      <th scope="col" className={styles.colCount}>문항</th>
                      <th scope="col" className={styles.colDate}>수정일</th>
                      <th scope="col" className={styles.colAction}>관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item, index) => {
                      const rowNumber = pageMeta.totalElements - ((pageMeta.page - 1) * pageMeta.size + index);
                      const editPath = `/admin/surveys/${item.id}/edit`;
                      return (
                        <tr
                          key={item.id}
                          onClick={() => navigate(editPath)}
                          onKeyDown={event => {
                            if (event.key === 'Enter' || event.key === ' ') {
                              event.preventDefault();
                              navigate(editPath);
                            }
                          }}
                          tabIndex={0}
                        >
                          <td>{rowNumber}</td>
                          <td><span className={getStatusClassName(item.status)}>{item.statusLabel}</span></td>
                          <td className={styles.titleCell}>
                            <strong>{item.title}</strong>
                            {item.description ? <span>{item.description}</span> : null}
                          </td>
                          <td className={styles.categoryCell}>{formatCategory(item)}</td>
                          <td>{item.questionCount}</td>
                          <td>{formatDateTime(item.updatedAt)}</td>
                          <td>
                            <button
                              type="button"
                              className={styles.rowActionButton}
                              onClick={event => {
                                event.stopPropagation();
                                navigate(editPath);
                              }}
                            >
                              수정
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}

            {pageMeta.totalElements > 0 ? (
              <div className={styles.pagination} aria-label="설문 목록 페이지">
                <button type="button" className={styles.pageButton} onClick={() => goToPage(1)} disabled={pageMeta.page <= 1}>
                  처음
                </button>
                <button type="button" className={styles.pageButton} onClick={() => goToPage(pageMeta.page - 1)} disabled={pageMeta.page <= 1}>
                  이전
                </button>
                {pageNumbers.map(number => (
                  <button
                    key={number}
                    type="button"
                    className={`${styles.pageButton} ${number === pageMeta.page ? styles.pageButtonActive : ''}`}
                    onClick={() => goToPage(number)}
                    aria-current={number === pageMeta.page ? 'page' : undefined}
                  >
                    {number}
                  </button>
                ))}
                <button type="button" className={styles.pageButton} onClick={() => goToPage(pageMeta.page + 1)} disabled={!pageMeta.hasNext}>
                  다음
                </button>
                <button type="button" className={styles.pageButton} onClick={() => goToPage(totalPages)} disabled={pageMeta.page >= totalPages}>
                  마지막
                </button>
              </div>
            ) : null}
          </div>
        </section>
      </div>
    </AdminMarketingShell>
  );
}

export function AdminSurveyEditorPage() {
  const { accessToken, authReady, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { surveyId } = useParams<{ readonly surveyId: string }>();
  const surveyIdNumber = useMemo(() => {
    if (!surveyId) return null;
    const parsed = Number(surveyId);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }, [surveyId]);
  const invalidSurveyId = Boolean(surveyId && surveyIdNumber === null);
  const isEditing = surveyIdNumber !== null;
  const [form, setForm] = useState<SurveyFormDraft>(EMPTY_FORM);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const secondaryCategoryOptions = useMemo(
    () => SECONDARY_CATEGORY_OPTIONS[form.primaryCategory],
    [form.primaryCategory],
  );

  useEffect(() => {
    const routeState = location.state as EditorRouteState | null;
    if (routeState?.surveyMessage) {
      setMessage(routeState.surveyMessage);
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.pathname, location.state, navigate]);

  const loadDetail = useCallback(async (id: number) => {
    if (!accessToken) return;
    setLoadingDetail(true);
    setError(null);
    try {
      const surveyForm = await fetchAdminSurveyForm(accessToken, id);
      setForm(toDraft(surveyForm));
    } catch (e) {
      setError(e instanceof Error ? e.message : '설문 정보를 불러오지 못했습니다.');
    } finally {
      setLoadingDetail(false);
    }
  }, [accessToken]);

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') return;
    if (!surveyId) {
      setForm(EMPTY_FORM);
      setError(null);
      return;
    }
    if (invalidSurveyId) {
      setError('잘못된 설문지 주소입니다.');
      setForm(EMPTY_FORM);
      return;
    }
    if (surveyIdNumber !== null) {
      void loadDetail(surveyIdNumber);
    }
  }, [accessToken, authReady, invalidSurveyId, loadDetail, surveyId, surveyIdNumber, user?.role]);

  const requiredCount = useMemo(() => form.questions.filter(q => q.required).length, [form.questions]);
  const choiceQuestionCount = useMemo(() => form.questions.filter(q => q.type !== 'TEXT').length, [form.questions]);

  const addQuestion = () => {
    if (form.questions.length >= MAX_OPERATIONAL_QUESTIONS) {
      window.alert(`문항은 최대 ${MAX_OPERATIONAL_QUESTIONS}개까지만 추가할 수 있습니다.`);
      return;
    }
    const nextOrder = form.questions.length + 1;
    setForm(current => ({
      ...current,
      questions: [...current.questions, { order: nextOrder, title: '', description: '', type: 'TEXT', required: false, options: [] }],
    }));
  };

  const updateQuestion = (index: number, updater: (question: SurveyQuestionDraft) => SurveyQuestionDraft) => {
    setForm(current => ({
      ...current,
      questions: current.questions.map((q, i) => (i === index ? updater(q) : q)),
    }));
  };

  const removeQuestion = (index: number) => {
    setForm(current => ({
      ...current,
      questions: current.questions.filter((_, i) => i !== index).map((q, i) => ({ ...q, order: i + 1 })),
    }));
  };

  const handlePrimaryCategoryChange = (primaryCategory: SurveyFormPrimaryCategory) => {
    setForm(current => ({
      ...current,
      primaryCategory,
      secondaryCategory: null,
    }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!accessToken) return setError('관리자 API를 사용하려면 로그인해 주세요.');
    if (invalidSurveyId) return setError('잘못된 설문지 주소입니다.');
    if (!form.title.trim()) return setError('설문 제목을 입력해 주세요.');
    if (form.primaryCategory !== 'ALL' && !form.secondaryCategory) {
      return setError('소분류를 선택해 주세요.');
    }

    const normalizedQuestions = normalizeQuestions(form.questions);
    if (normalizedQuestions.length === 0) {
      window.alert('문항이 없으면 저장할 수 없습니다.');
      return;
    }
    if (normalizedQuestions.length > MAX_OPERATIONAL_QUESTIONS) {
      window.alert(`문항은 최대 ${MAX_OPERATIONAL_QUESTIONS}개까지만 저장할 수 있습니다.`);
      return;
    }

    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim() || null,
        descriptionTags: toDescriptionTags(form.descriptionTagText),
        status: form.status,
        primaryCategory: form.primaryCategory,
        secondaryCategory: form.primaryCategory === 'ALL' ? null : form.secondaryCategory,
        questions: normalizedQuestions,
      };
      const response = isEditing && surveyIdNumber !== null
        ? await updateAdminSurveyForm(accessToken, surveyIdNumber, payload)
        : await createAdminSurveyForm(accessToken, payload);
      setForm(toDraft(response));
      if (isEditing) {
        setMessage('설문지를 수정했습니다.');
      } else {
        navigate(`/admin/surveys/${response.id}/edit`, {
          replace: true,
          state: { surveyMessage: '설문지를 등록했습니다.' } satisfies EditorRouteState,
        });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : '설문 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (!authReady) return <main className={shellStyles.page}><div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div></main>;
  if (!accessToken || user?.role !== 'ADMIN') return <main className={shellStyles.page}><div className={shellStyles.denied}>관리자만 설문관리 화면을 사용할 수 있습니다.</div></main>;

  return (
    <AdminMarketingShell
      currentPath="/admin/surveys"
      title={isEditing ? '설문지 수정' : '설문지 등록'}
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
        stats: shellToolbar.stats,
        statCard: shellToolbar.statCard,
        statLabel: shellToolbar.statLabel,
        statValue: shellToolbar.statValue,
      }}
      statusText={error ? error : loadingDetail ? '설문 정보를 불러오는 중입니다.' : null}
      stats={[
        { label: '문항 수', value: <SurveyStatCount n={form.questions.length} /> },
        { label: '필수 문항 수', value: <SurveyStatCount n={requiredCount} /> },
        { label: '선택형 문항 수', value: <SurveyStatCount n={choiceQuestionCount} /> },
      ]}
    >
      <div className={styles.container}>
        <div className={styles.sheet}>
          <form className={styles.form} onSubmit={handleSubmit}>
            <section className={styles.panel}>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>{isEditing ? `설문지 #${surveyIdNumber}` : '새 설문지'}</h2>
                <button type="button" className={styles.secondaryButton} onClick={() => navigate('/admin/surveys')}>
                  목록
                </button>
              </div>
              <div className={styles.gridTwo}>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>진행상태</span>
                  <select className={styles.select} value={form.status} onChange={e => setForm(c => ({ ...c, status: e.target.value as SurveyFormStatus }))}>
                    {STATUS_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>대분류</span>
                  <select className={styles.select} value={form.primaryCategory} onChange={e => handlePrimaryCategoryChange(e.target.value as SurveyFormPrimaryCategory)}>
                    {PRIMARY_CATEGORY_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>소분류</span>
                  <select
                    className={styles.select}
                    value={form.secondaryCategory ?? ''}
                    onChange={e => setForm(c => ({ ...c, secondaryCategory: e.target.value as SurveyFormSecondaryCategory }))}
                    disabled={secondaryCategoryOptions.length === 0}
                  >
                    <option value="">{secondaryCategoryOptions.length === 0 ? '대분류를 먼저 선택하세요' : '소분류 선택'}</option>
                    {secondaryCategoryOptions.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>설문 제목</span>
                  <input className={styles.input} value={form.title} onChange={e => setForm(c => ({ ...c, title: e.target.value }))} />
                </label>
              </div>
            </section>

            <section className={styles.panel}>
              <MarkdownEditorField
                label="설문 설명"
                value={form.description}
                onChange={value => setForm(c => ({ ...c, description: value }))}
                placeholder="설문 설명을 마크다운으로 작성해 주세요."
                minHeight={320}
                tagsValue={form.descriptionTagText}
                onTagsChange={value => setForm(c => ({ ...c, descriptionTagText: value }))}
                tagsPlaceholder="태그를 입력하세요 (예: 투어 활동 K-pop)"
                showPreview={false}
              />
            </section>

            <section className={styles.panel}>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>설문 문항 관리</h2>
                <button type="button" className={styles.submitButton} onClick={addQuestion} disabled={form.questions.length >= MAX_OPERATIONAL_QUESTIONS}>
                  문항 추가
                </button>
              </div>
              <div className={styles.questionList}>
                {form.questions.length === 0 ? <p className={styles.helper}>문항을 추가해 설문지를 구성하세요.</p> : null}
                {form.questions.map((q, index) => (
                  <article key={`${q.order}-${index}`} className={styles.questionCard}>
                    <div className={styles.questionHeader}>
                      <strong className={styles.questionOrder}>Q{index + 1}</strong>
                      <button type="button" className={styles.removeButton} onClick={() => removeQuestion(index)}>삭제</button>
                    </div>
                    <div className={styles.gridTwo}>
                      <label className={`${styles.field} ${styles.fullWidth}`}>
                        <span className={styles.label}>문항 제목</span>
                        <input className={styles.input} value={q.title} onChange={e => updateQuestion(index, p => ({ ...p, title: e.target.value }))} />
                      </label>
                      <label className={`${styles.field} ${styles.fullWidth}`}>
                        <span className={styles.label}>문항 설명</span>
                        <textarea className={styles.textarea} rows={3} value={q.description ?? ''} onChange={e => updateQuestion(index, p => ({ ...p, description: e.target.value }))} />
                      </label>
                      <label className={styles.field}>
                        <span className={styles.label}>문항 타입</span>
                        <select
                          className={styles.select}
                          value={q.type}
                          onChange={e => updateQuestion(index, p => ({
                            ...p,
                            type: e.target.value as SurveyQuestionType,
                            options: e.target.value === 'TEXT' ? [] : (p.options.length > 0 ? p.options : ['']),
                          }))}
                        >
                          {QUESTION_TYPE_OPTIONS.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                        </select>
                      </label>
                      <label className={styles.field}>
                        <span className={styles.label}>필수 여부</span>
                        <select className={styles.select} value={q.required ? 'Y' : 'N'} onChange={e => updateQuestion(index, p => ({ ...p, required: e.target.value === 'Y' }))}>
                          <option value="Y">필수</option>
                          <option value="N">선택</option>
                        </select>
                      </label>
                    </div>
                    {q.type !== 'TEXT' ? (
                      <div className={styles.optionEditor}>
                        <span className={styles.label}>선택지</span>
                        {q.options.map((opt, optIdx) => (
                          <div key={optIdx} className={styles.optionRow}>
                            <input className={styles.input} value={opt} onChange={e => updateQuestion(index, p => ({ ...p, options: p.options.map((v, i) => i === optIdx ? e.target.value : v) }))} />
                            <button type="button" className={styles.removeButton} onClick={() => updateQuestion(index, p => ({ ...p, options: p.options.filter((_, i) => i !== optIdx) }))}>제거</button>
                          </div>
                        ))}
                        <button type="button" className={styles.addOptionButton} onClick={() => updateQuestion(index, p => ({ ...p, options: [...p.options, ''] }))}>
                          선택지 추가
                        </button>
                      </div>
                    ) : null}
                  </article>
                ))}
              </div>
              {message ? <p className={styles.message}>{message}</p> : null}
            </section>

            <div className={styles.actionBar}>
              <button type="button" className={styles.secondaryButton} onClick={() => navigate('/admin/surveys')}>
                목록으로
              </button>
              <button type="submit" className={styles.submitButton} disabled={saving || loadingDetail || invalidSurveyId}>
                {saving ? '저장 중...' : isEditing ? '설문지 저장' : '설문지 등록'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </AdminMarketingShell>
  );
}

export default AdminSurveysPage;
