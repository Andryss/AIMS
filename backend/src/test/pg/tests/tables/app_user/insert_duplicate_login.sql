-- @name INSERT с повторным login отклоняется без изменения данных
-- @object public.app_user.login
-- @rule UNIQUE(login)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(3);

-- Подготовка данных
SELECT test_support.step('Подготовка данных');

SELECT test_support.create_app_user() AS user_id \gset

SELECT
    count(*) AS count_before
FROM
    public.app_user \gset

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
    $$
);

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.sqlstate_is(
    'Повторный login вызывает нарушение уникальности',
    $$
        INSERT INTO public.app_user(login, password_hash)
            VALUES ('db-test-user', 'another-hash')
    $$,
    '23505'
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Количество пользователей сохранилось',
    $$
        SELECT
            count(*) AS total
        FROM
            public.app_user
    $$,
    jsonb_build_array(jsonb_build_object('total', :count_before::bigint))
);

SELECT * FROM test_support.query_is(
    'Исходная запись сохранилась полностью',
    format('SELECT * FROM public.app_user WHERE id = %s', :user_id::bigint),
    jsonb_build_array(:'user_before'::jsonb)
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
