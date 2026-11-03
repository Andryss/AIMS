import React from 'react';
import { DurationPercentiles as DurationPercentilesValue } from '../types';
import { formatAnalyticsDuration } from '../utils/analyticsFormatting';
import { EmptyState } from './ui/EmptyState';

export function DurationPercentiles({ value }: { value: DurationPercentilesValue }) {
  return <>
    <p className="analytics-note">Выборка: {value.sampleCount}</p>
    {value.excludedSampleCount > 0 && <p className="analytics-note">Исключено: {value.excludedSampleCount}</p>}
    {value.sampleCount === 0 ? <EmptyState title="Нет данных" /> :
      <dl className="analytics-percentiles">{([
        ['P50', value.p50Seconds], ['P90', value.p90Seconds], ['P95', value.p95Seconds], ['P99', value.p99Seconds],
      ] as const).map(([label, seconds]) => <div key={label} aria-label={label}>
        <dt>{label}</dt><dd>{formatAnalyticsDuration(seconds)}</dd>
      </div>)}</dl>}
  </>;
}
