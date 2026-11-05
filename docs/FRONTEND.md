# React frontend – guide

Guide for whoever builds the new frontend (person or AI). The old Angular frontend was removed;
`frontend/` now holds an empty React + TypeScript project (Vite) that already talks to the backend.

## Stack

- React 19 + TypeScript, built with Vite
- No router, state or UI library yet. Add one only when a page needs it
  (a router will be needed for the pages below).
- Lint: `npm run lint` (oxlint)

## Run

1. Backend (school database), in the project root:
   ```powershell
   .\start-local.ps1 -EnvFile .env.school -Profiles ''
   ```
   Needs your own `.env.school` (see README). Backend runs on http://localhost:8080.
2. Frontend dev server, second terminal:
   ```powershell
   cd frontend
   npm install      # first time only
   npm run dev
   ```
   Open http://localhost:4200. Vite forwards every `/api/...` request to the backend
   (`vite.config.ts`), so the page and the API share one origin.
3. Production build: `npm --prefix frontend run build` writes `frontend/dist`.
   Maven copies it into the backend jar (`pom.xml`), so http://localhost:8080 serves it too.

## Files

| File | Purpose |
|---|---|
| `frontend/src/api/types.ts` | API request/response types. Copy of `docs/api-types.ts` plus `AppInfo`; keep both in sync |
| `frontend/src/api/client.ts` | `api<T>(path, options)`: fetch wrapper. Adds `/api`, JSON header and the JWT; throws `ApiError` |
| `frontend/src/App.tsx` | Starter page: shows app name and categories (connection check). Replace it |
| `frontend/vite.config.ts` | Dev port 4200 and `/api` proxy |

Example:
```ts
import { api, setToken } from './api/client'
import type { AuthResponse } from './api/types'

const auth = await api<AuthResponse>('/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email, password }),
})
setToken(auth.accessToken)
```

## API

All endpoints, rules and error format: [API.md](API.md). Types: [api-types.ts](api-types.ts).

- Login returns a JWT (`accessToken`). `setToken()` keeps it in memory, so a refresh logs out.
  Moving it to `sessionStorage` is a valid later decision.
- Roles: `CUSTOMER`, `PROVIDER`, `ADMIN`. Public registration only allows CUSTOMER/PROVIDER.
- `GET /api/info` returns `{ name, demo, demoAccounts }`. Show demo hints only when they are true.

## Pages to build

Same features the Angular version had, plus the service editor:

| URL | Page | Who |
|---|---|---|
| `/` | Catalog: search by name/city/category, paging (`GET /api/services`) | everyone |
| `/sluzby/:id` | Service detail + order form (`POST /api/orders`) | everyone / CUSTOMER orders |
| `/prihlasenie`, `/registracia` | Login, register | everyone |
| `/objednavky` | My orders + status changes (`PATCH /api/orders/{id}/status`) | logged in |
| `/profil` | Provider profile + subscription (`/api/providers/me`, `/subscription`) | PROVIDER |
| new | **Service editor** (`/api/services/mine`, `POST`/`PUT /api/services`) – missing so far, needed for orders | PROVIDER |

The backend serves `index.html` for these URLs on refresh (`WebController.java`).
**A new page URL must be added there**, otherwise refreshing it returns 404.

## Rules

- Comments and commit messages in English, short and simple.
- No AI attribution anywhere (no `Co-Authored-By`, no "Generated with ...").
- Never commit `.env.*` files or passwords.
- Work on a branch, merge into `main` when a part works.
