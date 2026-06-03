--liquibase formatted sql

--changeset aims:create-incident-table
create table incident (
    id bigserial primary key,
    status text not null,
    event_type int not null,
    location text not null,
    detected_at timestamp not null,
    description text not null,
    attachment_file_ids jsonb not null default '[]',
    created_by_user_id bigint not null references app_user(id),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index idx_incident_status on incident(status);
create index idx_incident_detected_at on incident(detected_at);

comment on table incident is 'Alien incident records';
