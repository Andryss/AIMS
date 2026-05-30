--liquibase formatted sql

--changeset andryss:create-app-user-table
create table app_user (
    id bigserial primary key,
    login text not null unique,
    password_hash text not null
);

comment on table app_user is 'Application users';
comment on column app_user.login is 'User login';
comment on column app_user.password_hash is 'BCrypt password hash';
