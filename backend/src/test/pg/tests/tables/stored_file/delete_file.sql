-- @name DELETE удаляет метаданные файла и сохраняет пользователя
-- @object public.stored_file
-- @rule Удаление файла

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(2);

-- Подготовка данных
SELECT test_support.step('Подготовка данных');

SELECT test_support.create_app_user(login => 'db-test-uploader', password_hash => 'hash') AS uploader_id \gset

SELECT test_support.create_stored_file(created_by_user_id => :uploader_id) AS file_id \gset

SELECT * FROM test_support.snapshot(
    'Файл и ссылка на пользователя до удаления',
    $$
        SELECT
            *
        FROM
            public.stored_file
        ORDER BY
            id
    $$
);

SELECT * FROM test_support.snapshot(
    'Пользователи до удаления файла',
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
    'Удаление метаданных файла',
    format('DELETE FROM public.stored_file WHERE id = %s', :file_id::bigint)
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Файл удалён',
    format('SELECT * FROM public.stored_file WHERE id = %s', :file_id::bigint),
    '[]'::jsonb
);

SELECT * FROM test_support.query_is(
    'Пользователь сохранился',
    format('SELECT login FROM public.app_user WHERE id = %s', :uploader_id::bigint),
    '[
        {
            "login": "db-test-uploader"
        }
    ]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
