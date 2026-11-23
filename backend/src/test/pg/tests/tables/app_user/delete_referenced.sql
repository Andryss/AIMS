-- @name DELETE пользователя со связанным файлом отклоняется
-- @object public.app_user
-- @rule FOREIGN KEY stored_file.created_by_user_id

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

SELECT test_support.create_stored_file(storage_id => 'db-test-linked-file', created_by_user_id => :user_id) AS file_id \gset

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.sqlstate_is(
    'Внешний ключ запрещает удаление пользователя',
    format('DELETE FROM public.app_user WHERE id = %s', :user_id::bigint),
    '23503'
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Исходный пользователь сохранился полностью',
    format('SELECT * FROM public.app_user WHERE id = %s', :user_id::bigint),
    jsonb_build_array(:'user_before'::jsonb)
);

SELECT * FROM test_support.query_is(
    'Ссылка файла на пользователя сохранилась',
    format('SELECT created_by_user_id FROM public.stored_file WHERE id = %s', :file_id::bigint),
    jsonb_build_array(jsonb_build_object('created_by_user_id', :user_id::bigint))
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
