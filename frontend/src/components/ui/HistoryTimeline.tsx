import React from 'react';

interface HistoryTimelineProps<Entry extends { id: React.Key }> {
  entries: readonly Entry[];
  previousEntry?: Entry;
  renderHeader: (entry: Entry, index: number) => React.ReactNode;
  children: (entry: Entry, previousEntry: Entry | undefined, index: number) => React.ReactNode;
}

/** Entries are chronological; the first row may continue a preceding page. */
export function HistoryTimeline<Entry extends { id: React.Key }>({ entries, previousEntry, renderHeader, children }: HistoryTimelineProps<Entry>) {
  return <ol className="history-timeline">{entries.map((entry, index) => <li key={entry.id} className="history-timeline__item">
    <div className="history-timeline__header">{renderHeader(entry, index)}</div>
    {children(entry, index > 0 ? entries[index - 1] : previousEntry, index)}
  </li>)}</ol>;
}
