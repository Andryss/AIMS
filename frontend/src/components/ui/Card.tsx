import React from 'react';

export interface CardProps {
  children: React.ReactNode;
  className?: string;
  flat?: boolean;
}

export function Card({ children, className = '', flat = false }: CardProps) {
  return (
    <section className={`card${flat ? ' card--flat' : ''}${className ? ` ${className}` : ''}`}>
      {children}
    </section>
  );
}

export interface CardHeaderProps {
  title: React.ReactNode;
  actions?: React.ReactNode;
}

export function CardHeader({ title, actions }: CardHeaderProps) {
  return (
    <div className="card__header">
      {typeof title === 'string' ? <h2>{title}</h2> : title}
      {actions}
    </div>
  );
}
