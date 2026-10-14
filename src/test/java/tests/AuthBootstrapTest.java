package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The minimal authenticated smoke test, independently runnable from IntelliJ. */
public class AuthBootstrapTest extends BaseApiTest {
    @Test
    @DisplayName("AUTH-01: login once, capture token, attach Bearer automatically and GET /billers")
    void autoLoginAndAuthenticatedGet() {
        String token = user1Token(); // calls /auth/login only once per JVM
        assertTrue(token.length() > 10, "Expected a non-empty sandbox token");
        Response r = ApiHelper.get(token, "/billers", Map.of());
        ApiAssertHelper.status(r, 200);
    }
}
