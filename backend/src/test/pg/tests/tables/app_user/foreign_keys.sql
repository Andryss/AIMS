-- @name app_user имеет ожидаемые внешние ключи
-- @object public.app_user
-- @rule FOREIGN KEY и ON DELETE

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.foreign_keys_are(
    'Колонки, целевая таблица и ON DELETE соответствуют ожиданию',
    'app_user',
    '[]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
