-- @name stored_file.file_size имеет ожидаемые свойства
-- @object public.stored_file
-- @rule Свойства file_size

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.column_is(
    'Тип, допустимость NULL и default соответствуют ожиданию',
    'stored_file',
    'file_size',
    'bigint',
    false,
    NULL
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
