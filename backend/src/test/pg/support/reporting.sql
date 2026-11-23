-- Передаёт структурированные данные отчёту через TAP-диагностику.
CREATE FUNCTION test_support.fact(payload jsonb) RETURNS text
LANGUAGE sql AS $$
    SELECT test_support.diag('AIMS_FACT ' || payload::text);
$$;

-- Отмечает начало этапа сценария в отчёте.
CREATE FUNCTION test_support.step(title text) RETURNS text
LANGUAGE plpgsql AS $$
BEGIN
    RETURN test_support.fact(jsonb_build_object(
        'kind', 'phase', 'name', title));
END;
$$;

-- Выполняет SELECT и возвращает строки JSON-массивом, включая пустой результат.
CREATE FUNCTION test_support.read_rows(query text) RETURNS jsonb
LANGUAGE plpgsql AS $$
DECLARE
    rows jsonb;
BEGIN
    EXECUTE format('SELECT coalesce(jsonb_agg(to_jsonb(result)), ''[]''::jsonb) FROM (%s) result', query)
        INTO rows;
    RETURN rows;
END;
$$;

-- Сохраняет SQL и снимок данных в отчёт без добавления утверждения pgTAP.
CREATE FUNCTION test_support.snapshot(title text, query text) RETURNS SETOF text
LANGUAGE plpgsql AS $$
DECLARE
    rows jsonb;
BEGIN
    rows := test_support.read_rows(query);
    RETURN NEXT test_support.fact(jsonb_build_object(
        'kind', 'snapshot', 'name', title, 'sql', query, 'actual', rows));
END;
$$;

-- Сравнивает строки SELECT с ожидаемым JSON и сохраняет оба значения.
CREATE FUNCTION test_support.query_is(title text, query text, expected jsonb) RETURNS SETOF text
LANGUAGE plpgsql AS $$
DECLARE
    actual jsonb;
BEGIN
    actual := test_support.read_rows(query);
    RETURN NEXT test_support.is(actual, expected, title);
    RETURN NEXT test_support.fact(jsonb_build_object(
        'kind', 'check', 'name', title, 'sql', query, 'expected', expected, 'actual', actual, 'passed', actual IS NOT DISTINCT FROM expected));
END;
$$;

-- Выполняет запрос один раз, перехватывает SQL-ошибку и проверяет SQLSTATE.
CREATE FUNCTION test_support.sqlstate_is(title text, query text, expected text) RETURNS SETOF text
LANGUAGE plpgsql AS $$
DECLARE
    actual text := '00000';
    error_message text;
    affected bigint := 0;
BEGIN
    BEGIN
        EXECUTE query;
        GET DIAGNOSTICS affected = ROW_COUNT;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS actual = RETURNED_SQLSTATE, error_message = MESSAGE_TEXT;
    END;
    RETURN NEXT test_support.is(actual, expected, title);
    RETURN NEXT test_support.fact(jsonb_build_object(
        'kind', 'check', 'name', title, 'sql', query, 'expected', expected, 'actual', actual, 'passed', actual IS NOT DISTINCT FROM expected,
        'sqlstate', actual, 'message', error_message, 'affected_rows', affected));
END;
$$;

-- Выполняет действие и сохраняет SQL и число изменённых строк в отчёт.
CREATE FUNCTION test_support.run_sql(title text, query text) RETURNS SETOF text
LANGUAGE plpgsql AS $$
DECLARE
    affected bigint;
BEGIN
    EXECUTE query;
    GET DIAGNOSTICS affected = ROW_COUNT;
    RETURN NEXT test_support.fact(jsonb_build_object(
        'kind', 'action', 'name', title, 'sql', query, 'actual', affected));
END;
$$;

-- Проверяет тип одной колонки таблицы public.
CREATE FUNCTION test_support.column_type_is(title text, table_name text, column_name text, expected text)
RETURNS SETOF text LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY SELECT * FROM test_support.query_is(
        title,
        format('SELECT data_type AS type FROM information_schema.columns WHERE table_schema = ''public'' AND table_name = %L AND column_name = %L',
            table_name, column_name),
        jsonb_build_array(jsonb_build_object('type', expected)));
END;
$$;

-- Проверяет колонки PRIMARY KEY или UNIQUE без проверки имён ограничений.
CREATE FUNCTION test_support.keys_are(title text, table_name text, key_type text, expected jsonb)
RETURNS SETOF text LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY SELECT * FROM test_support.query_is(title, format($query$
        SELECT array_agg(k.column_name::text ORDER BY k.ordinal_position) AS columns
          FROM information_schema.table_constraints c
          JOIN information_schema.key_column_usage k
            ON k.constraint_schema = c.constraint_schema
           AND k.constraint_name = c.constraint_name
           AND k.table_name = c.table_name
         WHERE c.table_schema = 'public' AND c.table_name = %L AND c.constraint_type = %L
         GROUP BY c.constraint_name
         ORDER BY array_agg(k.column_name::text ORDER BY k.ordinal_position)
        $query$, table_name, key_type), expected);
END;
$$;

-- Проверяет тип колонки, допустимость NULL и значение DEFAULT.
CREATE FUNCTION test_support.column_is(title text, table_name text, column_name text, expected_type text,
    nullable boolean, default_value text) RETURNS SETOF text
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY SELECT * FROM test_support.query_is(title, format($query$
        SELECT data_type AS type, is_nullable = 'YES' AS nullable, column_default AS default
          FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = %L AND column_name = %L
        $query$, table_name, column_name),
        jsonb_build_array(jsonb_build_object('type', expected_type, 'nullable', nullable, 'default', default_value)));
END;
$$;

-- Проверяет полный список колонок таблицы и их порядок.
CREATE FUNCTION test_support.table_columns_are(title text, table_name text, expected text[])
RETURNS SETOF text LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY SELECT * FROM test_support.query_is(title, format($query$
        SELECT array_agg(column_name::text ORDER BY ordinal_position) AS columns
          FROM information_schema.columns WHERE table_schema = 'public' AND table_name = %L
        $query$, table_name), jsonb_build_array(jsonb_build_object('columns', to_jsonb(expected))));
END;
$$;

-- Проверяет одиночные внешние ключи: колонки, целевые таблицы и ON DELETE.
CREATE FUNCTION test_support.foreign_keys_are(title text, table_name text, expected jsonb)
RETURNS SETOF text LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY SELECT * FROM test_support.query_is(title, format($query$
        SELECT (SELECT attname FROM pg_attribute WHERE attrelid = c.conrelid AND attnum = c.conkey[1]) AS column,
               c.confrelid::regclass::text AS target_table,
               (SELECT attname FROM pg_attribute WHERE attrelid = c.confrelid AND attnum = c.confkey[1]) AS target_column,
               c.confdeltype::text AS on_delete,
               cardinality(c.conkey) AS column_count
          FROM pg_constraint c
         WHERE c.conrelid = %L::regclass AND c.contype = 'f'
         ORDER BY c.conkey[1]
        $query$, format('%I.%I', 'public', table_name)), expected);
END;
$$;
