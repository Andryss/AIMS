-- @name UPDATE со ссылкой на отсутствующего пользователя отклоняется
-- @object public.stored_file
-- @rule FOREIGN KEY(created_by_user_id)

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(2);

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

SELECT * FROM test_support.sqlstate_is(
    'Получен SQLSTATE FOREIGN KEY',
    format(
        $$
            UPDATE
                public.stored_file
            SET
                created_by_user_id = -9223372036854775808
            WHERE
                id = %s
        $$,
        :file_id::bigint
    ),
    '23503'
);

-- Проверка результата
SELECT test_support.step('Проверка результата');

SELECT * FROM test_support.query_is(
    'Исходные метаданные файла сохранились полностью',
    format('SELECT * FROM public.stored_file WHERE id = %s', :file_id::bigint),
    jsonb_build_array(:'file_before'::jsonb)
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
