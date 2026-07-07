import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  createAdminPopup,
  deleteAdminPopup,
  fetchAdminPopups,
  resolveSiteContentImageUrl,
  updateAdminPopup,
  uploadAdminPopupImage,
  type AdminPopupItem,
  type PopupAdminInput,
} from '../../../shared/data/siteContentAdminApi';
import '../../../../../samples/portal-stat-bar.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminPopupsPage.module.css';

const PAGE_SIZE = 10;
const PAGE_NUMBER_GROUP_SIZE = 5;

interface PopupFormState {
  readonly title: string;
  readonly fileUrl: string;
  readonly linkTarget: string;
  readonly imagePath: string;
  readonly imageFileName: string;
  readonly noticeStartDate: string;
  readonly noticeEndDate: string;
  readonly stopViewYn: string;
  readonly noticeYn: string;
}

const EMPTY_FORM: PopupFormState = {
  title: '',
  fileUrl: '',
  linkTarget: '_blank',
  imagePath: '',
  imageFileName: '',
  noticeStartDate: '',
  noticeEndDate: '',
  stopViewYn: 'Y',
  noticeYn: 'Y',
};

const ArrowIcon = ({ direction }: { readonly direction: 'left' | 'right' }) => (
  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d={direction === 'left' ? 'M15 6L9 12L15 18' : 'M9 6L15 12L9 18'}
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const PopupStatCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>건</span>
  </>
);

const toFormState = (item: AdminPopupItem): PopupFormState => ({
  title: item.title ?? '',
  fileUrl: item.fileUrl ?? '',
  linkTarget: item.linkTarget ?? '_blank',
  imagePath: item.imagePath ?? '',
  imageFileName: item.imageFileName ?? '',
  noticeStartDate: item.noticeStartDate ?? '',
  noticeEndDate: item.noticeEndDate ?? '',
  stopViewYn: item.stopViewYn ?? 'Y',
  noticeYn: item.noticeYn ?? 'Y',
});

const extractStoredFileName = (objectKey: string, fallback: string): string => {
  const trimmed = objectKey.trim();
  if (!trimmed) {
    return fallback;
  }

  const segments = trimmed.split('/');
  return segments[segments.length - 1] || fallback;
};

const compactToDateTimeLocal = (value: string): string => {
  const digits = value.replace(/\D/g, '');
  if (digits.length < 8) {
    return '';
  }

  const normalized = digits.padEnd(12, '0').slice(0, 12);
  const year = normalized.slice(0, 4);
  const month = normalized.slice(4, 6);
  const day = normalized.slice(6, 8);
  const hour = normalized.slice(8, 10);
  const minute = normalized.slice(10, 12);

  return `${year}-${month}-${day}T${hour}:${minute}`;
};

const dateTimeLocalToCompact = (value: string): string => {
  const digits = value.replace(/\D/g, '');
  return digits.length === 12 ? digits : '';
};

const compactToDateNumber = (value: string): number | null => {
  const digits = value.replace(/\D/g, '');
  if (digits.length !== 12) {
    return null;
  }

  const normalized = Number(digits);
  return Number.isFinite(normalized) ? normalized : null;
};

export function AdminPopupsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminPopupItem[]>([]);
  const [form, setForm] = useState<PopupFormState>(EMPTY_FORM);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [registeredTotalCount, setRegisteredTotalCount] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');

  const actor = user?.nickname || user?.email || 'admin';

  const loadItems = async (page = currentPage, keyword = searchKeyword) => {
    if (!accessToken) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await fetchAdminPopups(accessToken, {
        page,
        size: PAGE_SIZE,
        searchType: 'all',
        keyword,
      });
      setItems(response.items);
      setTotalElements(response.totalElements);
      setTotalPages(Math.max(1, response.totalPages || 1));

      if (keyword) {
        const totalResponse = await fetchAdminPopups(accessToken, {
          page: 1,
          size: 1,
        });
        setRegisteredTotalCount(totalResponse.totalElements);
      } else {
        setRegisteredTotalCount(response.totalElements);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '팝업 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') {
      return;
    }
    void loadItems(currentPage, searchKeyword);
  }, [accessToken, authReady, currentPage, searchKeyword, user?.role]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  useEffect(() => {
    return () => {
      if (previewUrl?.startsWith('blob:')) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const visibleCountOnPage = useMemo(
    () => items.filter(item => (item.noticeYn ?? 'N') === 'Y').length,
    [items]
  );
  const createLimitReached = !editingId && registeredTotalCount >= 5;

  const currentGroupStart =
    Math.floor((currentPage - 1) / PAGE_NUMBER_GROUP_SIZE) * PAGE_NUMBER_GROUP_SIZE + 1;
  const currentGroupEnd = Math.min(
    currentGroupStart + PAGE_NUMBER_GROUP_SIZE - 1,
    Math.max(1, totalPages)
  );
  const visiblePageNumbers = Array.from(
    { length: currentGroupEnd - currentGroupStart + 1 },
    (_, index) => currentGroupStart + index
  );
  const pageStartRow = totalElements === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
  const pageEndRow = Math.min(currentPage * PAGE_SIZE, totalElements);

  const handleImageUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';

    if (!file || !accessToken) {
      return;
    }

    const localPreviewUrl = URL.createObjectURL(file);

    setUploading(true);
    setError(null);
    setMessage(null);

    try {
      const uploaded = await uploadAdminPopupImage(accessToken, file);
      setForm(current => ({
        ...current,
        imagePath: uploaded.url,
        imageFileName: extractStoredFileName(uploaded.objectKey, file.name),
      }));

      setPreviewUrl(current => {
        if (current?.startsWith('blob:')) {
          URL.revokeObjectURL(current);
        }
        return localPreviewUrl;
      });
      setMessage('팝업 이미지를 업로드했습니다.');
    } catch (uploadError) {
      URL.revokeObjectURL(localPreviewUrl);
      setError(uploadError instanceof Error ? uploadError.message : '팝업 이미지 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!accessToken) {
      return;
    }

    if (createLimitReached) {
      setError('팝업은 최대 5개까지 등록할 수 있습니다. 기존 팝업을 수정하거나 삭제해 주세요.');
      setMessage(null);
      return;
    }

    if (!form.imagePath.trim()) {
      setError('팝업 이미지를 먼저 업로드해 주세요.');
      setMessage(null);
      return;
    }

    const startDateNumber = compactToDateNumber(form.noticeStartDate);
    const endDateNumber = compactToDateNumber(form.noticeEndDate);

    if (startDateNumber !== null && endDateNumber !== null && startDateNumber > endDateNumber) {
      setError('게시 종료일은 게시 시작일보다 빠를 수 없습니다.');
      setMessage(null);
      return;
    }

    const payload: PopupAdminInput = {
      title: form.title.trim(),
      fileUrl: form.fileUrl.trim(),
      linkTarget: form.linkTarget,
      imagePath: form.imagePath.trim(),
      imageFileName: form.imageFileName.trim(),
      noticeStartDate: form.noticeStartDate.trim(),
      noticeEndDate: form.noticeEndDate.trim(),
      stopViewYn: form.stopViewYn,
      noticeYn: form.noticeYn,
      createdBy: actor,
      updatedBy: actor,
    };

    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      if (editingId) {
        await updateAdminPopup(accessToken, editingId, payload);
        setMessage('팝업을 수정했습니다.');
      } else {
        await createAdminPopup(accessToken, payload);
        setMessage('팝업을 등록했습니다.');
      }
      setForm(EMPTY_FORM);
      setEditingId(null);
      setPreviewUrl(current => {
        if (current?.startsWith('blob:')) {
          URL.revokeObjectURL(current);
        }
        return null;
      });
      await loadItems(currentPage, searchKeyword);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : '팝업 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (item: AdminPopupItem) => {
    setEditingId(item.popupId);
    setForm(toFormState(item));
    setPreviewUrl(current => {
      if (current?.startsWith('blob:')) {
        URL.revokeObjectURL(current);
      }
      return null;
    });
    setMessage(null);
    setError(null);
  };

  const handleDelete = async (item: AdminPopupItem) => {
    if (!accessToken || !window.confirm(`'${item.title ?? '팝업'}'을 삭제할까요?`)) {
      return;
    }

    setError(null);
    setMessage(null);
    try {
      await deleteAdminPopup(accessToken, item.popupId);
      if (editingId === item.popupId) {
        setEditingId(null);
        setForm(EMPTY_FORM);
        setPreviewUrl(current => {
          if (current?.startsWith('blob:')) {
            URL.revokeObjectURL(current);
          }
          return null;
        });
      }
      setMessage('팝업을 삭제했습니다.');
      await loadItems(currentPage, searchKeyword);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '팝업 삭제에 실패했습니다.');
    }
  };

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCurrentPage(1);
    setSearchKeyword(searchInput.trim());
  };

  const resetSearch = () => {
    setSearchInput('');
    setSearchKeyword('');
    setCurrentPage(1);
  };

  const previewSource = previewUrl || resolveSiteContentImageUrl(form.imagePath);

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
        <div className={shellStyles.denied}>관리자만 팝업을 등록하고 수정할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/popups"
      title="팝업 관리"
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
      statusText={loading ? '팝업 목록을 불러오는 중입니다.' : null}
      stats={[
        { label: '총 팝업', value: <PopupStatCount n={registeredTotalCount} /> },
        { label: '현재 페이지 게시중', value: <PopupStatCount n={visibleCountOnPage} /> },
        { label: '추가 등록 가능', value: <PopupStatCount n={Math.max(0, 5 - registeredTotalCount)} /> },
      ]}
    >
      <div className={styles.grid}>
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>{editingId ? '팝업 수정' : '팝업 등록'}</h2>
          </div>
          <div className={styles.panelBody}>
            {message ? <p className={styles.message}>{message}</p> : null}
            {error ? <p className={styles.error}>{error}</p> : null}
            {createLimitReached ? (
              <p className={styles.message}>최대 5개까지 등록되어 있어 새 팝업 등록은 잠겨 있습니다.</p>
            ) : null}

            <form onSubmit={handleSubmit}>
              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span className={styles.label}>링크 대상</span>
                  <select
                    className={styles.select}
                    value={form.linkTarget}
                    onChange={event =>
                      setForm(current => ({ ...current, linkTarget: event.target.value }))
                    }
                  >
                    <option value="_blank">새 창</option>
                    <option value="_self">현재 창</option>
                  </select>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>팝업 제목</span>
                  <input
                    className={styles.input}
                    value={form.title}
                    onChange={event => setForm(current => ({ ...current, title: event.target.value }))}
                    required
                  />
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>연결 링크</span>
                  <input
                    className={styles.input}
                    value={form.fileUrl}
                    onChange={event =>
                      setForm(current => ({ ...current, fileUrl: event.target.value }))
                    }
                    placeholder="https://... 또는 /articles"
                  />
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>이미지 파일 업로드</span>
                  <input
                    className={styles.fileInput}
                    type="file"
                    accept="image/*"
                    onChange={handleImageUpload}
                    disabled={uploading}
                  />
                  <p className={styles.fileMeta}>
                    {uploading
                      ? '팝업 이미지를 업로드하는 중입니다.'
                      : '파일을 선택하면 업로드 후 바로 미리보기에서 확인할 수 있습니다.'}
                  </p>
                </label>
                <label className={styles.field}>
                  <span className={styles.label}>하루동안 보지 않기</span>
                  <select
                    className={styles.select}
                    value={form.stopViewYn}
                    onChange={event =>
                      setForm(current => ({ ...current, stopViewYn: event.target.value }))
                    }
                  >
                    <option value="Y">사용</option>
                    <option value="N">사용 안함</option>
                  </select>
                </label>
                <label className={styles.field}>
                  <span className={styles.label}>게시 시작</span>
                  <input
                    className={styles.input}
                    type="datetime-local"
                    value={compactToDateTimeLocal(form.noticeStartDate)}
                    onChange={event =>
                      setForm(current => ({
                        ...current,
                        noticeStartDate: dateTimeLocalToCompact(event.target.value),
                      }))
                    }
                  />
                  <p className={styles.fileMeta}>달력과 시간을 선택하면 자동으로 저장됩니다.</p>
                </label>
                <label className={styles.field}>
                  <span className={styles.label}>게시 종료</span>
                  <input
                    className={styles.input}
                    type="datetime-local"
                    value={compactToDateTimeLocal(form.noticeEndDate)}
                    onChange={event =>
                      setForm(current => ({
                        ...current,
                        noticeEndDate: dateTimeLocalToCompact(event.target.value),
                      }))
                    }
                  />
                  <p className={styles.fileMeta}>비워두면 종료 시점 없이 계속 노출됩니다.</p>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>게시 여부</span>
                  <select
                    className={styles.select}
                    value={form.noticeYn}
                    onChange={event =>
                      setForm(current => ({ ...current, noticeYn: event.target.value }))
                    }
                  >
                    <option value="Y">게시</option>
                    <option value="N">미게시</option>
                  </select>
                </label>
              </div>

              {previewSource ? (
                <div className={styles.preview}>
                  <img
                    src={previewSource}
                    alt="팝업 미리보기"
                    onError={() => {
                      if (!previewUrl) {
                        setError('업로드한 이미지를 미리보기로 불러오지 못했습니다.');
                      }
                    }}
                  />
                </div>
              ) : null}

              <div className={styles.formActions}>
                <button
                  type="submit"
                  className={styles.primaryButton}
                  disabled={saving || uploading || createLimitReached}
                >
                  {saving ? '저장 중...' : editingId ? '팝업 수정' : '팝업 등록'}
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => {
                    setEditingId(null);
                    setForm(EMPTY_FORM);
                    setPreviewUrl(current => {
                      if (current?.startsWith('blob:')) {
                        URL.revokeObjectURL(current);
                      }
                      return null;
                    });
                    setMessage(null);
                    setError(null);
                  }}
                >
                  초기화
                </button>
              </div>
            </form>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>등록된 팝업</h2>
          </div>
          <div className={styles.panelBody}>
            <div className={styles.listToolbar}>
              <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
                <input
                  className={styles.searchInput}
                  value={searchInput}
                  onChange={event => setSearchInput(event.target.value)}
                  placeholder="팝업 제목, 링크, 파일명으로 검색"
                />
                <button type="submit" className={styles.searchButton}>
                  검색
                </button>
                {searchKeyword ? (
                  <button type="button" className={styles.ghostButton} onClick={resetSearch}>
                    초기화
                  </button>
                ) : null}
              </form>
            </div>

            {loading ? (
              <div className={styles.empty}>팝업 목록을 불러오는 중입니다.</div>
            ) : items.length === 0 ? (
              <div className={styles.empty}>
                {searchKeyword ? '검색 결과가 없습니다.' : '등록된 팝업이 없습니다.'}
              </div>
            ) : (
              <>
                <div className={styles.list}>
                  {items.map(item => (
                    <article key={item.popupId} className={styles.card}>
                      <div className={styles.thumb}>
                        {resolveSiteContentImageUrl(item.imagePath) ? (
                          <img
                            src={resolveSiteContentImageUrl(item.imagePath) ?? ''}
                            alt={item.title ?? '팝업'}
                          />
                        ) : (
                          <div className={styles.thumbFallback}>이미지 없음</div>
                        )}
                      </div>
                      <div className={styles.cardBody}>
                        <div className={styles.topRow}>
                          <h3 className={styles.name}>{item.title ?? '이름 없는 팝업'}</h3>
                          <div className={styles.rowActions}>
                            <button
                              type="button"
                              className={styles.secondaryButton}
                              onClick={() => startEdit(item)}
                            >
                              수정
                            </button>
                            <button
                              type="button"
                              className={styles.dangerButton}
                              onClick={() => handleDelete(item)}
                            >
                              삭제
                            </button>
                          </div>
                        </div>
                        <div className={styles.chips}>
                          <span className={styles.chip}>
                            {item.noticeYn === 'Y' ? '게시중' : '미게시'}
                          </span>
                          <span className={styles.chip}>{item.linkTarget ?? '_blank'}</span>
                        </div>
                        {item.fileUrl ? <p className={styles.meta}>링크: {item.fileUrl}</p> : null}
                        <p className={styles.meta}>
                          기간: {item.noticeStartDate || '즉시'} ~ {item.noticeEndDate || '상시'}
                        </p>
                      </div>
                    </article>
                  ))}
                </div>

                <div className={styles.paginationWrap}>
                  <p className={styles.paginationInfo} aria-live="polite">
                    전체 <strong>{totalElements}</strong>건 · 페이지당 <strong>{PAGE_SIZE}</strong>건 ·{' '}
                    <strong>
                      {pageStartRow}-{pageEndRow}
                    </strong>
                    번째 표시 · <strong>{currentPage}</strong> / <strong>{Math.max(1, totalPages)}</strong>{' '}
                    페이지
                  </p>
                  <div className={styles.pagination}>
                    <button
                      type="button"
                      className={styles.pageArrowButton}
                      onClick={() => setCurrentPage(1)}
                      disabled={currentPage <= 1}
                      aria-label="첫 페이지"
                      title="첫 페이지"
                    >
                      «
                    </button>
                    <button
                      type="button"
                      className={styles.pageArrowButton}
                      onClick={() => setCurrentPage(previous => Math.max(1, previous - 1))}
                      disabled={currentPage <= 1}
                      aria-label="이전 페이지"
                      title="이전 페이지"
                    >
                      <ArrowIcon direction="left" />
                    </button>

                    <div className={styles.paginationNumbers}>
                      {visiblePageNumbers.map(pageNumber => (
                        <button
                          key={pageNumber}
                          type="button"
                          className={[
                            styles.pageButton,
                            pageNumber === currentPage ? styles.pageButtonActive : '',
                          ]
                            .filter(Boolean)
                            .join(' ')}
                          onClick={() => setCurrentPage(pageNumber)}
                          aria-current={pageNumber === currentPage ? 'page' : undefined}
                        >
                          {pageNumber}
                        </button>
                      ))}
                    </div>

                    <button
                      type="button"
                      className={styles.pageArrowButton}
                      onClick={() =>
                        setCurrentPage(previous => Math.min(previous + 1, Math.max(1, totalPages)))
                      }
                      disabled={currentPage >= Math.max(1, totalPages)}
                      aria-label="다음 페이지"
                      title="다음 페이지"
                    >
                      <ArrowIcon direction="right" />
                    </button>
                    <button
                      type="button"
                      className={styles.pageArrowButton}
                      onClick={() => setCurrentPage(Math.max(1, totalPages))}
                      disabled={currentPage >= Math.max(1, totalPages)}
                      aria-label="마지막 페이지"
                      title="마지막 페이지"
                    >
                      »
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </section>
      </div>
    </AdminMarketingShell>
  );
}

export default AdminPopupsPage;
