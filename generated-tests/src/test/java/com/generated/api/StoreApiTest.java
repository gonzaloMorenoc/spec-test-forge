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


public class StoreApiTest {

    @BeforeAll
    static void setup() {
        Properties props = new Properties();
        try (InputStream is = StoreApiTest.class.getClassLoader().getResourceAsStream("specforge.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}

        String baseUrl = props.getProperty("baseUrl", "http://localhost:8080");
        RestAssured.baseURI = baseUrl;
    }

    @Test
    @DisplayName("GET /store/inventory - HAPPY_PATH")
    void getInventory_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)

        ;
            requestSpec
                .when()
                    .request("GET", "/store/inventory")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getInventory_200.json"));

    }

    @Test
    @DisplayName("POST /store/order - HAPPY_PATH")
    void placeOrder_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("application/json")
            .body("{\"id\":1,\"quantity\":1}")

        ;
            requestSpec
                .when()
                    .request("POST", "/store/order")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/placeOrder_200.json"));

    }

    @Test
    @DisplayName("GET /store/order/{orderId} - HAPPY_PATH")
    void getOrderById_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)

        ;
            requestSpec
                .when()
                    .request("GET", "/store/order/1")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getOrderById_200.json"));

    }

    @Test
    @DisplayName("DELETE /store/order/{orderId} - HAPPY_PATH")
    void deleteOrder_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)

        ;
            requestSpec
                .when()
                    .request("DELETE", "/store/order/1")
                .then()
                    .statusCode(400);

    }

}
