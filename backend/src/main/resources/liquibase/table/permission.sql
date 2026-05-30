--liquibase formatted sql

--changeset andryss:create-permission-table
create table permission (
    id bigserial primary key,
    code text not null unique,
    description text
);

comment on table permission is 'RBAC permissions';
comment on column permission.code is 'Permission code';
