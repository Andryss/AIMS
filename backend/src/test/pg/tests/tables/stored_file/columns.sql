-- @name stored_file содержит ожидаемый набор колонок
-- @object public.stored_file
-- @rule Состав колонок

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.table_columns_are(
    'Состав и порядок колонок соответствует ожиданию',
    'stored_file',
    ARRAY['id', 'storage_id', 'file_name', 'content_type', 'file_size', 'created_at', 'created_by_user_id']
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
