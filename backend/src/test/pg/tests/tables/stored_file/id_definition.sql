-- @name stored_file.id имеет свойства BIGSERIAL
-- @object public.stored_file
-- @rule BIGSERIAL PRIMARY KEY

BEGIN;
SET LOCAL search_path = public, test_support;
SELECT plan(1);

-- Проверка схемы

SELECT * FROM test_support.query_is(
    'Тип, обязательность, генерация и привязка последовательности соответствуют ожиданию',
    $$
        SELECT
            data_type AS type,
            is_nullable = 'YES' AS nullable,
            column_default LIKE 'nextval(%' AS generated,
            pg_get_serial_sequence('public.stored_file', 'id') IS NOT NULL AS owned_sequence
        FROM
            information_schema.columns
        WHERE
            table_schema = 'public'
            AND table_name = 'stored_file'
            AND column_name = 'id'
    $$,
    '[
        {
            "type": "bigint",
            "nullable": false,
            "generated": true,
            "owned_sequence": true
        }
    ]'::jsonb
);

-- Завершение и откат тестовых данных
SELECT * FROM finish();
ROLLBACK;
