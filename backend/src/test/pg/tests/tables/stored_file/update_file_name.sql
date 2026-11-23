-- @name UPDATE меняет file_name и сохраняет остальные метаданные
-- @object public.stored_file
-- @rule Изменение file_name

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

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

-- Действие
SELECT test_support.step('Действие');

SELECT * FROM test_support.run_sql(
    'Изменение имени файла',
    format('UPDATE public.stored_file SET file_name = %L WHERE id = %s', 'updated.txt', :file_id::bigint)
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Новое имя и остальные метаданные соответствуют ожиданию',
    format('SELECT * FROM public.stored_file WHERE id = %s', :file_id::bigint),
    jsonb_build_array(:'file_before'::jsonb || jsonb_build_object('file_name', 'updated.txt'))
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
