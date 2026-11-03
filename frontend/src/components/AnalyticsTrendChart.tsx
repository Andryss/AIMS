import React, { useEffect, useId, useState } from 'react';
import { AnalyticsBucketUnit, AnalyticsTrendBucket } from '../types';
import { formatAnalyticsDate, formatAnalyticsNumber } from '../utils/analyticsFormatting';

export interface AnalyticsSeries { key: string; label: string; }

export function AnalyticsTrendChart({ buckets, series, label, bucketUnit }: {
  buckets: AnalyticsTrendBucket[]; series: AnalyticsSeries[]; label: string; bucketUnit?: AnalyticsBucketUnit;
}) {
  const [activeIndex, setActiveIndex] = useState<number | null>(null);
  const tooltipId = useId();
  const hintId = useId();
  const active = activeIndex == null ? null : buckets[activeIndex];
  useEffect(() => setActiveIndex(null), [buckets]);

  const selectAtPointer = (event: React.PointerEvent<SVGSVGElement>) => {
    if (!buckets.length) { return; }
    const bounds = event.currentTarget.getBoundingClientRect();
    if (!bounds.width) { return; }
    const position = (event.clientX - bounds.left) / bounds.width * 880;
    setActiveIndex(Math.max(0, Math.min(buckets.length - 1, Math.round((position - 45) / 800 * (buckets.length - 1)))));
  };
  const crossesDay = buckets.length > 0 && new Date(buckets[0].from).toDateString()
    !== new Date(buckets[buckets.length - 1].to).toDateString();
  const axisLabel = (value: string) => bucketUnit === 'HOUR'
    ? new Date(value).toLocaleString('ru-RU', { hour: '2-digit', minute: '2-digit',
      ...(crossesDay ? { day: '2-digit' as const, month: '2-digit' as const } : {}) })
    : new Date(value).toLocaleDateString('ru-RU');
  const max = Math.max(1, ...buckets.flatMap((bucket) => series.map((item) => bucket.values[item.key] ?? 0)));
  const hasEvents = buckets.some((bucket) => series.some((item) => (bucket.values[item.key] ?? 0) > 0));
  const x = (index: number) => 45 + index / Math.max(buckets.length - 1, 1) * 800;
  const y = (count: number) => 175 - count / max * 145;
  return <figure className="analytics-chart">
    <ul className="analytics-chart__legend">{series.map((item, index) =>
      <li key={item.key} className={`analytics-series--${index + 1}`}><span aria-hidden="true" />{item.label}</li>)}</ul>
    {!hasEvents && <p className="analytics-note">За период событий нет</p>}
    <p id={hintId} className="visually-hidden">Наведите на замер, коснитесь графика или используйте стрелки влево и вправо. Escape закрывает подсказку.</p>
    <svg viewBox="0 0 880 215" role="img" aria-label={label} tabIndex={0}
      aria-describedby={active ? tooltipId : hintId}
      onPointerMove={selectAtPointer}
      onPointerDown={(event) => { event.currentTarget.focus(); selectAtPointer(event); }}
      onPointerLeave={(event) => {
        if (event.pointerType !== 'touch' && document.activeElement !== event.currentTarget) { setActiveIndex(null); }
      }}
      onFocus={() => setActiveIndex((previous) => previous ?? (buckets.length ? 0 : null))}
      onBlur={() => setActiveIndex(null)}
      onKeyDown={(event) => {
        if (event.key === 'Escape') { setActiveIndex(null); event.preventDefault(); return; }
        if (!buckets.length) { return; }
        const next = event.key === 'ArrowRight' ? Math.min((activeIndex ?? -1) + 1, buckets.length - 1)
          : event.key === 'ArrowLeft' ? Math.max((activeIndex ?? 1) - 1, 0)
          : event.key === 'Home' ? 0 : event.key === 'End' ? buckets.length - 1 : null;
        if (next != null) { event.preventDefault(); setActiveIndex(next); }
      }}>
      {active && activeIndex != null && <g aria-hidden="true">
        <rect x={x(activeIndex) - 10} y="25" width="20" height="155" className="analytics-chart__highlight" />
        <line x1={x(activeIndex)} x2={x(activeIndex)} y1="25" y2="180" className="analytics-chart__cursor" />
      </g>}
      <line x1="45" y1="175" x2="845" y2="175" className="analytics-chart__axis" />
      <line x1="45" y1="30" x2="845" y2="30" className="analytics-chart__axis" />
      <text x="34" y="179" textAnchor="end">0</text><text x="34" y="34" textAnchor="end">{formatAnalyticsNumber(max)}</text>
      {series.map((item, seriesIndex) => <g key={item.key} className={`analytics-series--${seriesIndex + 1}`}>
        <polyline fill="none" strokeWidth="2.5" points={buckets.map((bucket, index) =>
          `${x(index)},${y(bucket.values[item.key] ?? 0)}`).join(' ')} />
        {buckets.map((bucket, index) => <circle key={bucket.from} cx={x(index)} cy={y(bucket.values[item.key] ?? 0)}
          r={activeIndex === index ? 5.5 : 3} className={activeIndex === index ? 'analytics-chart__point--active' : ''} />)}
      </g>)}
      {buckets.length > 0 && <><text x="45" y="203">{axisLabel(buckets[0].from)}</text>
        <text x="845" y="203" textAnchor="end">{axisLabel(buckets[buckets.length - 1].to)}</text></>}
    </svg>
    {active && <div id={tooltipId} role="tooltip" aria-live="polite"
      className={`analytics-chart__tooltip${activeIndex != null && activeIndex >= (buckets.length - 1) / 2 ? ' analytics-chart__tooltip--left' : ''}`}>
      <div className="analytics-chart__interval"><span>От</span><time dateTime={active.from}>{formatAnalyticsDate(active.from)}</time></div>
      <div className="analytics-chart__interval"><span>До</span><time dateTime={active.to}>{formatAnalyticsDate(active.to)}</time></div>
      <dl>{series.map((item, index) => <div key={item.key} className={`analytics-series--${index + 1}`}>
        <dt><span aria-hidden="true" />{item.label}</dt><dd>{formatAnalyticsNumber(active.values[item.key] ?? 0)}</dd>
      </div>)}</dl>
    </div>}
    <details><summary>Значения графика</summary><div className="incidents-table-wrapper"><table className="incidents-table">
      <caption className="visually-hidden">{label}</caption>
      <thead><tr><th>От</th><th>До</th>{series.map((item) => <th key={item.key}>{item.label}</th>)}</tr></thead>
      <tbody>{buckets.map((bucket) => <tr key={bucket.from}><td>{formatAnalyticsDate(bucket.from)}</td><td>{formatAnalyticsDate(bucket.to)}</td>
        {series.map((item) => <td key={item.key}>{formatAnalyticsNumber(bucket.values[item.key] ?? 0)}</td>)}</tr>)}</tbody>
    </table></div></details>
  </figure>;
}
