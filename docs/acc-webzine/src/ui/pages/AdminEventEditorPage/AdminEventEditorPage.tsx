import { useEffect, useMemo, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchAdminEvent,
  type AdminEventStatus,
  upsertAdminEvent,
  uploadAdminEventImage,
} from '../../../shared/data/adminEventsApi';
import {
  hasNoticeBoardMarker,
  isHiddenBoardMarkerTag,
  normalizeEventTagsForSave,
} from '../../../shared/data/mbrdBoardMarkers';
import { renderIdolGlowMarkdown } from '../../../shared/markdown/idolGlowMarkdown';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminEventsPage/AdminEventsPage.module.css';

type EventFormState = {
  readonly documentId: string | null;
  readonly title: string;
  readonly urlSlug: string;
  readonly introduction: string;
  readonly thumbnailImageUrl: string;
  readonly tagsText: string;
  readonly startDate: string;
  readonly endDate: string;
  readonly markdown: string;
};

type LinkDialogState = {
  readonly open: boolean;
  readonly text: string;
  readonly url: string;
  readonly target: '_self' | '_blank';
};

const EMPTY_FORM: EventFormState = {
  documentId: null,
  title: '',
  urlSlug: '',
  introduction: '',
  thumbnailImageUrl: '',
  tagsText: 'event, idol glow',
  startDate: '',
  endDate: '',
  markdown:
    '# Idol Glow 이벤트 제목\n\n이벤트 내용을 작성해 주세요.\n\n| 결과 | 닉네임 | 번호뒷자리 |\n| --- | --- | --- |\n| 1등 | 예시 | 1234 |\n',
};

const toTagList = (value: string): string[] =>
  Array.from(
    new Set(
      value
        .split(/[\s,]+/)
        .map(item => item.trim().replace(/#/g, ''))
        .filter(Boolean)
    )
  );

const TAG_ALLOWED_PATTERN = /^[0-9A-Za-z가-힣ㄱ-ㅎㅏ-ㅣ#_,\s]*$/;

const META_START_PREFIX = 'event-start:';
const META_END_PREFIX = 'event-end:';
const DRAFT_PREFIX = '[임시저장-이벤트 시작] ';
const PROGRESS_PREFIX = '[이벤트 진행중] ';
const ENDED_PREFIX = '[이벤트종료] ';

const stripEventPrefixes = (title: string): string =>
  title
    .replace(DRAFT_PREFIX, '')
    .replace(PROGRESS_PREFIX, '')
    .replace(ENDED_PREFIX, '')
    .trim();

const extractDateMeta = (tags: readonly string[]) => {
  let startDate = '';
  let endDate = '';
  const visibleTags: string[] = [];

  tags.forEach(tag => {
    if (tag.startsWith(META_START_PREFIX)) {
      startDate = tag.slice(META_START_PREFIX.length);
      return;
    }
    if (tag.startsWith(META_END_PREFIX)) {
      endDate = tag.slice(META_END_PREFIX.length);
      return;
    }
    if (isHiddenBoardMarkerTag(tag)) {
      return;
    }
    visibleTags.push(tag);
  });

  return { startDate, endDate, visibleTags };
};

export function AdminEventEditorPage() {
  const { documentId } = useParams<{ readonly documentId?: string }>();
  const isEdit = Boolean(documentId);
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();

  const [form, setForm] = useState<EventFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [linkDialog, setLinkDialog] = useState<LinkDialogState>({
    open: false,
    text: '',
    url: '',
    target: '_blank',
  });
  const [selectedImageAlign, setSelectedImageAlign] = useState<'left' | 'center' | 'right'>('center');
  const [selectedImageWidth, setSelectedImageWidth] = useState<'40%' | '60%' | '80%' | '100%'>('60%');
  const [selectedImageHeight, setSelectedImageHeight] = useState<'auto' | '240px' | '320px' | '420px'>('auto');
  const markdownTextareaRef = useRef<HTMLTextAreaElement | null>(null);
  const markdownImageInputRef = useRef<HTMLInputElement | null>(null);

  const authorFallback = user?.nickname || user?.email || 'admin';

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN') return;
    if (!documentId) return;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const detail = await fetchAdminEvent(accessToken, documentId);
        if (hasNoticeBoardMarker(detail.tags)) {
          setError('이 문서는 공지사항입니다. 공지사항 관리(/admin/notices)에서 수정해 주세요.');
          setLoading(false);
          return;
        }
        const { startDate, endDate, visibleTags } = extractDateMeta(detail.tags);
        setForm({
          documentId: detail.documentId,
          title: stripEventPrefixes(detail.title),
          urlSlug: detail.urlSlug ?? '',
          introduction: detail.introduction ?? '',
          thumbnailImageUrl: detail.thumbnailImageUrl ?? '',
          tagsText: visibleTags.join(' '),
          startDate,
          endDate,
          markdown: detail.markdown,
        });
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '이벤트 상세를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [authReady, user?.role, accessToken, documentId, authorFallback]);

  const previewHtml = useMemo(() => renderIdolGlowMarkdown(form.markdown), [form.markdown]);
  const liveTags = useMemo(() => toTagList(form.tagsText), [form.tagsText]);

  const handleTagInputChange = (raw: string) => {
    if (!TAG_ALLOWED_PATTERN.test(raw)) {
      window.alert('해시태그는 한글, 영문, 숫자, #, _ 만 입력할 수 있습니다.');
      return;
    }
    // 입력창에서는 사용자가 공백 포함 원문을 그대로 입력할 수 있게 둔다.
    // 해시태그 파싱/보정은 실시간 표시(useMemo)와 저장 시점(toTagList)에서 수행한다.
    setForm(previous => ({ ...previous, tagsText: raw }));
  };

  const handleRemoveTag = (targetTag: string) => {
    const nextTags = toTagList(form.tagsText).filter(tag => tag !== targetTag);
    setForm(previous => ({ ...previous, tagsText: nextTags.join(' ') }));
  };

  const surroundSelection = (prefix: string, suffix = '', fallbackText = '') => {
    const target = markdownTextareaRef.current;
    if (!target) return;
    const start = target.selectionStart ?? 0;
    const end = target.selectionEnd ?? 0;
    const selected = form.markdown.slice(start, end);
    const wrapped = `${prefix}${selected || fallbackText}${suffix}`;
    const next = `${form.markdown.slice(0, start)}${wrapped}${form.markdown.slice(end)}`;
    setForm(previous => ({ ...previous, markdown: next }));
    requestAnimationFrame(() => {
      target.focus();
      const caret = start + wrapped.length;
      target.setSelectionRange(caret, caret);
    });
  };

  const insertLinePrefix = (prefix: string, fallback = '') => {
    const target = markdownTextareaRef.current;
    if (!target) return;
    const start = target.selectionStart ?? 0;
    const end = target.selectionEnd ?? 0;
    const selected = form.markdown.slice(start, end);
    if (selected) {
      const selectedLines = selected
        .split('\n')
        .map(line => `${prefix}${line}`)
        .join('\n');
      const nextSelected = `${form.markdown.slice(0, start)}${selectedLines}${form.markdown.slice(end)}`;
      setForm(previous => ({ ...previous, markdown: nextSelected }));
      requestAnimationFrame(() => {
        target.focus();
        const caret = start + selectedLines.length;
        target.setSelectionRange(caret, caret);
      });
      return;
    }
    const lineStart = form.markdown.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
    const next = `${form.markdown.slice(0, lineStart)}${prefix}${fallback}${form.markdown.slice(lineStart)}`;
    setForm(previous => ({ ...previous, markdown: next }));
    requestAnimationFrame(() => {
      target.focus();
      const caret = start + prefix.length + fallback.length;
      target.setSelectionRange(caret, caret);
    });
  };

  const updateImageAttributesAtCursor = (attributes: string) => {
    const target = markdownTextareaRef.current;
    if (!target) return;
    const start = target.selectionStart ?? 0;
    const lineStart = form.markdown.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
    const nextLineBreak = form.markdown.indexOf('\n', start);
    const lineEnd = nextLineBreak === -1 ? form.markdown.length : nextLineBreak;
    const line = form.markdown.slice(lineStart, lineEnd);
    const imageMatch = line.match(/^\s*!\[([^\]]*)\]\(([^)]+)\)(\{[^}]+\})?\s*$/);
    if (!imageMatch) {
      setError('이미지 마크다운 라인에서만 정렬/크기 옵션을 적용할 수 있습니다.');
      return;
    }
    const updatedLine = `![${imageMatch[1]}](${imageMatch[2]}){${attributes}}`;
    const updatedMarkdown = `${form.markdown.slice(0, lineStart)}${updatedLine}${form.markdown.slice(lineEnd)}`;
    setForm(previous => ({ ...previous, markdown: updatedMarkdown }));
  };

  const buildImageAttributes = (): string => {
    const attrs = [`align=${selectedImageAlign}`, `width=${selectedImageWidth}`];
    if (selectedImageHeight !== 'auto') {
      attrs.push(`height=${selectedImageHeight}`);
    }
    return attrs.join(' ');
  };

  const handleInsertLink = () => {
    const target = markdownTextareaRef.current;
    const start = target?.selectionStart ?? 0;
    const end = target?.selectionEnd ?? 0;
    const selected = form.markdown.slice(start, end).trim();
    setLinkDialog({
      open: true,
      text: selected || '',
      url: '',
      target: '_blank',
    });
  };

  const applyLinkDialog = () => {
    const text = linkDialog.text.trim();
    const url = linkDialog.url.trim();
    if (!text || !url) {
      setError('링크명과 주소를 입력해 주세요.');
      return;
    }
    const markdownLink = `[${text}](${url}){target=${linkDialog.target}}`;
    insertMarkdownAtCursor(markdownLink);
    setLinkDialog(previous => ({ ...previous, open: false }));
  };

  const insertMarkdownAtCursor = (value: string) => {
    const target = markdownTextareaRef.current;
    if (!target) {
      setForm(previous => ({ ...previous, markdown: `${previous.markdown}\n${value}`.trimStart() }));
      return;
    }

    const start = target.selectionStart ?? form.markdown.length;
    const end = target.selectionEnd ?? form.markdown.length;
    const current = form.markdown;
    const next = `${current.slice(0, start)}${value}${current.slice(end)}`;
    setForm(previous => ({ ...previous, markdown: next }));

    requestAnimationFrame(() => {
      target.focus();
      const caret = start + value.length;
      target.setSelectionRange(caret, caret);
    });
  };

  const handleUploadMarkdownImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    setUploading(true);
    setError(null);
    try {
      const uploaded = await uploadAdminEventImage(accessToken, file);
      const imageMarkdown = `\n![이미지](${uploaded.url}){${buildImageAttributes()}}\n`;
      insertMarkdownAtCursor(imageMarkdown);
      setForm(previous => ({
        ...previous,
        thumbnailImageUrl: previous.thumbnailImageUrl || uploaded.url,
      }));
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : '이미지 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
    }
  };

  const buildTagsWithDateMeta = () => {
    const tags = toTagList(form.tagsText);
    if (form.startDate) {
      tags.push(`${META_START_PREFIX}${form.startDate}`);
    }
    if (form.endDate) {
      tags.push(`${META_END_PREFIX}${form.endDate}`);
    }
    return normalizeEventTagsForSave(Array.from(new Set(tags)));
  };

  const saveEvent = async (status: AdminEventStatus) => {
    const baseTitle = stripEventPrefixes(form.title);
    const title = status === 'draft' ? `${DRAFT_PREFIX}${baseTitle}` : baseTitle;
    const saved = await upsertAdminEvent(accessToken, {
      documentId: form.documentId,
      title,
      author: authorFallback,
      markdown: form.markdown,
      tags: buildTagsWithDateMeta(),
      urlSlug: form.urlSlug.trim() || null,
      introduction: form.introduction.trim() || null,
      thumbnailImageUrl: form.thumbnailImageUrl.trim() || null,
      status,
    });
    navigate(`/admin/events/${saved.documentId}`);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form.title.trim() || !form.markdown.trim()) {
      setError('제목과 본문은 필수입니다.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await saveEvent('published');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : '이벤트 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveDraft = async () => {
    if (!form.title.trim() || !form.markdown.trim()) {
      setError('제목과 본문은 필수입니다.');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await saveEvent('draft');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : '임시저장에 실패했습니다.');
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

  if (user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>관리자만 이벤트 관리 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/events"
      title={isEdit ? '이벤트 수정' : '이벤트 등록'}
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
      }}
      statusText={null}
      stats={[]}
    >
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <div className={styles.headerActions}>
            <button type="button" className={styles.secondaryButton} onClick={() => navigate('/admin/events')}>
              목록 이동
            </button>
          </div>
        </div>
        <div className={styles.panelBody}>
          <form onSubmit={handleSubmit}>
            <div className={styles.formGrid}>
              <label className={`${styles.field} ${styles.fullWidth}`}>
                <input
                  className={styles.editorTitleInput}
                  value={form.title}
                  onChange={event => setForm(previous => ({ ...previous, title: event.target.value }))}
                  placeholder="제목을 입력하세요"
                  required
                />
              </label>
              <label className={`${styles.field} ${styles.fullWidth}`}>
                <input
                  className={styles.editorTagInput}
                  value={form.tagsText}
                  onChange={event => handleTagInputChange(event.target.value)}
                  placeholder="태그를 입력하세요"
                />
                {liveTags.length > 0 ? (
                  <div className={styles.tagRow}>
                    {liveTags.map(tag => (
                      <button
                        key={tag}
                        type="button"
                        className={styles.tagChip}
                        onClick={() => handleRemoveTag(tag)}
                        title="클릭하면 태그가 삭제됩니다."
                      >
                        #{tag.replace(/^#/, '').replace(/_/g, ' ')}
                      </button>
                    ))}
                  </div>
                ) : null}
              </label>
              <div className={styles.field}>
                <span className={styles.label}>이벤트 기간</span>
                <div className={styles.dateRangeGrid}>
                  <label className={styles.field}>
                    <span className={styles.label}>시작날짜</span>
                    <input
                      type="date"
                      className={styles.input}
                      value={form.startDate}
                      onChange={event => setForm(previous => ({ ...previous, startDate: event.target.value }))}
                    />
                  </label>
                  <label className={styles.field}>
                    <span className={styles.label}>종료날짜</span>
                    <input
                      type="date"
                      className={styles.input}
                      value={form.endDate}
                      onChange={event => setForm(previous => ({ ...previous, endDate: event.target.value }))}
                    />
                  </label>
                </div>
              </div>
              <label className={`${styles.field} ${styles.fullWidth}`}>
                <span className={styles.label}>소개 문구</span>
                <textarea
                  className={`${styles.textarea} ${styles.introTextarea}`}
                  value={form.introduction}
                  onChange={event => setForm(previous => ({ ...previous, introduction: event.target.value }))}
                  maxLength={150}
                  placeholder="소개문구를 150자 내외로 입력해 주세요."
                />
              </label>
              <label className={`${styles.field} ${styles.fullWidth}`}>
                <div className={styles.markdownHeader}>
                  <span className={styles.label}>마크다운 본문</span>
                  <div className={styles.markdownTools}>
                    <button type="button" className={styles.markdownTextTool} onClick={() => insertLinePrefix('# ', '제목')}>
                      H1
                    </button>
                    <button type="button" className={styles.markdownTextTool} onClick={() => insertLinePrefix('## ', '소제목')}>
                      H2
                    </button>
                    <button type="button" className={styles.markdownTextTool} onClick={() => insertLinePrefix('### ', '소제목')}>
                      H3
                    </button>
                    <button type="button" className={styles.markdownTextTool} onClick={() => insertLinePrefix('#### ', '소제목')}>
                      H4
                    </button>
                    <span className={styles.markdownDivider} />
                    <button
                      type="button"
                      className={styles.markdownTextTool}
                      onClick={() => surroundSelection('**', '**', '강조 텍스트')}
                    >
                      B
                    </button>
                    <button
                      type="button"
                      className={styles.markdownTextTool}
                      onClick={() => surroundSelection('*', '*', '기울임 텍스트')}
                    >
                      I
                    </button>
                    <button type="button" className={styles.markdownTextTool} onClick={() => insertLinePrefix('- ', '항목')}>
                      •
                    </button>
                    <span className={styles.markdownDivider} />
                    <button type="button" className={styles.markdownTextTool} onClick={() => insertLinePrefix('> ', '인용문')}>
                      ❞
                    </button>
                    <button type="button" className={styles.markdownTextTool} onClick={handleInsertLink}>
                      🔗
                    </button>
                    <select
                      className={styles.markdownSelect}
                      value={selectedImageAlign}
                      onChange={event => setSelectedImageAlign(event.target.value as 'left' | 'center' | 'right')}
                      aria-label="이미지 정렬"
                      title="이미지 정렬"
                    >
                      <option value="left">좌</option>
                      <option value="center">중</option>
                      <option value="right">우</option>
                    </select>
                    <select
                      className={styles.markdownSelect}
                      value={selectedImageWidth}
                      onChange={event => setSelectedImageWidth(event.target.value as '40%' | '60%' | '80%' | '100%')}
                      aria-label="이미지 너비"
                      title="이미지 너비"
                    >
                      <option value="40%">40%</option>
                      <option value="60%">60%</option>
                      <option value="80%">80%</option>
                      <option value="100%">100%</option>
                    </select>
                    <select
                      className={styles.markdownSelect}
                      value={selectedImageHeight}
                      onChange={event =>
                        setSelectedImageHeight(event.target.value as 'auto' | '240px' | '320px' | '420px')
                      }
                      aria-label="이미지 높이"
                      title="이미지 높이"
                    >
                      <option value="auto">높이 자동</option>
                      <option value="240px">240px</option>
                      <option value="320px">320px</option>
                      <option value="420px">420px</option>
                    </select>
                    <button
                      type="button"
                      className={styles.markdownTextTool}
                      onClick={() => updateImageAttributesAtCursor(buildImageAttributes())}
                      title="이미지 크기/높이 적용"
                    >
                      적용
                    </button>
                    <input
                      ref={markdownImageInputRef}
                      className={styles.hiddenFileInput}
                      type="file"
                      accept="image/*"
                      onChange={handleUploadMarkdownImage}
                    />
                    <button
                      type="button"
                      className={styles.markdownImageButton}
                      onClick={() => markdownImageInputRef.current?.click()}
                      disabled={uploading}
                      title="이미지 삽입"
                      aria-label="이미지 삽입"
                    >
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M3 5.5A2.5 2.5 0 0 1 5.5 3h13A2.5 2.5 0 0 1 21 5.5v13a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 18.5v-13Zm2 0v10.86l4.12-4.12a1.5 1.5 0 0 1 2.12 0l1.76 1.76 2.38-2.38a1.5 1.5 0 0 1 2.12 0L19 13.12V5.5a.5.5 0 0 0-.5-.5h-13a.5.5 0 0 0-.5.5Zm14 12.58-2.56-2.56L14 18h4.5a.5.5 0 0 0 .5-.42ZM9 8.75a1.75 1.75 0 1 1 3.5 0 1.75 1.75 0 0 1-3.5 0Z" />
                      </svg>
                    </button>
                  </div>
                </div>
                {linkDialog.open ? (
                  <div className={styles.linkDialog}>
                    <p className={styles.linkDialogTitle}>링크 등록</p>
                    <div className={styles.linkDialogGrid}>
                      <input
                        className={styles.input}
                        value={linkDialog.text}
                        onChange={event => setLinkDialog(previous => ({ ...previous, text: event.target.value }))}
                        placeholder="한 줄 텍스트"
                      />
                      <input
                        className={styles.input}
                        value={linkDialog.url}
                        onChange={event => setLinkDialog(previous => ({ ...previous, url: event.target.value }))}
                        placeholder="주소를 입력하세요"
                      />
                      <select
                        className={styles.select}
                        value={linkDialog.target}
                        onChange={event =>
                          setLinkDialog(previous => ({
                            ...previous,
                            target: event.target.value as '_self' | '_blank',
                          }))
                        }
                      >
                        <option value="_self">현재 창(_self)</option>
                        <option value="_blank">새 창(_blank)</option>
                      </select>
                      <div className={styles.linkDialogActions}>
                        <button type="button" className={styles.primaryButton} onClick={applyLinkDialog}>
                          확인
                        </button>
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          onClick={() => setLinkDialog(previous => ({ ...previous, open: false }))}
                        >
                          취소
                        </button>
                      </div>
                    </div>
                  </div>
                ) : null}
                <textarea
                  ref={markdownTextareaRef}
                  className={styles.textarea}
                  value={form.markdown}
                  onChange={event => setForm(previous => ({ ...previous, markdown: event.target.value }))}
                  rows={16}
                  placeholder="당신의 이야기를 적어보세요..."
                  required
                />
              </label>
            </div>

            <div className={`${styles.formActions} ${styles.editorActions}`}>
              <button type="submit" className={styles.primaryButton} disabled={saving || uploading}>
                {saving ? '저장 중...' : isEdit ? '수정 저장' : '등록 저장'}
              </button>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void handleSaveDraft()}
                disabled={saving || uploading}
              >
                임시저장
              </button>
            </div>
          </form>

          <section className={styles.previewSection}>
            <div className={styles.previewHeader}>
              <h3 className={styles.previewTitle}>마크다운 미리보기</h3>
              {liveTags.length > 0 ? (
                <div className={styles.previewTagsRight}>
                  {liveTags.map(tag => (
                    <button
                      key={`preview-${tag}`}
                      type="button"
                      className={styles.tagChip}
                      onClick={() => handleRemoveTag(tag)}
                      title="클릭하면 태그가 삭제됩니다."
                    >
                      #{tag.replace(/^#/, '').replace(/_/g, ' ')}
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
            <div className={styles.previewContent} dangerouslySetInnerHTML={{ __html: previewHtml }} />
          </section>
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminEventEditorPage;
