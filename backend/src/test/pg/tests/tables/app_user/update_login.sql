-- @name UPDATE меняет login и сохраняет остальные поля
-- @object public.app_user
-- @rule Изменение login

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Подготовка данных
SELECT test_support.step('Подготовка данных');

SELECT test_support.create_app_user() AS user_id \gset

SELECT
    to_jsonb(u) AS user_before
FROM
    public.app_user u
WHERE
    id = :user_id \gset

SELECT * FROM test_support.snapshot(
    'Пользователи до действия',
    $$
        SELECT
            *
        FROM
            public.app_user
        ORDER BY
            id
    $$
);

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.run_sql(
    'Изменение login',
    format('UPDATE public.app_user SET login = %L WHERE id = %s', 'updated-login', :user_id::bigint)
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Изменённое поле и сохранность остальных полей соответствуют ожиданию',
    format('SELECT * FROM public.app_user WHERE id = %s', :user_id::bigint),
    jsonb_build_array(:'user_before'::jsonb || jsonb_build_object('login', 'updated-login'))
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
