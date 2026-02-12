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


public class PetApiTest {

    @BeforeAll
    static void setup() {
        Properties props = new Properties();
        try (InputStream is = PetApiTest.class.getClassLoader().getResourceAsStream("specforge.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}

        String baseUrl = props.getProperty("baseUrl", "http://localhost:8080");
        RestAssured.baseURI = baseUrl;
    }

    @Test
    @DisplayName("POST /pet/{petId}/uploadImage - HAPPY_PATH")
    void uploadFile_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("multipart/form-data")
            .body("{\"additionalMetadata\":\"value\"}")

        ;
            requestSpec
                .when()
                    .request("POST", "/pet/1/uploadImage")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/uploadFile_200.json"));

    }

    @Test
    @DisplayName("POST /pet - HAPPY_PATH")
    void addPet_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("application/json")
            .body("{\"id\":1,\"name\":\"value\",\"photoUrls\":[\"value\"],\"tags\":[{}],\"status\":\"available\"}")

        ;
            requestSpec
                .when()
                    .request("POST", "/pet")
                .then()
                    .statusCode(405);

    }

    @Test
    @DisplayName("PUT /pet - HAPPY_PATH")
    void updatePet_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("application/json")
            .body("{\"name\":\"value\",\"photoUrls\":[\"value\"]}")

        ;
            requestSpec
                .when()
                    .request("PUT", "/pet")
                .then()
                    .statusCode(400);

    }

    @Test
    @DisplayName("GET /pet/findByStatus - HAPPY_PATH")
    void findPetsByStatus_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("status", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/pet/findByStatus")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/findPetsByStatus_200.json"));

    }

    @Test
    @DisplayName("GET /pet/findByTags - HAPPY_PATH")
    void findPetsByTags_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("tags", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/pet/findByTags")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/findPetsByTags_200.json"));

    }

    @Test
    @DisplayName("GET /pet/{petId} - HAPPY_PATH")
    void getPetById_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)

        ;
            requestSpec
                .when()
                    .request("GET", "/pet/1")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getPetById_200.json"));

    }

    @Test
    @DisplayName("POST /pet/{petId} - HAPPY_PATH")
    void updatePetWithForm_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .contentType("application/x-www-form-urlencoded")
            .body("{}")

        ;
            requestSpec
                .when()
                    .request("POST", "/pet/1")
                .then()
                    .statusCode(405);

    }

    @Test
    @DisplayName("DELETE /pet/{petId} - HAPPY_PATH")
    void deletePet_happyPath() {
        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)

        ;
            requestSpec
                .when()
                    .request("DELETE", "/pet/1")
                .then()
                    .statusCode(400);

    }

}
