# API endpoint matrix (contract: Frontend `src/lib/api.ts`)

Single monolith base URL. Auth: JWT via `Authorization: Bearer` and `X-Auth-Token`.

## Auth `/auth`

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | /auth/register | Public | |
| POST | /auth/login | Public | |
| POST | /auth/login/otp | Public | |
| POST | /auth/otp/send | Public | |
| POST | /auth/otp/verify | Public | |
| POST | /auth/refresh | Public | body: refreshToken |
| POST | /auth/logout | Public | |
| GET | /auth/me | JWT | |
| PUT | /auth/me | JWT | |
| POST | /auth/password/change | JWT | |
| DELETE | /auth/account | JWT | |

## Cloud `/api/cloud`

| Method | Path | Auth |
|--------|------|------|
| POST | /api/cloud/profile-image | JWT |
| POST | /api/cloud/background-image | JWT |
| POST | /api/cloud/upload?folder= | JWT |
| DELETE | /api/cloud/delete?url= | JWT |

## Users `/api/v1/users`

| Method | Path | Auth |
|--------|------|------|
| GET/PUT | /api/v1/users/me/profile | JWT |
| CRUD | /api/v1/users/me/family, /{id} | JWT |
| GET/PUT | /api/v1/users/me/settings | JWT |
| GET/PUT | /api/v1/users/me/privacy | JWT |
| GET/PUT | /api/v1/users/me/security | JWT |
| GET | /api/v1/users/{id}/profile | JWT |
| GET | /api/v1/users/{id}/contact | JWT |
| GET | /api/v1/users/{id}/visible-profile | JWT |
| GET | /api/v1/users/search | JWT |
| POST | /api/v1/users/contact-requests | JWT |
| GET | .../incoming, .../outgoing | JWT |
| PUT | .../contact-requests/{id}/respond | JWT |
| GET | /api/v1/users/directory | JWT |

## Directory `/api/v1/directory`

| GET | /api/v1/directory | JWT |
| GET | /api/v1/directory/{userId} | JWT |
| GET/PUT | /api/v1/directory/me/settings | JWT |

## Gallery, Documents, Notifications, News, Emergency, Events, Community, Search, Suggestions, Exams, Matrimony

See `api.ts` for full list; each module maps to same paths and verbs.

## Admin `/admin`

| GET | /admin/users | Admin JWT |
| POST | /admin/users | Parent admin |
| PUT/DELETE | /admin/users/{id} | |
| GET | /admin/sub-admins | |
| POST | /admin/sub-admins | |
| POST | /admin/sub-admins/{id}/reset-password | |
| PUT | /admin/sub-admins/{id}/services | |
| GET | /admin/dashboard/overview | |
| GET/PATCH | /admin/moderation/{service}/pending, /admin/moderation/{service}/{id} | |

Admin also calls user JWT endpoints with admin token for news/documents/events where `adminApi` is used.
