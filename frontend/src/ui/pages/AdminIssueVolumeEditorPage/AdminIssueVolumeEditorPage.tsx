import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  createAdminIssue,
  deleteAdminIssue,
  fetchAdminIssuePage,
  fetchAdminIssueVolume,
  updateAdminIssue,
  uploadAdminIssueImage,
  type AdminIssueSummary,
  type IssueAdminIssueInput,
} from '../../../shared/data/issueAdminApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import styles from './AdminIssueVolumeEditorPage.module.css';

const ACCEPTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);
const MAX_IMAGE_SIZE_BYTES = 8 * 1024 * 1024;

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
    return '표지 이미지는 8MB 이하로 업로드해 주세요.';
  }

  return null;
};

export const AdminIssueVolumeEditorPage = () => {
  const navigate = useNavigate();
  const { issueSlug = '' } = useParams();
  const isEditMode = issueSlug.length > 0;
  const { accessToken, authReady, user } = useAuth();

  const [volume, setVolume] = useState('');
  const [issueDate, setIssueDate] = useState('');
  const [coverImageUrl, setCoverImageUrl] = useState('');
  const [teaser, setTeaser] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [existingIssues, setExistingIssues] = useState<readonly AdminIssueSummary[]>([]);

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') {
      return;
    }

    let cancelled = false;

    const run = async () => {
      try {
        const [issueListResponse, issueResponse] = await Promise.all([
          fetchAdminIssuePage(accessToken, { page: 1, size: 8 }),
          isEditMode ? fetchAdminIssueVolume(accessToken, issueSlug) : Promise.resolve(null),
        ]);

        if (cancelled) {
          return;
        }

        setExistingIssues(issueListResponse.issues);

        if (issueResponse) {
          setVolume(String(issueResponse.volume));
          setIssueDate(issueResponse.issueDate);
          setCoverImageUrl(issueResponse.coverImageUrl);
          setTeaser(issueResponse.teaser);
        }
      } catch (fetchError) {
        if (!cancelled) {
          setError(fetchError instanceof Error ? fetchError.message : '트랜드 정보를 불러오지 못했습니다.');
        }
      }
    };

    void run();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, isEditMode, issueSlug, user?.role]);

  const slugPreview = useMemo(() => {
    const parsedVolume = Number(volume);
    return Number.isFinite(parsedVolume) && parsedVolume > 0 ? `vol-${parsedVolume}` : 'vol-number';
  }, [volume]);

  const latestVolume = useMemo(
    () => existingIssues.reduce((max, issue) => Math.max(max, issue.volume), 0),
    [existingIssues]
  );

  const handleCoverUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';

    if (!file) {
      return;
    }

    const validationError = validateImageFile(file);

    if (validationError) {
      setError(validationError);
      setMessage(null);
      return;
    }

    if (!accessToken) {
      setError('업로드 전에는 관리자 로그인이 필요합니다.');
      return;
    }

    setUploading(true);
    setError(null);
    setMessage(null);

    try {
      const upload = await uploadAdminIssueImage(accessToken, file, `issues/${slugPreview}/cover`);
      setCoverImageUrl(upload.url);
      setMessage(`표지 이미지 ${file.name} (${formatBytes(file.size)}) 업로드를 완료했습니다.`);
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : '표지 이미지 업로드에 실패했습니다.');
      setMessage(null);
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const parsedVolume = Number(volume);

    if (!accessToken) {
      setError('관리자 API를 사용하려면 로그인해 주세요.');
      return;
    }

    if (!Number.isFinite(parsedVolume) || parsedVolume <= 0) {
      setError('발행 트랜드 번호를 올바르게 입력해 주세요.');
      return;
    }

    if (!issueDate.trim()) {
      setError('발행일을 입력해 주세요. 예: 2026.03.');
      return;
    }

    if (!coverImageUrl.trim()) {
      setError('표지 이미지를 등록해 주세요.');
      return;
    }

    const payload: IssueAdminIssueInput = {
      volume: parsedVolume,
      issueDate: issueDate.trim(),
      coverImageUrl: coverImageUrl.trim(),
      teaser: teaser.trim(),
    };

    setSaving(true);
    setError(null);

    try {
      const savedIssue = isEditMode
        ? await updateAdminIssue(accessToken, issueSlug, payload)
        : await createAdminIssue(accessToken, payload);

      navigate(`/admin/issues/${savedIssue.slug}`);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : '트랜드 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!isEditMode || !accessToken) {
      return;
    }

    const confirmed = window.confirm(
      `Vol.${volume || '?'}을 삭제할까요?\n해당 트랜드의 기사도 함께 삭제됩니다.`
    );

    if (!confirmed) {
      return;
    }

    setSaving(true);
    setError(null);

    try {
      await deleteAdminIssue(accessToken, issueSlug);
      navigate('/admin/issues');
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '트랜드 삭제에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (!authReady) {
    return (
      <main className={styles.page}>
        <div className={styles.container}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (!accessToken || user?.role !== 'ADMIN') {
    return (
      <main className={styles.page}>
        <div className={styles.container}>관리자만 트랜드 보기를 등록하고 수정할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/issues"
      title={isEditMode ? '트랜드 수정' : '트랜드 등록'}
      description="발행 트랜드 기본 정보와 표지 이미지를 관리합니다."
      statusText={
        error
          ? error
          : message
            ? message
            : uploading
              ? '표지 이미지를 업로드하는 중입니다.'
              : saving
                ? '트랜드 정보를 저장하는 중입니다.'
                : '트랜드 보기 편집 화면입니다.'
      }
      stats={[
        { label: '등록된 트랜드', value: existingIssues.length },
        { label: '최신 트랜드', value: latestVolume || '-' },
        { label: '예상 슬러그', value: slugPreview },
      ]}
    >
      <div className={styles.container}>
        <div className={styles.topRow}>
          <Link to={isEditMode ? `/admin/issues/${issueSlug}` : '/admin/issues'} className={styles.backLink}>
            트랜드 보기로 돌아가기
          </Link>
          <span className={styles.modeChip}>{isEditMode ? '트랜드 수정' : '트랜드 등록'}</span>
        </div>

        <header className={styles.header}>
          <h1 className={styles.title}>{isEditMode ? 'Idol Glow 트랜드 수정' : 'Idol Glow 트랜드 등록'}</h1>
          <p className={styles.subtitle}>
            먼저 트랜드 정보를 만들고, 저장한 뒤 해당 트랜드 안에서 기사 내용을 등록하는 흐름입니다.
          </p>
        </header>

        <form className={styles.form} onSubmit={handleSubmit}>
          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2 className={styles.panelTitle}>트랜드 기본 정보</h2>
            </div>

            <div className={styles.gridTwo}>
              <label className={styles.field}>
                <span className={styles.label}>발행 트랜드</span>
                <input
                  className={styles.input}
                  type="number"
                  min="1"
                  step="1"
                  value={volume}
                  onChange={event => setVolume(event.target.value)}
                  placeholder="예: 101"
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>발행일</span>
                <input
                  className={styles.input}
                  value={issueDate}
                  onChange={event => setIssueDate(event.target.value)}
                  placeholder="예: 2026.03."
                />
              </label>
            </div>

            <div className={styles.slugPreviewBox}>
              <span className={styles.slugLabel}>생성 예정 슬러그</span>
              <strong className={styles.slugValue}>{slugPreview}</strong>
            </div>

            <label className={styles.field}>
              <span className={styles.label}>표지 이미지 URL</span>
              <div className={styles.uploadInputRow}>
                <input
                  className={styles.input}
                  value={coverImageUrl}
                  onChange={event => setCoverImageUrl(event.target.value)}
                  placeholder="리스트와 상세 상단에 사용할 표지 이미지"
                />
                <label className={styles.uploadButton}>
                  파일 선택
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    className={styles.hiddenInput}
                    onChange={event => {
                      void handleCoverUpload(event);
                    }}
                  />
                </label>
                {coverImageUrl ? (
                  <button
                    type="button"
                    className={styles.clearButton}
                    onClick={() => setCoverImageUrl('')}
                  >
                    지우기
                  </button>
                ) : null}
              </div>
            </label>

            {message ? <p className={styles.uploadMessage}>{message}</p> : null}
            {error ? <p className={styles.uploadError}>{error}</p> : null}

            {coverImageUrl ? (
              <div className={styles.previewCard}>
                <img src={coverImageUrl} alt="표지 이미지 미리보기" className={styles.previewImage} />
              </div>
            ) : null}

            <label className={styles.field}>
              <span className={styles.label}>소개 문구</span>
              <textarea
                className={styles.textarea}
                rows={5}
                value={teaser}
                onChange={event => setTeaser(event.target.value)}
                placeholder="목록 카드에 들어갈 간단한 소개 문구를 입력해 주세요."
              />
            </label>
          </section>

          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2 className={styles.panelTitle}>현재 등록된 트랜드</h2>
            </div>

            <div className={styles.existingVolumes}>
              {existingIssues.map(existingIssue => (
                <div key={existingIssue.id} className={styles.issueChip}>
                  <strong>Vol.{existingIssue.volume}</strong>
                  <span>{existingIssue.issueDate}</span>
                </div>
              ))}
            </div>
          </section>

          <div className={styles.actionBar}>
            {isEditMode ? (
              <button
                type="button"
                className={styles.deleteButton}
                onClick={() => {
                  void handleDelete();
                }}
                disabled={saving}
              >
                삭제
              </button>
            ) : null}
            <Link to={isEditMode ? `/admin/issues/${issueSlug}` : '/admin/issues'} className={styles.cancelButton}>
              취소
            </Link>
            <button type="submit" className={styles.submitButton} disabled={saving || uploading}>
              {saving ? '저장 중...' : isEditMode ? '트랜드 수정 저장' : '트랜드 등록'}
            </button>
          </div>
        </form>
      </div>
    </AdminMarketingShell>
  );
};

export default AdminIssueVolumeEditorPage;
