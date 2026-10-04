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

### After : 
- groups
- period
- refresh_tokens
    - id
    - user_id
    - token_hash
    - creation_time
    - expiration_time
    - revoked

