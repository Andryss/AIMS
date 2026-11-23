-- @name stored_file.created_by_user_id имеет ожидаемые свойства
-- @object public.stored_file
-- @rule Свойства created_by_user_id

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.column_is(
    'Тип, допустимость NULL и default соответствуют ожиданию',
    'stored_file',
    'created_by_user_id',
    'bigint',
    true,
    NULL
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
