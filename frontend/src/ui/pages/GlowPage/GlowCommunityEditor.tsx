import { type FormEvent, useEffect, useState } from "react";
import { apiErrorMessage } from "../../../shared/data/apiClient";
import { uploadCommentImage } from "../../../shared/data/contentCommentApi";
import {
  GLOW_COMMUNITY_TAGS,
  GLOW_COMMUNITY_TAG_LABELS,
  type GlowCommunityPost,
  type GlowCommunityTag,
  createGlowCommunityPost,
  isGlowCommunityTag,
  updateGlowCommunityPost,
} from "../../../shared/data/glowCommunityApi";
import { MarkdownEditorField } from "../../components/MarkdownEditorField/MarkdownEditorField";
import { SafeMarkdown } from "../../components/SafeMarkdown/SafeMarkdown";
import styles from "./GlowCommunity.module.css";

const TITLE_MAX = 150;
const CONTENT_MAX = 20000;
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = [
  "image/png",
  "image/jpeg",
  "image/gif",
  "image/webp",
];

type GlowCommunityEditorProps = {
  readonly accessToken: string;
  readonly post?: GlowCommunityPost | null;
  readonly defaultTag?: GlowCommunityTag;
  readonly onClose: () => void;
  readonly onSaved: (post: GlowCommunityPost, created: boolean) => void;
};

export function GlowCommunityEditor({
  accessToken,
  post,
  defaultTag = "talk",
  onClose,
  onSaved,
}: GlowCommunityEditorProps) {
  const editing = Boolean(post);
  const [tag, setTag] = useState<GlowCommunityTag>(
    post && isGlowCommunityTag(post.tag) ? post.tag : defaultTag,
  );
  const [title, setTitle] = useState(post?.title ?? "");
  const [content, setContent] = useState(post?.content ?? "");
  const [preview, setPreview] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", closeOnEscape);
      document.body.style.overflow = previousOverflow;
    };
  }, [onClose]);

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
      setError(apiErrorMessage(uploadError, "이미지를 올리지 못했습니다."));
      throw uploadError;
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedTitle = title.trim();
    const trimmedContent = content.trim();
    if (!trimmedTitle || !trimmedContent || submitting) return;
    if (trimmedContent.length > CONTENT_MAX) {
      setError(
        `본문은 ${CONTENT_MAX.toLocaleString()}자 이하로 입력해 주세요.`,
      );
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const input = { tag, title: trimmedTitle, content: trimmedContent };
      const saved = post
        ? await updateGlowCommunityPost(post.id, input, accessToken)
        : await createGlowCommunityPost(input, accessToken);
      onSaved(saved, !post);
    } catch (saveError) {
      setError(apiErrorMessage(saveError, "게시글을 저장하지 못했습니다."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.overlay} role="presentation" onMouseDown={onClose}>
      <form
        className={styles.editorDialog}
        // biome-ignore lint/a11y/useSemanticElements: custom overlay modal shared with existing GLOW popups
        role="dialog"
        aria-modal="true"
        aria-labelledby="community-editor-title"
        onMouseDown={(event) => event.stopPropagation()}
        onSubmit={(event) => void handleSubmit(event)}
      >
        <div className={styles.dialogHeader}>
          <h2 id="community-editor-title">{editing ? "글 수정" : "글쓰기"}</h2>
          <button type="button" aria-label="닫기" onClick={onClose}>
            ×
          </button>
        </div>
        <fieldset className={styles.editorTags}>
          <legend>말머리</legend>
          {GLOW_COMMUNITY_TAGS.map((option) => (
            <label
              key={option}
              className={tag === option ? styles.tagChipActive : styles.tagChip}
            >
              <input
                type="radio"
                name="community-tag"
                value={option}
                checked={tag === option}
                onChange={() => setTag(option)}
              />
              # {GLOW_COMMUNITY_TAG_LABELS[option]}
            </label>
          ))}
        </fieldset>
        <label className={styles.editorField}>
          <span>제목</span>
          <input
            value={title}
            maxLength={TITLE_MAX}
            required
            placeholder="제목을 입력해 주세요"
            onChange={(event) => setTitle(event.target.value)}
          />
        </label>
        <div className={styles.editorField}>
          <div
            className={styles.editorTabs}
            role="tablist"
            aria-label="본문 작성 방식"
          >
            <button
              type="button"
              role="tab"
              aria-selected={!preview}
              className={!preview ? styles.editorTabActive : styles.editorTab}
              onClick={() => setPreview(false)}
            >
              작성
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={preview}
              className={preview ? styles.editorTabActive : styles.editorTab}
              onClick={() => setPreview(true)}
            >
              미리보기
            </button>
            <span className={styles.editorCount}>
              {content.length.toLocaleString()} / {CONTENT_MAX.toLocaleString()}
            </span>
          </div>
          {preview ? (
            <div className={styles.editorPreview}>
              <SafeMarkdown
                markdown={content}
                emptyText="미리볼 내용이 없습니다."
              />
            </div>
          ) : (
            <MarkdownEditorField
              label="본문 (마크다운)"
              value={content}
              onChange={setContent}
              placeholder="내용을 입력해 주세요. 마크다운과 이미지를 쓸 수 있어요."
              minHeight={260}
              showPreview={false}
              onUploadImage={handleUpload}
            />
          )}
        </div>
        {error ? (
          <p className={styles.errorBox} role="alert">
            {error}
          </p>
        ) : null}
        <div className={styles.dialogActions}>
          <button
            type="button"
            className={styles.ghostButton}
            onClick={onClose}
          >
            취소
          </button>
          <button
            type="submit"
            className={styles.primaryButton}
            disabled={submitting || !title.trim() || !content.trim()}
          >
            {submitting ? "저장 중..." : editing ? "수정" : "등록"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default GlowCommunityEditor;
