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
- Personal and group-shared todo lists
- Groups with owner-managed membership
- Admin views for users, events, and todos

# Frontend

React + TypeScript with Vite. Includes the dashboard, calendar, todo list, profile, groups, and admin views.

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

**Groups**
- GET       /api/groups
- POST      /api/groups
- GET       /api/groups/:id
- PATCH     /api/groups/:id
- DELETE    /api/groups/:id
- POST      /api/groups/:id/members
- DELETE    /api/groups/:id/members/:userId

**Todos**
- GET       /api/todos?completed=&groupId=
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
- DELETE    /api/admin/users/:id

## Group sharing

Any authenticated user can create a group and becomes its owner. The owner can rename or delete the
group and add existing users by login. Group members can view and edit events and todos shared with
the group. Personal items remain private.

Removing a member revokes their access to the group's shared items. Deleting a group also deletes
its shared events and todos. The `groupId` field is optional on event and todo creation; omitting it
creates a personal item.


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

### Events
| Type | Name |
| ---- | ---- |
| UUID  | uuid |
| String | name |
| String | description |
| UUID | user_id |
| UUID | group_id (nullable) |
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
- front/back : we can group todo, like basics app todo (folder if todo)
- front/back : add period events, like birthdays... (every 3 days...), and the possibility to set an
  event for a whole day, with no hour needed
- front/back : maybe add the ability to change password (so admin also)
