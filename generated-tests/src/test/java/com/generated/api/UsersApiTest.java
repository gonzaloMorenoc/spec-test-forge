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
    @DisplayName("GET /users - HAPPY_PATH")
    void listUsers_happyPath() {
        given()
            .accept(ContentType.JSON)
        .when()
            .request("GET", "/users")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("GET /users/{id} - HAPPY_PATH")
    void getUser_happyPath() {
        given()
            .accept(ContentType.JSON)
        .when()
            .request("GET", "/users/{id}")
        .then()
            .statusCode(200);
    }

}
