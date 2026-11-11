# Architecture Notes

## Verdict

Good homelab architecture. Keep the monolith. About 7/10 for one-person maintenance.
It is simple enough to understand, but several manual processes will cause drift over time.

## Shape

```text
Browser
  └─ Nginx :8080
       ├─ static React SPA
       └─ /api ──> Quarkus :8081 ──> PostgreSQL
```

- Backend: REST resource → service → Panache repository. DTOs live under `rest/dto`.
- Database: PostgreSQL only. Flyway owns the schema; Hibernate validates it.
- Frontend: React + TypeScript + Vite. `api.ts` owns bearer tokens and refresh.
- Deployment: Docker Compose. Database is loopback-only; API is not published; only Nginx is exposed.

## What works well

- Resources are thin. Business rules stay in services.
- One database and one migration tool. No auto-DDL surprises.
- One frontend API client. Auth and refresh logic are centralized.
- Small stack with few moving parts. Easy to run and debug.
- Integration tests cover the main user flows.
- CI builds the API with tests and builds the frontend on PRs, `main`, and tags.
- Admin lists are paginated with server-side search and event date filters.

## Main maintainability risks

| Risk | Why it matters | Suggested fix |
|---|---|---|
| Hand-maintained OpenAPI | Contract will drift from code | Generate it, or add a schema drift test |
| `App.tsx` owns too much | Session, dialogs, data, and navigation in one file | Extract `useSession` and workspace-data hooks |
| One large integration test class | Shared database makes tests stateful and slower | Split by domain and use Testcontainers |
| Unbounded group/member lists | Group lists still expand every member | Paginate group and member lists |
| Hand-rolled JWT | Security-sensitive code needs ongoing review | Consider Quarkus JWT support when convenient |
| No API healthcheck | Compose cannot tell whether the API is ready | Add an internal health endpoint and healthcheck |

## Suggested order

1. Add Testcontainers for backend tests.
2. Split tests by domain.
3. Add pagination to group and member lists.
4. Extract frontend session logic from `App.tsx`.
5. Move to a standard JWT library, optional.
6. Add TLS and deployment hardening when needed.

## Do not add

- Microservices.
- Keycloak or an external identity provider.
- A framework rewrite.
- Generic repository or service abstractions.

## Operational notes

- Rotate the local `DB_PASSWORD`; the database port is loopback-only, but the default password is still weak.
- Add TLS if the dashboard is reachable beyond a trusted LAN.
- Password changes revoke refresh tokens. Access tokens remain valid for up to 15 minutes.
- Swagger is dev-only. `/q/` is blocked by the production Nginx config.
