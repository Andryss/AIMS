import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLElement> {
  children: React.ReactNode;
  className?: string;
  flat?: boolean;
}

export function Card({ children, className = '', flat = false, ...rest }: CardProps) {
  return (
    <section className={`card${flat ? ' card--flat' : ''}${className ? ` ${className}` : ''}`} {...rest}>
      {children}
    </section>
  );
}

export type CardHeaderProps = {
  title: React.ReactNode;
  actions?: React.ReactNode;
  children?: never;
} | {
  children: React.ReactNode;
  title?: never;
  actions?: never;
};

export function CardHeader({ title, actions, children }: CardHeaderProps) {
  return (
    <div className="card__header">
      {children ?? <>{typeof title === 'string' ? <h2>{title}</h2> : title}{actions}</>}
    </div>
  );
}
