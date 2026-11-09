import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../../auth/AuthContext';
import {
  fetchAdminServerStatus,
  type AdminInfrastructureStatus,
  type AdminServerStatusResponse,
  type ServerStatusLevel,
} from '../../../shared/data/adminServerStatusApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from './AdminServerStatusPage.module.css';

const EMPTY_STATUS: AdminServerStatusResponse = {
  generatedAt: '',
  overallStatus: 'UP',
  summary: {
    cpuUsagePercent: 0,
    memoryUsagePercent: 0,
    diskUsagePercent: 0,
    jvmHeapUsagePercent: 0,
    uptimeSeconds: 0,
  },
  system: {
    cpu: {
      systemUsagePercent: 0,
      processUsagePercent: 0,
      systemLoadAverage: 0,
      availableProcessors: 0,
    },
    memory: {
      totalBytes: 0,
      freeBytes: 0,
      usedBytes: 0,
      maxBytes: 0,
      usagePercent: 0,
    },
    disk: {
      mountPath: '',
      fileStoreName: '',
      totalBytes: 0,
      freeBytes: 0,
      usedBytes: 0,
      usagePercent: 0,
    },
    jvm: {
      heapUsedBytes: 0,
      heapCommittedBytes: 0,
      heapMaxBytes: 0,
      heapUsagePercent: 0,
      nonHeapUsedBytes: 0,
      nonHeapCommittedBytes: 0,
      liveThreadCount: 0,
      daemonThreadCount: 0,
      peakThreadCount: 0,
      uptimeSeconds: 0,
      startTime: '',
    },
  },
  infrastructure: [],
  actuator: {
    enabled: false,
    healthEndpoint: '/actuator/health',
    metricsEndpoint: '/actuator/metrics',
    metrics: [],
  },
};

type GaugeItem = {
  readonly label: string;
  readonly shortLabel: string;
  readonly value: number | null;
  readonly accent: string;
};

type ChartDatum = {
  readonly label: string;
  readonly value: number;
  readonly accent: string;
};

type MetricItem = {
  readonly key?: string;
  readonly label: string;
  readonly value: string;
};

type ResponseItem = {
  readonly label: string;
  readonly responseTimeMs: number | null;
  readonly status: ServerStatusLevel;
};

const clampPercent = (value: number | null): number => {
  if (value == null || Number.isNaN(value)) {
    return 0;
  }

  return Math.min(100, Math.max(0, value));
};

const toChartValue = (value: number | null, fallback = 0): number =>
  value == null || Number.isNaN(value) ? fallback : value;

const formatBytes = (value: number): string => {
  if (!Number.isFinite(value) || value <= 0) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let current = value;
  let unitIndex = 0;

  while (current >= 1024 && unitIndex < units.length - 1) {
    current /= 1024;
    unitIndex += 1;
  }

  return `${current.toFixed(current >= 100 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
};

const formatDuration = (seconds: number): string => {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '0분';
  }

  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);

  if (days > 0) {
    return `${days}일 ${hours}시간`;
  }

  if (hours > 0) {
    return `${hours}시간 ${minutes}분`;
  }

  return `${Math.max(1, minutes)}분`;
};

const formatDateTime = (value: string): string => {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
};

const formatResponseTime = (value: number | null): string => (value == null ? '-' : `${value} ms`);

const formatStatusLabel = (status: ServerStatusLevel): string => {
  switch (status) {
    case 'UP':
      return '사용중';
    case 'DOWN':
      return '미사용';
    case 'DEGRADED':
    case 'UNKNOWN':
      return '확인필요';
    case 'NOT_CONFIGURED':
      return '미구성';
    default:
      return '확인필요';
  }
};

const statusToneClassName = (status: ServerStatusLevel): string => {
  switch (status) {
    case 'UP':
      return styles.statusUp;
    case 'DOWN':
      return styles.statusDown;
    case 'DEGRADED':
    case 'UNKNOWN':
      return styles.statusWarn;
    default:
      return styles.statusNeutral;
  }
};

const parseDatabaseProduct = (details: Record<string, string>): string => {
  const product = details.product?.trim().toLowerCase();
  if (product?.includes('postgresql')) {
    return 'PostgreSQL';
  }
  if (product?.includes('mysql')) {
    return 'MySQL';
  }

  const url = details.url?.trim().toLowerCase();
  if (url?.includes('postgresql')) {
    return 'PostgreSQL';
  }
  if (url?.includes('mysql')) {
    return 'MySQL';
  }

  return '데이터베이스';
};

const getInfrastructureDisplay = (
  item: AdminInfrastructureStatus
): { readonly typeLabel: string; readonly titleLabel: string } => {
  if (item.type === 'MINIO') {
    const rawLabel = item.label?.trim();
    const storageTitle =
      rawLabel && rawLabel.length > 0
        ? rawLabel.replace(/^minio$/i, 'Minio')
        : 'Minio';
    return {
      typeLabel: '파일서버',
      titleLabel: storageTitle,
    };
  }

  if (item.type === 'DATABASE') {
    return {
      typeLabel: '데이터서버',
      titleLabel: parseDatabaseProduct(item.details ?? {}),
    };
  }

  return {
    typeLabel: item.type,
    titleLabel: item.label,
  };
};

function RingBoard({
  items,
}: {
  readonly items: readonly GaugeItem[];
}) {
  const average = items.reduce((sum, item) => sum + clampPercent(item.value), 0) / Math.max(items.length, 1);
  const radius = 74;
  const circumference = 2 * Math.PI * radius;
  const total = items.reduce((sum, item) => sum + clampPercent(item.value), 0) || 1;
  let consumedLength = 0;

  const segments = items.map(item => {
    const length = (clampPercent(item.value) / total) * circumference;
    const segment = {
      label: item.label,
      accent: item.accent,
      dashArray: `${length} ${Math.max(circumference - length, 0)}`,
      dashOffset: -consumedLength,
    };
    consumedLength += length;
    return segment;
  });

  return (
    <div className={styles.ringBoard}>
      <div className={styles.ringWrap}>
        <svg viewBox="0 0 180 180" className={styles.ringSvg} aria-hidden="true">
          <circle cx="90" cy="90" r={radius} className={styles.ringBase} />
          {segments.map(segment => (
            <circle
              key={segment.label}
              cx="90"
              cy="90"
              r={radius}
              className={styles.ringSegment}
              style={{
                stroke: segment.accent,
                strokeDasharray: segment.dashArray,
                strokeDashoffset: segment.dashOffset,
              }}
            />
          ))}
        </svg>

        <div className={styles.ringCenter}>
          <span className={styles.ringCaption}>전체 부하</span>
          <strong className={styles.ringValue}>{average.toFixed(1)}%</strong>
        </div>
      </div>

      <div className={styles.legendList}>
        {items.map(item => (
          <div key={item.label} className={styles.legendRow}>
            <span className={styles.legendMeta}>
              <span
                className={styles.legendDot}
                style={{ backgroundColor: item.accent }}
                aria-hidden="true"
              />
              {item.label}
            </span>
            <strong>{`${clampPercent(item.value).toFixed(1)}%`}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

function ColumnBoard({ items }: { readonly items: readonly ChartDatum[] }) {
  return (
    <div className={styles.columnBoard}>
      <div className={styles.columnPlot}>
        {[20, 40, 60, 80, 100].map(line => (
          <span key={line} className={styles.columnGridLine} style={{ bottom: `${line}%` }} />
        ))}
        {items.map(item => (
          <div key={item.label} className={styles.columnItem}>
            <div className={styles.columnValue}>{item.value.toFixed(0)}%</div>
            <div className={styles.columnTrack}>
              <div
                className={styles.columnFill}
                style={{
                  height: `${Math.max(8, clampPercent(item.value))}%`,
                  backgroundColor: item.accent,
                }}
              />
            </div>
            <span className={styles.columnLabel}>{item.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function AreaProfileChart({ items }: { readonly items: readonly ChartDatum[] }) {
  const width = 560;
  const height = 190;

  if (items.length === 0) {
    return null;
  }

  const step = items.length > 1 ? width / (items.length - 1) : width;
  const points = items.map((item, index) => ({
    x: index * step,
    y: height - clampPercent(item.value) * 1.9,
    value: item.value,
    label: item.label,
  }));

  const linePath = points
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`)
    .join(' ');
  const areaPath = `${linePath} L ${width} ${height} L 0 ${height} Z`;

  return (
    <div className={styles.areaChart}>
      <svg viewBox={`0 0 ${width} ${height}`} className={styles.areaSvg} aria-hidden="true">
        {[0.25, 0.5, 0.75].map(line => (
          <line
            key={line}
            x1="0"
            y1={height * (1 - line)}
            x2={width}
            y2={height * (1 - line)}
            className={styles.areaGrid}
          />
        ))}
        <path d={areaPath} className={styles.areaFill} />
        <path d={linePath} className={styles.areaStroke} />
        {points.map(point => (
          <g key={point.label}>
            <circle cx={point.x} cy={point.y} r="5" className={styles.areaPointOuter} />
            <circle cx={point.x} cy={point.y} r="2.5" className={styles.areaPointInner} />
          </g>
        ))}
      </svg>

      <div className={styles.areaLabels}>
        {points.map(point => (
          <div key={point.label} className={styles.areaLabelItem}>
            <span>{point.label}</span>
            <strong>{`${point.value.toFixed(0)}%`}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

function MetricMatrix({ items }: { readonly items: readonly MetricItem[] }) {
  return (
    <div className={styles.metricMatrix}>
      {items.map((item, index) => (
        <article key={item.key ?? `${item.label}-${index}`} className={styles.metricTile}>
          <span className={styles.metricTileLabel}>{item.label}</span>
          <strong className={styles.metricTileValue}>{item.value}</strong>
        </article>
      ))}
    </div>
  );
}

function ResponseBars({ items }: { readonly items: readonly ResponseItem[] }) {
  const maxResponse = items.reduce((max, item) => Math.max(max, item.responseTimeMs ?? 0), 0);

  return (
    <div className={styles.responseBars}>
      {items.map(item => {
        const width =
          item.responseTimeMs == null || maxResponse <= 0 ? 0 : Math.max(8, (item.responseTimeMs / maxResponse) * 100);

        return (
          <div key={item.label} className={styles.responseRow}>
            <div className={styles.responseMeta}>
              <span>{item.label}</span>
              <strong>{formatResponseTime(item.responseTimeMs)}</strong>
            </div>
            <div className={styles.responseTrack}>
              <div
                className={`${styles.responseFill} ${statusToneClassName(item.status)}`}
                style={{ width: `${width}%` }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}

function StatusComposition({
  up,
  down,
  notConfigured,
  other,
}: {
  readonly up: number;
  readonly down: number;
  readonly notConfigured: number;
  readonly other: number;
}) {
  const total = Math.max(1, up + down + notConfigured + other);

  return (
    <div className={styles.compositionBoard}>
      <div className={styles.compositionBar}>
        <span
          className={`${styles.compositionSegment} ${styles.segmentUp}`}
          style={{ width: `${(up / total) * 100}%` }}
        />
        <span
          className={`${styles.compositionSegment} ${styles.segmentDown}`}
          style={{ width: `${(down / total) * 100}%` }}
        />
        <span
          className={`${styles.compositionSegment} ${styles.segmentNeutral}`}
          style={{ width: `${(notConfigured / total) * 100}%` }}
        />
        <span
          className={`${styles.compositionSegment} ${styles.segmentWarn}`}
          style={{ width: `${(other / total) * 100}%` }}
        />
      </div>

      <div className={styles.compositionLegend}>
        <div className={styles.compositionRow}>
          <span>정상</span>
          <strong>{up}</strong>
        </div>
        <div className={styles.compositionRow}>
          <span>장애</span>
          <strong>{down}</strong>
        </div>
        <div className={styles.compositionRow}>
          <span>미구성</span>
          <strong>{notConfigured}</strong>
        </div>
        <div className={styles.compositionRow}>
          <span>확인필요</span>
          <strong>{other}</strong>
        </div>
      </div>
    </div>
  );
}

function InfrastructureCard({ item }: { readonly item: AdminInfrastructureStatus }) {
  const detailEntries = Object.entries(item.details ?? {});
  const display = getInfrastructureDisplay(item);

  return (
    <article className={styles.infraCard}>
      <div className={styles.infraTop}>
        <div>
          <div className={styles.infraLabelRow}>
            <span className={styles.infraType}>{display.typeLabel}</span>
            <h3 className={styles.infraTitle}>{display.titleLabel}</h3>
          </div>
        </div>

        <span className={`${styles.statusPill} ${statusToneClassName(item.status)}`}>
          {formatStatusLabel(item.status)}
        </span>
      </div>

      <div className={styles.infraInfoRow}>
        <span>응답시간 {formatResponseTime(item.responseTimeMs)}</span>
        <span>상세 {detailEntries.length}개</span>
      </div>

      <dl className={styles.infraDetailList}>
        {detailEntries.map(([key, value]) => (
          <div key={key} className={styles.infraDetailRow}>
            <dt>{key}</dt>
            <dd>{value || '-'}</dd>
          </div>
        ))}
      </dl>
    </article>
  );
}

export function AdminServerStatusPage() {
  const { authReady, accessToken, user } = useAuth();
  const [status, setStatus] = useState<AdminServerStatusResponse>(EMPTY_STATUS);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || !accessToken || user?.role !== 'ADMIN') {
      return;
    }

    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const next = await fetchAdminServerStatus(accessToken);
        if (!cancelled) {
          setStatus(next);
        }
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : '서버 상태를 불러오지 못했습니다.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      cancelled = true;
    };
  }, [authReady, accessToken, user?.role]);

  const visibleInfrastructure = useMemo(
    () => status.infrastructure.filter(item => item.status !== 'NOT_CONFIGURED'),
    [status.infrastructure]
  );

  const usageItems = useMemo<readonly GaugeItem[]>(
    () => [
      { label: 'CPU', shortLabel: 'CPU', value: status.summary.cpuUsagePercent, accent: '#2784a4' },
      { label: '메모리', shortLabel: 'MEM', value: status.summary.memoryUsagePercent, accent: '#3d8bff' },
      { label: '디스크', shortLabel: 'DSK', value: status.summary.diskUsagePercent, accent: '#8b5cf6' },
      { label: 'JVM Heap', shortLabel: 'JVM', value: status.summary.jvmHeapUsagePercent, accent: '#0ea5a8' },
    ],
    [status.summary]
  );

  const barItems = useMemo<readonly ChartDatum[]>(
    () =>
      usageItems.map(item => ({
        label: item.shortLabel,
        value: toChartValue(item.value),
        accent: item.accent,
      })),
    [usageItems]
  );

  const profileItems = useMemo<readonly ChartDatum[]>(
    () => [
      { label: '시스템 CPU', value: toChartValue(status.system.cpu.systemUsagePercent), accent: '#2784a4' },
      { label: '프로세스 CPU', value: toChartValue(status.system.cpu.processUsagePercent), accent: '#3d8bff' },
      { label: '메모리', value: toChartValue(status.summary.memoryUsagePercent), accent: '#8b5cf6' },
      { label: '디스크', value: toChartValue(status.summary.diskUsagePercent), accent: '#0ea5a8' },
      { label: 'JVM Heap', value: toChartValue(status.summary.jvmHeapUsagePercent), accent: '#f97316' },
      {
        label: '로드 평균',
        value: Math.min(100, Math.max(0, (status.system.cpu.systemLoadAverage ?? 0) * 10)),
        accent: '#ef4444',
      },
    ],
    [status]
  );

  const infrastructureCounts = useMemo(
    () =>
      visibleInfrastructure.reduce(
        (accumulator, item) => {
          if (item.status === 'UP') {
            accumulator.up += 1;
          } else if (item.status === 'DOWN') {
            accumulator.down += 1;
          } else if (item.status === 'NOT_CONFIGURED') {
            accumulator.notConfigured += 1;
          } else {
            accumulator.other += 1;
          }
          return accumulator;
        },
        { up: 0, down: 0, notConfigured: 0, other: 0 }
      ),
    [visibleInfrastructure]
  );

  const actuatorTiles = useMemo<readonly MetricItem[]>(
    () =>
      status.actuator.metrics
        .filter(metric => metric.value != null)
        .slice(0, 6)
        .map((metric, index) => ({
          key: `${metric.name}-${index}`,
          label: metric.name.replace(/^.*\./, ''),
          value: typeof metric.value === 'number' ? metric.value.toFixed(2) : '-',
        })),
    [status.actuator.metrics]
  );

  const responseItems = useMemo<readonly ResponseItem[]>(
    () =>
      visibleInfrastructure.map(item => ({
        label: item.label,
        responseTimeMs: item.responseTimeMs,
        status: item.status,
      })),
    [visibleInfrastructure]
  );

  const quickStats = useMemo(
    () => [
      {
        label: '메모리 사용',
        value: formatBytes(status.system.memory.usedBytes),
        helper: `최대 ${formatBytes(status.system.memory.maxBytes)}`,
      },
      {
        label: '디스크 사용',
        value: formatBytes(status.system.disk.usedBytes),
        helper: `여유 ${formatBytes(status.system.disk.freeBytes)}`,
      },
    ],
    [status]
  );

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={styles.stateBox}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (!accessToken || user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={styles.stateBox}>관리자만 서버 상태 대시보드를 확인할 수 있습니다.</div>
      </main>
    );
  }

  const statusText = error
    ? error
    : loading
      ? '서버 상태를 새로 불러오는 중입니다.'
      : `전체 상태 ${formatStatusLabel(status.overallStatus)} · 정상 인프라 ${infrastructureCounts.up}개`;

  return (
    <AdminMarketingShell
      currentPath="/admin/server-status"
      title="서버 상태"
      description=""
      statusText={statusText}
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: styles.toolbarStatusText,
      }}
      headerAside={
        <div className={styles.toolbarQuickStats}>
          {quickStats.map(item => (
            <article key={item.label} className={styles.quickCard}>
              <span className={styles.quickLabel}>{item.label}</span>
              <strong className={styles.quickValue}>{item.value}</strong>
              <p className={styles.quickHelper}>{item.helper}</p>
            </article>
          ))}
        </div>
      }
      stats={[]}
    >
      <div className={styles.dashboard}>
        <section className={styles.heroGrid}>
          <section className={`${styles.widget} ${styles.primaryWidget}`}>
            <div className={styles.widgetHeader}>
              <div>
                <h2 className={styles.widgetTitle}>리소스 점유율</h2>
              </div>
            </div>
            <RingBoard items={usageItems} />
          </section>

          <section className={`${styles.widget} ${styles.primaryWidget}`}>
            <div className={styles.widgetHeader}>
              <div>
                <h2 className={styles.widgetTitle}>핵심 사용량</h2>
              </div>
              <span className={styles.widgetMeta}>{formatDateTime(status.generatedAt)}</span>
            </div>
            <ColumnBoard items={barItems} />
          </section>
        </section>

        <section className={styles.secondaryGrid}>
          <section className={`${styles.widget} ${styles.wideWidget}`}>
            <div className={styles.widgetHeader}>
              <div>
                <h2 className={styles.widgetTitle}>시스템 프로파일</h2>
              </div>
            </div>
            <AreaProfileChart items={profileItems} />
          </section>
        </section>

        <section className={styles.tertiaryGrid}>
          <section className={styles.widget}>
            <div className={styles.widgetHeader}>
              <div>
                <h2 className={styles.widgetTitle}>인프라 개요</h2>
              </div>
            </div>
            <div className={styles.infraOverviewGrid}>
              <div className={styles.infraOverviewSection}>
                <h3 className={styles.infraOverviewTitle}>응답시간</h3>
                <ResponseBars items={responseItems} />
              </div>
              <div className={styles.infraOverviewSection}>
                <h3 className={styles.infraOverviewTitle}>구성 상태</h3>
                <StatusComposition
                  up={infrastructureCounts.up}
                  down={infrastructureCounts.down}
                  notConfigured={infrastructureCounts.notConfigured}
                  other={infrastructureCounts.other}
                />
              </div>
            </div>
          </section>

          <section className={styles.widget}>
            <div className={styles.widgetHeader}>
              <div>
                <h2 className={styles.widgetTitle}>Actuator Metrics</h2>
              </div>
            </div>
            <MetricMatrix
              items={actuatorTiles.length > 0 ? actuatorTiles : [{ key: 'metrics', label: 'metrics', value: 'N/A' }]}
            />
          </section>
        </section>

        <section className={styles.detailSection}>
          <div className={styles.sectionTop}>
            <div>
              <h2 className={styles.sectionHeading}>인프라 상세</h2>
            </div>
          </div>

          <div className={styles.infraGrid}>
            {visibleInfrastructure.map(item => (
              <InfrastructureCard key={item.type} item={item} />
            ))}
          </div>
        </section>
      </div>
    </AdminMarketingShell>
  );
}

export default AdminServerStatusPage;
