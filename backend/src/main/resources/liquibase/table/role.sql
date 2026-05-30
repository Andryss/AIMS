--liquibase formatted sql

--changeset andryss:create-role-table
create table role (
    id bigserial primary key,
    name text not null unique,
    description text
);

comment on table role is 'RBAC roles';
comment on column role.name is 'Role name';
