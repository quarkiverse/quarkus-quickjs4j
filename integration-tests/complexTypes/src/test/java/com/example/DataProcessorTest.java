package com.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test class for DataProcessor with complex types.
 */
@QuarkusTest
public class DataProcessorTest {

    @Test
    public void testProcessPerson() {
        given()
                .when().get("/data-processor/process")
                .then()
                .statusCode(200)
                .body("message", containsString("Alice"))
                .body("message", containsString("Springfield"))
                .body("value", notNullValue())
                .body("tags", notNullValue());
    }

    @Test
    public void testBuiltinsMjsFileGenerated() {
        String filePath = "META-INF/quickjs4j/DataProcessor_Builtins.mjs";
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        assertNotNull(is, "Generated .mjs file should exist on classpath: " + filePath);

        String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        // Verify content contains expected function exports
        assertTrue(content.contains("export function validatePerson"),
                ".mjs file should export validatePerson function");
        assertTrue(content.contains("export function createAddress"),
                ".mjs file should export createAddress function");
        assertTrue(content.contains("export function formatAddress"),
                ".mjs file should export formatAddress function");
        assertTrue(content.contains("DataProcessor_Builtins"),
                ".mjs file should reference DataProcessor_Builtins global object");
    }

    @Test
    public void testBuiltinsDtsFileGenerated() {
        String filePath = "META-INF/quickjs4j/DataProcessor_Builtins.d.ts";
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        assertNotNull(is, "Generated .d.ts file should exist on classpath: " + filePath);

        String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        // Verify content contains expected TypeScript definitions
        assertTrue(content.contains("export function validatePerson"),
                ".d.ts file should declare validatePerson function");
        assertTrue(content.contains("export function createAddress"),
                ".d.ts file should declare createAddress function");
        assertTrue(content.contains("export function formatAddress"),
                ".d.ts file should declare formatAddress function");
        assertTrue(content.contains(": any"),
                ".d.ts file should contain TypeScript type annotations");
    }
}
