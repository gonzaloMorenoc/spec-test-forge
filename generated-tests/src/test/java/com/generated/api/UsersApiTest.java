package com.generated.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;


public class UsersApiTest {

    @BeforeAll
    static void setup() {
        Properties props = new Properties();
        try (InputStream is = UsersApiTest.class.getClassLoader().getResourceAsStream("specforge.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}

        String baseUrl = props.getProperty("baseUrl", "http://localhost:8080");
        RestAssured.baseURI = baseUrl;
    }

    @Test
    @DisplayName("POST /users - HAPPY_PATH")
    void createUser_happyPath() {
        given()
            .accept(ContentType.JSON)
            .queryParam("tenantId", 1)
            .contentType("application/json")
            .body("{\"name\":\"value\",\"email\":\"user@example.com\",\"age\":18,\"metadata\":{\"active\":true}}")

        .when()
            .request("POST", "/users")
        .then()
            .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/createUser_201.json"));
    }

    @Test
    @DisplayName("GET /users/{id} - HAPPY_PATH")
    void getUser_happyPath() {
        given()
            .accept(ContentType.JSON)
            .queryParam("includeDetails", true)

        .when()
            .request("GET", "/users/1")
        .then()
            .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/getUser_200.json"));
    }

}
