-- @name INSERT с повторным storage_id отклоняется
-- @object public.stored_file
-- @rule UNIQUE(storage_id)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(3);

-- Подготовка данных
SELECT test_support.step('Подготовка данных');

SELECT test_support.create_stored_file() AS file_id \gset

SELECT
    to_jsonb(f) AS file_before
FROM
    public.stored_file f
WHERE
    id = :file_id \gset

SELECT * FROM test_support.snapshot(
    'Файлы до действия',
    $$
        SELECT
            *
        FROM
            public.stored_file
        ORDER BY
            id
    $$
);

SELECT
    count(*) AS count_before
FROM
    public.stored_file \gset

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.sqlstate_is(
    'Получен SQLSTATE UNIQUE',
    $$
        INSERT INTO public.stored_file(storage_id, file_name, content_type, file_size)
            VALUES ('db-test-storage', 'another.txt', 'text/plain', 2048)
    $$,
    '23505'
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Число файлов сохранилось',
    $$
        SELECT
            count(*) AS total
        FROM
            public.stored_file
    $$,
    jsonb_build_array(jsonb_build_object('total', :count_before::bigint))
);

SELECT * FROM test_support.query_is(
    'Исходные метаданные файла сохранились полностью',
    format('SELECT * FROM public.stored_file WHERE id = %s', :file_id::bigint),
    jsonb_build_array(:'file_before'::jsonb)
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
