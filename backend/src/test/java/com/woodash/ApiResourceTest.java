package com.woodash;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

@QuarkusTest
class ApiResourceTest {

    @Inject
    DataSource dataSource;

    private final Set<String> createdLogins = new LinkedHashSet<>();
    private final String password = "test-password-123";

    @Test
    void registerValidatesInputAndReturnsSafeUserBody() {
        String login = uniqueLogin("register");
        createdLogins.add(login);
        Response response = given().contentType(ContentType.JSON)
                .body(Map.of("login", login, "password", password))
                .when().post("/api/auth/register");

        response.then().statusCode(201)
                .body("id", matchesPattern("[0-9a-fA-F-]{36}"))
                .body("login", equalTo(login))
                .body("role", equalTo("USER"))
                .body("creationTime", notNullValue())
                .body("lastModificationTime", notNullValue())
                .body("passwordHash", nullValue());

        given().contentType(ContentType.JSON)
                .body(Map.of("login", login, "password", password))
                .when().post("/api/auth/register")
                .then().statusCode(409).body("message", equalTo("Login is already taken"));

        given().contentType(ContentType.JSON).body("{\"login\":\"  \",\"password\":\"short\"}")
                .when().post("/api/auth/register")
                .then().statusCode(400).body("message", notNullValue());

        given().contentType(ContentType.JSON).body("{}")
                .when().post("/api/auth/register")
                .then().statusCode(400).body("message", notNullValue());
    }

    @Test
    void loginReturnsUsableAccessAndRefreshTokensAndMeReturnsCurrentUser() {
        TestUser user = createUser("login");

        given().when().get("/api/me")
                .then().statusCode(401).body("message", equalTo("Missing, invalid or expired access token"));
        given().auth().oauth2("not-a-jwt").when().get("/api/me")
                .then().statusCode(401).body("message", equalTo("Missing, invalid or expired access token"));

        TokenPair tokens = login(user);
        given().auth().oauth2(tokens.accessToken())
                .when().get("/api/me")
                .then().statusCode(200)
                .body("id", equalTo(user.id()))
                .body("login", equalTo(user.login()))
                .body("role", equalTo("USER"))
                .body("creationTime", notNullValue())
                .body("lastModificationTime", notNullValue())
                .body("passwordHash", nullValue());

        given().contentType(ContentType.JSON)
                .body(Map.of("login", user.login(), "password", "wrong-password"))
                .when().post("/api/auth/login")
                .then().statusCode(401).body("message", equalTo("Invalid login or password"));
        given().contentType(ContentType.JSON).body("{}")
                .when().post("/api/auth/login")
                .then().statusCode(400).body("message", notNullValue());
    }

    @Test
    void refreshRotatesTokensAndLogoutRevokesTheRefreshToken() {
        TestUser user = createUser("refresh");
        TokenPair initialTokens = login(user);

        Response refreshResponse = given().contentType(ContentType.JSON)
                .body(Map.of("refreshToken", initialTokens.refreshToken()))
                .when().post("/api/auth/refresh");
        refreshResponse.then().statusCode(200)
                .body("accessToken", matchesPattern("[^.]+\\.[^.]+\\.[^.]+"))
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", equalTo(900));
        TokenPair rotatedTokens = tokenPair(refreshResponse);
        org.junit.jupiter.api.Assertions.assertNotEquals(initialTokens.refreshToken(), rotatedTokens.refreshToken());

        given().auth().oauth2(rotatedTokens.accessToken()).when().get("/api/me")
                .then().statusCode(200).body("id", equalTo(user.id()));
        given().contentType(ContentType.JSON).body(Map.of("refreshToken", initialTokens.refreshToken()))
                .when().post("/api/auth/refresh")
                .then().statusCode(401).body("message", equalTo("Refresh token is invalid, expired or revoked"));

        given().contentType(ContentType.JSON).body(Map.of("refreshToken", rotatedTokens.refreshToken()))
                .when().post("/api/auth/logout")
                .then().statusCode(401).body("message", equalTo("Missing, invalid or expired access token"));
        given().auth().oauth2(rotatedTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("refreshToken", rotatedTokens.refreshToken()))
                .when().post("/api/auth/logout")
                .then().statusCode(204).body(equalTo(""));
        given().contentType(ContentType.JSON).body(Map.of("refreshToken", rotatedTokens.refreshToken()))
                .when().post("/api/auth/refresh")
                .then().statusCode(401).body("message", equalTo("Refresh token is invalid, expired or revoked"));
    }

    @Test
    void todoEndpointsCreateFilterPatchDeleteAndReturnUpdatedBodies() {
        TestUser user = createUser("todo");
        TokenPair tokens = login(user);

        Response createOpen = given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Open task", "description", "Keep this"))
                .when().post("/api/todos");
        createOpen.then().statusCode(201)
                .body("id", matchesPattern("[0-9a-fA-F-]{36}"))
                .body("name", equalTo("Open task"))
                .body("description", equalTo("Keep this"))
                .body("userId", equalTo(user.id()))
                .body("completed", equalTo(false))
                .body("creationTime", notNullValue())
                .body("lastModificationTime", notNullValue());
        String openTodoId = createOpen.path("id");
        createOpen.then().header("Location", endsWith("/api/todos/" + openTodoId));

        String completedTodoId = given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Done task", "completed", true))
                .when().post("/api/todos")
                .then().statusCode(201).body("completed", equalTo(true)).extract().path("id");

        given().auth().oauth2(tokens.accessToken()).when().get("/api/todos/{userId}", user.id())
                .then().statusCode(200).body("size()", equalTo(2)).body("id", hasItems(openTodoId, completedTodoId));
        given().auth().oauth2(tokens.accessToken()).queryParam("completed", false)
                .when().get("/api/todos/{userId}", user.id())
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(openTodoId));
        given().auth().oauth2(tokens.accessToken()).queryParam("completed", true)
                .when().get("/api/todos/{userId}", user.id())
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(completedTodoId));

        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body("{\"description\":null}")
                .when().patch("/api/todos/{id}", openTodoId)
                .then().statusCode(200)
                .body("id", equalTo(openTodoId))
                .body("name", equalTo("Open task"))
                .body("description", nullValue())
                .body("completed", equalTo(false))
                .body("userId", equalTo(user.id()));
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("completed", true))
                .when().patch("/api/todos/{id}", openTodoId)
                .then().statusCode(200).body("completed", equalTo(true)).body("name", equalTo("Open task"));
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON).body("{}")
                .when().patch("/api/todos/{id}", openTodoId)
                .then().statusCode(400).body("message", notNullValue());
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON).body("{\"completed\":null}")
                .when().patch("/api/todos/{id}", openTodoId)
                .then().statusCode(400).body("message", notNullValue());
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", ""))
                .when().post("/api/todos")
                .then().statusCode(400).body("message", notNullValue());

        given().auth().oauth2(tokens.accessToken()).when().delete("/api/todos/{id}", openTodoId)
                .then().statusCode(204).body(equalTo(""));
        given().auth().oauth2(tokens.accessToken()).when().delete("/api/todos/{id}", completedTodoId)
                .then().statusCode(204);
        given().auth().oauth2(tokens.accessToken()).when().get("/api/todos/{userId}", user.id())
                .then().statusCode(200).body("size()", equalTo(0));
        given().auth().oauth2(tokens.accessToken()).when().delete("/api/todos/{id}", UUID.randomUUID())
                .then().statusCode(404).body("message", equalTo("Resource not found"));
    }

    @Test
    void todoEndpointsEnforceUserOwnershipAndReturnNotFoundForUnknownUsers() {
        TestUser owner = createUser("todo-owner");
        TestUser other = createUser("todo-other");
        TokenPair ownerTokens = login(owner);
        TokenPair otherTokens = login(other);
        String todoId = given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Private task"))
                .when().post("/api/todos").then().statusCode(201).extract().path("id");

        given().auth().oauth2(otherTokens.accessToken()).when().get("/api/todos/{userId}", owner.id())
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(otherTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("completed", true)).when().patch("/api/todos/{id}", todoId)
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(otherTokens.accessToken()).when().delete("/api/todos/{id}", todoId)
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(ownerTokens.accessToken()).when().get("/api/todos/{userId}", UUID.randomUUID())
                .then().statusCode(404).body("message", equalTo("Resource not found"));
    }

    @Test
    void eventEndpointsFilterOverlappingWindowsPatchDeleteAndEnforceOwnership() {
        TestUser owner = createUser("event-owner");
        TestUser other = createUser("event-other");
        TokenPair ownerTokens = login(owner);
        TokenPair otherTokens = login(other);

        Response createEvent = given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Backup", "description", "Weekly", "startTime", "2026-10-12T18:00:00",
                        "endTime", "2026-10-12T19:00:00"))
                .when().post("/api/events");
        createEvent.then().statusCode(201)
                .body("id", matchesPattern("[0-9a-fA-F-]{36}"))
                .body("name", equalTo("Backup"))
                .body("description", equalTo("Weekly"))
                .body("userId", equalTo(owner.id()))
                .body("startTime", equalTo("2026-10-12T18:00:00"))
                .body("endTime", equalTo("2026-10-12T19:00:00"))
                .body("creationTime", notNullValue())
                .body("lastModificationTime", notNullValue());
        String firstEventId = createEvent.path("id");
        createEvent.then().header("Location", endsWith("/api/events/" + firstEventId));

        String secondEventId = given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Maintenance", "startTime", "2026-10-14T18:00:00",
                        "endTime", "2026-10-14T19:00:00"))
                .when().post("/api/events")
                .then().statusCode(201).body("description", nullValue()).extract().path("id");

        given().auth().oauth2(ownerTokens.accessToken()).when().get("/api/events")
                .then().statusCode(200).body("size()", equalTo(2)).body("id", hasItems(firstEventId, secondEventId));
        given().auth().oauth2(ownerTokens.accessToken()).queryParam("from", "2026-10-12T18:30:00")
                .queryParam("to", "2026-10-12T20:00:00").when().get("/api/events")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(firstEventId));
        given().auth().oauth2(ownerTokens.accessToken()).queryParam("from", "2026-10-13T00:00:00")
                .when().get("/api/events")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(secondEventId));
        given().auth().oauth2(ownerTokens.accessToken()).queryParam("to", "2026-10-12T17:59:59")
                .when().get("/api/events")
                .then().statusCode(200).body("size()", equalTo(0));
        given().auth().oauth2(ownerTokens.accessToken()).queryParam("from", "not-a-date")
                .when().get("/api/events")
                .then().statusCode(400).body("message", notNullValue());
        given().auth().oauth2(ownerTokens.accessToken()).queryParam("from", "2026-10-15T00:00:00")
                .queryParam("to", "2026-10-14T00:00:00").when().get("/api/events")
                .then().statusCode(400).body("message", notNullValue());

        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("description", "Changed"))
                .when().patch("/api/events/{id}", firstEventId)
                .then().statusCode(200)
                .body("id", equalTo(firstEventId))
                .body("name", equalTo("Backup"))
                .body("description", equalTo("Changed"))
                .body("startTime", equalTo("2026-10-12T18:00:00"))
                .body("endTime", equalTo("2026-10-12T19:00:00"));
        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("endTime", "2026-10-12T17:00:00"))
                .when().patch("/api/events/{id}", firstEventId)
                .then().statusCode(400).body("message", notNullValue());
        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON).body("{}")
                .when().patch("/api/events/{id}", firstEventId)
                .then().statusCode(400).body("message", notNullValue());

        given().auth().oauth2(otherTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Stolen")).when().patch("/api/events/{id}", firstEventId)
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(otherTokens.accessToken()).when().delete("/api/events/{id}", firstEventId)
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));

        given().auth().oauth2(ownerTokens.accessToken()).when().delete("/api/events/{id}", firstEventId)
                .then().statusCode(204).body(equalTo(""));
        given().auth().oauth2(ownerTokens.accessToken()).when().get("/api/events")
                .then().statusCode(200).body("id", not(hasItem(firstEventId))).body("size()", equalTo(1));
        given().auth().oauth2(ownerTokens.accessToken()).when().delete("/api/events/{id}", firstEventId)
                .then().statusCode(404).body("message", equalTo("Resource not found"));
    }

    @Test
    void adminEndpointsListAllResourcesAndDeleteUsersWithTheirData() throws SQLException {
        TestUser admin = createUser("admin");
        TestUser regularUser = createUser("regular");
        TokenPair regularTokens = login(regularUser);
        String todoId = given().auth().oauth2(regularTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Cascade todo"))
                .when().post("/api/todos").then().statusCode(201).extract().path("id");
        String eventId = given().auth().oauth2(regularTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Cascade event", "startTime", "2026-10-12T18:00:00",
                        "endTime", "2026-10-12T19:00:00"))
                .when().post("/api/events").then().statusCode(201).extract().path("id");

        given().auth().oauth2(regularTokens.accessToken()).when().get("/api/admin/users")
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(regularTokens.accessToken()).when().get("/api/admin/events")
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(regularTokens.accessToken()).when().get("/api/admin/todos")
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(regularTokens.accessToken()).when().delete("/api/admin/users/{id}", regularUser.id())
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));

        setRole(admin.login(), "ADMIN");
        TokenPair adminTokens = login(admin);
        given().auth().oauth2(adminTokens.accessToken()).when().get("/api/admin/users")
                .then().statusCode(200)
                .body("login", hasItems(admin.login(), regularUser.login()))
                .body("role", hasItem("ADMIN"))
                .body("[0].creationTime", notNullValue());
        given().auth().oauth2(adminTokens.accessToken()).when().get("/api/admin/events")
                .then().statusCode(200)
                .body("id", hasItem(eventId))
                .body("name", hasItem("Cascade event"))
                .body("userId", hasItem(regularUser.id()));
        given().auth().oauth2(adminTokens.accessToken()).when().get("/api/admin/todos")
                .then().statusCode(200)
                .body("id", hasItem(todoId))
                .body("name", hasItem("Cascade todo"))
                .body("userId", hasItem(regularUser.id()));
        given().auth().oauth2(adminTokens.accessToken()).when().get("/api/todos/{userId}", regularUser.id())
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(todoId));

        given().auth().oauth2(adminTokens.accessToken()).when().delete("/api/admin/users/{id}", regularUser.id())
                .then().statusCode(204).body(equalTo(""));
        given().auth().oauth2(adminTokens.accessToken()).when().get("/api/admin/users")
                .then().statusCode(200).body("login", not(hasItem(regularUser.login())));
        given().auth().oauth2(adminTokens.accessToken()).when().get("/api/admin/events")
                .then().statusCode(200).body("id", not(hasItem(eventId)));
        given().auth().oauth2(adminTokens.accessToken()).when().get("/api/admin/todos")
                .then().statusCode(200).body("id", not(hasItem(todoId)));
        given().auth().oauth2(adminTokens.accessToken()).when().get("/api/todos/{userId}", regularUser.id())
                .then().statusCode(404).body("message", equalTo("Resource not found"));
        given().auth().oauth2(adminTokens.accessToken()).when().delete("/api/admin/users/{id}", UUID.randomUUID())
                .then().statusCode(404).body("message", equalTo("Resource not found"));
    }

    @AfterEach
    void cleanUpCreatedAccounts() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE login = ?")) {
            for (String login : createdLogins) {
                statement.setString(1, login);
                statement.executeUpdate();
            }
        }
    }

    private TestUser createUser(String prefix) {
        String login = uniqueLogin(prefix);
        createdLogins.add(login);
        Response response = given().contentType(ContentType.JSON)
                .body(Map.of("login", login, "password", password))
                .when().post("/api/auth/register");
        response.then().statusCode(201)
                .body("id", matchesPattern("[0-9a-fA-F-]{36}"))
                .body("login", equalTo(login))
                .body("role", equalTo("USER"))
                .body("creationTime", notNullValue())
                .body("lastModificationTime", notNullValue())
                .body("passwordHash", nullValue());
        return new TestUser(response.path("id"), login, password);
    }

    private TokenPair login(TestUser user) {
        Response response = given().contentType(ContentType.JSON)
                .body(Map.of("login", user.login(), "password", user.password()))
                .when().post("/api/auth/login");
        response.then().statusCode(200)
                .body("accessToken", matchesPattern("[^.]+\\.[^.]+\\.[^.]+"))
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", equalTo(900));
        return tokenPair(response);
    }

    private TokenPair tokenPair(Response response) {
        Map<String, Object> body = response.as(new TypeRef<Map<String, Object>>() {});
        return new TokenPair((String) body.get("accessToken"), (String) body.get("refreshToken"));
    }

    private String uniqueLogin(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void setRole(String login, String role) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("UPDATE users SET role = ? WHERE login = ?")) {
            statement.setString(1, role);
            statement.setString(2, login);
            org.junit.jupiter.api.Assertions.assertEquals(1, statement.executeUpdate());
        }
    }

    private record TestUser(String id, String login, String password) {
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
