--liquibase formatted sql

--changeset aims:create-tender-participant-table
create table tender_participant (
    id bigserial primary key,
    tender_id bigint not null references tender(id),
    name text not null,
    proposal text not null,
    status text not null,
    created_by_user_id bigint not null references app_user(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index tender_participant_tender_idx on tender_participant (tender_id, created_at, id);
create unique index tender_participant_winner_unique on tender_participant (tender_id) where status = 'WINNER';

comment on table tender_participant is 'Tender participants and their offers';
