-- @name DELETE автора существующего инцидента отклоняется
-- @object public.app_user.id; public.incident.created_by_user_id
-- @rule FOREIGN KEY incident.created_by_user_id

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(3);

-- Подготовка данных
SELECT test_support.step('Подготовка данных');

SELECT test_support.create_app_user(login => 'db-test-incident-author') AS user_id \gset

SELECT
    to_jsonb(u) AS user_before
FROM
    public.app_user u
WHERE
    id = :user_id \gset

SELECT test_support.create_incident(created_by_user_id => :user_id) AS incident_id \gset

SELECT * FROM test_support.snapshot(
    'Автор до действия',
    $$
        SELECT
            *
        FROM
            public.app_user
        ORDER BY
            id
    $$
);

SELECT * FROM test_support.snapshot(
    'Инцидент до действия',
    $$
        SELECT
            *
        FROM
            public.incident
        ORDER BY
            id
    $$
);

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.sqlstate_is(
    'Ссылка инцидента запрещает удаление автора',
    format('DELETE FROM public.app_user WHERE id = %s', :user_id::bigint),
    '23503'
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Данные автора сохранились',
    format('SELECT * FROM public.app_user WHERE id = %s', :user_id::bigint),
    jsonb_build_array(:'user_before'::jsonb)
);

SELECT * FROM test_support.query_is(
    'Ссылка инцидента сохранилась',
    format('SELECT created_by_user_id FROM public.incident WHERE id = %s', :incident_id::bigint),
    jsonb_build_array(jsonb_build_object('created_by_user_id', :user_id::bigint))
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
