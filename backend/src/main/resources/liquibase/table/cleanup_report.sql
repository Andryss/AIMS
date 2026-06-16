--liquibase formatted sql

--changeset aims:create-cleanup-report-table
create table cleanup_report (
    id bigserial primary key,
    incident_id bigint not null unique references incident(id),
    description text not null,
    attachment_file_ids jsonb not null default '[]',
    created_by_user_id bigint not null references app_user(id),
    created_at timestamp not null default now()
);

comment on table cleanup_report is 'One-time cleanup report per incident';

--changeset aims:add-incident-cleanup-fields
alter table incident add column cleanup_status text;
alter table incident add column cleanup_report_id bigint references cleanup_report(id);

comment on column incident.cleanup_status is 'Cleanup workflow status (nullable until first set)';
comment on column incident.cleanup_report_id is 'FK to cleanup report';
