-- @name INSERT создаёт id и сохраняет данные пользователя
-- @object public.app_user
-- @rule Сохранение корректных данных

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.run_sql(
    'Создание пользователя',
    $$
        INSERT INTO public.app_user(login, password_hash)
            VALUES ('db-test-user', 'original-hash')
    $$
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Отдельный SELECT подтверждает сгенерированный id и сохранённые значения',
    $$
        SELECT
            id > 0 AS generated_id,
            login,
            password_hash
        FROM
            public.app_user
        WHERE
            login = 'db-test-user'
    $$,
    '[
        {
            "generated_id": true,
            "login": "db-test-user",
            "password_hash": "original-hash"
        }
    ]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
