export const SEOUL_AREA_CODE = 11;

export const TOUR_AREA_OPTIONS: readonly { readonly label: string; readonly areaCode: number }[] = [
  { label: "서울특별시", areaCode: 11 },
];

const DISTRICT_ENTRIES: readonly { readonly label: string; readonly signguCode: number }[] = [
  { label: "강남구", signguCode: 11680 },
  { label: "강동구", signguCode: 11740 },
  { label: "강북구", signguCode: 11305 },
  { label: "강서구", signguCode: 11500 },
  { label: "관악구", signguCode: 11620 },
  { label: "광진구", signguCode: 11215 },
  { label: "구로구", signguCode: 11530 },
  { label: "금천구", signguCode: 11545 },
  { label: "노원구", signguCode: 11350 },
  { label: "도봉구", signguCode: 11320 },
  { label: "동대문구", signguCode: 11230 },
  { label: "동작구", signguCode: 11590 },
  { label: "마포구", signguCode: 11440 },
  { label: "서대문구", signguCode: 11410 },
  { label: "서초구", signguCode: 11650 },
  { label: "성동구", signguCode: 11200 },
  { label: "성북구", signguCode: 11290 },
  { label: "송파구", signguCode: 11710 },
  { label: "양천구", signguCode: 11470 },
  { label: "영등포구", signguCode: 11560 },
  { label: "용산구", signguCode: 11170 },
  { label: "은평구", signguCode: 11380 },
  { label: "종로구", signguCode: 11110 },
  { label: "중구", signguCode: 11140 },
  { label: "중랑구", signguCode: 11260 },
];

export const SEOUL_DISTRICT_SIGNGU_OPTIONS: readonly { readonly label: string; readonly signguCode: number }[] =
  [...DISTRICT_ENTRIES].sort((a, b) => a.label.localeCompare(b.label, "ko"));

export function getDefaultTourBaseYmSeoul(): string {
  const formatter = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "numeric",
  });
  const parts = formatter.formatToParts(new Date());
  const year = Number(parts.find(part => part.type === "year")?.value ?? 0);
  const month = Number(parts.find(part => part.type === "month")?.value ?? 1);
  let resolvedYear = year;
  let resolvedMonth = month - 1;
  if (resolvedMonth < 1) {
    resolvedMonth = 12;
    resolvedYear -= 1;
  }
  return `${resolvedYear}${String(resolvedMonth).padStart(2, "0")}`;
}

export function composeTourBaseYm(year: number, month: number): string {
  const safeYear = Number.isFinite(year) ? Math.trunc(year) : new Date().getFullYear();
  const safeMonth = Number.isFinite(month) ? Math.min(12, Math.max(1, Math.trunc(month))) : 1;
  return `${safeYear}${String(safeMonth).padStart(2, "0")}`;
}

export function parseTourBaseYm(baseYm: string): { year: number; month: number } | null {
  const trimmed = baseYm.trim();
  if (!/^\d{6}$/.test(trimmed)) {
    return null;
  }
  return {
    year: Number(trimmed.slice(0, 4)),
    month: Number(trimmed.slice(4, 6)),
  };
}

export function formatTourSpotLabel(item: {
  readonly areaName: string | null;
  readonly signguName: string | null;
  readonly name: string;
}): string {
  const region = [item.areaName, item.signguName].filter(Boolean).join(" ");
  const name = item.name.trim();
  if (region && name) {
    return `${region} ${name}`;
  }
  if (name) {
    return name;
  }
  return region;
}
