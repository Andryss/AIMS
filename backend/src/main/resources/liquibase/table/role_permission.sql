--liquibase formatted sql

--changeset andryss:create-role-permission-table
create table role_permission (
    role_id bigint not null references role (id) on delete cascade,
    permission_id bigint not null references permission (id) on delete cascade,
    primary key (role_id, permission_id)
);

comment on table role_permission is 'Role to permission mapping';
