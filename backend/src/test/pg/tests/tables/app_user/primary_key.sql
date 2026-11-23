-- @name Первичный ключ app_user состоит из id
-- @object public.app_user
-- @rule PRIMARY KEY(id)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.keys_are(
    'Первичный ключ состоит из id',
    'app_user',
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
