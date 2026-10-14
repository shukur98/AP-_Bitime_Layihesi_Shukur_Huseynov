package helper;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import utils.Config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatic sandbox authentication, aligned with CallMan AUTH-01 / AUTH-02.
 *
 * 1) Explicit QA_TOKEN_USERn takes precedence (existing sandbox).
 * 2) Explicit QA_USERn_EMAIL/PASSWORD logs in automatically.
 * 3) When and ONLY when using the teacher's exact training API host, use
 *    the publicly documented CallMan demonstration accounts as defaults.
 *
 * A successful login creates an isolated sandbox. JVM cache + an optional,
 * private, git-ignored session file preserve the SAME token across JUnit and
 * Gauge invocations. A new login is made only when there is no reusable token
 * or QA_FRESH_SANDBOX=true. Passwords are never written to disk.
 */
public final class AuthHelper {
    private AuthHelper() {}
    private static final String TEACHER_API = "https://api.anarabbas.com";
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    private static void validateUser(int user) {
        if (user != 1 && user != 2) {
            throw new IllegalArgumentException("Unsupported QA user: " + user);
        }
    }

    private static boolean useTeacherDefaults() {
        return TEACHER_API.equals(Config.apiUrl());
    }

    private static String suppliedToken(int user) {
        return Config.get("QA_TOKEN_USER" + user)
                .replaceFirst("(?i)^Bearer\\s+", "").trim();
    }

    private static Map<String, String> credentials(int user) {
        validateUser(user);
        String email = Config.get("QA_USER" + user + "_EMAIL");
        String password = Config.get("QA_USER" + user + "_PASSWORD");
        // If one variable is supplied, do not silently pair it with a demo
        // credential. Catch incomplete / custom configuration explicitly.
        if (email.isBlank() && password.isBlank() && useTeacherDefaults()) {
            email = "user" + user + "@test.com";
            password = "User1234";
        }
        Map<String, String> out = new LinkedHashMap<>();
        out.put("email", email);
        out.put("password", password);
        return out;
    }

    public static String authMode(int user) {
        validateUser(user);
        if (!suppliedToken(user).isBlank()) return "provided-token";
        String email = Config.get("QA_USER" + user + "_EMAIL");
        String password = Config.get("QA_USER" + user + "_PASSWORD");
        if (!email.isBlank() || !password.isBlank()) {
            return email.isBlank() || password.isBlank() ? "incomplete-credentials" : "custom-credentials";
        }
        return useTeacherDefaults() ? "teacher-sandbox-auto-login" : "missing-credentials";
    }

    public static boolean configured(int user) {
        String mode = authMode(user);
        return !mode.equals("missing-credentials") && !mode.equals("incomplete-credentials");
    }

    public static String token(int user) {
        validateUser(user);
        String direct = suppliedToken(user);
        if (!direct.isBlank()) return direct;
        if (!configured(user)) {
            throw new IllegalStateException("Authentication setup incomplete for user" + user
                    + " (mode=" + authMode(user) + "). Provide both email and password,"
                    + " or a token; the built-in training accounts are used only for " + TEACHER_API);
        }
        return CACHE.computeIfAbsent("user" + user, ignored -> {
            String email = credentials(user).get("email");
            return SandboxSessionStore.acquire(user, email, () -> login(user));
        });
    }

    private static String login(int user) {
        Map<String, String> body = credentials(user);
        Response r = io.restassured.RestAssured.given()
                .spec(ApiHelper.baseSpec())
                .contentType(ContentType.JSON)
                .body(body)
                .post("/auth/login");
        if (r.statusCode() != 200) {
            throw new IllegalStateException("POST " + Config.apiUrl() + "/auth/login failed for user"
                    + user + ": HTTP " + r.statusCode()
                    + " (authMode=" + authMode(user) + "). "
                    + "Check teacher sandbox availability and credential validity."
                    + " No secret values were logged. An authenticated test has NOT run.");
        }
        String result = r.jsonPath().getString("token");
        if (result == null || result.isBlank()) {
            // Supporting an alternative field does not change the teacher's
            // real contract, which uses top-level 'token'.
            result = r.jsonPath().getString("accessToken");
        }
        if (result == null || result.isBlank()) {
            throw new IllegalStateException("POST /auth/login returned HTTP 200 but no token/accessToken."
                    + " Verify response contract; do not proceed without a token.");
        }
        return result.replaceFirst("(?i)^Bearer\\s+", "").trim();
    }

    /** Clears JVM memory only. To reset the stored sandbox, run scripts/reset-sandbox.sh. */
    public static void clear() { CACHE.clear(); }
}
