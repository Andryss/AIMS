--liquibase formatted sql

--changeset andryss:create-user-role-table
create table user_role (
    user_id bigint not null references app_user (id) on delete cascade,
    role_id bigint not null references role (id) on delete cascade,
    primary key (user_id, role_id)
);

comment on table user_role is 'User to role mapping';
