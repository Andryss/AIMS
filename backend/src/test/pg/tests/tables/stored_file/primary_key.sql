-- @name Первичный ключ stored_file состоит из id
-- @object public.stored_file
-- @rule PRIMARY KEY(id)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.keys_are(
    'Первичный ключ состоит из id',
    'stored_file',
    'PRIMARY KEY',
    '[
        {
            "columns": [
                "id"
            ]
        }
    ]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
