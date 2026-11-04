--liquibase formatted sql

--changeset aims:seed-auth-data
insert into permission (code, description) values
    ('INCIDENT_READ', 'Просмотр карточек и списка инцидентов'),
    ('INCIDENT_CREATE', 'Создание инцидентов'),
    ('INCIDENT_STATUS_CHANGE', 'Смена статуса инцидента'),
    ('INCIDENT_COMMENT', 'Добавление комментариев к инциденту'),
    ('INCIDENT_THREAT_CALCULATE', 'Расчёт и принятие уровня угрозы инцидента'),
    ('INCIDENT_ALIEN_CLASSIFY', 'Предложение вида по изображению инцидента'),
    ('INCIDENT_ALIEN_LINK', 'Привязка типа инопланетянина к инциденту'),
    ('ALIEN_READ', 'Просмотр и поиск в справочнике инопланетян'),
    ('ALIEN_WRITE', 'Создание и редактирование записей справочника инопланетян'),
    ('USER_READ', 'Поиск пользователей и пакетное чтение профилей'),
    ('INCIDENT_ASSIGN', 'Назначение ответственного и исполнителей на инцидент'),
    ('EQUIPMENT_READ', 'Просмотр склада оборудования'),
    ('EQUIPMENT_WRITE', 'Регистрация и редактирование оборудования'),
    ('EQUIPMENT_MOVEMENT_READ', 'Просмотр заявок на движение оборудования'),
    ('EQUIPMENT_MOVEMENT_CREATE', 'Создание заявок на движение оборудования'),
    ('EQUIPMENT_MOVEMENT_PROCESS', 'Обработка заявок на движение оборудования'),
    ('TENDER_READ', 'Просмотр тендеров'),
    ('TENDER_CREATE', 'Создание тендеров'),
    ('TENDER_EDIT', 'Редактирование тендеров'),
    ('TENDER_MANAGE', 'Обработка тендеров и предложений'),
    ('TENDER_RECEIVE', 'Приёмка оборудования по тендеру'),
    ('DETENTION_READ', 'Просмотр заявок на заключение'),
    ('DETENTION_CREATE', 'Создание заявок на заключение'),
    ('DETENTION_EDIT', 'Редактирование заявок на заключение'),
    ('DETENTION_PROCESS', 'Рассмотрение заявок и учёт заключения'),
    ('CLEANUP_REPORT_READ', 'Просмотр отчёта об очистке'),
    ('CLEANUP_REPORT_CREATE', 'Создание отчёта об очистке'),
    ('CLEANUP_STATUS_CHANGE', 'Смена статуса очистки'),
    ('ANALYTICS_READ', 'Просмотр аналитики инцидентов, склада и заключений'),
    ('FILE_UPLOAD', 'Загрузка файлов'),
    ('FILE_READ', 'Скачивание файлов'),
    ('NOTIFICATION_READ', 'Просмотр уведомлений'),
    ('MONITORING_ALERT_READ', 'Просмотр алертов внешнего мониторинга')
on conflict (code) do nothing;

insert into role (name, description) values
    ('OPERATOR', 'Оператор'),
    ('ANALYST', 'Аналитик'),
    ('ADMIN', 'Администратор'),
    ('AGENT', 'Оперативный агент'),
    ('STOREKEEPER', 'Завхоз'),
    ('TENDER_MANAGER', 'Менеджер закупок'),
    ('MANAGEMENT', 'Руководство'),
    ('PRISON_GUARD', 'Тюремный надзиратель'),
    ('CLEANER', 'Специалист по прикрытию');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'OPERATOR' and p.code in (
    'INCIDENT_READ', 'INCIDENT_CREATE', 'INCIDENT_STATUS_CHANGE',
    'INCIDENT_COMMENT', 'USER_READ', 'EQUIPMENT_READ',
    'EQUIPMENT_MOVEMENT_READ', 'EQUIPMENT_MOVEMENT_CREATE', 'CLEANUP_REPORT_READ',
    'FILE_UPLOAD', 'FILE_READ', 'NOTIFICATION_READ',
    'MONITORING_ALERT_READ'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'ANALYST' and p.code in (
    'INCIDENT_READ', 'INCIDENT_STATUS_CHANGE', 'INCIDENT_COMMENT', 'INCIDENT_THREAT_CALCULATE',
    'INCIDENT_ALIEN_CLASSIFY', 'INCIDENT_ALIEN_LINK', 'ALIEN_READ', 'ALIEN_WRITE',
    'USER_READ', 'EQUIPMENT_READ', 'EQUIPMENT_MOVEMENT_READ',
    'EQUIPMENT_MOVEMENT_CREATE', 'CLEANUP_REPORT_READ', 'FILE_UPLOAD',
    'FILE_READ', 'NOTIFICATION_READ'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'ADMIN' and p.code in (
    'INCIDENT_READ', 'INCIDENT_CREATE', 'INCIDENT_STATUS_CHANGE',
    'INCIDENT_COMMENT', 'INCIDENT_THREAT_CALCULATE', 'INCIDENT_ALIEN_CLASSIFY', 'INCIDENT_ALIEN_LINK', 'ALIEN_READ',
    'ALIEN_WRITE', 'USER_READ', 'INCIDENT_ASSIGN',
    'EQUIPMENT_READ', 'EQUIPMENT_WRITE', 'EQUIPMENT_MOVEMENT_READ',
    'EQUIPMENT_MOVEMENT_CREATE', 'EQUIPMENT_MOVEMENT_PROCESS',
    'TENDER_READ', 'TENDER_CREATE', 'TENDER_EDIT', 'TENDER_MANAGE', 'TENDER_RECEIVE',
    'DETENTION_READ', 'DETENTION_CREATE', 'DETENTION_EDIT', 'DETENTION_PROCESS',
    'ANALYTICS_READ', 'CLEANUP_REPORT_READ',
    'FILE_UPLOAD', 'FILE_READ', 'NOTIFICATION_READ'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'AGENT' and p.code in (
    'INCIDENT_READ', 'INCIDENT_STATUS_CHANGE', 'INCIDENT_COMMENT',
    'ALIEN_READ', 'USER_READ', 'INCIDENT_ASSIGN',
    'DETENTION_READ', 'DETENTION_CREATE', 'DETENTION_EDIT',
    'EQUIPMENT_READ', 'EQUIPMENT_MOVEMENT_READ', 'EQUIPMENT_MOVEMENT_CREATE',
    'CLEANUP_REPORT_READ', 'FILE_UPLOAD', 'FILE_READ',
    'NOTIFICATION_READ'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'STOREKEEPER' and p.code in (
    'INCIDENT_READ', 'USER_READ', 'EQUIPMENT_READ',
    'EQUIPMENT_WRITE', 'EQUIPMENT_MOVEMENT_READ', 'EQUIPMENT_MOVEMENT_CREATE',
    'EQUIPMENT_MOVEMENT_PROCESS',
    'TENDER_READ', 'TENDER_CREATE', 'TENDER_EDIT', 'TENDER_RECEIVE'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'TENDER_MANAGER' and p.code in (
    'INCIDENT_READ', 'USER_READ', 'EQUIPMENT_READ',
    'TENDER_READ', 'TENDER_CREATE', 'TENDER_EDIT', 'TENDER_MANAGE', 'TENDER_RECEIVE'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'MANAGEMENT' and p.code in (
    'ANALYTICS_READ', 'INCIDENT_READ', 'EQUIPMENT_READ', 'EQUIPMENT_MOVEMENT_READ',
    'TENDER_READ', 'DETENTION_READ', 'ALIEN_READ', 'USER_READ', 'FILE_READ'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'PRISON_GUARD' and p.code in (
    'INCIDENT_READ', 'ALIEN_READ', 'USER_READ',
    'DETENTION_READ', 'DETENTION_EDIT', 'DETENTION_PROCESS'
);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'CLEANER' and p.code in (
    'INCIDENT_READ', 'INCIDENT_COMMENT', 'ALIEN_READ',
    'USER_READ', 'CLEANUP_REPORT_READ', 'CLEANUP_REPORT_CREATE',
    'CLEANUP_STATUS_CHANGE', 'FILE_UPLOAD', 'FILE_READ',
    'NOTIFICATION_READ'
);

insert into app_user (login, password_hash) values
    ('operator', '$2a$10$3Lk4s75nd2Q0G1E8UWTmFu05efZd8lnw6OUw8yiJMvkN4fjhMKSfi'),
    ('analyst', '$2a$10$4jOz4wa0A4i9xlzxtJX5du72d2cbNfCjHt3TwPyDF6OlDXtIxK56W'),
    ('admin', '$2a$10$3ikPsa0.oG0Rolvk8W65Ke02QdCf1NguC9DxvAijlbu.54Vp/Wpb2'),
    ('agent', '$2a$10$6KMyGyv/5dg7pb9EROtxzeyLowuW/RhvFP/ZUy53PuhoSrkTIR7U2'),
    ('agent2', '$2y$10$PzesjIfh8erxGjUyrlVlMOWKVIC0Eezbyy2lAHHUB.dAjPjU3P7Ke'),
    ('storekeeper', '$2a$10$yzdLf5jhGEbEBjVWo8ZwOO.P7SyWE3mi8kOjYP.DxSu4ASMfug8Wu'),
    ('tender_manager', '$2a$10$3ikPsa0.oG0Rolvk8W65Ke02QdCf1NguC9DxvAijlbu.54Vp/Wpb2'),
    ('management', '$2a$10$3ikPsa0.oG0Rolvk8W65Ke02QdCf1NguC9DxvAijlbu.54Vp/Wpb2'),
    ('prison_guard', '$2a$10$3ikPsa0.oG0Rolvk8W65Ke02QdCf1NguC9DxvAijlbu.54Vp/Wpb2'),
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
where u.login = 'storekeeper' and r.name = 'STOREKEEPER';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'tender_manager' and r.name = 'TENDER_MANAGER';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'management' and r.name = 'MANAGEMENT';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'prison_guard' and r.name = 'PRISON_GUARD';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'cleaner' and r.name = 'CLEANER';
