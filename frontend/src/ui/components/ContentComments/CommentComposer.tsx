import { type FormEvent, useState } from "react";
import { uploadCommentImage } from "../../../shared/data/contentCommentApi";
import { MarkdownEditorField } from "../MarkdownEditorField/MarkdownEditorField";
import { SafeMarkdown } from "../SafeMarkdown/SafeMarkdown";
import styles from "./ContentComments.module.css";

const MAX_LENGTH = 5000;
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = [
  "image/png",
  "image/jpeg",
  "image/gif",
  "image/webp",
];

type CommentComposerProps = {
  readonly accessToken: string;
  readonly initialValue?: string;
  readonly label: string;
  readonly placeholder?: string;
  readonly submitLabel: string;
  readonly minHeight?: number;
  readonly onSubmit: (markdown: string) => Promise<void>;
  readonly onCancel?: () => void;
};

export function CommentComposer({
  accessToken,
  initialValue = "",
  label,
  placeholder,
  submitLabel,
  minHeight = 110,
  onSubmit,
  onCancel,
}: CommentComposerProps) {
  const [value, setValue] = useState(initialValue);
  const [preview, setPreview] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleUpload = async (file: File): Promise<string> => {
    setError(null);
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      const message = "PNG, JPG, GIF, WEBP 이미지만 올릴 수 있습니다.";
      setError(message);
      throw new Error(message);
    }
    if (file.size > MAX_IMAGE_BYTES) {
      const message = "이미지는 10MB 이하만 올릴 수 있습니다.";
      setError(message);
      throw new Error(message);
    }
    try {
      return await uploadCommentImage(file, accessToken);
    } catch (uploadError) {
      const message =
        uploadError instanceof Error
          ? uploadError.message
          : "이미지를 올리지 못했습니다.";
      setError(message);
      throw uploadError;
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = value.trim();
    if (!trimmed || submitting) return;
    if (trimmed.length > MAX_LENGTH) {
      setError(`댓글은 ${MAX_LENGTH.toLocaleString()}자 이하로 입력해 주세요.`);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit(trimmed);
      setValue("");
      setPreview(false);
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "댓글을 저장하지 못했습니다.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form
      className={styles.composer}
      onSubmit={(event) => void handleSubmit(event)}
    >
      <div
        className={styles.composerTabs}
        role="tablist"
        aria-label={`${label} 작성 방식`}
      >
        <button
          type="button"
          role="tab"
          aria-selected={!preview}
          className={!preview ? styles.composerTabActive : styles.composerTab}
          onClick={() => setPreview(false)}
        >
          작성
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={preview}
          className={preview ? styles.composerTabActive : styles.composerTab}
          onClick={() => setPreview(true)}
        >
          미리보기
        </button>
        <span className={styles.composerCount}>
          {value.length.toLocaleString()} / {MAX_LENGTH.toLocaleString()}
        </span>
      </div>
      {preview ? (
        <div className={styles.composerPreview} style={{ minHeight }}>
          <SafeMarkdown
            markdown={value}
            compact
            emptyText="미리볼 내용이 없습니다."
          />
        </div>
      ) : (
        <MarkdownEditorField
          label={label}
          value={value}
          onChange={setValue}
          placeholder={placeholder}
          minHeight={minHeight}
          showPreview={false}
          onUploadImage={handleUpload}
        />
      )}
      {error ? (
        <p className={styles.formError} role="alert">
          {error}
        </p>
      ) : null}
      <div className={styles.composerActions}>
        {onCancel ? (
          <button
            type="button"
            className={styles.ghostButton}
            onClick={onCancel}
          >
            취소
          </button>
        ) : null}
        <button
          type="submit"
          className={styles.primaryButton}
          disabled={submitting || !value.trim()}
        >
          {submitting ? "저장 중..." : submitLabel}
        </button>
      </div>
    </form>
  );
}

export default CommentComposer;
