import React from 'react';
import { AnalyticsPeriodQuery, CountAnalyticsResponse } from '../types';
import { formatAnalyticsNumber } from '../utils/analyticsFormatting';
import { AnalyticsWidget } from './AnalyticsWidget';

interface AnalyticsSummaryProps<Q extends AnalyticsPeriodQuery> {
  title: string; token: string; query: Q;
  load: (token: string, query: Q) => Promise<CountAnalyticsResponse>;
}

export function AnalyticsSummary<Q extends AnalyticsPeriodQuery>(props: AnalyticsSummaryProps<Q>) {
  return <AnalyticsWidget {...props} compact>{(data) =>
    <p className="analytics-value">{formatAnalyticsNumber(data.count)}</p>
  }</AnalyticsWidget>;
}
