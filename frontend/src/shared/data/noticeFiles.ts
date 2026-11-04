const NOTICE_FILES_PREFIX = "[notice-files]";
const IMAGE_FILE = /\.(jpe?g|png|gif|webp|svg|bmp|avif)(?:$|\?)/i;

export type NoticeAttachment = {
  readonly name: string;
  readonly url: string;
};

export function parseNoticeAttachments(
  introduction: string | null,
): NoticeAttachment[] {
  if (!introduction) return [];
  const trimmed = introduction.trim();
  if (!trimmed.startsWith(NOTICE_FILES_PREFIX)) return [];
  const body = trimmed.slice(NOTICE_FILES_PREFIX.length).trim();
  if (!body) return [];
  return body
    .split("|")
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => {
      const [name, url] = item.split("::");
      return {
        name: name?.trim() || "uploaded-file",
        url: url?.trim() || "",
      };
    })
    .filter((item) => item.url.length > 0);
}

export function isNoticeImage(file: NoticeAttachment): boolean {
  return IMAGE_FILE.test(file.name) || IMAGE_FILE.test(file.url);
}

export function firstNoticeImageUrl(
  introduction: string | null,
): string | null {
  const image = parseNoticeAttachments(introduction).find(isNoticeImage);
  return image?.url ?? null;
}
