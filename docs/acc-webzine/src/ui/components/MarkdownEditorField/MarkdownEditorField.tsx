import { useMemo, useRef, useState, type ChangeEvent } from 'react';
import { renderIdolGlowMarkdown } from '../../../shared/markdown/idolGlowMarkdown';
import styles from './MarkdownEditorField.module.css';

interface MarkdownEditorFieldProps {
  readonly label: string;
  readonly value: string;
  readonly onChange: (value: string) => void;
  readonly placeholder?: string;
  readonly minHeight?: number;
  readonly tagsValue?: string;
  readonly onTagsChange?: (value: string) => void;
  readonly tagsPlaceholder?: string;
  readonly onUploadImage?: (file: File) => Promise<string>;
  readonly showPreview?: boolean;
}

export function MarkdownEditorField({
  label,
  value,
  onChange,
  placeholder,
  minHeight = 320,
  tagsValue,
  onTagsChange,
  tagsPlaceholder = '태그를 입력하세요',
  onUploadImage,
  showPreview = true,
}: MarkdownEditorFieldProps) {
  const previewHtml = renderIdolGlowMarkdown(value);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const [uploading, setUploading] = useState(false);
  const [tagsError, setTagsError] = useState<string | null>(null);
  const [selectedImageAlign, setSelectedImageAlign] = useState<'left' | 'center' | 'right'>('center');
  const [selectedImageWidth, setSelectedImageWidth] = useState<'40%' | '60%' | '80%' | '100%'>('60%');
  const [selectedImageHeight, setSelectedImageHeight] = useState<'auto' | '240px' | '320px' | '420px'>('auto');

  const parsedTags = useMemo(() => {
    if (!tagsValue) {
      return [];
    }
    return Array.from(
      new Set(
        tagsValue
          .split(/[\s,]+/)
          .map(item => item.trim().replace(/^#/, ''))
          .filter(Boolean),
      ),
    );
  }, [tagsValue]);

  const updateAtSelection = (nextValue: string, start: number, end: number) => {
    const next = `${value.slice(0, start)}${nextValue}${value.slice(end)}`;
    onChange(next);
    requestAnimationFrame(() => {
      const target = textareaRef.current;
      if (!target) {
        return;
      }
      target.focus();
      const caret = start + nextValue.length;
      target.setSelectionRange(caret, caret);
    });
  };

  const surroundSelection = (prefix: string, suffix = '', fallbackText = '') => {
    const target = textareaRef.current;
    if (!target) {
      return;
    }
    const start = target.selectionStart ?? 0;
    const end = target.selectionEnd ?? 0;
    const selected = value.slice(start, end);
    updateAtSelection(`${prefix}${selected || fallbackText}${suffix}`, start, end);
  };

  const insertLinePrefix = (prefix: string, fallback = '') => {
    const target = textareaRef.current;
    if (!target) {
      return;
    }
    const start = target.selectionStart ?? 0;
    const end = target.selectionEnd ?? 0;
    const selected = value.slice(start, end);
    if (selected) {
      const selectedLines = selected
        .split('\n')
        .map(line => `${prefix}${line}`)
        .join('\n');
      updateAtSelection(selectedLines, start, end);
      return;
    }
    const lineStart = value.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
    updateAtSelection(`${prefix}${fallback}`, lineStart, lineStart);
  };

  const applyHeading = (level: 1 | 2 | 3 | 4, fallback = '제목') => {
    const target = textareaRef.current;
    if (!target) {
      return;
    }
    const start = target.selectionStart ?? 0;
    const end = target.selectionEnd ?? 0;
    const selected = value.slice(start, end);
    const prefix = `${'#'.repeat(level)} `;
    if (selected && !selected.includes('\n')) {
      updateAtSelection(`[[h${level}:${selected}]]`, start, end);
      return;
    }
    insertLinePrefix(prefix, fallback);
  };

  const insertAtCursor = (insertValue: string) => {
    const target = textareaRef.current;
    if (!target) {
      onChange(`${value}\n${insertValue}`.trimStart());
      return;
    }
    const start = target.selectionStart ?? value.length;
    const end = target.selectionEnd ?? value.length;
    updateAtSelection(insertValue, start, end);
  };

  const buildImageAttributes = (): string => {
    const attrs = [`align=${selectedImageAlign}`, `width=${selectedImageWidth}`];
    if (selectedImageHeight !== 'auto') {
      attrs.push(`height=${selectedImageHeight}`);
    }
    return attrs.join(' ');
  };

  const applyImageAttributes = () => {
    const target = textareaRef.current;
    if (!target) {
      return;
    }
    const start = target.selectionStart ?? 0;
    const lineStart = value.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
    const nextLineBreak = value.indexOf('\n', start);
    const lineEnd = nextLineBreak === -1 ? value.length : nextLineBreak;
    const line = value.slice(lineStart, lineEnd);
    const imageMatch = line.match(/^\s*!\[([^\]]*)\]\(([^)]+)\)(\{[^}]+\})?\s*$/);
    if (!imageMatch) {
      return;
    }
    const updatedLine = `![${imageMatch[1]}](${imageMatch[2]}){${buildImageAttributes()}}`;
    onChange(`${value.slice(0, lineStart)}${updatedLine}${value.slice(lineEnd)}`);
  };

  const handleUploadMarkdownImage = async (event: ChangeEvent<HTMLInputElement>) => {
    if (!onUploadImage) {
      return;
    }
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }
    setUploading(true);
    try {
      const uploadedUrl = await onUploadImage(file);
      insertAtCursor(`\n![이미지](${uploadedUrl}){${buildImageAttributes()}}\n`);
    } finally {
      setUploading(false);
    }
  };

  const handleInsertLink = () => {
    const target = textareaRef.current;
    if (!target) {
      return;
    }
    const start = target.selectionStart ?? 0;
    const end = target.selectionEnd ?? 0;
    const selected = value.slice(start, end).trim();
    const text = window.prompt('링크 텍스트', selected || '링크');
    if (!text) {
      return;
    }
    const url = window.prompt('링크 주소 (https://...)', 'https://');
    if (!url) {
      return;
    }
    insertAtCursor(`[${text}](${url}){target=_blank}`);
  };

  const removeTag = (tag: string) => {
    if (!onTagsChange) {
      return;
    }
    const nextTags = parsedTags.filter(item => item !== tag);
    onTagsChange(nextTags.join(' '));
  };

  const handleTagsInputChange = (rawValue: string) => {
    if (!onTagsChange) {
      return;
    }
    const normalizedUnderscore = rawValue.replace(/_/g, ' ');
    const hadForbiddenChar = /[^0-9A-Za-z가-힣\s]/.test(normalizedUnderscore);
    const normalized = normalizedUnderscore
      .replace(/[^0-9A-Za-z가-힣\s]/g, '')
      .replace(/\s+/g, ' ')
      .trimStart();
    onTagsChange(normalized);
    setTagsError(hadForbiddenChar ? '해시태그/특수기호는 입력할 수 없습니다. (공백으로 구분해 주세요)' : null);
  };

  return (
    <div className={`${styles.wrapper} ${showPreview ? '' : styles.singlePane}`}>
      <section className={styles.editorPane}>
        <div className={styles.markdownHeader}>
          <span className={styles.label}>{label}</span>
          <div className={styles.markdownTools}>
            <button type="button" className={styles.markdownTextTool} onClick={() => applyHeading(1, '제목')}>
              H1
            </button>
            <button type="button" className={styles.markdownTextTool} onClick={() => applyHeading(2, '소제목')}>
              H2
            </button>
            <button type="button" className={styles.markdownTextTool} onClick={() => applyHeading(3, '소제목')}>
              H3
            </button>
            <button type="button" className={styles.markdownTextTool} onClick={() => applyHeading(4, '소제목')}>
              H4
            </button>
            <span className={styles.markdownDivider} />
            <button type="button" className={styles.markdownTextTool} onClick={() => surroundSelection('**', '**', '강조 텍스트')}>
              B
            </button>
            <button type="button" className={styles.markdownTextTool} onClick={() => surroundSelection('*', '*', '기울임 텍스트')}>
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
              onChange={event => setSelectedImageHeight(event.target.value as 'auto' | '240px' | '320px' | '420px')}
              aria-label="이미지 높이"
              title="이미지 높이"
            >
              <option value="auto">높이 자동</option>
              <option value="240px">240px</option>
              <option value="320px">320px</option>
              <option value="420px">420px</option>
            </select>
            <button type="button" className={styles.markdownTextTool} title="이미지 크기/높이 적용" onClick={applyImageAttributes}>
              적용
            </button>
            {onUploadImage ? (
              <>
                <input
                  ref={imageInputRef}
                  className={styles.hiddenFileInput}
                  type="file"
                  accept="image/*"
                  onChange={handleUploadMarkdownImage}
                />
                <button
                  type="button"
                  className={styles.markdownImageButton}
                  onClick={() => imageInputRef.current?.click()}
                  disabled={uploading}
                  title="이미지 삽입"
                  aria-label="이미지 삽입"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M3 5.5A2.5 2.5 0 0 1 5.5 3h13A2.5 2.5 0 0 1 21 5.5v13a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 18.5v-13Zm2 0v10.86l4.12-4.12a1.5 1.5 0 0 1 2.12 0l1.76 1.76 2.38-2.38a1.5 1.5 0 0 1 2.12 0L19 13.12V5.5a.5.5 0 0 0-.5-.5h-13a.5.5 0 0 0-.5.5Zm14 12.58-2.56-2.56L14 18h4.5a.5.5 0 0 0 .5-.42ZM9 8.75a1.75 1.75 0 1 1 3.5 0 1.75 1.75 0 0 1-3.5 0Z" />
                  </svg>
                </button>
              </>
            ) : null}
          </div>
        </div>
        <textarea
          ref={textareaRef}
          className={styles.textarea}
          style={{ minHeight }}
          value={value}
          onChange={event => onChange(event.target.value)}
          placeholder={placeholder}
        />
        {onTagsChange ? (
          <label className={styles.tagField}>
            <input
              className={styles.editorTagInput}
              placeholder={tagsPlaceholder}
              value={tagsValue ?? ''}
              onChange={event => handleTagsInputChange(event.target.value)}
            />
            {tagsError ? <p className={styles.tagError}>{tagsError}</p> : null}
            <div className={styles.tagRow}>
              {parsedTags.map(tag => (
                <button
                  key={tag}
                  type="button"
                  className={styles.tagChip}
                  onClick={() => removeTag(tag)}
                  title="클릭하면 태그가 삭제됩니다."
                >
                  #{tag}
                </button>
              ))}
            </div>
          </label>
        ) : null}
      </section>

      {showPreview ? (
        <section className={styles.previewPane}>
          <header className={styles.paneHeader}>
            <h3 className={styles.title}>미리보기</h3>
          </header>
          <div
            className={styles.previewBody}
            style={{ minHeight }}
            dangerouslySetInnerHTML={{
              __html: previewHtml || `<p class="${styles.empty}">미리보기 내용이 없습니다.</p>`,
            }}
          />
        </section>
      ) : null}
    </div>
  );
}

export default MarkdownEditorField;
