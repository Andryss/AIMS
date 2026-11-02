--liquibase formatted sql

--changeset aims:create-equipment-movement-table
create table equipment_movement (
    id bigserial primary key,
    operation text not null,
    status text not null,
    incident_id bigint not null references incident(id),
    responsible_user_id bigint not null references app_user(id),
    source_issue_movement_id bigint references equipment_movement(id),
    created_by_user_id bigint not null references app_user(id),
    comment text,
    created_at timestamptz not null default now(),
    completed_at timestamptz
);

create index equipment_movement_incident_status_idx on equipment_movement (incident_id, status);
create index equipment_movement_created_idx on equipment_movement (created_at desc, id desc);

comment on table equipment_movement is 'Requests for equipment issue and return linked to incidents';
