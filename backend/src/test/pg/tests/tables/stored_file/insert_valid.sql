-- @name INSERT сохраняет файл без загрузившего пользователя и заполняет defaults
-- @object public.stored_file
-- @rule Сохранение метаданных и DEFAULT now()

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.run_sql(
    'Создание метаданных файла',
    $$
        INSERT INTO public.stored_file(storage_id, file_name, content_type, file_size)
            VALUES ('db-test-storage', 'incident.txt', 'text/plain', 1024)
    $$
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Отдельное чтение подтверждает метаданные, id, NULL автора и время начала транзакции',
    $$
        SELECT
            id > 0 AS generated_id,
            storage_id,
            file_name,
            content_type,
            file_size,
            created_by_user_id,
            created_at = transaction_timestamp() AS transaction_default
        FROM
            public.stored_file
        WHERE
            storage_id = 'db-test-storage'
    $$,
    '[
        {
            "generated_id": true,
            "storage_id": "db-test-storage",
            "file_name": "incident.txt",
            "content_type": "text/plain",
            "file_size": 1024,
            "created_by_user_id": null,
            "transaction_default": true
        }
    ]'::jsonb
);

SELECT * FROM test_support.snapshot(
    'Фактическое время создания и ожидаемая граница DEFAULT now()',
    $$
        SELECT
            created_at,
            transaction_timestamp() AS expected_created_at
        FROM
            public.stored_file
        WHERE
            storage_id = 'db-test-storage'
    $$
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
