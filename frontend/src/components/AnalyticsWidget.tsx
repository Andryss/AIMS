import React from 'react';
import { AnalyticsMeta, AnalyticsPeriodQuery } from '../types';
import { useAnalyticsResource } from '../hooks/useAnalyticsResource';
import { Button } from './ui/Button';
import { LoadingBlock } from './ui/LoadingBlock';

interface AnalyticsWidgetProps<T extends { meta: AnalyticsMeta }, Q extends AnalyticsPeriodQuery> {
  title: string;
  token: string;
  query: Q;
  load: (token: string, query: Q) => Promise<T>;
  children: (value: T) => React.ReactNode;
  compact?: boolean;
}

export function AnalyticsWidget<T extends { meta: AnalyticsMeta }, Q extends AnalyticsPeriodQuery>(
  { title, token, query, load, children, compact }: AnalyticsWidgetProps<T, Q>,
) {
  const { data, loading, error, retry } = useAnalyticsResource(token, query, load);
  return (
    <section className={`analytics-widget${compact ? ' analytics-widget--compact' : ''}`} aria-label={title} aria-busy={loading}>
      <h3>{title}</h3>
      {loading && <LoadingBlock label={`Загрузка: ${title}…`} inline />}
      {error && <><p className="alert alert--error" role="alert">{error}</p>
        <Button variant="outline" size="sm" aria-label={`Повторить: ${title}`} onClick={retry}>Повторить</Button></>}
      {!loading && !error && data && <>
        {children(data)}
        {data.meta.recordsWithoutSnapshot > 0 && <p className="analytics-note" role="note">
          Неполная история: {data.meta.recordsWithoutSnapshot}. Некоторые записи не учтены.
        </p>}
      </>}
    </section>
  );
}
