import React from 'react';
import { Button } from './Button';

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  loading?: boolean;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, totalElements, loading, onChange }: PaginationProps) {
  if (totalPages < 1) {
    return null;
  }

  return (
    <div className="pagination">
      <Button variant="secondary" disabled={loading || page === 0} onClick={() => onChange(page - 1)}>
        ← Предыдущая
      </Button>
      <span className="pagination__info">Страница {page + 1} из {totalPages} ({totalElements} всего)</span>
      <Button variant="secondary" disabled={loading || page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Следующая →
      </Button>
    </div>
  );
}
