const DISPLAY_DATE_TIME = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}/;
const ISO_LOCAL_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?$/;
const DISPLAY_DATE = /^\d{4}-\d{2}-\d{2}$/;
const DISPLAY_TIME = /^\d{2}:\d{2}(:\d{2})?$/;

function formatSeoulDateTime(epochMs: number): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(epochMs));
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((item) => item.type === type)?.value ?? '';
  return `${part('year')}-${part('month')}-${part('day')} ${part('hour')}:${part('minute')}`;
}

export function formatIdolGlowDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  const trimmed = value.trim();
  if (DISPLAY_DATE.test(trimmed)) return trimmed;
  if (DISPLAY_DATE_TIME.test(trimmed)) return trimmed.slice(0, 16);
  if (ISO_LOCAL_DATE_TIME.test(trimmed)) return trimmed.slice(0, 16).replace('T', ' ');
  const parsed = Date.parse(trimmed);
  if (!Number.isNaN(parsed)) return formatSeoulDateTime(parsed);
  return trimmed;
}

export function formatIdolGlowDate(value: string | null | undefined): string {
  if (!value) return '-';
  const trimmed = value.trim();
  if (DISPLAY_DATE.test(trimmed)) return trimmed;
  const formatted = formatIdolGlowDateTime(trimmed);
  if (formatted === '-') return formatted;
  return formatted.slice(0, 10);
}

export function formatIdolGlowTime(value: string | null | undefined): string {
  if (!value) return '-';
  const trimmed = value.trim();
  if (DISPLAY_TIME.test(trimmed)) return trimmed.slice(0, 5);
  const formatted = formatIdolGlowDateTime(trimmed);
  if (formatted.includes(' ') && formatted.length >= 16) return formatted.slice(11, 16);
  return trimmed;
}
