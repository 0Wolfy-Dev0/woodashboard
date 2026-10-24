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
import java.util.List;
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

    @Test
    void groupEndpointsShareEventsAndTodosWithMembersAndEnforceMembership() {
        TestUser owner = createUser("group-owner");
        TestUser member = createUser("group-member");
        TestUser outsider = createUser("group-outsider");
        TokenPair ownerTokens = login(owner);
        TokenPair memberTokens = login(member);
        TokenPair outsiderTokens = login(outsider);

        Response createGroup = given().auth().oauth2(ownerTokens.accessToken())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Household", "description", "Shared plans"))
                .when().post("/api/groups");
        createGroup.then().statusCode(201)
                .header("Location", matchesPattern(".*/api/groups/[0-9a-fA-F-]{36}"))
                .body("id", matchesPattern("[0-9a-fA-F-]{36}"))
                .body("name", equalTo("Household"))
                .body("description", equalTo("Shared plans"))
                .body("ownerId", equalTo(owner.id()))
                .body("ownerLogin", equalTo(owner.login()))
                .body("members.size()", equalTo(1))
                .body("members[0].login", equalTo(owner.login()))
                .body("creationTime", notNullValue())
                .body("lastModificationTime", notNullValue());
        String groupId = createGroup.path("id");

        given().auth().oauth2(ownerTokens.accessToken()).when().get("/api/groups")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(groupId));
        given().auth().oauth2(outsiderTokens.accessToken()).when().get("/api/groups")
                .then().statusCode(200).body("id", not(hasItem(groupId)));
        given().auth().oauth2(outsiderTokens.accessToken()).when().get("/api/groups/{id}", groupId)
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));

        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("login", member.login()))
                .when().post("/api/groups/{id}/members", groupId)
                .then().statusCode(200)
                .body("members.size()", equalTo(2))
                .body("members.login", hasItems(owner.login(), member.login()));
        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("login", member.login()))
                .when().post("/api/groups/{id}/members", groupId)
                .then().statusCode(409).body("message", equalTo("User is already a member of this group"));
        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("login", "missing-user"))
                .when().post("/api/groups/{id}/members", groupId)
                .then().statusCode(404).body("message", equalTo("Resource not found"));

        given().auth().oauth2(memberTokens.accessToken()).when().get("/api/groups/{id}", groupId)
                .then().statusCode(200).body("members.size()", equalTo(2));
        given().auth().oauth2(memberTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Renamed"))
                .when().patch("/api/groups/{id}", groupId)
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));

        String eventId = given().auth().oauth2(memberTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Group backup", "startTime", "2026-10-25T18:00:00",
                        "endTime", "2026-10-25T19:00:00", "groupId", groupId))
                .when().post("/api/events")
                .then().statusCode(201)
                .body("groupId", equalTo(groupId))
                .body("userId", equalTo(member.id()))
                .extract().path("id");
        String todoId = given().auth().oauth2(memberTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Group task", "groupId", groupId))
                .when().post("/api/todos")
                .then().statusCode(201)
                .body("groupId", equalTo(groupId))
                .body("userId", equalTo(member.id()))
                .extract().path("id");

        given().auth().oauth2(ownerTokens.accessToken()).when().get("/api/events")
                .then().statusCode(200).body("id", hasItem(eventId));
        given().auth().oauth2(ownerTokens.accessToken()).queryParam("groupId", groupId)
                .when().get("/api/events")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(eventId));
        given().auth().oauth2(memberTokens.accessToken()).queryParam("groupId", groupId)
                .when().get("/api/todos")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(todoId));
        given().auth().oauth2(outsiderTokens.accessToken()).queryParam("groupId", groupId)
                .when().get("/api/events")
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(outsiderTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Not allowed", "startTime", "2026-10-25T18:00:00",
                        "endTime", "2026-10-25T19:00:00", "groupId", groupId))
                .when().post("/api/events")
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));

        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("description", "Updated by another member"))
                .when().patch("/api/events/{id}", eventId)
                .then().statusCode(200).body("groupId", equalTo(groupId));
        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("completed", true))
                .when().patch("/api/todos/{id}", todoId)
                .then().statusCode(200).body("groupId", equalTo(groupId)).body("completed", equalTo(true));

        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Household renamed"))
                .when().patch("/api/groups/{id}", groupId)
                .then().statusCode(200).body("name", equalTo("Household renamed"));
        given().auth().oauth2(ownerTokens.accessToken()).when().delete("/api/groups/{id}/members/{userId}",
                groupId, owner.id())
                .then().statusCode(400).body("message", equalTo("The group owner cannot be removed"));
        given().auth().oauth2(ownerTokens.accessToken()).when().delete("/api/groups/{id}/members/{userId}",
                groupId, member.id())
                .then().statusCode(204);
        given().auth().oauth2(memberTokens.accessToken()).when().get("/api/groups/{id}", groupId)
                .then().statusCode(403);

        given().auth().oauth2(ownerTokens.accessToken()).when().delete("/api/events/{id}", eventId)
                .then().statusCode(204);
        given().auth().oauth2(ownerTokens.accessToken()).when().delete("/api/todos/{id}", todoId)
                .then().statusCode(204);
        given().auth().oauth2(ownerTokens.accessToken()).when().delete("/api/groups/{id}", groupId)
                .then().statusCode(204);
        given().auth().oauth2(ownerTokens.accessToken()).when().get("/api/groups/{id}", groupId)
                .then().statusCode(404);
    }

    @Test
    void todoFoldersOrganizeTasksAndSurviveFolderDeletion() {
        TestUser owner = createUser("folder-owner");
        TestUser other = createUser("folder-other");
        TokenPair ownerTokens = login(owner);
        TokenPair otherTokens = login(other);

        Response createFolder = given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Home"))
                .when().post("/api/todo-folders");
        createFolder.then().statusCode(201)
                .header("Location", matchesPattern(".*/api/todo-folders/[0-9a-fA-F-]{36}"))
                .body("id", matchesPattern("[0-9a-fA-F-]{36}"))
                .body("name", equalTo("Home"))
                .body("userId", equalTo(owner.id()))
                .body("creationTime", notNullValue())
                .body("lastModificationTime", notNullValue());
        String homeFolderId = createFolder.path("id");

        String workFolderId = given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Work"))
                .when().post("/api/todo-folders")
                .then().statusCode(201).extract().path("id");

        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "home"))
                .when().post("/api/todo-folders")
                .then().statusCode(409).body("message", equalTo("A todo folder with this name already exists"));
        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON).body("{}")
                .when().post("/api/todo-folders")
                .then().statusCode(400).body("message", notNullValue());

        given().auth().oauth2(ownerTokens.accessToken()).when().get("/api/todo-folders")
                .then().statusCode(200).body("size()", equalTo(2)).body("name", hasItems("Home", "Work"));
        given().auth().oauth2(otherTokens.accessToken()).when().get("/api/todo-folders")
                .then().statusCode(200).body("size()", equalTo(0));

        String filedTodoId = given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Buy batteries", "folderId", homeFolderId))
                .when().post("/api/todos")
                .then().statusCode(201).body("folderId", equalTo(homeFolderId)).extract().path("id");
        String unfiledTodoId = given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Loose task"))
                .when().post("/api/todos")
                .then().statusCode(201).body("folderId", nullValue()).extract().path("id");

        given().auth().oauth2(ownerTokens.accessToken()).queryParam("folderId", homeFolderId)
                .when().get("/api/todos")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(filedTodoId));
        given().auth().oauth2(otherTokens.accessToken()).queryParam("folderId", homeFolderId)
                .when().get("/api/todos")
                .then().statusCode(404).body("message", equalTo("Resource not found"));
        given().auth().oauth2(otherTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Not mine", "folderId", homeFolderId))
                .when().post("/api/todos")
                .then().statusCode(404).body("message", equalTo("Resource not found"));

        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("folderId", workFolderId))
                .when().patch("/api/todos/{id}", unfiledTodoId)
                .then().statusCode(200).body("folderId", equalTo(workFolderId));
        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body("{\"folderId\":null}")
                .when().patch("/api/todos/{id}", unfiledTodoId)
                .then().statusCode(200).body("folderId", nullValue());

        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Home chores"))
                .when().patch("/api/todo-folders/{id}", homeFolderId)
                .then().statusCode(200).body("name", equalTo("Home chores"));
        given().auth().oauth2(otherTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Stolen"))
                .when().patch("/api/todo-folders/{id}", homeFolderId)
                .then().statusCode(404).body("message", equalTo("Resource not found"));
        given().auth().oauth2(otherTokens.accessToken()).when().delete("/api/todo-folders/{id}", homeFolderId)
                .then().statusCode(404);
        given().auth().oauth2(ownerTokens.accessToken()).contentType(ContentType.JSON).body("{}")
                .when().patch("/api/todo-folders/{id}", homeFolderId)
                .then().statusCode(400).body("message", notNullValue());

        given().auth().oauth2(ownerTokens.accessToken()).when().delete("/api/todo-folders/{id}", homeFolderId)
                .then().statusCode(204).body(equalTo(""));
        given().auth().oauth2(ownerTokens.accessToken()).when().get("/api/todos/{userId}", owner.id())
                .then().statusCode(200)
                .body("size()", equalTo(2))
                .body("findAll { it.id == '%s' }.folderId".formatted(filedTodoId), hasItem(nullValue()));
        given().auth().oauth2(ownerTokens.accessToken()).queryParam("folderId", homeFolderId)
                .when().get("/api/todos")
                .then().statusCode(404);
    }

    @Test
    void recurringAndAllDayEventsExpandOnlyInsideTheRequestedWindow() {
        TestUser user = createUser("recurrence");
        TokenPair tokens = login(user);

        Response allDay = given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Conference", "startTime", "2026-10-12T15:30:00",
                        "endTime", "2026-10-13T10:15:00", "allDay", true))
                .when().post("/api/events");
        allDay.then().statusCode(201)
                .body("allDay", equalTo(true))
                .body("startTime", equalTo("2026-10-12T00:00:00"))
                .body("endTime", equalTo("2026-10-13T23:59:59"))
                .body("seriesStartTime", equalTo("2026-10-12T00:00:00"))
                .body("seriesEndTime", equalTo("2026-10-13T23:59:59"))
                .body("recurrenceUnit", nullValue())
                .body("recurrenceInterval", nullValue());

        Response recurring = given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Water plants", "startTime", "2026-10-12T09:00:00",
                        "endTime", "2026-10-12T10:00:00", "recurrenceUnit", "DAYS",
                        "recurrenceInterval", 3, "recurrenceEnd", "2026-10-20T23:59:59"))
                .when().post("/api/events");
        recurring.then().statusCode(201)
                .body("allDay", equalTo(false))
                .body("recurrenceUnit", equalTo("DAYS"))
                .body("recurrenceInterval", equalTo(3))
                .body("recurrenceEnd", equalTo("2026-10-20T23:59:59"))
                .body("seriesStartTime", equalTo("2026-10-12T09:00:00"));
        String recurringId = recurring.path("id");

        List<Map<String, Object>> expanded = given().auth().oauth2(tokens.accessToken())
                .queryParam("from", "2026-10-11T00:00:00")
                .queryParam("to", "2026-10-19T23:59:59")
                .when().get("/api/events")
                .then().statusCode(200).extract().as(new TypeRef<List<Map<String, Object>>>() {});
        org.junit.jupiter.api.Assertions.assertEquals(4, expanded.size());
        List<String> recurringStarts = expanded.stream()
                .filter(event -> recurringId.equals(event.get("id")))
                .map(event -> (String) event.get("startTime"))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(
                List.of("2026-10-12T09:00:00", "2026-10-15T09:00:00", "2026-10-18T09:00:00"),
                recurringStarts);
        org.junit.jupiter.api.Assertions.assertTrue(expanded.stream()
                .filter(event -> recurringId.equals(event.get("id")))
                .allMatch(event -> "2026-10-12T09:00:00".equals(event.get("seriesStartTime"))));

        given().auth().oauth2(tokens.accessToken())
                .queryParam("from", "2026-10-15T00:00:00")
                .queryParam("to", "2026-10-15T23:59:59")
                .when().get("/api/events")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].id", equalTo(recurringId));

        // Without a window the series definitions are returned, not an endless expansion.
        given().auth().oauth2(tokens.accessToken()).when().get("/api/events")
                .then().statusCode(200).body("size()", equalTo(2));

        String birthdayId = given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Birthday", "startTime", "2000-05-04T00:00:00",
                        "endTime", "2000-05-04T23:59:59", "allDay", true, "recurrenceUnit", "YEARS"))
                .when().post("/api/events")
                .then().statusCode(201).body("recurrenceInterval", equalTo(1)).extract().path("id");
        given().auth().oauth2(tokens.accessToken())
                .queryParam("from", "2026-05-04T00:00:00")
                .queryParam("to", "2026-05-04T23:59:59")
                .when().get("/api/events")
                .then().statusCode(200).body("size()", equalTo(1))
                .body("[0].id", equalTo(birthdayId))
                .body("[0].startTime", equalTo("2026-05-04T00:00:00"))
                .body("[0].endTime", equalTo("2026-05-04T23:59:59"));

        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body("{\"recurrenceUnit\":null}")
                .when().patch("/api/events/{id}", recurringId)
                .then().statusCode(200)
                .body("recurrenceUnit", nullValue())
                .body("recurrenceInterval", nullValue())
                .body("recurrenceEnd", nullValue());
        given().auth().oauth2(tokens.accessToken())
                .queryParam("from", "2026-10-11T00:00:00")
                .queryParam("to", "2026-10-19T23:59:59")
                .when().get("/api/events")
                .then().statusCode(200)
                .body("findAll { it.id == '%s' }.size()".formatted(recurringId), equalTo(1));

        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Bad all-day", "startTime", "2026-10-13T00:00:00",
                        "endTime", "2026-10-12T00:00:00", "allDay", true))
                .when().post("/api/events")
                .then().statusCode(400).body("message", notNullValue());
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Bad interval", "startTime", "2026-10-12T09:00:00",
                        "endTime", "2026-10-12T10:00:00", "recurrenceUnit", "DAYS",
                        "recurrenceInterval", 0))
                .when().post("/api/events")
                .then().statusCode(400).body("message", notNullValue());
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body("{\"name\":\"Interval without unit\",\"startTime\":\"2026-10-12T09:00:00\","
                        + "\"endTime\":\"2026-10-12T10:00:00\",\"recurrenceInterval\":3}")
                .when().post("/api/events")
                .then().statusCode(400).body("message", notNullValue());
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("name", "Bad series end", "startTime", "2026-10-12T09:00:00",
                        "endTime", "2026-10-12T10:00:00", "recurrenceUnit", "DAYS",
                        "recurrenceEnd", "2026-10-01T09:00:00"))
                .when().post("/api/events")
                .then().statusCode(400).body("message", notNullValue());
    }

    @Test
    void usersCanChangeTheirPasswordAndAdminsCanResetAnyPassword() throws SQLException {
        TestUser user = createUser("password");
        TokenPair tokens = login(user);

        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("currentPassword", "wrong-password", "newPassword", "new-password-123"))
                .when().patch("/api/me/password")
                .then().statusCode(400).body("message", equalTo("Current password is incorrect"));
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("currentPassword", user.password(), "newPassword", "short"))
                .when().patch("/api/me/password")
                .then().statusCode(400).body("message", notNullValue());
        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("currentPassword", user.password(), "newPassword", user.password()))
                .when().patch("/api/me/password")
                .then().statusCode(400).body("message", notNullValue());

        given().auth().oauth2(tokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("currentPassword", user.password(), "newPassword", "new-password-123"))
                .when().patch("/api/me/password")
                .then().statusCode(204).body(equalTo(""));
        given().contentType(ContentType.JSON)
                .body(Map.of("login", user.login(), "password", user.password()))
                .when().post("/api/auth/login")
                .then().statusCode(401).body("message", equalTo("Invalid login or password"));
        TokenPair changed = login(new TestUser(user.id(), user.login(), "new-password-123"));
        given().auth().oauth2(changed.accessToken()).when().get("/api/me")
                .then().statusCode(200).body("id", equalTo(user.id()));

        TestUser admin = createUser("password-admin");
        setRole(admin.login(), "ADMIN");
        TokenPair adminTokens = login(admin);
        TestUser target = createUser("password-target");
        TokenPair targetTokens = login(target);

        given().auth().oauth2(targetTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("newPassword", "target-password-123"))
                .when().patch("/api/admin/users/{id}/password", target.id())
                .then().statusCode(403).body("message", equalTo("You are not allowed to access this resource"));
        given().auth().oauth2(adminTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("newPassword", "target-password-123"))
                .when().patch("/api/admin/users/{id}/password", target.id())
                .then().statusCode(204).body(equalTo(""));
        given().auth().oauth2(adminTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("newPassword", "target-password-123"))
                .when().patch("/api/admin/users/{id}/password", UUID.randomUUID())
                .then().statusCode(404).body("message", equalTo("Resource not found"));
        given().auth().oauth2(adminTokens.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("newPassword", "short"))
                .when().patch("/api/admin/users/{id}/password", target.id())
                .then().statusCode(400).body("message", notNullValue());

        given().contentType(ContentType.JSON).body(Map.of("refreshToken", targetTokens.refreshToken()))
                .when().post("/api/auth/refresh")
                .then().statusCode(401).body("message", equalTo("Refresh token is invalid, expired or revoked"));
        given().contentType(ContentType.JSON)
                .body(Map.of("login", target.login(), "password", target.password()))
                .when().post("/api/auth/login")
                .then().statusCode(401).body("message", equalTo("Invalid login or password"));
        login(new TestUser(target.id(), target.login(), "target-password-123"));
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
