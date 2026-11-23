-- @name DELETE удаляет пользователя без внешних ссылок
-- @object public.app_user
-- @rule Удаление несвязанной записи

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
    'Удаление пользователя',
    format('DELETE FROM public.app_user WHERE id = %s', :user_id::bigint)
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Удалённый пользователь отсутствует',
    format('SELECT * FROM public.app_user WHERE id = %s', :user_id::bigint),
    '[]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
