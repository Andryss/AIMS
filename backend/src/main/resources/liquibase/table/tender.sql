--liquibase formatted sql

--changeset aims:create-tender-table
create table tender (
    id bigserial primary key,
    equipment_type text not null,
    quantity integer not null,
    conditions text,
    incident_id bigint references incident(id),
    status text not null,
    created_by_user_id bigint not null references app_user(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    completed_at timestamptz
);

create index tender_incident_idx on tender (incident_id);
create index tender_created_idx on tender (created_at desc, id desc);

comment on table tender is 'Equipment procurement tenders';
