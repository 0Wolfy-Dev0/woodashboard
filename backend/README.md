# db-service

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Open SQL shell

```shell
docker exec -it woodash-db psql -U woodash -d woodash
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

## Running with Docker Compose

From the repository root, if `docker/.env` does not exist yet, copy the template and set
`JWT_SECRET` to a random value (for example, the output of `openssl rand -hex 32`). Compose reads
the file beside `docker-compose.yml`, and `env_file` passes its values into the API container:

```shell
cp -n docker/.env.example docker/.env
docker compose -f docker/docker-compose.yml up --build
```

Compose supplies the secret to the running API container; it is not copied into the image. The
frontend is available at <http://localhost:8080>; its Nginx server proxies `/api` and `/q` to the
backend. The API is not published directly by Compose; use <http://localhost:8080/q/swagger-ui/>.
When running Quarkus in dev mode outside Docker, it listens on <http://localhost:8081>.

## API authentication

Register with `POST /api/auth/register`, then log in with `POST /api/auth/login`. Use the returned
`accessToken` as a Bearer token in Swagger's **Authorize** dialog. Access tokens last 15 minutes by
default; `POST /api/auth/refresh` rotates the refresh token.

For deployments, set `JWT_SECRET` to a long, random secret. The default key in
`application.properties` is only for local development. New accounts have the `USER` role; to
promote an account for testing admin endpoints, set its `users.role` to `ADMIN` in PostgreSQL and
log in again to receive a token with the updated role.

```shell script
# in sql shell
update users set role = 'ADMIN' where login = 'user' ;
```

## Group sharing

Users can create groups, add members by login, and share events/todos with a group. Any group member
can edit or delete shared items; only the group owner or an admin can manage the group and its
members. Group endpoints are documented in the root `README.md` and in Swagger.

Todos can also be organized into private folders, events can be all-day or repeat on a simple
schedule, and every account can change its password from the profile page. Admins can reset another
user's password from the administration page. The root `README.md` and Swagger describe the request
fields.

The endpoint tests use the PostgreSQL instance from `docker/docker-compose.yml`. Start it first if
needed, then run:

```shell
docker compose -f docker/docker-compose.yml up -d db
cd backend
./mvnw test
```

> **_NOTE:_** Quarkus' Dev UI is available in dev mode at <http://localhost:8081/q/dev/>. Set
> `HTTP_PORT` to override the backend port.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/db-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)
- Flyway ([guide](https://quarkus.io/guides/flyway)): Handle your database schema migrations
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplified JPA/Hibernate data access layer with active record and repository patterns
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)


[Related Hibernate with Panache section...](https://quarkus.io/guides/hibernate-orm-panache)


### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
