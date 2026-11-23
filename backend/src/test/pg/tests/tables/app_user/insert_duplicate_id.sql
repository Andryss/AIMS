-- @name INSERT с повторным id отклоняется
-- @object public.app_user
-- @rule PRIMARY KEY(id)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(3);

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

SELECT
    count(*) AS count_before
FROM
    public.app_user \gset

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.sqlstate_is(
    'Повторный id вызывает нарушение уникальности',
    format(
        $$
            INSERT INTO public.app_user(id, login, password_hash)
                VALUES (%s, %L, %L)
        $$,
        :user_id::bigint,
        'another-user',
        'another-hash'
    ),
    '23505'
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Число записей сохранилось',
    $$
        SELECT
            count(*) AS total
        FROM
            public.app_user
    $$,
    jsonb_build_array(jsonb_build_object('total', :count_before::bigint))
);

SELECT * FROM test_support.query_is(
    'Исходный пользователь сохранился полностью',
    format('SELECT * FROM public.app_user WHERE id = %s', :user_id::bigint),
    jsonb_build_array(:'user_before'::jsonb)
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
