-- @name UPDATE с NULL в password_hash отклоняется
-- @object public.app_user
-- @rule NOT NULL(password_hash)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(2);

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

SELECT * FROM test_support.sqlstate_is(
    'Получен SQLSTATE NOT NULL',
    format('UPDATE public.app_user SET password_hash = NULL WHERE id = %s', :user_id::bigint),
    '23502'
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Исходный пользователь сохранился полностью',
    format('SELECT * FROM public.app_user WHERE id = %s', :user_id::bigint),
    jsonb_build_array(:'user_before'::jsonb)
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
