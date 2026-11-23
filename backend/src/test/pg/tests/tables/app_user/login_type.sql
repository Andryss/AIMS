-- @name app_user.login имеет тип text
-- @object public.app_user.login
-- @rule Тип колонки login

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.column_type_is(
    'Тип login соответствует ожиданию',
    'app_user',
    'login',
    'text'
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
