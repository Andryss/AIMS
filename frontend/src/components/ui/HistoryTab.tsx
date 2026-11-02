import React, { useId } from 'react';

export function HistoryTab({ children }: { children: React.ReactNode }) {
  const id = useId();

  return (
    <section className="section">
      <div className="tabs">
        <div className="tabs__bar" role="tablist" aria-label="Разделы карточки">
          <button type="button" role="tab" id={`${id}-tab`} aria-selected="true" aria-controls={`${id}-panel`}
            className="tabs__tab tabs__tab--active">История</button>
        </div>
        <div id={`${id}-panel`} className="tabs__panel" role="tabpanel" aria-labelledby={`${id}-tab`}>
          {children}
        </div>
      </div>
    </section>
  );
}
