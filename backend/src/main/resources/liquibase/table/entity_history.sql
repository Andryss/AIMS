--liquibase formatted sql

--changeset andryss:create-entity-history-table
create table entity_history (
    id bigserial primary key,
    entity_type text not null,
    entity_id bigint not null,
    snapshot jsonb not null,
    changed_by_user_id bigint not null references app_user(id),
    changed_at timestamptz not null default now()
);

create index entity_history_snapshot_idx on entity_history (entity_type, entity_id, changed_at desc, id desc);
create index entity_history_event_idx on entity_history (entity_type, changed_at);

comment on table entity_history is 'Audit snapshots of entity state after change';
comment on column entity_history.entity_type is 'Entity type code';
comment on column entity_history.snapshot is 'Full JSON dump of new entity state';
