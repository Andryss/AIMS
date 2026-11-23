-- @name stored_file.created_at имеет ожидаемые свойства
-- @object public.stored_file
-- @rule Свойства created_at

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.column_is(
    'Тип, допустимость NULL и default соответствуют ожиданию',
    'stored_file',
    'created_at',
    'timestamp with time zone',
    false,
    'now()'
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
