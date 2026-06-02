--liquibase formatted sql

--changeset andryss:create-stored-file-table
create table stored_file (
    id bigserial primary key,
    storage_id text not null unique,
    file_name text not null,
    content_type text not null,
    file_size bigint not null,
    created_at timestamptz not null default now(),
    created_by_user_id bigint references app_user(id)
);

comment on table stored_file is 'File metadata';
comment on column stored_file.storage_id is 'Unique storage key';
comment on column stored_file.created_by_user_id is 'Uploader user id';
