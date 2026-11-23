-- Создаёт пользователя с переданными полями и возвращает его id.
CREATE FUNCTION test_support.create_app_user(
    login text DEFAULT 'db-test-user',
    password_hash text DEFAULT 'original-hash'
) RETURNS bigint
LANGUAGE sql AS $$
    INSERT INTO public.app_user (login, password_hash)
    VALUES (login, password_hash)
    RETURNING id;
$$;

-- Создаёт метаданные файла с переданными полями и возвращает его id.
CREATE FUNCTION test_support.create_stored_file(
    storage_id text DEFAULT 'db-test-storage',
    file_name text DEFAULT 'incident.txt',
    content_type text DEFAULT 'text/plain',
    file_size bigint DEFAULT 1024,
    created_by_user_id bigint DEFAULT NULL,
    created_at timestamptz DEFAULT now()
) RETURNS bigint
LANGUAGE sql AS $$
    INSERT INTO public.stored_file (
        storage_id, file_name, content_type, file_size, created_by_user_id, created_at
    )
    VALUES (storage_id, file_name, content_type, file_size, created_by_user_id, created_at)
    RETURNING id;
$$;

-- Создаёт инцидент с переданными полями и возвращает его id.
CREATE FUNCTION test_support.create_incident(
    created_by_user_id bigint,
    status text DEFAULT 'NEW',
    event_type text DEFAULT 'OBSERVATION',
    location text DEFAULT 'Test location',
    detected_at timestamp DEFAULT LOCALTIMESTAMP,
    description text DEFAULT 'Test incident',
    victim_count integer DEFAULT NULL,
    threat_level integer DEFAULT NULL,
    attachment_file_ids jsonb DEFAULT '[]'::jsonb,
    responsible_user_id bigint DEFAULT NULL,
    executor_user_ids jsonb DEFAULT '[]'::jsonb,
    created_at timestamp DEFAULT LOCALTIMESTAMP,
    updated_at timestamp DEFAULT LOCALTIMESTAMP
) RETURNS bigint
LANGUAGE sql AS $$
    INSERT INTO public.incident (
        status, event_type, location, detected_at, description,
        victim_count, threat_level, attachment_file_ids, created_by_user_id,
        responsible_user_id, executor_user_ids, created_at, updated_at
    )
    VALUES (
        status, event_type, location, detected_at, description,
        victim_count, threat_level, attachment_file_ids, created_by_user_id,
        responsible_user_id, executor_user_ids, created_at, updated_at
    )
    RETURNING id;
$$;
