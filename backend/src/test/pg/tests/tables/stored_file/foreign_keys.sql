-- @name stored_file имеет ожидаемые внешние ключи
-- @object public.stored_file
-- @rule FOREIGN KEY и ON DELETE

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.foreign_keys_are(
    'Колонки, целевая таблица и ON DELETE соответствуют ожиданию',
    'stored_file',
    '[
        {
            "column": "created_by_user_id",
            "target_table": "app_user",
            "target_column": "id",
            "on_delete": "a",
            "column_count": 1
        }
    ]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
