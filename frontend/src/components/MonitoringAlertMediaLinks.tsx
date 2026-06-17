import React from 'react';

function linkLabel(url: string, index: number): string {
  try {
    const parsed = new URL(url);
    const fileName = parsed.pathname.split('/').filter(Boolean).pop();
    if (fileName) {
      return fileName;
    }
  } catch {
    // keep fallback below
  }
  return `Материал ${index + 1}`;
}

interface MonitoringAlertMediaLinksProps {
  urls: string[];
  compact?: boolean;
}

export function MonitoringAlertMediaLinks({ urls, compact = false }: MonitoringAlertMediaLinksProps) {
  if (urls.length === 0) {
    return null;
  }

  return (
    <div className={compact ? 'monitoring-alert-media monitoring-alert-media--compact' : 'monitoring-alert-media'}>
      <p className="monitoring-alert-media__title">Медиа от внешней системы</p>
      <ul className="monitoring-alert-media__list">
        {urls.map((url, index) => (
          <li key={`${url}-${index}`} className="monitoring-alert-media__item">
            <a
              href={url}
              className="monitoring-alert-media__link"
              target="_blank"
              rel="noreferrer noopener"
            >
              <span className="monitoring-alert-media__link-text">{linkLabel(url, index)}</span>
              <span className="monitoring-alert-media__link-hint" aria-hidden="true">
                ↗
              </span>
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
