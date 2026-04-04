package com.leticia;

import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookingTests {

        public static Faker faker;
        private static RequestSpecification request;
        private static Booking booking;
        private static BookingDates bookingDates;
        private static User user;
        private static String token;
        private static int bookingId;

        @BeforeAll
        public static void Setup() {
                RestAssured.baseURI = "https://restful-booker.herokuapp.com";
                faker = new Faker();

                user = new User(
                                faker.name().username(),
                                faker.name().firstName(),
                                faker.name().lastName(),
                                faker.internet().safeEmailAddress(),
                                faker.internet().password(8, 10),
                                faker.phoneNumber().toString());

                bookingDates = new BookingDates("2025-01-01", "2025-01-10");

                booking = new Booking(
                                user.getFirstName(),
                                user.getLastName(),
                                (float) faker.number().randomDouble(2, 50, 100000),
                                true,
                                bookingDates,
                                "Breakfast");

                RestAssured.filters(
                                new RequestLoggingFilter(),
                                new ResponseLoggingFilter(),
                                new ErrorLoggingFilter());
        }

        @BeforeEach
        void setRequest() {
                request = given()
                                .config(RestAssured.config()
                                                .logConfig(logConfig()
                                                                .enableLoggingOfRequestAndResponseIfValidationFails()))
                                .contentType(ContentType.JSON)
                                .auth().basic("admin", "password123");
        }

        @Test
        @Order(1)
        public void createAuthToken_returnOk() {
                Credentials credentials = new Credentials("admin", "password123");

                Response response = given()
                                .contentType(ContentType.JSON)
                                .body(credentials)
                                .when()
                                .post("/auth")
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .extract()
                                .response();

                token = response.jsonPath().getString("token");
                Assertions.assertNotNull(token, "Token não deve ser nulo");
        }

        @Test
        @Order(2)
        public void getAllBookings_returnOk() {
                Response response = request
                                .when()
                                .get("/booking")
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .extract()
                                .response();

                Assertions.assertNotNull(response);
        }

        @Test
        @Order(3)
        public void createBooking_withValidData_returnOk() {
                Response response = given()
                                .config(RestAssured.config()
                                                .logConfig(logConfig()
                                                                .enableLoggingOfRequestAndResponseIfValidationFails()))
                                .contentType(ContentType.JSON)
                                .body(booking)
                                .when()
                                .post("/booking")
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
                                .extract()
                                .response();

                bookingId = response.jsonPath().getInt("bookingid");
                Assertions.assertTrue(bookingId > 0, "BookingId deve ser maior que 0");
        }

        @Test
        @Order(4)
        public void getBookingById_returnOk() {
                request
                                .when()
                                .get("/booking/" + bookingId)
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("firstname", equalTo(booking.getFirstname()))
                                .body("lastname", equalTo(booking.getLastname()));
        }

        @Test
        @Order(5)
        public void updateBooking_withValidData_returnOk() {
                Booking updatedBooking = new Booking(
                                faker.name().firstName(),
                                faker.name().lastName(),
                                (float) faker.number().randomDouble(2, 50, 100000),
                                true,
                                bookingDates,
                                "Lunch");

                request
                                .header("Cookie", "token=" + token)
                                .body(updatedBooking)
                                .when()
                                .put("/booking/" + bookingId)
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("firstname", equalTo(updatedBooking.getFirstname()))
                                .body("lastname", equalTo(updatedBooking.getLastname()));
        }

        @Test
        @Order(6)
        public void partialUpdateBooking_returnOk() {
                Booking partialBooking = new Booking(
                                "Leticia",
                                "Correa",
                                (float) faker.number().randomDouble(2, 50, 100000),
                                true,
                                bookingDates,
                                "Dinner");

                request
                                .header("Cookie", "token=" + token)
                                .body(partialBooking)
                                .when()
                                .patch("/booking/" + bookingId)
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("firstname", equalTo("Leticia"))
                                .body("lastname", equalTo("Correa"));
        }

        @Test
        @Order(7)
        public void deleteBooking_returnOk() {
                request
                                .header("Cookie", "token=" + token)
                                .when()
                                .delete("/booking/" + bookingId)
                                .then()
                                .assertThat()
                                .statusCode(201);
        }
}