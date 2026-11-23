-- @name stored_file.storage_id имеет ограничение уникальности
-- @object public.stored_file
-- @rule UNIQUE(storage_id)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.keys_are(
    'Ожидаемый уникальный ключ существует',
    'stored_file',
    'UNIQUE',
    '[
        {
            "columns": [
                "storage_id"
            ]
        }
    ]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
