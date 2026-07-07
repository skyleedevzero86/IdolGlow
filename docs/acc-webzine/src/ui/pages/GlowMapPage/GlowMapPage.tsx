import { useCallback, useEffect, useMemo, useRef, useState, type KeyboardEvent } from "react";
import { useAuth } from "../../../auth/AuthContext";
import {
  fetchAdminProduct,
  fetchAdminProducts,
  type AdminProductSummary,
  type ProductLocationSummary,
  type ProductTourAttractionItem,
} from "../../../shared/data/adminBookingApi";
import {
  fetchFestivalEventsByDate,
  type FestivalEventItem,
} from "../../../shared/data/festivalInfoApi";
import { useInfiniteScroll } from "../../hooks/useInfiniteScroll";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import styles from "./GlowMapPage.module.css";

type KakaoLatLng = unknown;

type KakaoMap = {
  addControl: (control: unknown, position: unknown) => void;
  panTo: (latLng: KakaoLatLng) => void;
  relayout: () => void;
  setCenter: (latLng: KakaoLatLng) => void;
};

type KakaoOverlay = {
  setMap: (map: KakaoMap | null) => void;
};

type KakaoMaps = {
  load: (callback: () => void) => void;
  Map: new (
    container: HTMLElement,
    options: {
      center: KakaoLatLng;
      level: number;
      scrollwheel?: boolean;
    },
  ) => KakaoMap;
  LatLng: new (lat: number, lng: number) => KakaoLatLng;
  ZoomControl: new () => unknown;
  CustomOverlay: new (options: {
    position: KakaoLatLng;
    content: HTMLElement;
    yAnchor?: number;
    zIndex?: number;
  }) => KakaoOverlay;
  ControlPosition: {
    RIGHT: unknown;
  };
};

declare global {
  interface Window {
    kakao?: {
      maps: KakaoMaps;
    };
  }
}

const KAKAO_MAP_APP_KEY = import.meta.env.VITE_KAKAO_MAP_APP_KEY?.trim() ?? "";
const KAKAO_SDK_ID = "kakao-map-sdk";
const RESULT_BATCH_SIZE = 8;
const EVENT_INFO_PAGE_SIZE = 30;
const EVENT_INFO_MAX_PAGES = 7;
const PRODUCT_RESULT_LIMIT = 200;
let kakaoMapLoader: Promise<KakaoMaps> | null = null;

type MapCoords = {
  readonly lat: number;
  readonly lng: number;
};

type MarkerVariant = "event" | "product" | "tour";

type GlowMapResultBase = {
  readonly id: string;
  readonly title: string;
  readonly categoryLabel: string;
  readonly filterLabels?: readonly string[];
  readonly markerLabel: string;
  readonly markerVariant: MarkerVariant;
  readonly coords: MapCoords | null;
  readonly image: string | null;
};

type EventResult = GlowMapResultBase & {
  readonly kind: "event";
  readonly period: string;
  readonly address: string | null;
  readonly source: string;
  readonly detailUrl: string | null;
  readonly synopsis: string | null;
};

type ProductResult = GlowMapResultBase & {
  readonly kind: "product";
  readonly description: string;
  readonly address: string;
  readonly priceLabel: string;
  readonly tags: readonly string[];
  readonly detailPath: string;
};

type ProductPickResult = GlowMapResultBase & {
  readonly kind: "product-pick";
  readonly productName: string;
  readonly areaLabel: string;
  readonly reason: string;
  readonly rank: number;
  readonly score: number;
};

type GlowMapResult =
  | EventResult
  | ProductResult
  | ProductPickResult;

const GLOW_MAP_CENTER = {
  lat: 34.74028,
  lng: 127.73596,
};

function dateToApi(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}`;
}

function toInputDate(yyyymmdd: string | null): string | null {
  if (!yyyymmdd || !/^\d{8}$/.test(yyyymmdd)) return yyyymmdd;
  return `${yyyymmdd.slice(0, 4)}-${yyyymmdd.slice(4, 6)}-${yyyymmdd.slice(6, 8)}`;
}

function formatPeriod(start: string | null, end: string | null): string {
  if (!start && !end) return "일정 정보 없음";
  return `${toInputDate(start) ?? "-"} ~ ${toInputDate(end) ?? "-"}`;
}

function dedupeLabels(labels: readonly (string | null | undefined)[]): readonly string[] {
  return [...new Set(labels.map((label) => label?.trim()).filter((label): label is string => Boolean(label)))];
}

function labelsFor(item: GlowMapResult): readonly string[] {
  return item.filterLabels?.length ? item.filterLabels : [item.categoryLabel];
}

function formatWon(value: number): string {
  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: "KRW",
    maximumFractionDigits: 0,
  }).format(value);
}

function coordsFromProductLocation(location: ProductLocationSummary | null): MapCoords | null {
  if (!location) return null;
  const lat = typeof location.latitude === "number" ? location.latitude : Number(location.latitude);
  const lng = typeof location.longitude === "number" ? location.longitude : Number(location.longitude);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  return { lat, lng };
}

function coordsFromTourPick(pick: ProductTourAttractionItem): MapCoords | null {
  if (pick.mapX == null || pick.mapY == null) return null;
  if (!Number.isFinite(pick.mapX) || !Number.isFinite(pick.mapY)) return null;
  return { lat: pick.mapY, lng: pick.mapX };
}

function productAddressOf(product: AdminProductSummary): string {
  const displayAddress = product.location?.displayAddress?.trim();
  if (displayAddress && displayAddress !== "—") return displayAddress;
  return product.location?.name?.trim() || "위치 정보 없음";
}

function productTagLabels(product: AdminProductSummary): readonly string[] {
  return dedupeLabels(product.tagNames);
}

function productToGlowMapResult(product: AdminProductSummary): ProductResult {
  const tags = productTagLabels(product);
  const categoryLabel = tags[0] ?? "상품예약";
  return {
    kind: "product",
    id: `product-${product.id}`,
    title: product.name,
    categoryLabel,
    filterLabels: dedupeLabels(["상품예약", ...tags]),
    markerLabel: "예약",
    markerVariant: "product",
    coords: coordsFromProductLocation(product.location),
    image: product.thumbnailUrl,
    description: product.description,
    address: productAddressOf(product),
    priceLabel: formatWon(product.totalPrice),
    tags,
    detailPath: `/products/${product.id}`,
  };
}

function productPickToGlowMapResult(
  product: AdminProductSummary,
  pick: ProductTourAttractionItem,
): ProductPickResult {
  const categoryLabel = pick.categoryMiddle?.trim() || pick.categoryLarge?.trim() || "상품 연계장소";
  const areaLabel = [pick.areaName, pick.signguName]
    .map((label) => label?.trim())
    .filter(Boolean)
    .join(" ");

  return {
    kind: "product-pick",
    id: `product-${product.id}-pick-${pick.attractionCode || pick.rank || pick.name}`,
    title: pick.name,
    categoryLabel,
    filterLabels: dedupeLabels([
      "상품 연계장소",
      "관광명소",
      pick.categoryLarge,
      pick.categoryMiddle,
    ]),
    markerLabel: "명소",
    markerVariant: "tour",
    coords: coordsFromTourPick(pick),
    image: product.thumbnailUrl,
    productName: product.name,
    areaLabel: areaLabel || "지역 정보 없음",
    reason: pick.reason,
    rank: pick.rank,
    score: pick.score,
  };
}

function eventCategoryOf(event: FestivalEventItem): string {
  const category = event.category?.trim();
  if (category) return category;
  if (event.source === "KOPIS" || event.source === "SJW") return "공연";
  return "행사정보";
}

function eventToGlowMapResult(event: FestivalEventItem): EventResult {
  const categoryLabel = eventCategoryOf(event);
  const hasCoords =
    event.mapX != null &&
    event.mapY != null &&
    Number.isFinite(event.mapX) &&
    Number.isFinite(event.mapY);
  return {
    kind: "event",
    id: `event-${event.source}-${event.contentId}`,
    title: event.title,
    categoryLabel,
    filterLabels: dedupeLabels(["행사정보", categoryLabel]),
    markerLabel: "행사",
    markerVariant: "event",
    coords: hasCoords ? { lat: event.mapY!, lng: event.mapX! } : null,
    image: event.thumbnailImageUrl || event.imageUrl,
    period: formatPeriod(event.eventStartDate, event.eventEndDate),
    address: event.address,
    source: event.source,
    detailUrl: event.detailUrl,
    synopsis: event.synopsis,
  };
}

function fetchFestivalEventsByDateWithTimeout(
  accessToken: string,
  date: string,
  pageNo: number,
): Promise<readonly FestivalEventItem[]> {
  return fetchFestivalEventsByDate(accessToken, date, {
    pageNo,
    numOfRows: EVENT_INFO_PAGE_SIZE,
  });
}

function mergeEventResults(
  previous: readonly EventResult[],
  next: readonly EventResult[],
): readonly EventResult[] {
  return [...new Map([...previous, ...next].map((item) => [item.id, item])).values()];
}

function eventInfoErrorMessage(cause: unknown): string {
  if (cause instanceof Error && /failed to fetch|network/i.test(cause.message)) {
    return "행사 정보 API에 연결하지 못했습니다. 백엔드 실행 상태와 프록시 설정을 확인해 주세요.";
  }
  return cause instanceof Error ? cause.message : "행사 정보를 불러오지 못했습니다.";
}

function loadKakaoMaps(appKey: string): Promise<KakaoMaps> {
  if (window.kakao?.maps) {
    return new Promise((resolve) => window.kakao?.maps.load(() => resolve(window.kakao!.maps)));
  }

  if (kakaoMapLoader) {
    return kakaoMapLoader;
  }

  kakaoMapLoader = new Promise<KakaoMaps>((resolve, reject) => {
    const existingScript = document.getElementById(KAKAO_SDK_ID) as HTMLScriptElement | null;
    const script = existingScript ?? document.createElement("script");

    script.id = KAKAO_SDK_ID;
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false`;

    script.addEventListener("load", () => {
      if (!window.kakao?.maps) {
        reject(new Error("Kakao 지도 SDK를 불러오지 못했습니다."));
        return;
      }
      window.kakao.maps.load(() => resolve(window.kakao!.maps));
    });
    script.addEventListener("error", () => reject(new Error("Kakao 지도 SDK 로드에 실패했습니다.")));

    if (!existingScript) {
      document.head.append(script);
    }
  }).catch((error: unknown) => {
    kakaoMapLoader = null;
    throw error;
  });

  return kakaoMapLoader;
}

function imageOf(item: GlowMapResult): string | null {
  return item.image;
}

function zIndexOf(item: GlowMapResult, selected: boolean): number {
  if (selected) return 10;
  if (item.kind === "event") return 5;
  if (item.kind === "product") return 4;
  return 3;
}

function createMarkerElement(
  item: GlowMapResult,
  selected: boolean,
  onSelectResult: (id: string) => void,
): HTMLElement {
  const button = document.createElement("button");
  button.type = "button";
  button.className = [
    styles.kakaoMarker,
    item.markerVariant === "event" ? styles.kakaoMarkerEvent : "",
    item.markerVariant === "product" ? styles.kakaoMarkerProduct : "",
    item.markerVariant === "tour" ? styles.kakaoMarkerTour : "",
    selected ? styles.kakaoMarkerSelected : "",
  ]
    .filter(Boolean)
    .join(" ");
  button.setAttribute("aria-label", item.title);
  button.title = item.title;
  button.addEventListener("click", (event) => {
    event.preventDefault();
    event.stopPropagation();
    onSelectResult(item.id);
  });

  const image = imageOf(item);
  if (selected && image) {
    const thumb = document.createElement("img");
    thumb.src = image;
    thumb.alt = "";
    button.append(thumb);
  } else {
    const text = document.createElement("span");
    text.textContent = item.markerLabel;
    button.append(text);
  }

  return button;
}

function KakaoGlowMap({
  results,
  selectedResultId,
  onSelectResult,
}: {
  readonly results: readonly GlowMapResult[];
  readonly selectedResultId: string | null;
  readonly onSelectResult: (id: string) => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const overlaysRef = useRef<KakaoOverlay[]>([]);
  const [mapsApi, setMapsApi] = useState<KakaoMaps | null>(null);
  const [status, setStatus] = useState<"missing-key" | "loading" | "ready" | "error">(
    KAKAO_MAP_APP_KEY ? "loading" : "missing-key",
  );

  useEffect(() => {
    if (!KAKAO_MAP_APP_KEY) {
      setStatus("missing-key");
      return;
    }

    const container = containerRef.current;
    if (!container) return;

    let cancelled = false;
    let resizeObserver: ResizeObserver | null = null;
    let overlays: KakaoOverlay[] = [];

    setStatus("loading");
    void loadKakaoMaps(KAKAO_MAP_APP_KEY)
      .then((maps) => {
        if (cancelled) return;

        const center = new maps.LatLng(GLOW_MAP_CENTER.lat, GLOW_MAP_CENTER.lng);
        const map = new maps.Map(container, {
          center,
          level: 4,
          scrollwheel: true,
        });
        mapRef.current = map;

        map.addControl(new maps.ZoomControl(), maps.ControlPosition.RIGHT);

        resizeObserver = new ResizeObserver(() => {
          map.relayout();
          map.setCenter(center);
        });
        resizeObserver.observe(container);

        window.setTimeout(() => {
          map.relayout();
          map.setCenter(center);
        }, 120);

        setMapsApi(maps);
        setStatus("ready");
      })
      .catch(() => {
        if (!cancelled) {
          setStatus("error");
        }
      });

    return () => {
      cancelled = true;
      resizeObserver?.disconnect();
      overlaysRef.current.forEach((overlay) => overlay.setMap(null));
      overlaysRef.current = [];
      overlays.forEach((overlay) => overlay.setMap(null));
      mapRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapsApi || !mapRef.current) return;
    const map = mapRef.current;

    overlaysRef.current.forEach((overlay) => overlay.setMap(null));
    overlaysRef.current = results
      .filter((item) => item.coords != null)
      .map((item) => {
        const selected = item.id === selectedResultId;
        const overlay = new mapsApi.CustomOverlay({
          position: new mapsApi.LatLng(item.coords!.lat, item.coords!.lng),
          content: createMarkerElement(item, selected, onSelectResult),
          yAnchor: selected ? 1.08 : 1,
          zIndex: zIndexOf(item, selected),
        });
        overlay.setMap(map);
        return overlay;
      });

    const selectedItem = results.find((item) => item.id === selectedResultId && item.coords);
    if (selectedItem?.coords) {
      map.panTo(new mapsApi.LatLng(selectedItem.coords.lat, selectedItem.coords.lng));
    }

    return () => {
      overlaysRef.current.forEach((overlay) => overlay.setMap(null));
      overlaysRef.current = [];
    };
  }, [mapsApi, onSelectResult, results, selectedResultId]);

  return (
    <>
      <div ref={containerRef} className={styles.kakaoMap} aria-hidden={status !== "ready"} />
      {status !== "ready" ? (
        <div className={styles.mapState} role={status === "error" ? "alert" : "status"}>
          <strong>
            {status === "missing-key"
              ? "Kakao 지도 앱 키가 필요합니다."
              : status === "error"
                ? "Kakao 지도를 불러오지 못했습니다."
                : "Kakao 지도를 불러오는 중입니다."}
          </strong>
          <span>
            {status === "missing-key"
              ? ".env.development에 VITE_KAKAO_MAP_APP_KEY를 추가하면 실제 Kakao 지도가 표시됩니다."
              : status === "error"
                ? "앱 키, 도메인 등록, 네트워크 상태를 확인해 주세요."
              : "잠시만 기다려 주세요."}
          </span>
        </div>
      ) : null}
    </>
  );
}

function ToolbarFilters({
  filters,
  activeFilter,
  onSelectFilter,
  onClearFilter,
}: {
  readonly filters: readonly string[];
  readonly activeFilter: string | null;
  readonly onSelectFilter: (filter: string) => void;
  readonly onClearFilter: () => void;
}) {
  return (
    <div className={styles.filterRail} aria-label="Glow 지도 필터">
      {filters.map((filter) => (
        <button
          key={filter}
          type="button"
          className={`${styles.filterChip} ${activeFilter === filter ? styles.filterChipActive : ""}`}
          onClick={() => onSelectFilter(filter)}
          aria-pressed={activeFilter === filter}
        >
          {filter}
        </button>
      ))}
      <label className={styles.cleanToggle}>
        <input type="checkbox" checked={!activeFilter} onChange={onClearFilter} />
        <span>모두 정리</span>
      </label>
    </div>
  );
}

function handleCardKeyDown(event: KeyboardEvent<HTMLElement>, onSelect: () => void) {
  if (event.key !== "Enter" && event.key !== " ") return;
  event.preventDefault();
  onSelect();
}

function EventResultCard({
  event,
  selected,
  onSelect,
}: {
  readonly event: EventResult;
  readonly selected: boolean;
  readonly onSelect: () => void;
}) {
  return (
    <article
      className={`${styles.placeCard} ${styles.eventResultCard} ${selected ? styles.resultCardSelected : ""}`}
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(keyboardEvent) => handleCardKeyDown(keyboardEvent, onSelect)}
      aria-pressed={selected}
    >
      <div className={styles.eventThumb}>
        {event.image ? (
          <img src={event.image} alt="" loading="lazy" decoding="async" />
        ) : (
          <span>행사</span>
        )}
      </div>
      <div className={styles.eventCopy}>
        <span>{event.categoryLabel}</span>
        <h3>{event.title}</h3>
        <p>{event.period}</p>
        <small>{event.address || "장소 정보 없음"}</small>
      </div>
    </article>
  );
}

function ProductResultCard({
  product,
  selected,
  onSelect,
}: {
  readonly product: ProductResult;
  readonly selected: boolean;
  readonly onSelect: () => void;
}) {
  return (
    <article
      className={`${styles.placeCard} ${styles.productResultCard} ${selected ? styles.resultCardSelected : ""}`}
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(keyboardEvent) => handleCardKeyDown(keyboardEvent, onSelect)}
      aria-pressed={selected}
    >
      <div className={styles.eventThumb}>
        {product.image ? (
          <img src={product.image} alt="" loading="lazy" decoding="async" />
        ) : (
          <span>예약</span>
        )}
      </div>
      <div className={styles.productCopy}>
        <span>{product.categoryLabel}</span>
        <h3>{product.title}</h3>
        <p>{product.priceLabel}</p>
        <small>{product.address}</small>
        {product.tags.length > 0 ? <em>{product.tags.join(", ")}</em> : null}
      </div>
    </article>
  );
}

function ProductPickResultCard({
  pick,
  selected,
  onSelect,
}: {
  readonly pick: ProductPickResult;
  readonly selected: boolean;
  readonly onSelect: () => void;
}) {
  return (
    <article
      className={`${styles.placeCard} ${styles.productPickCard} ${selected ? styles.resultCardSelected : ""}`}
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(keyboardEvent) => handleCardKeyDown(keyboardEvent, onSelect)}
      aria-pressed={selected}
    >
      <div className={styles.simplePlaceIcon} aria-hidden="true">
        명소
      </div>
      <div className={styles.simplePlaceCopy}>
        <span>{pick.categoryLabel}</span>
        <h3>{pick.title}</h3>
        <p>{pick.productName} 연계 장소 · {pick.areaLabel}</p>
        <small>{pick.reason || `추천 점수 ${pick.score.toFixed(1)}`}</small>
      </div>
    </article>
  );
}

function ResultCard({
  item,
  selected,
  onSelect,
}: {
  readonly item: GlowMapResult;
  readonly selected: boolean;
  readonly onSelect: () => void;
}) {
  if (item.kind === "event") {
    return <EventResultCard event={item} selected={selected} onSelect={onSelect} />;
  }
  if (item.kind === "product") {
    return <ProductResultCard product={item} selected={selected} onSelect={onSelect} />;
  }
  if (item.kind === "product-pick") {
    return <ProductPickResultCard pick={item} selected={selected} onSelect={onSelect} />;
  }
  return null;
}

export const GlowMapPage = () => {
  const { accessToken, authReady } = useAuth();
  const [eventResults, setEventResults] = useState<readonly EventResult[]>([]);
  const [eventLoading, setEventLoading] = useState(false);
  const [eventError, setEventError] = useState<string | null>(null);
  const [productResults, setProductResults] = useState<readonly (ProductResult | ProductPickResult)[]>([]);
  const [productLoading, setProductLoading] = useState(false);
  const [productError, setProductError] = useState<string | null>(null);
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [selectedResultId, setSelectedResultId] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady) return;
    if (!accessToken) {
      setEventResults([]);
      setEventError("로그인이 필요합니다.");
      return;
    }

    let cancelled = false;
    setEventLoading(true);
    setEventError(null);

    void (async () => {
      const requestDate = dateToApi(new Date());
      const collected: EventResult[] = [];

      try {
        for (let pageNo = 1; pageNo <= EVENT_INFO_MAX_PAGES; pageNo += 1) {
          const rows = await fetchFestivalEventsByDateWithTimeout(accessToken, requestDate, pageNo);
          if (cancelled) return;

          const mapped = rows.map(eventToGlowMapResult);
          collected.push(...mapped);
          setEventResults((previous) => mergeEventResults(previous, mapped));

          if (pageNo === 1) {
            setEventLoading(false);
          }

          if (rows.length < EVENT_INFO_PAGE_SIZE) {
            break;
          }
        }

        if (!cancelled && collected.length === 0) {
          setEventError("오늘 표시할 행사 정보가 없습니다.");
        }
      } catch (cause) {
        if (cancelled) return;
        if (collected.length === 0) {
          setEventResults([]);
          setEventError(eventInfoErrorMessage(cause));
        } else {
          setEventError("일부 행사 정보만 불러왔습니다. 잠시 후 다시 시도해 주세요.");
        }
      } finally {
        if (!cancelled) {
          setEventLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady]);

  useEffect(() => {
    if (!authReady) return;

    let cancelled = false;
    setProductLoading(true);
    setProductError(null);

    void (async () => {
      try {
        const products = await fetchAdminProducts(accessToken, undefined, {
          limit: PRODUCT_RESULT_LIMIT,
        });
        if (cancelled) return;

        const productCards = products.map(productToGlowMapResult);
        const productsWithPicks = products.filter((product) => product.tourAttractionPickCount > 0);
        const detailResults = await Promise.allSettled(
          productsWithPicks.map((product) => fetchAdminProduct(accessToken, product.id)),
        );
        if (cancelled) return;

        const pickCards = detailResults.flatMap((result, index) => {
          if (result.status !== "fulfilled") return [];
          const product = productsWithPicks[index];
          return (result.value.tourAttractionPicks ?? []).map((pick) =>
            productPickToGlowMapResult(product, pick),
          );
        });

        setProductResults([...productCards, ...pickCards]);

        if (detailResults.some((result) => result.status === "rejected")) {
          setProductError("일부 상품 연계장소를 불러오지 못했습니다.");
        }
      } catch (cause) {
        if (cancelled) return;
        setProductResults([]);
        setProductError(cause instanceof Error ? cause.message : "상품예약 정보를 불러오지 못했습니다.");
      } finally {
        if (!cancelled) {
          setProductLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady]);

  const allResults = useMemo(
    () => [...eventResults, ...productResults],
    [eventResults, productResults],
  );

  const filterOptions = useMemo(() => {
    const labels = new Set<string>();
    allResults.forEach((item) => labelsFor(item).forEach((label) => labels.add(label)));
    return [...labels];
  }, [allResults]);

  const filteredResults = useMemo(
    () => (activeFilter ? allResults.filter((item) => labelsFor(item).includes(activeFilter)) : allResults),
    [activeFilter, allResults],
  );

  const [visibleCount, setVisibleCount] = useState(() =>
    Math.min(RESULT_BATCH_SIZE, filteredResults.length),
  );
  const hasMoreResults = visibleCount < filteredResults.length;

  const loadMoreResults = useCallback(() => {
    setVisibleCount((current) => Math.min(current + RESULT_BATCH_SIZE, filteredResults.length));
  }, [filteredResults.length]);

  const { observerRef } = useInfiniteScroll(loadMoreResults, {
    rootMargin: "160px",
    threshold: 0.01,
  });

  useEffect(() => {
    setVisibleCount(Math.min(RESULT_BATCH_SIZE, filteredResults.length));
  }, [activeFilter, filteredResults.length]);

  useEffect(() => {
    if (!activeFilter) return;
    if (filterOptions.includes(activeFilter)) return;
    setActiveFilter(null);
  }, [activeFilter, filterOptions]);

  useEffect(() => {
    if (!selectedResultId) return;
    if (filteredResults.some((item) => item.id === selectedResultId)) return;
    setSelectedResultId(null);
  }, [filteredResults, selectedResultId]);

  const visibleResults = useMemo(
    () => filteredResults.slice(0, visibleCount),
    [filteredResults, visibleCount],
  );

  const selectResult = useCallback((id: string) => {
    setSelectedResultId(id);
  }, []);

  return (
    <AdminMarketingShell
      currentPath="/glow_map"
      title="Glow 지도"
      description=""
      headerAside={
        <ToolbarFilters
          filters={filterOptions}
          activeFilter={activeFilter}
          onSelectFilter={(filter) => setActiveFilter((current) => (current === filter ? null : filter))}
          onClearFilter={() => setActiveFilter(null)}
        />
      }
      classNames={{
        main: styles.shellMain,
        toolbarCard: styles.toolbarCard,
        header: styles.toolbarHeader,
        title: styles.srOnly,
      }}
      stats={[]}
    >
      <section className={styles.mapShell} aria-labelledby="glow-map-title">
        <h2 id="glow-map-title" className={styles.srOnly}>
          Glow 지도
        </h2>

        <aside className={styles.resultPanel} aria-label="인근 지역 목록">
          <div className={styles.resultHeader}>
            <strong>인근 지역</strong>
            <span>
              {visibleResults.length}/{filteredResults.length}곳
            </span>
          </div>

          <div className={styles.resultList}>
            {eventLoading ? <p className={styles.resultStatus}>행사 정보를 지도에 불러오는 중입니다.</p> : null}
            {productLoading ? <p className={styles.resultStatus}>상품예약 정보를 지도에 불러오는 중입니다.</p> : null}
            {eventError ? <p className={styles.resultError}>{eventError}</p> : null}
            {productError ? <p className={styles.resultError}>{productError}</p> : null}
            {!eventLoading && !productLoading && filteredResults.length === 0 ? (
              <p className={styles.resultStatus}>
                {activeFilter
                  ? "선택한 필터에 표시할 데이터가 없습니다."
                  : "지도에 표시할 행사정보 또는 상품예약 데이터가 없습니다."}
              </p>
            ) : null}
            {visibleResults.map((item) => (
              <ResultCard
                key={item.id}
                item={item}
                selected={selectedResultId === item.id}
                onSelect={() => selectResult(item.id)}
              />
            ))}
            {filteredResults.length > 0 ? (
              <div ref={observerRef} className={styles.resultScrollTrigger}>
                {hasMoreResults ? "더 불러오는 중..." : "모든 장소를 불러왔습니다."}
              </div>
            ) : null}
          </div>
        </aside>

        <section className={styles.mapPanel} aria-label="여수 중앙동 Kakao Glow 지도">
          <KakaoGlowMap
            results={filteredResults}
            selectedResultId={selectedResultId}
            onSelectResult={selectResult}
          />
          <button type="button" className={styles.nearbyCta}>
            근처 명소를 발견해 보세요!
            <span aria-hidden="true">×</span>
          </button>
        </section>
      </section>
    </AdminMarketingShell>
  );
};

export default GlowMapPage;
