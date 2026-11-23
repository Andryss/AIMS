-- @name INSERT сохраняет ссылку на существующего пользователя
-- @object public.stored_file
-- @rule FOREIGN KEY(created_by_user_id)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Подготовка данных
SELECT test_support.step('Подготовка данных');

SELECT test_support.create_app_user(login => 'db-test-uploader', password_hash => 'hash') AS uploader_id \gset

SELECT * FROM test_support.snapshot(
    'Пользователь до создания файла',
    'SELECT id, login FROM public.app_user ORDER BY id'
);

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.run_sql(
    'Создание файла с загрузившим пользователем',
    format(
        $$
            INSERT INTO public.stored_file(storage_id, file_name, content_type, file_size, created_by_user_id)
                VALUES (%L, %L, %L, 1024, %s)
        $$,
        'db-test-storage',
        'incident.txt',
        'text/plain',
        :uploader_id::bigint
    )
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Ссылка на пользователя сохранена',
    $$
        SELECT
            created_by_user_id
        FROM
            public.stored_file
        WHERE
            storage_id = 'db-test-storage'
    $$,
    jsonb_build_array(jsonb_build_object('created_by_user_id', :uploader_id::bigint))
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
