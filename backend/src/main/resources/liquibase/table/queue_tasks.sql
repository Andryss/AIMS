--liquibase formatted sql

--changeset andryss:create-queue_tasks-table
CREATE TABLE queue_tasks (
    id                BIGSERIAL PRIMARY KEY,
    queue_name        TEXT NOT NULL,
    payload           TEXT,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT now(),
    next_process_at   TIMESTAMP WITH TIME ZONE DEFAULT now(),
    attempt           INTEGER                  DEFAULT 0,
    reenqueue_attempt INTEGER                  DEFAULT 0,
    total_attempt     INTEGER                  DEFAULT 0
) WITH (
    fillfactor = 30,
    autovacuum_vacuum_cost_delay = 5,
    autovacuum_vacuum_cost_limit = 500,
    autovacuum_vacuum_scale_factor = 0.0001
);

CREATE INDEX queue_tasks_name_time_desc_idx
    ON queue_tasks USING btree (queue_name, next_process_at, id DESC);

COMMENT ON TABLE queue_tasks IS 'События для асинхронной обработки';
