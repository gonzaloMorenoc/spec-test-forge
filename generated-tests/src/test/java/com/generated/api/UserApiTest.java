package com.generated.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;


public class UserApiTest {

    @BeforeAll
    static void setup() {
        Properties props = new Properties();
        try (InputStream is = UserApiTest.class.getClassLoader().getResourceAsStream("specforge.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}

        String baseUrl = props.getProperty("baseUrl", "http://localhost:8080");
        RestAssured.baseURI = baseUrl;
    }

    @Test
    @DisplayName("POST /user/createWithList - HAPPY_PATH")
    void createUsersWithListInput_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("application/json")
            .body("[{\"lastName\":\"value\"}]")

        ;
            requestSpec
                .when()
                    .request("POST", "/user/createWithList")
                .then()
                    .statusCode(200);

    }

    @Test
    @DisplayName("GET /user/{username} - HAPPY_PATH")
    void getUserByName_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)

        ;
            requestSpec
                .when()
                    .request("GET", "/user/1")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getUserByName_200.json"));

    }

    @Test
    @DisplayName("PUT /user/{username} - HAPPY_PATH")
    void updateUser_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("application/json")
            .body("{\"id\":1,\"lastName\":\"value\",\"password\":\"value\"}")

        ;
            requestSpec
                .when()
                    .request("PUT", "/user/1")
                .then()
                    .statusCode(400);

    }

    @Test
    @DisplayName("DELETE /user/{username} - HAPPY_PATH")
    void deleteUser_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)

        ;
            requestSpec
                .when()
                    .request("DELETE", "/user/1")
                .then()
                    .statusCode(400);

    }

    @Test
    @DisplayName("GET /user/login - HAPPY_PATH")
    void loginUser_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("username", "value")
            .queryParam("password", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/user/login")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/loginUser_200.json"));

    }

    @Test
    @DisplayName("GET /user/logout - HAPPY_PATH")
    void logoutUser_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)

        ;
            requestSpec
                .when()
                    .request("GET", "/user/logout")
                .then()
                    .statusCode(200);

    }

    @Test
    @DisplayName("POST /user/createWithArray - HAPPY_PATH")
    void createUsersWithArrayInput_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("application/json")
            .body("[{\"id\":1,\"username\":\"value\"}]")

        ;
            requestSpec
                .when()
                    .request("POST", "/user/createWithArray")
                .then()
                    .statusCode(200);

    }

    @Test
    @DisplayName("POST /user - HAPPY_PATH")
    void createUser_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("application/json")
            .body("{\"id\":1,\"username\":\"value\",\"firstName\":\"value\",\"lastName\":\"value\",\"email\":\"value\"}")

        ;
            requestSpec
                .when()
                    .request("POST", "/user")
                .then()
                    .statusCode(200);

    }

}
