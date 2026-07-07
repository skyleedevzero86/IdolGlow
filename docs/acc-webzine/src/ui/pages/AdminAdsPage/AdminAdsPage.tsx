import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  createAdminMainImage,
  deleteAdminMainImage,
  fetchAdminMainImages,
  resolveSiteContentImageUrl,
  updateAdminMainImage,
  uploadAdminMainImage,
  type AdminMainImageItem,
  type MainImageAdminInput,
} from '../../../shared/data/siteContentAdminApi';
import '../../../../../samples/portal-stat-bar.css';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminAdsPage.module.css';

const PAGE_SIZE = 10;
const PAGE_NUMBER_GROUP_SIZE = 5;

interface MainImageFormState {
  readonly imageName: string;
  readonly imagePath: string;
  readonly imageFileName: string;
  readonly description: string;
  readonly activeYn: string;
}

const EMPTY_FORM: MainImageFormState = {
  imageName: '',
  imagePath: '',
  imageFileName: '',
  description: '',
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

const AdsStatCount = ({ n }: { readonly n: number }) => (
  <>
    <span className={shellToolbar.statNumber}>{n}</span>
    <span className={shellToolbar.statUnit}>건</span>
  </>
);

const toFormState = (item: AdminMainImageItem): MainImageFormState => ({
  imageName: item.imageName ?? '',
  imagePath: item.imagePath ?? '',
  imageFileName: item.imageFileName ?? '',
  description: item.description ?? '',
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

export function AdminAdsPage() {
  const { accessToken, authReady, user } = useAuth();
  const [items, setItems] = useState<readonly AdminMainImageItem[]>([]);
  const [form, setForm] = useState<MainImageFormState>(EMPTY_FORM);
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
      const response = await fetchAdminMainImages(accessToken, {
        page,
        size: PAGE_SIZE,
        searchType: 'all',
        keyword,
      });
      setItems(response.items);
      setTotalElements(response.totalElements);
      setTotalPages(Math.max(1, response.totalPages || 1));
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : '메인 슬라이드 목록을 불러오지 못했습니다.'
      );
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

    const localPreview = URL.createObjectURL(file);

    setUploading(true);
    setError(null);
    setMessage(null);

    try {
      const uploaded = await uploadAdminMainImage(accessToken, file);
      setForm(current => ({
        ...current,
        imagePath: uploaded.url,
        imageFileName: extractStoredFileName(uploaded.objectKey, file.name),
      }));
      setPreviewUrl(current => {
        if (current?.startsWith('blob:')) {
          URL.revokeObjectURL(current);
        }
        return localPreview;
      });
      setMessage('메인 슬라이드 이미지를 업로드했습니다.');
    } catch (uploadError) {
      URL.revokeObjectURL(localPreview);
      setError(
        uploadError instanceof Error
          ? uploadError.message
          : '메인 슬라이드 이미지 업로드에 실패했습니다.'
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
      setError('메인 슬라이드 이미지를 먼저 업로드해 주세요.');
      setMessage(null);
      return;
    }

    const payload: MainImageAdminInput = {
      imageName: form.imageName.trim(),
      imagePath: form.imagePath.trim(),
      imageFileName: form.imageFileName.trim(),
      description: form.description.trim(),
      activeYn: form.activeYn,
      createdBy: actor,
    };

    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      if (editingId) {
        await updateAdminMainImage(accessToken, editingId, payload);
        setMessage('메인 슬라이드를 수정했습니다.');
      } else {
        await createAdminMainImage(accessToken, payload);
        setMessage('메인 슬라이드를 등록했습니다.');
      }

      setForm(EMPTY_FORM);
      setEditingId(null);
      setPreviewUrl(current => {
        if (current?.startsWith('blob:')) {
          URL.revokeObjectURL(current);
        }
        return null;
      });

      if (editingId) {
        await loadItems(currentPage, searchKeyword);
      } else {
        setCurrentPage(1);
        await loadItems(1, searchKeyword);
      }
    } catch (submitError) {
      setError(
        submitError instanceof Error ? submitError.message : '메인 슬라이드 저장에 실패했습니다.'
      );
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (item: AdminMainImageItem) => {
    setEditingId(item.imageId);
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

  const handleDelete = async (item: AdminMainImageItem) => {
    if (!accessToken || !window.confirm(`'${item.imageName ?? '슬라이드'}'를 삭제할까요?`)) {
      return;
    }

    setError(null);
    setMessage(null);
    try {
      await deleteAdminMainImage(accessToken, item.imageId);
      if (editingId === item.imageId) {
        setEditingId(null);
        setForm(EMPTY_FORM);
        setPreviewUrl(current => {
          if (current?.startsWith('blob:')) {
            URL.revokeObjectURL(current);
          }
          return null;
        });
      }
      setMessage('메인 슬라이드를 삭제했습니다.');
      await loadItems(currentPage, searchKeyword);
    } catch (deleteError) {
      setError(
        deleteError instanceof Error ? deleteError.message : '메인 슬라이드 삭제에 실패했습니다.'
      );
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
        <div className={shellStyles.denied}>관리자만 메인 슬라이드를 등록하고 수정할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/ads"
      title="광고 관리"
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
      statusText={loading ? '메인 슬라이드 목록을 불러오는 중입니다.' : null}
      stats={[
        { label: '총 슬라이드', value: <AdsStatCount n={totalElements} /> },
        { label: '현재 페이지 노출중', value: <AdsStatCount n={activeCountOnPage} /> },
      ]}
    >
      <div className={styles.grid}>
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>{editingId ? '슬라이드 수정' : '슬라이드 등록'}</h2>
          </div>
          <div className={styles.panelBody}>
            {message ? <p className={styles.message}>{message}</p> : null}
            {error ? <p className={styles.error}>{error}</p> : null}

            <form onSubmit={handleSubmit}>
              <div className={styles.formGrid}>
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
                  <span className={styles.label}>슬라이드 제목</span>
                  <input
                    className={styles.input}
                    value={form.imageName}
                    onChange={event =>
                      setForm(current => ({ ...current, imageName: event.target.value }))
                    }
                    required
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
                      ? 'MINIO에 메인 슬라이드 이미지를 업로드하는 중입니다.'
                      : '파일을 선택하면 업로드 직후 바로 미리보기를 확인할 수 있습니다.'}
                  </p>
                </label>

                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span className={styles.label}>슬라이드 설명</span>
                  <textarea
                    className={styles.textarea}
                    value={form.description}
                    onChange={event =>
                      setForm(current => ({ ...current, description: event.target.value }))
                    }
                    placeholder="메인 화면에서 보일 설명을 입력해 주세요."
                  />
                </label>
              </div>

              {previewSource ? (
                <div className={styles.preview}>
                  <img src={previewSource} alt="메인 슬라이드 미리보기" />
                </div>
              ) : null}

              <div className={styles.formActions}>
                <button
                  type="submit"
                  className={styles.primaryButton}
                  disabled={saving || uploading}
                >
                  {saving ? '저장 중...' : editingId ? '슬라이드 수정' : '슬라이드 등록'}
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
            <h2 className={styles.panelTitle}>등록된 메인 슬라이드</h2>
          </div>
          <div className={styles.panelBody}>
            <div className={styles.listToolbar}>
              <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
                <input
                  className={styles.searchInput}
                  value={searchInput}
                  onChange={event => setSearchInput(event.target.value)}
                  placeholder="슬라이드 제목, 설명, 파일명으로 검색"
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
              <div className={styles.empty}>메인 슬라이드 목록을 불러오는 중입니다.</div>
            ) : items.length === 0 ? (
              <div className={styles.empty}>
                {searchKeyword ? '검색 결과가 없습니다.' : '등록된 슬라이드가 없습니다.'}
              </div>
            ) : (
              <>
                <div className={styles.list}>
                  {items.map(item => {
                    const thumbnailUrl = resolveSiteContentImageUrl(item.imagePath);

                    return (
                      <article key={item.imageId} className={styles.card}>
                        <div className={styles.thumb}>
                          {thumbnailUrl ? (
                            <img src={thumbnailUrl} alt={item.imageName ?? '슬라이드'} />
                          ) : (
                            <div className={styles.thumbFallback}>이미지 없음</div>
                          )}
                        </div>
                        <div className={styles.cardBody}>
                          <div className={styles.topRow}>
                            <h3 className={styles.name}>{item.imageName ?? '이름 없는 슬라이드'}</h3>
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
                              {item.activeYn === 'Y' ? '노출중' : '대기'}
                            </span>
                          </div>
                          {item.description ? <p className={styles.meta}>{item.description}</p> : null}
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

export default AdminAdsPage;
