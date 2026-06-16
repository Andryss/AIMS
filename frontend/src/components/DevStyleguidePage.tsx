import React from 'react';
import { Button } from './ui/Button';
import { Card, CardHeader } from './ui/Card';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { Spinner } from './ui/Spinner';
import { StatusChip } from './ui/StatusChip';

export function DevStyleguidePage() {
  return (
    <div className="styleguide">
      <Card>
        <CardHeader title="AIMS UI Styleguide" />
        <p className="text-muted">Живая документация дизайн-системы (только dev).</p>
      </Card>

      <Card className="styleguide__section">
        <h3>Кнопки</h3>
        <div className="styleguide__row">
          <Button variant="primary">Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="outline">Outline</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="danger">Danger</Button>
          <Button variant="primary" loading>
            Loading
          </Button>
        </div>
      </Card>

      <Card className="styleguide__section">
        <h3>Статусы</h3>
        <div className="styleguide__row">
          <StatusChip status="draft" label="Черновик" />
          <StatusChip status="executing" label="Выполняется" />
          <StatusChip status="cleanup_completed" label="Очистка завершена" />
        </div>
      </Card>

      <Card className="styleguide__section">
        <h3>Форма</h3>
        <form className="form" onSubmit={(e) => e.preventDefault()}>
          <FormField label="Пример поля" required>
            <input type="text" placeholder="Введите текст" />
          </FormField>
          <FormField label="С ошибкой" error="Обязательное поле">
            <input type="text" aria-invalid />
          </FormField>
        </form>
      </Card>

      <Card className="styleguide__section">
        <h3>Состояния</h3>
        <LoadingBlock label="Загрузка данных…" />
        <EmptyState title="Нет данных" hint="Создайте первую запись, чтобы увидеть список." />
        <div className="styleguide__row">
          <Spinner />
        </div>
      </Card>

      <Card className="styleguide__section">
        <h3>Алерты</h3>
        <div className="alert alert--error">Пример сообщения об ошибке</div>
      </Card>

      <Card className="styleguide__section">
        <h3>Цвета (токены)</h3>
        <div className="styleguide__swatches">
          <div className="styleguide__swatch" style={{ background: 'var(--color-primary)' }} title="primary" />
          <div className="styleguide__swatch" style={{ background: 'var(--status-draft-bg)' }} title="draft" />
          <div className="styleguide__swatch" style={{ background: 'var(--status-executing-bg)' }} title="executing" />
        </div>
      </Card>
    </div>
  );
}
