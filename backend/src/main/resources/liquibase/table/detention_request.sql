--liquibase formatted sql

--changeset aims:create-detention-request-table
create table detention_request (
    id bigserial primary key,
    incident_id bigint not null references incident(id),
    subject_name text not null,
    subject_description text not null,
    alien_id bigint references alien(id),
    duration_days integer not null,
    status text not null,
    created_by_user_id bigint not null references app_user(id),
    reviewed_by_user_id bigint references app_user(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    admitted_at timestamptz,
    released_at timestamptz
);

create index detention_request_incident_status_idx on detention_request (incident_id, status);
create index detention_request_created_idx on detention_request (created_at desc, id desc);

comment on table detention_request is 'Detention requests and actual custody linked to incidents';
