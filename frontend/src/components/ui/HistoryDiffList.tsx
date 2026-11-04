import React from 'react';
import { HistoryDiffRow } from '../../utils/historyDiff';

interface HistoryDiffListProps<Value> {
  rows: readonly HistoryDiffRow<Value>[];
  renderValue: (value: Value) => React.ReactNode;
  emptyMessage?: React.ReactNode;
}

export function HistoryDiffList<Value>({ rows, renderValue, emptyMessage }: HistoryDiffListProps<Value>) {
  if (rows.length === 0) {
    return emptyMessage ? <p className="text-muted">{emptyMessage}</p> : null;
  }
  return <ul className="history-diff-list">{rows.map((row) => <li key={row.label} className="history-diff__row">
    <span className="history-diff__label">{row.label}:</span>
    <span className="text-pre-wrap">
      {row.oldValue !== undefined && <>{renderValue(row.oldValue)}{' → '}</>}{renderValue(row.newValue)}
    </span>
  </li>)}</ul>;
}
