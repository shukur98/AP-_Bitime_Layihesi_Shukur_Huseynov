package tests;

import io.restassured.module.jsv.JsonSchemaValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Offline contract quality check: each schema must be parseable by the exact
 * RestAssured json-schema-validator used by the integration tests.
 *
 * An empty JSON object is intentionally not a valid API response; this test
 * checks SCHEMA syntax, not an API instance. No HTTP or auth is required.
 */
public class SchemaSyntaxTest {
    @ParameterizedTest(name = "RestAssured recognizes schema {0}")
    @ValueSource(strings = {
            "beneficiary-list.schema.json",
            "biller-list.schema.json",
            "card-state.schema.json",
            "payment-history.schema.json",
            "validation-error.schema.json",
            "virtual-card.schema.json"
    })
    void schemaCanBeLoadedAndCompiled(String file) {
        assertDoesNotThrow(() -> {
            // The validator reads from src/test/resources on the test classpath.
            // The matcher is exercised so semantic/syntax validation is triggered.
            JsonSchemaValidator.matchesJsonSchemaInClasspath("schema/" + file)
                    .matches("{}");
        }, "Invalid JSON Schema: " + file);
    }
}
