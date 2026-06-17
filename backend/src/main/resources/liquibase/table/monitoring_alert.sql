--liquibase formatted sql

--changeset aims:create-monitoring-alert-table
create table monitoring_alert (
    id bigserial primary key,
    external_event_id text not null,
    source_system text not null,
    status text not null,
    event_type text not null,
    location text not null,
    detected_at timestamp not null,
    description text not null,
    media_urls jsonb not null default '[]',
    raw_payload jsonb not null,
    incident_id bigint references incident(id),
    received_at timestamp not null default now(),
    created_at timestamp not null default now(),
    constraint uq_monitoring_alert_external_event_id unique (external_event_id)
);

create index idx_monitoring_alert_status on monitoring_alert (status);
create index idx_monitoring_alert_incident_id on monitoring_alert (incident_id);

comment on table monitoring_alert is 'Inbound alerts from external monitoring systems';

--changeset aims:add-incident-monitoring-alert-id
alter table incident add column monitoring_alert_id bigint references monitoring_alert(id);
create unique index uq_incident_monitoring_alert_id on incident (monitoring_alert_id) where monitoring_alert_id is not null;
