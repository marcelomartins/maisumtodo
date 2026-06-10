package net.marcelomartins.maisumtodo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class ApiFlowTest {

    @Test
    void registerCreateProjectCreateTaskAndInvalidateTokenOnLogout() {
        String email = "user-" + System.nanoTime() + "@example.com";

        String authToken = given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "123456"))
                .when().post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("email", is(email))
                .cookie("auth_token", notNullValue())
                .extract().cookie("auth_token");

        given()
                .cookie("auth_token", authToken)
                .when().get("/api/auth/me")
                .then()
                .statusCode(200)
                .body("email", is(email));

        String projectUuid = given()
                .cookie("auth_token", authToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Pessoal"))
                .when().post("/api/projects")
                .then()
                .statusCode(201)
                .body("name", is("Pessoal"))
                .extract().path("uuid");

        String taskUuid = given()
                .cookie("auth_token", authToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Comprar cafe"))
                .when().post("/api/projects/{projectUuid}/tasks", projectUuid)
                .then()
                .statusCode(201)
                .body("title", is("Comprar cafe"))
                .body("status", is("TODO"))
                .extract().path("uuid");

        given()
                .cookie("auth_token", authToken)
                .contentType(ContentType.JSON)
                .body(Map.of("status", "DOING"))
                .when().put("/api/tasks/{taskUuid}", taskUuid)
                .then()
                .statusCode(200)
                .body("status", is("DOING"));

        given()
                .cookie("auth_token", authToken)
                .when().post("/api/auth/logout")
                .then()
                .statusCode(200);

        given()
                .cookie("auth_token", authToken)
                .when().get("/api/auth/me")
                .then()
                .statusCode(401);
    }
}
