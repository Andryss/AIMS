--liquibase formatted sql

--changeset aims:create-equipment-item-table
create table equipment_item (
    id bigserial primary key,
    type text not null,
    model text not null,
    serial_number text not null,
    status text not null,
    holder_user_id bigint references app_user(id),
    current_issue_movement_id bigint references equipment_movement(id),
    created_at timestamptz not null default now()
);

create unique index equipment_serial_number_unique on equipment_item (lower(serial_number));
create index equipment_current_issue_idx on equipment_item (current_issue_movement_id);

comment on table equipment_item is 'Individually tracked equipment units';
