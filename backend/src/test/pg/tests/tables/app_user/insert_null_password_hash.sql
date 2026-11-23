-- @name INSERT с NULL в password_hash отклоняется
-- @object public.app_user
-- @rule NOT NULL

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(2);

-- Подготовка данных
SELECT test_support.step('Подготовка данных');

SELECT
    count(*) AS count_before
FROM
    public.app_user \gset

SELECT * FROM test_support.snapshot(
    'Количество записей до действия',
    'SELECT count(*) AS total FROM public.app_user'
);

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.sqlstate_is(
    'Получен SQLSTATE NOT NULL',
    $$
        INSERT INTO public.app_user(login, password_hash)
            VALUES ('db-test-user', NULL)
    $$,
    '23502'
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

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
