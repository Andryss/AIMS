-- @name app_user.login имеет ограничение уникальности
-- @object public.app_user
-- @rule UNIQUE(login)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.keys_are(
    'Ожидаемый уникальный ключ существует',
    'app_user',
    'UNIQUE',
    '[
        {
            "columns": [
                "login"
            ]
        }
    ]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
