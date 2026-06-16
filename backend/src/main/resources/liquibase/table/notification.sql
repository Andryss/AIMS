--liquibase formatted sql

--changeset andryss:create-notification-table
create table notification (
    id bigserial primary key,
    recipient_user_id bigint not null references app_user(id),
    message text not null,
    related_entities jsonb not null default '[]',
    read_at timestamptz,
    created_at timestamptz not null default now()
);

comment on table notification is 'In-app notifications for users';
comment on column notification.related_entities is 'JSON array of entity refs, e.g. INCIDENT:612';
