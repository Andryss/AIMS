--liquibase formatted sql

--changeset aims:create-incident-comment-table
create table incident_comment (
    id bigserial primary key,
    incident_id bigint not null references incident(id),
    author_user_id bigint not null references app_user(id),
    text text not null,
    created_at timestamptz not null default now()
);

comment on table incident_comment is 'User comments on incidents';
