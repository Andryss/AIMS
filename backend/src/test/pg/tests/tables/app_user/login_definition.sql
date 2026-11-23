-- @name app_user.login имеет ожидаемые свойства
-- @object public.app_user
-- @rule Свойства login

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.column_is(
    'Тип, допустимость NULL и default соответствуют ожиданию',
    'app_user',
    'login',
    'text',
    false,
    NULL
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
