import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  createAdminBanner,
  deleteAdminBanner,
  fetchAdminBanners,
  resolveSiteContentImageUrl,
  updateAdminBanner,
  uploadAdminBannerImage,
  type AdminBannerItem,
  type BannerAdminInput,
} from '../../../shared/data/siteContentAdminApi';
import '../../../../../samples/portal-stat-bar.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminBannersPage.module.css';

const PAGE_SIZE = 10;
const PAGE_NUMBER_GROUP_SIZE = 5;

interface BannerFormState {
  readonly bannerName: string;
  readonly linkUrl: string;
  readonly imagePath: string;
  readonly imageFileName: string;
  readonly description: string;
  readonly sortOrder: string;
  readonly activeYn: string;
}

const EMPTY_FORM: BannerFormState = {
  bannerName: '',
  linkUrl: '/articles',
  imagePath: '',
  imageFileName: '',
  description: '',
  sortOrder: '0',
  activeYn: 'Y',
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

const BannerStatCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>건</span>
  </>
);

const toFormState = (item: AdminBannerItem): BannerFormState => ({
  bannerName: item.bannerName ?? '',
  linkUrl: item.linkUrl ?? '/articles',
  imagePath: item.imagePath ?? '',
  imageFileName: item.imageFileName ?? '',
  description: item.description ?? '',
  sortOrder: String(item.sortOrder ?? 0),
  activeYn: item.activeYn ?? 'Y',
});

const extractStoredFileName = (objectKey: string, fallback: string): string => {
  const trimmed = objectKey.trim();
  if (!trimmed) {
    return fallback;
  }

  const segments = trimmed.split('/');
  return segments[segments.length - 1] || fallback;
};

export function AdminBannersPage() {
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminBannerItem[]>([]);
  const [form, setForm] = useState<BannerFormState>(EMPTY_FORM);
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
      const response = await fetchAdminBanners(accessToken, {
        page,
        size: PAGE_SIZE,
        searchType: 'all',
        keyword,
      });
      setItems(response.items);
      setTotalElements(response.totalElements);
      setTotalPages(Math.max(1, response.totalPages || 1));
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '배너 목록을 불러오지 못했습니다.');
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

  const activeCountOnPage = useMemo(
    () => items.filter(item => (item.activeYn ?? 'N') === 'Y').length,
    [items]
  );

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
      const uploaded = await uploadAdminBannerImage(accessToken, file);
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
      setMessage('배너 이미지를 업로드했습니다.');
    } catch (uploadError) {
      URL.revokeObjectURL(localPreviewUrl);
      setError(
        uploadError instanceof Error ? uploadError.message : '배너 이미지 업로드에 실패했습니다.'
      );
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!accessToken) {
      return;
    }

    if (!form.imagePath.trim()) {
      setError('배너 이미지를 먼저 업로드해 주세요.');
      setMessage(null);
      return;
    }

    const payload: BannerAdminInput = {
      bannerName: form.bannerName.trim(),
      linkUrl: form.linkUrl.trim(),
      imagePath: form.imagePath.trim(),
      imageFileName: form.imageFileName.trim(),
      description: form.description.trim(),
      sortOrder: Number(form.sortOrder) || 0,
      activeYn: form.activeYn,
      createdBy: actor,
    };

    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      if (editingId) {
        await updateAdminBanner(accessToken, editingId, payload);
        setMessage('배너를 수정했습니다.');
      } else {
        await createAdminBanner(accessToken, payload);
        setMessage('배너를 등록했습니다.');
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
      setError(submitError instanceof Error ? submitError.message : '배너 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (item: AdminBannerItem) => {
    setEditingId(item.bannerId);
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

  const handleDelete = async (item: AdminBannerItem) => {
    if (!accessToken || !window.confirm(`'${item.bannerName ?? '배너'}'를 삭제할까요?`)) {
      return;
    }

    setError(null);
    setMessage(null);
    try {
      await deleteAdminBanner(accessToken, item.bannerId);
      if (editingId === item.bannerId) {
        setEditingId(null);
        setForm(EMPTY_FORM);
        setPreviewUrl(current => {
          if (current?.startsWith('blob:')) {
            URL.revokeObjectURL(current);
          }
          return null;
        });
      }
      setMessage('배너를 삭제했습니다.');
      await loadItems(currentPage, searchKeyword);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : '배너 삭제에 실패했습니다.');
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
        <div className={shellStyles.denied}>관리자만 배너를 등록하고 수정할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/banners"
      title="배너 관리"
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
      statusText={loading ? '배너 목록을 불러오는 중입니다.' : null}
      stats={[
        { label: '총 배너', value: <BannerStatCount n={totalElements} /> },
        { label: '현재 페이지 활성', value: <BannerStatCount n={activeCountOnPage} /> },
      ]}
    >
      <div className={styles.grid}>
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>{editingId ? '배너 수정' : '배너 등록'}</h2>
          </div>
          <div className={styles.panelBody}>
            {message ? <p className={styles.message}>{message}</p> : null}
            {error ? <p className={styles.error}>{error}</p> : null}

            <form onSubmit={handleSubmit}>
              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span className={styles.label}>정렬 순서</span>
                  <input
                    className={styles.input}
                    type="number"
                    value={form.sortOrder}
                    onChange={event =>
                      setForm(current => ({ ...current, sortOrder: event.target.value }))
                    }
                  />
                </label>
                <label className={styles.field}>
                  <span className={styles.label}>사용 여부</span>
                  <select
                    className={styles.select}
                    value={form.activeYn}
                    onChange={event =>
                      setForm(current => ({ ...current, activeYn: event.target.value }))
                    }
                  >
                    <option value="Y">사용</option>
                    <option value="N">미사용</option>
                  </select>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>배너명</span>
                  <input
                    className={styles.input}
                    value={form.bannerName}
                    onChange={event =>
                      setForm(current => ({ ...current, bannerName: event.target.value }))
                    }
                    required
                  />
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>이동 링크</span>
                  <input
                    className={styles.input}
                    value={form.linkUrl}
                    onChange={event =>
                      setForm(current => ({ ...current, linkUrl: event.target.value }))
                    }
                    placeholder="/articles 또는 https://..."
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
                      ? 'MINIO에 배너 이미지를 업로드하는 중입니다.'
                      : '파일을 선택하면 바로 미리보기에서 확인할 수 있습니다.'}
                  </p>
                </label>
                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>설명</span>
                  <textarea
                    className={styles.textarea}
                    value={form.description}
                    onChange={event =>
                      setForm(current => ({ ...current, description: event.target.value }))
                    }
                  />
                </label>
              </div>

              {previewSource ? (
                <div className={styles.preview}>
                  <img src={previewSource} alt="배너 미리보기" />
                </div>
              ) : null}

              <div className={styles.formActions}>
                <button
                  type="submit"
                  className={styles.primaryButton}
                  disabled={saving || uploading}
                >
                  {saving ? '저장 중...' : editingId ? '배너 수정' : '배너 등록'}
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
            <h2 className={styles.panelTitle}>등록된 배너</h2>
          </div>
          <div className={styles.panelBody}>
            <div className={styles.listToolbar}>
              <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
                <input
                  className={styles.searchInput}
                  value={searchInput}
                  onChange={event => setSearchInput(event.target.value)}
                  placeholder="배너명, 링크, 설명으로 검색"
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
              <div className={styles.empty}>배너 목록을 불러오는 중입니다.</div>
            ) : items.length === 0 ? (
              <div className={styles.empty}>
                {searchKeyword ? '검색 결과가 없습니다.' : '등록된 배너가 없습니다.'}
              </div>
            ) : (
              <>
                <div className={styles.list}>
                  {items.map(item => {
                    const thumbnailUrl = resolveSiteContentImageUrl(item.imagePath);

                    return (
                      <article key={item.bannerId} className={styles.card}>
                        <div className={styles.thumb}>
                          {thumbnailUrl ? (
                            <img src={thumbnailUrl} alt={item.bannerName ?? '배너'} />
                          ) : (
                            <div className={styles.thumbFallback}>이미지 없음</div>
                          )}
                        </div>
                        <div className={styles.cardBody}>
                          <div className={styles.topRow}>
                            <h3 className={styles.name}>{item.bannerName ?? '이름 없는 배너'}</h3>
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
                              {item.activeYn === 'Y' ? '사용중' : '대기'}
                            </span>
                            <span className={styles.chip}>정렬 {item.sortOrder}</span>
                          </div>
                          {item.description ? <p className={styles.meta}>{item.description}</p> : null}
                          {item.linkUrl ? <p className={styles.meta}>링크: {item.linkUrl}</p> : null}
                        </div>
                      </article>
                    );
                  })}
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

export default AdminBannersPage;
