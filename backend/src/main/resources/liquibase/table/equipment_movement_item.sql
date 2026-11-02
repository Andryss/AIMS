--liquibase formatted sql

--changeset aims:create-equipment-movement-item-table
create table equipment_movement_item (
    id bigserial primary key,
    movement_id bigint not null references equipment_movement(id),
    equipment_item_id bigint not null references equipment_item(id),
    unique (movement_id, equipment_item_id)
);

create index equipment_movement_item_equipment_idx on equipment_movement_item (equipment_item_id);

comment on table equipment_movement_item is 'Equipment units selected for a movement request';
