--liquibase formatted sql

--changeset aims:create-alien-table
create table alien (
    id bigserial primary key,
    name text not null,
    description text not null,
    threat_level smallint not null,
    created_at timestamp not null default now()
);

create index idx_alien_name_lower on alien (lower(name));

comment on table alien is 'Alien knowledge base entries';

--changeset aims:add-incident-alien-id
alter table incident add column alien_id bigint references alien(id);

create index idx_incident_alien_id on incident(alien_id);
