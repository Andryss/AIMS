--liquibase formatted sql

--changeset aims:create-incident-comment-table
create table incident_comment (
    id bigserial primary key,
    incident_id bigint not null references incident(id),
    author_user_id bigint not null references app_user(id),
    text text not null,
    created_at timestamptz not null default now()
);

create index idx_incident_comment_incident_created
    on incident_comment (incident_id, created_at desc);

comment on table incident_comment is 'User comments on incidents';

--changeset aims:entity-history-list-index
create index idx_entity_history_entity_changed
    on entity_history (entity_type, entity_id, changed_at);
