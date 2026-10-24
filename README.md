# woodashboard
Customize dashboard for my homelab with calendar and list

# Goal

Create a customize dashboard for my homelab with calendar, event, todo list, reminder... (and more if i want to add)

so im thinking for this to go through an REST-API and a nodeJS frontend (evrrything on docker)
maybe use postgreeslq for the database, and if i use java maybe quarkus or springboot

i will have it locally in my home, so anyone in the network can access, but of course i will still keep login and basics
account (with admin part), with token and refresh token maybe also

# Features

- JWT authentication with refresh-token rotation
- Personal and group-shared calendar events
- Recurring events and all-day events
- Personal and group-shared todo lists
- Personal todo folders
- Groups with owner-managed membership
- Password changes, plus admin password resets
- Admin views for users, events, and todos

# Frontend

React + TypeScript with Vite. Includes the dashboard, calendar (all-day and recurring events),
todo lists with personal folders, profile, groups, and admin views.

# Backend

Java with Quarkus, PostgreSQL, and Flyway migrations.

## Run the dashboard

Copy `docker/.env.example` to `docker/.env`, set `JWT_SECRET` to a random value, then start the stack:

```sh
cp -n docker/.env.example docker/.env
docker compose -f docker/docker-compose.yml up --build
```

Open the frontend at <http://localhost:8080>. Nginx serves the app and proxies API requests to the
backend; Swagger is available at <http://localhost:8080/q/swagger-ui/>.

For frontend development with Vite hot reload, start the backend in dev mode on port `8081`, then run:

```sh
cd frontend
npm install
npm run dev
```

Vite serves the frontend at <http://localhost:5173> and proxies `/api` calls to the backend.

## Endpoints

All endpoints except register, login, and refresh require `Authorization: Bearer <accessToken>`.

**Authentication**
- POST      /api/auth/register
- POST      /api/auth/login
- POST      /api/auth/logout
- POST      /api/auth/refresh

**Current user**
- GET       /api/me
- PATCH     /api/me/password

**Groups**
- GET       /api/groups
- POST      /api/groups
- GET       /api/groups/:id
- PATCH     /api/groups/:id
- DELETE    /api/groups/:id
- POST      /api/groups/:id/members
- DELETE    /api/groups/:id/members/:userId

**Todo folders**
- GET       /api/todo-folders
- POST      /api/todo-folders
- PATCH     /api/todo-folders/:id
- DELETE    /api/todo-folders/:id

**Todos**
- GET       /api/todos?completed=&groupId=&folderId=
- GET       /api/todos/:user_id
- POST      /api/todos
- PATCH     /api/todos/:id
- DELETE    /api/todos/:id

**Events**
- GET       /api/events?from=&to=&groupId=
- POST      /api/events
- PATCH     /api/events/:id
- DELETE    /api/events/:id

**Administration**
- GET       /api/admin/users
- GET       /api/admin/events
- GET       /api/admin/todos
- PATCH     /api/admin/users/:id/password
- DELETE    /api/admin/users/:id

## Group sharing

Any authenticated user can create a group and becomes its owner. The owner can rename or delete the
group and add existing users by login. Group members can view and edit events and todos shared with
the group. Personal items remain private.

Removing a member revokes their access to the group's shared items. Deleting a group also deletes
its shared events and todos. The `groupId` field is optional on event and todo creation; omitting it
creates a personal item.

## Todo folders

Todos can optionally be filed into a personal folder. Folders are private to their owner; deleting
a folder keeps its tasks and moves them back to "No folder". The `folderId` field is optional on todo
creation and can be changed on update; send `null` to remove a task from its folder.

## Recurring and all-day events

Events can be all-day or repeat on a simple schedule. For recurrence, send a `recurrenceUnit`
(`DAYS`, `WEEKS`, `MONTHS`, or `YEARS`) plus an optional `recurrenceInterval` (default `1`, so
`DAYS` + `3` means every three days) and an optional `recurrenceEnd`. Omitting `recurrenceUnit`
creates a one-off event; sending `null` on update stops an existing series.

When `GET /api/events` is called with a `from`/`to` window, recurring series are expanded into one
entry per occurrence. The entries keep the series `id`, while `startTime`/`endTime` describe the
occurrence and `seriesStartTime`/`seriesEndTime` describe the series definition. Editing or deleting
an event affects the whole series. All-day events are stored with a `00:00:00` start and a
`23:59:59` end on the selected dates.

## Passwords

Any signed-in user can change their own password with `PATCH /api/me/password`, providing
`currentPassword` and `newPassword`. Administrators can set a new password for any account with
`PATCH /api/admin/users/:id/password`; that reset revokes all refresh tokens for the account, so the
user has to sign in again. Passwords must be between 8 and 128 characters.


# Database

PostgreSQL

## Tables
### Users
| Type | Name |
| ---- | ---- |
| UUID  | uuid |
| String | login |
| String | password_hash |
| Role_enum | role |
| LocalDateTime | creation_time |
| LocalDateTime | last_modification_time |

### Groups
| Type | Name |
| ---- | ---- |
| UUID  | uuid |
| String | name |
| String | description |
| UUID | owner_id |
| LocalDateTime | creation_time |
| LocalDateTime | last_modification_time |

### Group members
| Type | Name |
| ---- | ---- |
| UUID  | uuid |
| UUID | group_id |
| UUID | user_id |
| LocalDateTime | creation_time |
| LocalDateTime | last_modification_time |

### Todo folders
| Type | Name |
| ---- | ---- |
| UUID  | uuid |
| String | name |
| UUID | user_id |
| LocalDateTime | creation_time |
| LocalDateTime | last_modification_time |

### Events
| Type | Name |
| ---- | ---- |
| UUID  | uuid |
| String | name |
| String | description |
| UUID | user_id |
| UUID | group_id (nullable) |
| Boolean | all_day |
| Recurrence_unit_enum | recurrence_unit (nullable) |
| Integer | recurrence_interval (nullable) |
| LocalDateTime | recurrence_end (nullable) |
| LocalDateTime | creation_time |
| LocalDateTime | start_time |
| LocalDateTime | end_time |
| LocalDateTime | last_modification_time |

### Todos
| Type | Name |
| ---- | ---- |
| UUID  | uuid |
| String | name |
| String | description |
| UUID | user_id |
| UUID | group_id (nullable) |
| UUID | folder_id (nullable) |
| LocalDateTime | creation_time |
| LocalDateTime | last_modification_time |
| Boolean | completed |

## TODO:
### V1.1.0: 
- DONE frontend : when i say to use nginx its for using port 80 and then passproxy to the backend (the default.conf example
  was just an example) you can arrange it if its not done yet
- DONE frontend : refresh the calendar and other is auto?, it should be
- DONE frontend : no need of the notification icon (on click it just go to calendar)
- DONE frontend : on the menu 'home worksapece' is not used, why? just remove it so no?

### V1.2.0:
- DONE front/back : add groups so many people can share the same events (and todo)
- DONE front/back : we can group todo, like basics app todo (folder if todo)
- DONE front/back : add period events, like birthdays... (every 3 days...), and the possibility to set an
  event for a whole day, with no hour needed
- DONE front/back : maybe add the ability to change password (so admin also)
