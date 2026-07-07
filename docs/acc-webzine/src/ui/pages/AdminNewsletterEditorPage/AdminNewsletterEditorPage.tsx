import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  createAdminNewsletter,
  fetchAdminNewsletter,
  updateAdminNewsletter,
  uploadAdminNewsletterImage,
  type AdminNewsletterDetail,
  type NewsletterAdminInput,
} from '../../../shared/data/newsletterAdminApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminNewsletterEditorPage.module.css';

interface NewsletterEditorFormState {
  readonly title: string;
  readonly categoryLabel: string;
  readonly publishedAt: string;
  readonly imageUrl: string;
  readonly tags: string;
  readonly summary: string;
  readonly body: string;
}

const ACCEPTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);
const MAX_IMAGE_SIZE_BYTES = 8 * 1024 * 1024;

const createEmptyForm = (): NewsletterEditorFormState => ({
  title: '',
  categoryLabel: 'Idol Glow 소식',
  publishedAt: new Date().toISOString().slice(0, 10).replace(/-/g, '.'),
  imageUrl: '',
  tags: '',
  summary: '',
  body: '',
});

const createInitialForm = (newsletter?: AdminNewsletterDetail | null): NewsletterEditorFormState => {
  if (!newsletter) {
    return createEmptyForm();
  }

  return {
    title: newsletter.title,
    categoryLabel: newsletter.categoryLabel,
    publishedAt: newsletter.publishedAt,
    imageUrl: newsletter.imageUrl,
    tags: newsletter.tags.join(', '),
    summary: newsletter.summary,
    body: newsletter.paragraphs.join('\n\n'),
  };
};

const splitTagValues = (value: string): string[] =>
  Array.from(
    new Set(
      value
        .split(/[,\n]/)
        .map(item => item.replace(/^#/, '').trim())
        .filter(Boolean)
    )
  );

const splitParagraphs = (value: string): string[] =>
  value
    .split(/\n{2,}/)
    .map(item => item.trim())
    .filter(Boolean);

const formatBytes = (bytes: number): string => {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  }
  return `${Math.max(1, Math.round(bytes / 1024))}KB`;
};

const validateImageFile = (file: File): string | null => {
  if (!ACCEPTED_IMAGE_TYPES.has(file.type)) {
    return 'JPG, PNG, WebP, GIF 형식만 업로드할 수 있습니다.';
  }

  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return '이미지 파일은 8MB 이하로 업로드해 주세요.';
  }

  return null;
};

export const AdminNewsletterEditorPage = () => {
  const navigate = useNavigate();
  const { newsletterSlug = '' } = useParams();
  const { accessToken, authReady, user } = useAuth();
  const isEditMode = Boolean(newsletterSlug);

  const [newsletter, setNewsletter] = useState<AdminNewsletterDetail | null>(null);
  const [form, setForm] = useState<NewsletterEditorFormState>(createEmptyForm());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [uploadMessage, setUploadMessage] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    if (!accessToken || user?.role !== 'ADMIN') {
      setGeneralError('관리자 API를 사용하려면 로그인해 주세요.');
      setNewsletter(null);
      return;
    }

    if (!isEditMode) {
      setNewsletter(null);
      setForm(createEmptyForm());
      setGeneralError(null);
      return;
    }

    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setGeneralError(null);

      try {
        const detail = await fetchAdminNewsletter(accessToken, newsletterSlug);
        if (!cancelled) {
          setNewsletter(detail);
          setForm(createInitialForm(detail));
        }
      } catch (fetchError) {
        if (!cancelled) {
          setGeneralError(
            fetchError instanceof Error ? fetchError.message : '뉴스레터 정보를 불러오지 못했습니다.'
          );
          setNewsletter(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, isEditMode, newsletterSlug, user?.role]);

  const imagePreviewUrl = useMemo(() => form.imageUrl.trim(), [form.imageUrl]);
  const tagCount = useMemo(() => splitTagValues(form.tags).length, [form.tags]);
  const paragraphCount = useMemo(() => splitParagraphs(form.body).length, [form.body]);

  const handleChange = <K extends keyof NewsletterEditorFormState>(
    key: K,
    value: NewsletterEditorFormState[K]
  ) => {
    setForm(current => ({ ...current, [key]: value }));
  };

  const handleImageUpload = async (event: ChangeEvent<HTMLInputElement>) => {
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
      setUploadError('이미지를 업로드하려면 관리자 로그인이 필요합니다.');
      return;
    }

    setUploadError(null);
    setUploadMessage(`대표 이미지 ${file.name} 업로드 중입니다...`);

    try {
      const upload = await uploadAdminNewsletterImage(
        accessToken,
        file,
        `newsletters/${isEditMode ? newsletterSlug : 'draft'}`
      );
      handleChange('imageUrl', upload.url);
      setUploadMessage(`대표 이미지 ${file.name} (${formatBytes(file.size)}) 업로드를 완료했습니다.`);
    } catch (uploadFailure) {
      setUploadError(uploadFailure instanceof Error ? uploadFailure.message : '이미지 업로드에 실패했습니다.');
      setUploadMessage(null);
    }
  };

  const clearImage = () => {
    handleChange('imageUrl', '');
    setUploadMessage(null);
    setUploadError(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken) {
      setGeneralError('관리자 API를 사용하려면 로그인해 주세요.');
      return;
    }

    const payload: NewsletterAdminInput = {
      title: form.title,
      categoryLabel: form.categoryLabel,
      publishedAt: form.publishedAt,
      imageUrl: form.imageUrl,
      tags: splitTagValues(form.tags),
      summary: form.summary,
      paragraphs: splitParagraphs(form.body),
    };

    setSaving(true);
    setGeneralError(null);

    try {
      const saved = isEditMode
        ? await updateAdminNewsletter(accessToken, newsletterSlug, payload)
        : await createAdminNewsletter(accessToken, payload);

      navigate(`/admin/newsletters/${saved.slug}`);
    } catch (submitError) {
      setGeneralError(submitError instanceof Error ? submitError.message : '뉴스레터 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (!accessToken || user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>관리자만 뉴스레터를 등록하고 수정할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/newsletters"
      title={isEditMode ? '뉴스레터 수정' : '뉴스레터 등록'}
      description="제목, 대표 이미지, 요약, 본문을 입력해 뉴스레터를 관리합니다."
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        subtitle: shellToolbar.subtitle,
        statusText: shellToolbar.statusText,
        stats: shellToolbar.stats,
        statCard: shellToolbar.statCard,
        statLabel: shellToolbar.statLabel,
        statValue: shellToolbar.statValue,
      }}
      statusText={
        loading
          ? '뉴스레터 편집 화면을 불러오는 중입니다.'
          : isEditMode && !newsletter
            ? generalError ?? '선택한 뉴스레터 정보를 찾을 수 없습니다.'
            : generalError
              ? generalError
              : uploadError
                ? uploadError
                : uploadMessage
                  ? uploadMessage
                  : saving
                    ? '뉴스레터를 저장하는 중입니다.'
                    : '뉴스레터 편집 화면입니다.'
      }
      stats={[
        { label: '편집 제목', value: form.title.trim() || '-' },
        { label: '태그 수', value: tagCount },
        { label: '문단 수', value: paragraphCount },
      ]}
    >
      <div className={styles.container}>
        <div className={styles.topRow}>
          <Link to="/admin/newsletters" className={styles.backLink}>
            뉴스레터 목록으로 돌아가기
          </Link>
          <span className={styles.modeChip}>{isEditMode ? '뉴스레터 수정' : '뉴스레터 등록'}</span>
        </div>

        <header className={styles.header}>
          <h1 className={styles.title}>{isEditMode ? 'Idol Glow 뉴스레터 수정' : 'Idol Glow 뉴스레터 등록'}</h1>
          <p className={styles.subtitle}>
            제목, 대표 이미지, 요약, 태그, 본문을 입력하고 관리자 화면에서 바로 검토할 수 있습니다.
          </p>
        </header>

        {isEditMode && !newsletter && !loading ? (
          <div className={styles.empty}>{generalError ?? '선택한 뉴스레터 정보를 찾을 수 없습니다.'}</div>
        ) : (
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
                    placeholder="뉴스레터 제목을 입력해 주세요."
                  />
                </label>

                <label className={styles.field}>
                  <span className={styles.label}>카테고리명</span>
                  <input
                    className={styles.input}
                    value={form.categoryLabel}
                    onChange={event => handleChange('categoryLabel', event.target.value)}
                    placeholder="예: Idol Glow 소식"
                  />
                </label>

                <label className={styles.field}>
                  <span className={styles.label}>게시일</span>
                  <input
                    className={styles.input}
                    value={form.publishedAt}
                    onChange={event => handleChange('publishedAt', event.target.value)}
                    placeholder="예: 2026.04.06"
                  />
                </label>

                <label className={styles.field}>
                  <span className={styles.label}>태그</span>
                  <input
                    className={styles.input}
                    value={form.tags}
                    onChange={event => handleChange('tags', event.target.value)}
                    placeholder="쉼표로 구분해 입력해 주세요."
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
                  placeholder="목록 화면에 노출할 요약 문장을 입력해 주세요."
                />
              </label>
            </section>

            <section className={styles.panel}>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>대표 이미지</h2>
              </div>

              <label className={styles.field}>
                <span className={styles.label}>대표 이미지 URL</span>
                <div className={styles.uploadInputRow}>
                  <input
                    className={styles.input}
                    value={form.imageUrl}
                    onChange={event => handleChange('imageUrl', event.target.value)}
                    placeholder="목록과 상세에 공통으로 사용할 대표 이미지"
                  />
                  <label className={styles.uploadButton}>
                    파일 선택
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp,image/gif"
                      className={styles.hiddenInput}
                      onChange={event => {
                        void handleImageUpload(event);
                      }}
                    />
                  </label>
                  {form.imageUrl ? (
                    <button type="button" className={styles.clearButton} onClick={clearImage}>
                      지우기
                    </button>
                  ) : null}
                </div>
              </label>

              {uploadMessage ? <p className={styles.uploadMessage}>{uploadMessage}</p> : null}
              {uploadError ? <p className={styles.uploadError}>{uploadError}</p> : null}
              {generalError ? <p className={styles.uploadError}>{generalError}</p> : null}

              {imagePreviewUrl ? (
                <div className={styles.previewCard}>
                  <img src={imagePreviewUrl} alt="대표 이미지 미리보기" className={styles.previewImage} />
                </div>
              ) : null}
            </section>

            <section className={styles.panel}>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>본문</h2>
              </div>

              <label className={styles.field}>
                <span className={styles.label}>본문 내용</span>
                <textarea
                  className={styles.textarea}
                  rows={14}
                  value={form.body}
                  onChange={event => handleChange('body', event.target.value)}
                  placeholder="문단 사이는 빈 줄로 구분해 주세요."
                />
              </label>
            </section>

            <div className={styles.actionBar}>
              <Link
                to={isEditMode ? `/admin/newsletters/${newsletterSlug}` : '/admin/newsletters'}
                className={styles.cancelButton}
              >
                취소
              </Link>
              <button type="submit" className={styles.submitButton} disabled={saving}>
                {saving ? '저장 중...' : isEditMode ? '수정 저장' : '뉴스레터 등록'}
              </button>
            </div>
          </form>
        )}
      </div>
    </AdminMarketingShell>
  );
};

export default AdminNewsletterEditorPage;
