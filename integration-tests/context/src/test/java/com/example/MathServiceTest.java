package com.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MathServiceTest {
    @Test
    public void testMathEndpoint() {
        given().when()
                .get("/math")
                .then()
                .statusCode(200)
                .body(is("41"));
    }
}
