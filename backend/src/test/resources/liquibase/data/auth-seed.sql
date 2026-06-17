--liquibase formatted sql

--changeset aims:seed-auth-data
insert into permission (code, description) values
    ('INCIDENT_READ', 'Просмотр карточек и списка инцидентов'),
    ('INCIDENT_CREATE', 'Создание инцидентов'),
    ('INCIDENT_STATUS_CHANGE', 'Смена статуса инцидента'),
    ('INCIDENT_COMMENT', 'Добавление комментариев к инциденту'),
    ('INCIDENT_ALIEN_LINK', 'Привязка типа инопланетянина к инциденту'),
    ('ALIEN_READ', 'Просмотр и поиск в справочнике инопланетян'),
    ('USER_READ', 'Поиск пользователей и пакетное чтение профилей'),
    ('INCIDENT_ASSIGN', 'Назначение ответственного и исполнителей на инцидент'),
    ('CLEANUP_REPORT_READ', 'Просмотр отчёта об очистке'),
    ('CLEANUP_REPORT_CREATE', 'Создание отчёта об очистке'),
    ('CLEANUP_STATUS_CHANGE', 'Смена статуса очистки'),
    ('FILE_UPLOAD', 'Загрузка файлов'),
    ('FILE_READ', 'Скачивание файлов'),
    ('NOTIFICATION_READ', 'Просмотр уведомлений')
on conflict (code) do nothing;

insert into role (name, description) values
    ('OPERATOR', 'Оператор'),
    ('ANALYST', 'Аналитик'),
    ('ADMIN', 'Администратор'),
    ('AGENT', 'Оперативный агент'),
    ('CLEANER', 'Специалист по прикрытию');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'OPERATOR' and p.code in ('INCIDENT_READ', 'INCIDENT_CREATE', 'INCIDENT_STATUS_CHANGE', 'INCIDENT_COMMENT', 'USER_READ', 'CLEANUP_REPORT_READ', 'FILE_UPLOAD', 'FILE_READ', 'NOTIFICATION_READ');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'ANALYST' and p.code in ('INCIDENT_READ', 'INCIDENT_STATUS_CHANGE', 'INCIDENT_COMMENT', 'INCIDENT_ALIEN_LINK', 'ALIEN_READ', 'USER_READ', 'CLEANUP_REPORT_READ', 'FILE_UPLOAD', 'FILE_READ', 'NOTIFICATION_READ');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'ADMIN' and p.code in (
    'INCIDENT_READ', 'INCIDENT_CREATE', 'INCIDENT_STATUS_CHANGE', 'INCIDENT_COMMENT',
    'INCIDENT_ALIEN_LINK', 'ALIEN_READ', 'USER_READ', 'INCIDENT_ASSIGN', 'CLEANUP_REPORT_READ',
    'FILE_UPLOAD', 'FILE_READ', 'NOTIFICATION_READ'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'AGENT' and p.code in (
    'INCIDENT_READ', 'USER_READ', 'INCIDENT_ASSIGN', 'INCIDENT_STATUS_CHANGE',
    'INCIDENT_COMMENT', 'ALIEN_READ', 'CLEANUP_REPORT_READ', 'FILE_UPLOAD', 'FILE_READ', 'NOTIFICATION_READ'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'CLEANER' and p.code in (
    'INCIDENT_READ', 'USER_READ', 'INCIDENT_COMMENT', 'ALIEN_READ',
    'CLEANUP_REPORT_READ', 'CLEANUP_REPORT_CREATE', 'CLEANUP_STATUS_CHANGE',
    'FILE_UPLOAD', 'FILE_READ', 'NOTIFICATION_READ'
);

insert into app_user (login, password_hash) values
    ('operator', '$2a$10$3Lk4s75nd2Q0G1E8UWTmFu05efZd8lnw6OUw8yiJMvkN4fjhMKSfi'),
    ('analyst', '$2a$10$4jOz4wa0A4i9xlzxtJX5du72d2cbNfCjHt3TwPyDF6OlDXtIxK56W'),
    ('admin', '$2a$10$3ikPsa0.oG0Rolvk8W65Ke02QdCf1NguC9DxvAijlbu.54Vp/Wpb2'),
    ('agent', '$2a$10$6KMyGyv/5dg7pb9EROtxzeyLowuW/RhvFP/ZUy53PuhoSrkTIR7U2'),
    ('agent2', '$2y$10$PzesjIfh8erxGjUyrlVlMOWKVIC0Eezbyy2lAHHUB.dAjPjU3P7Ke'),
    ('cleaner', '$2y$10$X6NQQD/q6eihp.rlpGjFAenVE2o0rqpNMI8.urRcNFQWv5u4eRRRS');

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'operator' and r.name = 'OPERATOR';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'analyst' and r.name = 'ANALYST';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'admin' and r.name = 'ADMIN';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'agent' and r.name = 'AGENT';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'agent2' and r.name = 'AGENT';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'cleaner' and r.name = 'CLEANER';
