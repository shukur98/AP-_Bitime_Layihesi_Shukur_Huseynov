package helper;

import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;

/** Reusable explicit assertions: a 4xx is PASS only when expected by the scenario. */
public final class ApiAssertHelper {
    private ApiAssertHelper() {}

    public static void status(Response r, int expected) {
        Assertions.assertEquals(expected, r.statusCode(),
                () -> "HTTP status mismatch, response=" + r.asString());
    }

    public static void schema(Response r, String file) {
        r.then().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schema/" + file));
    }

    public static void error(Response r, int expectedStatus, String code) {
        status(r, expectedStatus);
        Assertions.assertEquals(code, r.jsonPath().getString("code"), r.asString());
    }

    @SuppressWarnings("unchecked")
    public static void validation(Response r, String field) {
        error(r, 400, "VALIDATION_ERROR");
        List<Map<String, Object>> details = r.jsonPath().getList("details");
        Assertions.assertNotNull(details, "details array is required");
        Assertions.assertTrue(details.stream().anyMatch(d -> field.equals(d.get("field"))),
                "Expected validation for field: " + field + ", actual=" + details);
        schema(r, "validation-error.schema.json");
    }
}
