# woodashboard
Customize dashboard for my homelab with calendar and list

# Goal

Create a customize dashboard for my homelab with calendar, event, todo list, reminder... (and more if i want to add)

so im thinking for this to go through an REST-API and a nodeJS frontend (evrrything on docker)
maybe use postgreeslq for the database, and if i use java maybe quarkus or springboot

i will have it locally in my home, so anyone in the network can access, but of course i will still keep login and basics
account (with admin part), with token and refresh token maybe also

# Frontend

React + Typescript : i will do it with AI, its not my purpose

# Backend

Java with quarkus
Flyway migration

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

## Endpoints :
**Administrations**
- GET       /api/admin/users
- GET       /api/admin/events
- GET       /api/admin/todos
- DELETE    /api/admin/users/:id


**Authentication**
- POST      /api/auth/register
- POST      /api/auth/login
- POST      /api/auth/logout
- POST      /api/auth/refresh

**Current user**
- GET       /api/me

**Todos**
- GET       /api/todos/:user_id
- POST      /api/todos
- PATCH     /api/todos/:id
- DELETE    /api/todos/:id

**Events**
- GET       /api/events
- POST      /api/events
- PATCH     /api/events/:id
- DELETE    /api/events/:id

### After : 
- groups
- period


# Database

PostgreeSQL

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

### Events
| Type | Name |
| ---- | ---- |
| UUID  | uuid |
| String | name |
| String | description |
| UUID | user_id |
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
| User | user_id |
| LocalDateTime | creation_time |
| LocalDateTime | last_modification_time |
| Boolean | completed |

## TODO:
### V1.1.0: 
- frontend : when i say to use nginx its for using port 80 and then passproxy to the backend (the default.conf example
  was just an example) you can arrange it if its not done yet
- frontend : refresh the calendar and other is auto?, it should be
- frontend : no need of the notification icon (on click it just go to calendar)
- frontend : on the menu 'home worksapece' is not used, why? just remove it so no?

after
- front/back : add groups so many people can share the same events (and todo)
- front/back : we can group todo, like basics app todo (folder if todo)

- front/back : add preiod event, like brithday... (every 3 days...), and possible to set an event for a all day, no hour
  needed

### V1.2.0:
- front/back : maybe add that we can change password (so admin also)
