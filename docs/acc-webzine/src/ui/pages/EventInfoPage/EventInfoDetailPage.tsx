import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
  fetchAdminProducts,
  type AdminProductSummary,
} from "../../../shared/data/adminBookingApi";
import {
  fetchCultureCalendarDetail,
  fetchFestivalCommonDetail,
  fetchFestivalImages,
  fetchKopisPerformanceDetail,
  fetchSjwPerformanceDetail,
  type FestivalCommonDetail,
  type FestivalEventItem,
  type FestivalImageItem,
} from "../../../shared/data/festivalInfoApi";
import shellStyles from "../AdminMarketingPage/AdminMarketingPage.module.css";
import { AdminMarketingShell } from "../AdminMarketingPage/AdminMarketingShell";
import styles from "./EventInfoDetailPage.module.css";

type EventInfoLocationState = {
  readonly item?: FestivalEventItem;
};

type MapCoords = {
  readonly lat: number;
  readonly lng: number;
};

type EventKakaoLatLng = unknown;

type EventKakaoMap = {
  relayout: () => void;
  setCenter: (latLng: EventKakaoLatLng) => void;
};

type EventKakaoMarker = {
  setMap: (map: EventKakaoMap | null) => void;
};

type EventKakaoMaps = {
  load: (callback: () => void) => void;
  Map: new (
    container: HTMLElement,
    options: {
      center: EventKakaoLatLng;
      level: number;
      scrollwheel?: boolean;
    },
  ) => EventKakaoMap;
  LatLng: new (lat: number, lng: number) => EventKakaoLatLng;
  Marker: new (options: { position: EventKakaoLatLng }) => EventKakaoMarker;
};

const KAKAO_MAP_APP_KEY = import.meta.env.VITE_KAKAO_MAP_APP_KEY?.trim() ?? "";
const KAKAO_SDK_ID = "kakao-map-sdk";
let kakaoMapLoader: Promise<EventKakaoMaps> | null = null;

function safeDecode(value: string | undefined): string {
  if (!value) return "";
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

function toInputDate(yyyymmdd: string | null): string | null {
  if (!yyyymmdd || !/^\d{8}$/.test(yyyymmdd)) return yyyymmdd;
  return `${yyyymmdd.slice(0, 4)}-${yyyymmdd.slice(4, 6)}-${yyyymmdd.slice(6, 8)}`;
}

function formatPeriod(start: string | null, end: string | null): string {
  if (!start && !end) return "일정 정보 없음";
  return `${toInputDate(start) ?? "-"} ~ ${toInputDate(end) ?? "-"}`;
}

function decodeHtml(value: string): string {
  const textarea = document.createElement("textarea");
  textarea.innerHTML = value;
  return textarea.value;
}

function cleanText(value: string | null | undefined): string {
  if (!value) return "";
  return decodeHtml(
    value
      .replace(/<br\s*\/?>/gi, "\n")
      .replace(/<\/p>/gi, "\n")
      .replace(/<[^>]+>/g, " "),
  )
    .replace(/[ \t]{2,}/g, " ")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function normalizeExternalLink(raw: string | null | undefined): { readonly href: string; readonly label: string } | null {
  const source = raw?.trim();
  if (!source) return null;
  const href = /href\s*=\s*["']([^"']+)["']/i.exec(source)?.[1];
  const text = source.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
  const hrefValue = decodeHtml(href ?? text);
  if (!/^https?:\/\//i.test(hrefValue)) return null;
  const label = decodeHtml(text).replace(/<[^>]+>/g, " ").trim() || hrefValue;
  return { href: hrefValue, label };
}

function dedupeUrls(urls: readonly (string | null | undefined)[]): readonly string[] {
  return [...new Set(urls.map((url) => url?.trim()).filter((url): url is string => Boolean(url)))];
}

function coordsFrom(
  eventItem: FestivalEventItem | null,
  commonDetail: FestivalCommonDetail | null,
): MapCoords | null {
  const mapX = commonDetail?.mapX ?? eventItem?.mapX ?? null;
  const mapY = commonDetail?.mapY ?? eventItem?.mapY ?? null;
  if (mapX == null || mapY == null) return null;
  if (!Number.isFinite(mapX) || !Number.isFinite(mapY)) return null;
  return { lat: mapY, lng: mapX };
}

function addressFrom(
  eventItem: FestivalEventItem | null,
  commonDetail: FestivalCommonDetail | null,
): string | null {
  const base = commonDetail?.address?.trim() || eventItem?.address?.trim() || "";
  const detail = commonDetail?.addressDetail?.trim() || "";
  const address = [base, detail].filter(Boolean).join(" ");
  return address || null;
}

function commonDetailToEventItem(
  source: string,
  contentId: string,
  detail: FestivalCommonDetail,
): FestivalEventItem {
  return {
    contentId,
    title: detail.title || "행사 상세",
    address: addressFrom(null, detail),
    eventStartDate: null,
    eventEndDate: null,
    thumbnailImageUrl: detail.firstImage2 || detail.firstImage,
    imageUrl: detail.firstImage || detail.firstImage2,
    mapX: detail.mapX,
    mapY: detail.mapY,
    phone: detail.tel,
    detailUrl: null,
    category: null,
    synopsis: detail.overview,
    source,
    cast: null,
    runningTime: null,
    age: null,
    bookingPlaces: null,
    introImageUrls: [],
  };
}

function placeLabelFromAddress(address: string | null): string {
  if (!address) return "Korea";
  const parts = address.split(/\s+/).filter(Boolean);
  return parts.find((part) => /시$|군$|구$/.test(part)) ?? parts[1] ?? parts[0] ?? "Korea";
}

function formatWon(value: number): string {
  if (!Number.isFinite(value) || value <= 0) return "가격 확인";
  return `${new Intl.NumberFormat("ko-KR").format(value)}원`;
}

function productAddressOf(product: AdminProductSummary): string {
  const display = product.location?.displayAddress?.trim();
  if (display && display !== "—") return display;
  return product.location?.name?.trim() || "위치 정보 없음";
}

function kakaoMapSearchUrl(title: string, address: string | null, coords: MapCoords | null): string {
  if (coords) {
    return `https://map.kakao.com/link/map/${encodeURIComponent(title)},${coords.lat},${coords.lng}`;
  }
  return `https://map.kakao.com/?q=${encodeURIComponent(address || title)}`;
}

function kakaoDirectionUrl(title: string, address: string | null, coords: MapCoords | null): string {
  if (coords) {
    return `https://map.kakao.com/link/to/${encodeURIComponent(title)},${coords.lat},${coords.lng}`;
  }
  return kakaoMapSearchUrl(title, address, coords);
}

function kakaoWindow(): Window & { kakao?: { maps: EventKakaoMaps } } {
  return window as Window & { kakao?: { maps: EventKakaoMaps } };
}

function loadKakaoMaps(appKey: string): Promise<EventKakaoMaps> {
  const win = kakaoWindow();
  if (win.kakao?.maps) {
    return new Promise((resolve) => win.kakao?.maps.load(() => resolve(win.kakao!.maps)));
  }

  if (kakaoMapLoader) {
    return kakaoMapLoader;
  }

  kakaoMapLoader = new Promise<EventKakaoMaps>((resolve, reject) => {
    const existingScript = document.getElementById(KAKAO_SDK_ID) as HTMLScriptElement | null;
    const script = existingScript ?? document.createElement("script");

    script.id = KAKAO_SDK_ID;
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false`;
    script.addEventListener("load", () => {
      const loadedWindow = kakaoWindow();
      if (!loadedWindow.kakao?.maps) {
        reject(new Error("Kakao 지도 SDK를 불러오지 못했습니다."));
        return;
      }
      loadedWindow.kakao.maps.load(() => resolve(loadedWindow.kakao!.maps));
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

function EventKakaoMapView({
  title,
  address,
  coords,
}: {
  readonly title: string;
  readonly address: string | null;
  readonly coords: MapCoords | null;
}) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const markerRef = useRef<EventKakaoMarker | null>(null);
  const [status, setStatus] = useState<"empty" | "missing-key" | "loading" | "ready" | "error">(
    coords ? (KAKAO_MAP_APP_KEY ? "loading" : "missing-key") : "empty",
  );

  useEffect(() => {
    if (!coords) {
      setStatus("empty");
      return;
    }
    if (!KAKAO_MAP_APP_KEY) {
      setStatus("missing-key");
      return;
    }
    const container = containerRef.current;
    if (!container) return;

    let cancelled = false;
    let resizeObserver: ResizeObserver | null = null;
    setStatus("loading");

    void loadKakaoMaps(KAKAO_MAP_APP_KEY)
      .then((maps) => {
        if (cancelled) return;
        const center = new maps.LatLng(coords.lat, coords.lng);
        const map = new maps.Map(container, {
          center,
          level: 3,
          scrollwheel: false,
        });
        const marker = new maps.Marker({ position: center });
        marker.setMap(map);
        markerRef.current = marker;

        resizeObserver = new ResizeObserver(() => {
          map.relayout();
          map.setCenter(center);
        });
        resizeObserver.observe(container);

        window.setTimeout(() => {
          map.relayout();
          map.setCenter(center);
        }, 120);
        setStatus("ready");
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });

    return () => {
      cancelled = true;
      resizeObserver?.disconnect();
      markerRef.current?.setMap(null);
      markerRef.current = null;
    };
  }, [coords]);

  return (
    <div className={styles.mapBox}>
      <div ref={containerRef} className={styles.kakaoMap} aria-hidden={status !== "ready"} />
      {status !== "ready" ? (
        <div className={styles.mapState} role={status === "error" ? "alert" : "status"}>
          <strong>
            {status === "missing-key"
              ? "Kakao 지도 앱 키가 필요합니다."
              : status === "error"
                ? "Kakao 지도를 불러오지 못했습니다."
                : status === "empty"
                  ? "표시할 좌표 정보가 없습니다."
                  : "Kakao 지도를 불러오는 중입니다."}
          </strong>
          <span>
            {status === "missing-key"
              ? ".env.development에 VITE_KAKAO_MAP_APP_KEY를 추가하면 실제 Kakao 지도가 표시됩니다."
              : status === "empty"
                ? "아래 버튼으로 Kakao 지도 검색을 열 수 있습니다."
                : "앱 키, 도메인 등록, 네트워크 상태를 확인해 주세요."}
          </span>
        </div>
      ) : null}
      <a className={styles.mapOpenLink} href={kakaoMapSearchUrl(title, address, coords)} target="_blank" rel="noreferrer">
        Kakao 지도에서 보기
      </a>
    </div>
  );
}

function ProductRecommendationCard({ product }: { readonly product: AdminProductSummary }) {
  return (
    <Link to={`/products/${product.id}`} className={styles.recommendCard}>
      <div className={styles.recommendThumb}>
        {product.thumbnailUrl ? (
          <img src={product.thumbnailUrl} alt="" loading="lazy" decoding="async" />
        ) : (
          <span>Idol Glow</span>
        )}
        <span className={styles.heartMark}>♡</span>
      </div>
      <div className={styles.recommendCopy}>
        <span>{productAddressOf(product)}</span>
        <h3>{product.name}</h3>
        <p>From {formatWon(product.minPrice)}</p>
        {product.tagNames.length > 0 ? <em>{product.tagNames.slice(0, 2).join(", ")}</em> : null}
      </div>
    </Link>
  );
}

export function EventInfoDetailPage() {
  const { source: sourceParam, contentId: contentIdParam } = useParams<{ source: string; contentId: string }>();
  const source = safeDecode(sourceParam);
  const contentId = safeDecode(contentIdParam);
  const location = useLocation();
  const { accessToken, authReady } = useAuth();

  const stateItem = useMemo(() => {
    const candidate = (location.state as EventInfoLocationState | null)?.item;
    if (!candidate) return null;
    return candidate.source === source && candidate.contentId === contentId ? candidate : null;
  }, [contentId, location.state, source]);

  const [eventItem, setEventItem] = useState<FestivalEventItem | null>(stateItem);
  const [commonDetail, setCommonDetail] = useState<FestivalCommonDetail | null>(null);
  const [detailImages, setDetailImages] = useState<readonly FestivalImageItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [relatedProducts, setRelatedProducts] = useState<readonly AdminProductSummary[]>([]);
  const [relatedLoading, setRelatedLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [shared, setShared] = useState(false);

  useEffect(() => {
    setEventItem(stateItem);
  }, [stateItem]);

  useEffect(() => {
    if (location.hash !== "#store-info") return;
    window.setTimeout(() => {
      document.getElementById("store-info")?.scrollIntoView({ block: "start" });
    }, 0);
  }, [location.hash, loading]);

  useEffect(() => {
    if (!authReady) return;
    if (!accessToken) {
      setError("로그인이 필요합니다.");
      return;
    }
    if (!source || !contentId) {
      setError("행사 상세 경로가 올바르지 않습니다.");
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);
    setCommonDetail(null);
    setDetailImages([]);

    void (async () => {
      try {
        if (source === "TOUR_API") {
          const [detail, images] = await Promise.all([
            fetchFestivalCommonDetail(accessToken, contentId),
            fetchFestivalImages(accessToken, contentId, "Y"),
          ]);
          if (cancelled) return;
          setCommonDetail(detail);
          setDetailImages(images);
          if (detail) setEventItem((previous) => previous ?? commonDetailToEventItem(source, contentId, detail));
        } else if (source.startsWith("KOPIS")) {
          const detail = await fetchKopisPerformanceDetail(accessToken, contentId);
          if (!cancelled && detail) setEventItem(detail);
        } else if (source === "SEOUL_SJW_PERFORM") {
          const detail = await fetchSjwPerformanceDetail(accessToken, contentId);
          if (!cancelled && detail) setEventItem(detail);
        } else if (source.startsWith("CULTURE")) {
          const detail = await fetchCultureCalendarDetail(accessToken, contentId);
          if (cancelled) return;
          setCommonDetail(detail);
          if (detail) setEventItem((previous) => previous ?? commonDetailToEventItem(source, contentId, detail));
        }
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "행사 상세 정보를 불러오지 못했습니다.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, contentId, source]);

  const title = commonDetail?.title || eventItem?.title || "행사 상세";
  const address = useMemo(() => addressFrom(eventItem, commonDetail), [commonDetail, eventItem]);
  const coords = useMemo(() => coordsFrom(eventItem, commonDetail), [commonDetail, eventItem]);
  const period = formatPeriod(eventItem?.eventStartDate ?? null, eventItem?.eventEndDate ?? null);
  const overview =
    cleanText(commonDetail?.overview) ||
    cleanText(eventItem?.synopsis) ||
    "상세 소개 정보가 준비 중입니다. 행사 장소와 일정은 위 정보를 먼저 확인해 주세요.";
  const phone = commonDetail?.tel || eventItem?.phone || null;
  const externalLink = normalizeExternalLink(commonDetail?.homepage) || normalizeExternalLink(eventItem?.detailUrl);
  const galleryImages = useMemo(
    () =>
      dedupeUrls([
        eventItem?.imageUrl,
        eventItem?.thumbnailImageUrl,
        commonDetail?.firstImage,
        commonDetail?.firstImage2,
        ...detailImages.map((image) => image.originImageUrl || image.smallImageUrl),
        ...(eventItem?.introImageUrls ?? []),
      ]),
    [commonDetail, detailImages, eventItem],
  );
  const heroImage = galleryImages[0] ?? null;
  const contentImages = galleryImages.length > 1 ? galleryImages.slice(1, 5) : galleryImages.slice(0, 1);
  const placeLabel = placeLabelFromAddress(address);

  useEffect(() => {
    if (!authReady) return;
    let cancelled = false;
    setRelatedLoading(true);

    void (async () => {
      try {
        const areaRows = placeLabel && placeLabel !== "Korea"
          ? await fetchAdminProducts(accessToken, placeLabel, { limit: 3 })
          : [];
        const fallbackRows =
          areaRows.length >= 3 ? [] : await fetchAdminProducts(accessToken, undefined, { limit: 3 });
        if (cancelled) return;
        const merged = [...new Map([...areaRows, ...fallbackRows].map((product) => [product.id, product])).values()];
        setRelatedProducts(merged.slice(0, 3));
      } catch {
        if (!cancelled) setRelatedProducts([]);
      } finally {
        if (!cancelled) setRelatedLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken, authReady, placeLabel]);

  const copyAddress = async () => {
    if (!address) return;
    try {
      await navigator.clipboard.writeText(address);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
    }
  };

  const shareDetail = async () => {
    const url = window.location.href;
    try {
      if (navigator.share) {
        await navigator.share({ title, text: overview.slice(0, 80), url });
      } else {
        await navigator.clipboard.writeText(url);
      }
      setShared(true);
      window.setTimeout(() => setShared(false), 1800);
    } catch {
      setShared(false);
    }
  };

  return (
    <AdminMarketingShell
      currentPath="/event-info"
      title="Glow 행사정보"
      description="행사 상세, 위치, 주변 추천을 한 페이지에서 확인"
      stats={[]}
    >
      <section className={`${shellStyles.panel} ${styles.detailPanel}`}>
        <div className={shellStyles.panelBody}>
          <div className={styles.detailTopBar}>
            <Link to="/event-info" className={styles.backLink}>
              행사 목록으로
            </Link>
            {externalLink ? (
              <a href={externalLink.href} target="_blank" rel="noreferrer" className={styles.externalLink}>
                외부 상세 보기
              </a>
            ) : null}
          </div>

          {loading ? <p className={styles.status}>행사 상세 정보를 불러오는 중입니다.</p> : null}
          {error ? <p className={styles.error}>{error}</p> : null}

          <article className={styles.detailPage}>
            <div className={styles.heroMedia}>
              {heroImage ? (
                <img src={heroImage} alt={title} loading="eager" decoding="async" />
              ) : (
                <div className={styles.heroFallback}>Idol Glow Event</div>
              )}
            </div>

            <div className={styles.contentGrid}>
              <div className={styles.mainColumn}>
                <h1>{title}</h1>
                <p className={styles.overview}>{overview}</p>
                {contentImages.length > 0 ? (
                  <div className={styles.imageStack}>
                    {contentImages.map((url, index) => (
                      <img key={url} src={url} alt={`${title} 이미지 ${index + 1}`} loading="lazy" decoding="async" />
                    ))}
                  </div>
                ) : null}
              </div>

              <aside className={styles.infoCard} aria-label="행사 기본 정보">
                <div className={styles.dateLine}>
                  <span aria-hidden="true">▣</span>
                  <strong>{period}</strong>
                </div>
                <dl>
                  <div>
                    <dt>Location</dt>
                    <dd>{address || "장소 정보 없음"}</dd>
                  </div>
                  <div>
                    <dt>Fees</dt>
                    <dd>요금 정보 없음</dd>
                  </div>
                  <div>
                    <dt>Hours Of Operation</dt>
                    <dd>{eventItem?.runningTime || "운영시간 정보 없음"}</dd>
                  </div>
                  {phone ? (
                    <div>
                      <dt>Contact</dt>
                      <dd>{phone}</dd>
                    </div>
                  ) : null}
                  {eventItem?.category ? (
                    <div>
                      <dt>Category</dt>
                      <dd>{eventItem.category}</dd>
                    </div>
                  ) : null}
                </dl>
              </aside>
            </div>

            <section id="store-info" className={styles.storeSection}>
              <h2>Store Info</h2>
              <EventKakaoMapView title={title} address={address} coords={coords} />
              <div className={styles.addressRow}>
                <span aria-hidden="true">●</span>
                <p>{address || "주소 정보 없음"}</p>
                {address ? (
                  <button type="button" onClick={copyAddress}>
                    {copied ? "복사됨" : "주소 복사"}
                  </button>
                ) : null}
              </div>
              <a className={styles.directionButton} href={kakaoDirectionUrl(title, address, coords)} target="_blank" rel="noreferrer">
                How to Get There
              </a>
            </section>

            <section className={styles.shareSection}>
              <h2>Like the information?</h2>
              <button type="button" onClick={shareDetail}>
                {shared ? "공유 링크가 준비됐습니다" : "Share with a friend"}
              </button>
            </section>

            <section className={styles.recommendSection}>
              <div className={styles.recommendHeader}>
                <h2>Best In {placeLabel}</h2>
                <Link to="/glow_map">더 보기</Link>
              </div>
              {relatedLoading ? <p className={styles.status}>주변 추천을 불러오는 중입니다.</p> : null}
              {!relatedLoading && relatedProducts.length > 0 ? (
                <div className={styles.recommendGrid}>
                  {relatedProducts.map((product) => (
                    <ProductRecommendationCard key={product.id} product={product} />
                  ))}
                </div>
              ) : null}
            </section>
          </article>
        </div>
      </section>
    </AdminMarketingShell>
  );
}

export default EventInfoDetailPage;
