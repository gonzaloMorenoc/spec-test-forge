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
import static org.hamcrest.Matchers.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;


public class DefaultApiTest {

    @BeforeAll
    static void setup() {
        Properties props = new Properties();
        try (InputStream is = DefaultApiTest.class.getClassLoader().getResourceAsStream("specforge.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}

        String baseUrl = props.getProperty("baseUrl", "http://localhost:8080");
        RestAssured.baseURI = baseUrl;
    }

    @Test
    @DisplayName("GET /flights - HAPPY_PATH")
    void getFlights_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/flights")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getFlights_200.json"));

    }

    @Test
    @DisplayName("GET /routes - HAPPY_PATH")
    void getRoutes_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/routes")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getRoutes_200.json"));

    }

    @Test
    @DisplayName("GET /airports - HAPPY_PATH")
    void getAirports_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/airports")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getAirports_200.json"));

    }

    @Test
    @DisplayName("GET /airlines - HAPPY_PATH")
    void getAirlines_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/airlines")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getAirlines_200.json"));

    }

    @Test
    @DisplayName("GET /airplanes - HAPPY_PATH")
    void getAirplanes_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/airplanes")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getAirplanes_200.json"));

    }

    @Test
    @DisplayName("GET /aircraft_types - HAPPY_PATH")
    void getAircraftTypes_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/aircraft_types")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getAircraftTypes_200.json"));

    }

    @Test
    @DisplayName("GET /taxes - HAPPY_PATH")
    void getTaxes_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/taxes")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getTaxes_200.json"));

    }

    @Test
    @DisplayName("GET /cities - HAPPY_PATH")
    void getCities_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/cities")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getCities_200.json"));

    }

    @Test
    @DisplayName("GET /countries - HAPPY_PATH")
    void getCountries_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/countries")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getCountries_200.json"));

    }

    @Test
    @DisplayName("GET /timetable - HAPPY_PATH")
    void getFlightSchedule_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")
            .queryParam("iataCode", "value")
            .queryParam("type", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/timetable")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getFlightSchedule_200.json"));

    }

    @Test
    @DisplayName("GET /flightsFuture - HAPPY_PATH")
    void getFutureFlights_happyPath() {


        RequestSpecification requestSpec = given()
            .accept(ContentType.JSON)
            .queryParam("access_key", "value")
            .queryParam("iataCode", "value")
            .queryParam("type", "value")
            .queryParam("date", "value")

        ;
            requestSpec
                .when()
                    .request("GET", "/flightsFuture")
                .then()
                    .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/getFutureFlights_200.json"));

    }

}
