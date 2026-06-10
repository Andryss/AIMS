--liquibase formatted sql

--changeset aims:seed-alien-data
insert into alien (name, description, threat_level, created_at) values
    ('Слизень', 'Жидкая зеленая субстанция', 3, '2026-01-01 00:00:00'),
    ('Слизистый червь', 'Длинный толстый червь-людоед', 6, '2026-01-01 00:00:00');
