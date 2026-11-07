import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { ISSUE_CATEGORY_LABELS, type IssueCategoryKey } from '../../../shared/data/mockIssues';
import {
  createAdminIssueArticle,
  fetchAdminIssueArticle,
  fetchAdminIssueVolume,
  updateAdminIssueArticle,
  uploadAdminIssueImage,
  type AdminIssueArticle,
  type AdminIssueVolume,
  type IssueAdminArticleInput,
  type IssueAdminSectionInput,
} from '../../../shared/data/issueAdminApi';
import styles from './AdminIssueArticleEditorPage.module.css';

interface SectionFormState {
  readonly id: string;
  readonly heading: string;
  readonly body: string;
  readonly note: string;
}

interface EditorFormState {
  readonly title: string;
  readonly kicker: string;
  readonly summary: string;
  readonly category: IssueCategoryKey;
  readonly formatLabel: string;
  readonly heroImageUrl: string;
  readonly cardImageUrl: string;
  readonly galleryImageUrls: string;
  readonly tags: string;
  readonly authorName: string;
  readonly authorEmail: string;
  readonly creditLine: string;
  readonly highlightQuote: string;
  readonly sections: readonly SectionFormState[];
}

type SingleImageFieldKey = 'heroImageUrl' | 'cardImageUrl';

const CATEGORY_OPTIONS: readonly IssueCategoryKey[] = [
  'exhibition',
  'performance',
  'forum',
  'event',
  'article',
  'video',
];

const ACCEPTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);
const MAX_IMAGE_SIZE_BYTES = 8 * 1024 * 1024;

const createEmptySection = (index: number): SectionFormState => ({
  id: `new-section-${Date.now()}-${index}`,
  heading: '',
  body: '',
  note: '',
});

const createEmptyForm = (): EditorFormState => ({
  title: '',
  kicker: '',
  summary: '',
  category: 'article',
  formatLabel: '아티클',
  heroImageUrl: '',
  cardImageUrl: '',
  galleryImageUrls: '',
  tags: '',
  authorName: '관리자',
  authorEmail: 'admin@idolglow.local',
  creditLine: 'Photo Idol Glow Archive',
  highlightQuote: '',
  sections: [createEmptySection(1)],
});

const createInitialForm = (article?: AdminIssueArticle): EditorFormState => {
  if (!article) {
    return createEmptyForm();
  }

  return {
    title: article.title,
    kicker: article.kicker,
    summary: article.summary,
    category: article.category,
    formatLabel: article.formatLabel,
    heroImageUrl: article.heroImageUrl,
    cardImageUrl: article.cardImageUrl,
    galleryImageUrls: article.galleryImageUrls.join('\n'),
    tags: article.tags.join(', '),
    authorName: article.authorName,
    authorEmail: article.authorEmail,
    creditLine: article.creditLine,
    highlightQuote: article.highlightQuote ?? '',
    sections:
      article.sections.length > 0
        ? article.sections.map(section => ({
            id: String(section.id),
            heading: section.heading ?? '',
            body: section.paragraphs.join('\n\n'),
            note: section.note ?? '',
          }))
        : [createEmptySection(1)],
  };
};

const splitGalleryLines = (value: string): string[] =>
  Array.from(
    new Set(
      value
        .split(/\n+/)
        .map(item => item.trim())
        .filter(Boolean)
    )
  );

const splitTagValues = (value: string): string[] =>
  Array.from(
    new Set(
      value
        .split(/[,\n]/)
        .map(item => item.replace(/^#/, '').trim())
        .filter(Boolean)
    )
  );

const formatBytes = (bytes: number): string => {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  }

  return `${Math.max(1, Math.round(bytes / 1024))}KB`;
};

const validateImageFile = (file: File): string | null => {
  if (!ACCEPTED_IMAGE_TYPES.has(file.type)) {
    return 'JPG, PNG, WebP, GIF 이미지만 업로드할 수 있습니다.';
  }

  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return '이미지 파일은 8MB 이하로 업로드해 주세요.';
  }

  return null;
};

export const AdminIssueArticleEditorPage = () => {
  const navigate = useNavigate();
  const { issueSlug = '', articleSlug = '' } = useParams();
  const { accessToken, authReady } = useAuth();
  const isEditMode = Boolean(articleSlug);

  const [issue, setIssue] = useState<AdminIssueVolume | null>(null);
  const [article, setArticle] = useState<AdminIssueArticle | null>(null);
  const [form, setForm] = useState<EditorFormState>(createEmptyForm());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploadMessage, setUploadMessage] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [generalError, setGeneralError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    if (!accessToken) {
      setGeneralError('관리자 API를 사용하려면 로그인해야 합니다.');
      setIssue(null);
      setArticle(null);
      return;
    }

    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setGeneralError(null);

      try {
        const [issueResponse, articleResponse] = await Promise.all([
          fetchAdminIssueVolume(accessToken, issueSlug),
          isEditMode ? fetchAdminIssueArticle(accessToken, issueSlug, articleSlug) : Promise.resolve(null),
        ]);

        if (cancelled) {
          return;
        }

        setIssue(issueResponse);
        setArticle(articleResponse);
        setForm(createInitialForm(articleResponse ?? undefined));
        setUploadMessage(null);
        setUploadError(null);
      } catch (fetchError) {
        if (!cancelled) {
          setGeneralError(fetchError instanceof Error ? fetchError.message : '편집 데이터를 불러오지 못했습니다.');
          setIssue(null);
          setArticle(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void run();

    return () => {
      cancelled = true;
    };
  }, [accessToken, articleSlug, authReady, isEditMode, issueSlug]);

  const galleryPreviewUrls = useMemo(() => splitGalleryLines(form.galleryImageUrls), [form.galleryImageUrls]);

  const updateSection = (sectionId: string, patch: Partial<SectionFormState>) => {
    setForm(current => ({
      ...current,
      sections: current.sections.map(section =>
        section.id === sectionId ? { ...section, ...patch } : section
      ),
    }));
  };

  const addSection = () => {
    setForm(current => ({
      ...current,
      sections: [...current.sections, createEmptySection(current.sections.length + 1)],
    }));
  };

  const removeSection = (sectionId: string) => {
    setForm(current => {
      if (current.sections.length === 1) {
        return current;
      }

      return {
        ...current,
        sections: current.sections.filter(section => section.id !== sectionId),
      };
    });
  };

  const handleChange = <K extends keyof EditorFormState>(key: K, value: EditorFormState[K]) => {
    setForm(current => ({ ...current, [key]: value }));
  };

  const uploadFolderBase = useMemo(() => {
    const articleSegment = isEditMode ? articleSlug : 'draft';
    return `issues/${issueSlug}/articles/${articleSegment}`;
  }, [articleSlug, isEditMode, issueSlug]);

  const handleSingleUpload =
    (field: SingleImageFieldKey, label: string, folderSuffix: string) =>
    async (event: ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      event.target.value = '';

      if (!file) {
        return;
      }

      const validationError = validateImageFile(file);

      if (validationError) {
        setUploadError(validationError);
        setUploadMessage(null);
        return;
      }

      if (!accessToken) {
        setUploadError('업로드 전에 관리자 로그인이 필요합니다.');
        return;
      }

      setUploadError(null);
      setUploadMessage(`${label} 업로드 중...`);

      try {
        const upload = await uploadAdminIssueImage(accessToken, file, `${uploadFolderBase}/${folderSuffix}`);
        handleChange(field, upload.url);
        setUploadMessage(`${label} ${file.name} (${formatBytes(file.size)}) 업로드를 완료했습니다.`);
      } catch (uploadFailure) {
        setUploadError(uploadFailure instanceof Error ? uploadFailure.message : `${label} 업로드에 실패했습니다.`);
        setUploadMessage(null);
      }
    };

  const handleGalleryUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    event.target.value = '';

    if (files.length === 0) {
      return;
    }

    if (!accessToken) {
      setUploadError('업로드 전에 관리자 로그인이 필요합니다.');
      return;
    }

    for (const file of files) {
      const validationError = validateImageFile(file);

      if (validationError) {
        setUploadError(validationError);
        setUploadMessage(null);
        return;
      }
    }

    setUploadError(null);
    setUploadMessage(`${files.length}개 이미지를 업로드 중입니다...`);

    try {
      const uploads = await Promise.all(
        files.map(file => uploadAdminIssueImage(accessToken, file, `${uploadFolderBase}/gallery`))
      );
      const nextGalleryUrls = Array.from(
        new Set([...galleryPreviewUrls, ...uploads.map(upload => upload.url)])
      );

      handleChange('galleryImageUrls', nextGalleryUrls.join('\n'));
      setUploadMessage(`${files.length}개 이미지를 슬라이드 목록에 추가했습니다.`);
    } catch (uploadFailure) {
      setUploadError(uploadFailure instanceof Error ? uploadFailure.message : '슬라이드 이미지 업로드에 실패했습니다.');
      setUploadMessage(null);
    }
  };

  const removeGalleryImage = (targetUrl: string) => {
    const nextGalleryUrls = galleryPreviewUrls.filter(url => url !== targetUrl);
    handleChange('galleryImageUrls', nextGalleryUrls.join('\n'));
  };

  const clearSingleImage = (field: SingleImageFieldKey) => {
    handleChange(field, '');
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken || !issue) {
      setGeneralError('기사 저장 전에 관리자 로그인이 필요합니다.');
      return;
    }

    const payload: IssueAdminArticleInput = {
      title: form.title,
      kicker: form.kicker,
      summary: form.summary,
      category: form.category,
      formatLabel: form.formatLabel,
      heroImageUrl: form.heroImageUrl,
      cardImageUrl: form.cardImageUrl,
      galleryImageUrls: splitGalleryLines(form.galleryImageUrls),
      tags: splitTagValues(form.tags),
      authorName: form.authorName,
      authorEmail: form.authorEmail,
      creditLine: form.creditLine,
      highlightQuote: form.highlightQuote,
      sections: form.sections.map<IssueAdminSectionInput>(section => ({
        heading: section.heading,
        body: section.body,
        note: section.note,
      })),
    };

    setSaving(true);
    setGeneralError(null);

    try {
      const savedArticle = isEditMode
        ? await updateAdminIssueArticle(accessToken, issue.slug, articleSlug, payload)
        : await createAdminIssueArticle(accessToken, issue.slug, payload);

      navigate(`/admin/issues/${issue.slug}/articles/${savedArticle.slug}`);
    } catch (submitError) {
      setGeneralError(submitError instanceof Error ? submitError.message : '기사 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <main className={styles.page} id="main-content">
        <div className={styles.container}>
          <Link to="/admin/issues" className={styles.backLink}>
            목록으로 돌아가기
          </Link>
          <div className={styles.empty}>편집 화면을 준비하는 중입니다.</div>
        </div>
      </main>
    );
  }

  if (!issue || (isEditMode && !article)) {
    return (
      <main className={styles.page} id="main-content">
        <div className={styles.container}>
          <Link to="/admin/issues" className={styles.backLink}>
            목록으로 돌아가기
          </Link>
          <div className={styles.empty}>{generalError ?? '편집할 기사 정보를 찾을 수 없습니다.'}</div>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page} id="main-content">
      <div className={styles.container}>
        <div className={styles.topRow}>
          <Link to={`/admin/issues/${issue.slug}`} className={styles.backLink}>
            Vol.{issue.volume} 트랜드 기사 목록으로 돌아가기
          </Link>
          <span className={styles.modeChip}>{isEditMode ? '기사 수정' : '기사 등록'}</span>
        </div>

        <header className={styles.header}>
          <h1 className={styles.title}>{isEditMode ? '웹진 기사 수정' : `Vol.${issue.volume} 트랜드 기사 등록`}</h1>
          <p className={styles.subtitle}>
            카드, 상세 상단, 본문, 태그, 작성자 정보까지 한 번에 수정할 수 있습니다.
          </p>
        </header>

        <form className={styles.form} onSubmit={handleSubmit}>
          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2 className={styles.panelTitle}>기본 정보</h2>
            </div>

            <div className={styles.gridTwo}>
              <label className={styles.field}>
                <span className={styles.label}>제목</span>
                <input
                  className={styles.input}
                  value={form.title}
                  onChange={event => handleChange('title', event.target.value)}
                  placeholder="기사 제목을 입력해 주세요."
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>부제</span>
                <input
                  className={styles.input}
                  value={form.kicker}
                  onChange={event => handleChange('kicker', event.target.value)}
                  placeholder="예: 전시 리뷰 / 기획 인터뷰"
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>카테고리</span>
                <select
                  className={styles.select}
                  value={form.category}
                  onChange={event => handleChange('category', event.target.value as IssueCategoryKey)}
                >
                  {CATEGORY_OPTIONS.map(category => (
                    <option key={category} value={category}>
                      {ISSUE_CATEGORY_LABELS[category]}
                    </option>
                  ))}
                </select>
              </label>

              <label className={styles.field}>
                <span className={styles.label}>형식 라벨</span>
                <input
                  className={styles.input}
                  value={form.formatLabel}
                  onChange={event => handleChange('formatLabel', event.target.value)}
                  placeholder="예: 아티클 / 비디오"
                />
              </label>
            </div>

            <label className={styles.field}>
              <span className={styles.label}>요약</span>
              <textarea
                className={styles.textarea}
                rows={4}
                value={form.summary}
                onChange={event => handleChange('summary', event.target.value)}
                placeholder="상세 상단에 노출할 요약 문장을 입력해 주세요."
              />
            </label>
          </section>

          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2 className={styles.panelTitle}>이미지와 태그</h2>
            </div>

            <div className={styles.gridTwo}>
              <div className={styles.field}>
                <span className={styles.label}>대표 이미지 URL</span>
                <div className={styles.uploadInputRow}>
                  <input
                    className={styles.input}
                    value={form.heroImageUrl}
                    onChange={event => handleChange('heroImageUrl', event.target.value)}
                    placeholder="상세 상단 슬라이드 대표 이미지"
                  />
                  <label className={styles.uploadButton}>
                    파일 선택
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp,image/gif"
                      className={styles.hiddenInput}
                      onChange={event => {
                        void handleSingleUpload('heroImageUrl', '대표 이미지', 'hero')(event);
                      }}
                    />
                  </label>
                  {form.heroImageUrl ? (
                    <button
                      type="button"
                      className={styles.clearButton}
                      onClick={() => clearSingleImage('heroImageUrl')}
                    >
                      지우기
                    </button>
                  ) : null}
                </div>
                {form.heroImageUrl ? (
                  <div className={styles.previewCard}>
                    <img src={form.heroImageUrl} alt="대표 이미지 미리보기" className={styles.previewImage} />
                  </div>
                ) : null}
              </div>

              <div className={styles.field}>
                <span className={styles.label}>카드 이미지 URL</span>
                <div className={styles.uploadInputRow}>
                  <input
                    className={styles.input}
                    value={form.cardImageUrl}
                    onChange={event => handleChange('cardImageUrl', event.target.value)}
                    placeholder="카드 목록용 이미지"
                  />
                  <label className={styles.uploadButton}>
                    파일 선택
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp,image/gif"
                      className={styles.hiddenInput}
                      onChange={event => {
                        void handleSingleUpload('cardImageUrl', '카드 이미지', 'card')(event);
                      }}
                    />
                  </label>
                  {form.cardImageUrl ? (
                    <button
                      type="button"
                      className={styles.clearButton}
                      onClick={() => clearSingleImage('cardImageUrl')}
                    >
                      지우기
                    </button>
                  ) : null}
                </div>
                {form.cardImageUrl ? (
                  <div className={styles.previewCard}>
                    <img src={form.cardImageUrl} alt="카드 이미지 미리보기" className={styles.previewImage} />
                  </div>
                ) : null}
              </div>
            </div>

            {uploadMessage ? <p className={styles.uploadMessage}>{uploadMessage}</p> : null}
            {uploadError ? <p className={styles.uploadError}>{uploadError}</p> : null}
            {generalError ? <p className={styles.uploadError}>{generalError}</p> : null}

            <label className={styles.field}>
              <span className={styles.label}>슬라이드 이미지 목록</span>
              <textarea
                className={styles.textarea}
                rows={5}
                value={form.galleryImageUrls}
                onChange={event => handleChange('galleryImageUrls', event.target.value)}
                placeholder="이미지 URL을 줄바꿈으로 여러 개 입력하거나 아래에서 파일을 올려 주세요."
              />
            </label>

            <div className={styles.galleryToolbar}>
              <label className={styles.uploadButton}>
                슬라이드 이미지 추가
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  multiple
                  className={styles.hiddenInput}
                  onChange={event => {
                    void handleGalleryUpload(event);
                  }}
                />
              </label>
              {galleryPreviewUrls.length > 0 ? (
                <span className={styles.galleryCount}>총 {galleryPreviewUrls.length}장</span>
              ) : null}
            </div>

            {galleryPreviewUrls.length > 0 ? (
              <div className={styles.previewGrid}>
                {galleryPreviewUrls.map((imageUrl, index) => (
                  <article key={`${imageUrl}-${index}`} className={styles.previewGridCard}>
                    <img
                      src={imageUrl}
                      alt={`슬라이드 이미지 ${index + 1}`}
                      className={styles.previewGridImage}
                    />
                    <button
                      type="button"
                      className={styles.previewRemoveButton}
                      onClick={() => removeGalleryImage(imageUrl)}
                    >
                      제거
                    </button>
                  </article>
                ))}
              </div>
            ) : null}

            <label className={styles.field}>
              <span className={styles.label}>태그</span>
              <input
                className={styles.input}
                value={form.tags}
                onChange={event => handleChange('tags', event.target.value)}
                placeholder="쉼표로 구분해 입력해 주세요."
              />
            </label>
          </section>

          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2 className={styles.panelTitle}>본문 섹션</h2>
              <button type="button" className={styles.addButton} onClick={addSection}>
                섹션 추가
              </button>
            </div>

            <div className={styles.sectionStack}>
              {form.sections.map((section, index) => (
                <article key={section.id} className={styles.sectionCard}>
                  <div className={styles.sectionCardHeader}>
                    <h3 className={styles.sectionCardTitle}>섹션 {index + 1}</h3>
                    <button
                      type="button"
                      className={styles.removeButton}
                      onClick={() => removeSection(section.id)}
                      disabled={form.sections.length === 1}
                    >
                      삭제
                    </button>
                  </div>

                  <div className={styles.gridTwo}>
                    <label className={styles.field}>
                      <span className={styles.label}>섹션 제목</span>
                      <input
                        className={styles.input}
                        value={section.heading}
                        onChange={event => updateSection(section.id, { heading: event.target.value })}
                        placeholder="예: 전시 기획 의도"
                      />
                    </label>

                    <label className={styles.field}>
                      <span className={styles.label}>주석</span>
                      <input
                        className={styles.input}
                        value={section.note}
                        onChange={event => updateSection(section.id, { note: event.target.value })}
                        placeholder="없으면 비워 두세요."
                      />
                    </label>
                  </div>

                  <label className={styles.field}>
                    <span className={styles.label}>본문</span>
                    <textarea
                      className={styles.textarea}
                      rows={8}
                      value={section.body}
                      onChange={event => updateSection(section.id, { body: event.target.value })}
                      placeholder="문단 사이는 빈 줄로 구분해 주세요."
                    />
                  </label>
                </article>
              ))}
            </div>
          </section>

          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2 className={styles.panelTitle}>작성자 정보</h2>
            </div>

            <div className={styles.gridTwo}>
              <label className={styles.field}>
                <span className={styles.label}>작성자명</span>
                <input
                  className={styles.input}
                  value={form.authorName}
                  onChange={event => handleChange('authorName', event.target.value)}
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>이메일</span>
                <input
                  className={styles.input}
                  value={form.authorEmail}
                  onChange={event => handleChange('authorEmail', event.target.value)}
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>크레딧</span>
                <input
                  className={styles.input}
                  value={form.creditLine}
                  onChange={event => handleChange('creditLine', event.target.value)}
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>강조 문구</span>
                <input
                  className={styles.input}
                  value={form.highlightQuote}
                  onChange={event => handleChange('highlightQuote', event.target.value)}
                  placeholder="없으면 비워 두세요."
                />
              </label>
            </div>
          </section>

          <div className={styles.actionBar}>
            <Link
              to={
                isEditMode
                  ? `/admin/issues/${issue.slug}/articles/${articleSlug}`
                  : `/admin/issues/${issue.slug}`
              }
              className={styles.cancelButton}
            >
              취소
            </Link>
            <button type="submit" className={styles.submitButton} disabled={saving}>
              {saving ? '저장 중...' : isEditMode ? '수정 저장' : '기사 등록'}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
};

export default AdminIssueArticleEditorPage;
