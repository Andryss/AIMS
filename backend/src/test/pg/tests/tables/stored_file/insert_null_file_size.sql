-- @name INSERT с NULL в file_size отклоняется
-- @object public.stored_file
-- @rule NOT NULL(file_size)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(2);

-- Подготовка данных
SELECT test_support.step('Подготовка данных');

SELECT
    count(*) AS count_before
FROM
    public.stored_file \gset

SELECT * FROM test_support.snapshot(
    'Количество записей до действия',
    'SELECT count(*) AS total FROM public.stored_file'
);

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.sqlstate_is(
    'Получен SQLSTATE NOT NULL',
    $$
        INSERT INTO public.stored_file(storage_id, file_name, content_type, file_size)
            VALUES ('db-test-storage', 'incident.txt', 'text/plain', NULL)
    $$,
    '23502'
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

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
