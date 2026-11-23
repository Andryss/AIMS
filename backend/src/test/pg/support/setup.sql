CREATE SCHEMA test_support;
CREATE EXTENSION pgtap SCHEMA test_support;
SET search_path = public, test_support;
\ir reporting.sql
\ir fixtures.sql
