--liquibase formatted sql

--changeset aims:create-incident-table
create table incident (
    id bigserial primary key,
    status text not null,
    event_type text not null,
    location text not null,
    detected_at timestamp not null,
    description text not null,
    attachment_file_ids jsonb not null default '[]',
    created_by_user_id bigint not null references app_user(id),
    responsible_user_id bigint references app_user(id),
    executor_user_ids jsonb not null default '[]',
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

comment on table incident is 'Alien incident records';
comment on column incident.responsible_user_id is 'Assigned responsible agent user id';
comment on column incident.executor_user_ids is 'Assigned executor agent user ids (json array)';
