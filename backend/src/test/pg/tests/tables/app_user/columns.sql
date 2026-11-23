-- @name app_user содержит ожидаемый набор колонок
-- @object public.app_user
-- @rule Состав колонок

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.table_columns_are(
    'Состав и порядок колонок соответствует ожиданию',
    'app_user',
    ARRAY['id', 'login', 'password_hash']
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
