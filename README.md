# Лабораторная работа 5 — JWT Access/Refresh и управление сессиями

## Описание
JWT access- и refresh-токены (HS256), серверный учёт сессий в таблице `user_sessions`.
Статусы сессии: `ACTIVE` → `USED` (после refresh) → `REVOKED` (повтор старого refresh).

## API

| Метод | Путь | Описание | Код |
| POST | `/api/auth/login` | вход, выдача пары токенов | 200 |
| POST | `/api/auth/register` | регистрация + пара токенов | 201 |
| POST | `/api/auth/refresh` | обмен refresh на новую пару | 200 |

Успешный ответ:
```json
{ "accessToken": "...", "refreshToken": "..." }
```

Access передаётся в заголовке `Authorization: Bearer <token>`.  
Refresh — только в JSON-теле запроса `/api/auth/refresh`.

## Проверка

1. Login → в БД сессия `ACTIVE`
2. Запрос с access к защищённому endpoint → 200
3. Refresh → старая сессия `USED`, новая `ACTIVE`
4. Повтор старого refresh → 401
5. В БД: `REVOKED` и `ACTIVE`

## Учётные записи

| Email | Пароль | Роль | |
| admin@example.com | Admin123! | ADMIN |
| operator@example.com | Operator1 | OPERATOR |
| sender@example.com | Sender123 | SENDER |

## Запуск

PostgreSQL: база `courierdb`, пользователь `courier_app` / `courier_pass`.
