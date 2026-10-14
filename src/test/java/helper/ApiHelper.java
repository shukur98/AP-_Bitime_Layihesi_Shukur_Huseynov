package helper;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import utils.Config;

import java.util.Map;
import java.util.UUID;

/** API-only adaptation of the teacher's helper/ApiHelper.java. */
public final class ApiHelper {
    private ApiHelper() {}

    public static RequestSpecification baseSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(Config.apiUrl())
                .setAccept(ContentType.JSON)
                .build();
    }

    public static RequestSpecification authorized(String token) {
        return io.restassured.RestAssured.given()
                .spec(baseSpec())
                .header("Authorization", "Bearer " + token);
    }

    public static Response get(String token, String path, Map<String, ?> params) {
        RequestSpecification request = authorized(token);
        params.forEach((key, value) -> request.queryParam(key, value));
        return request.get(path);
    }

    public static Response post(String token, String path, Object body) {
        return authorized(token).contentType(ContentType.JSON).body(body).post(path);
    }

    public static Response paymentPost(String token, Object body, String key) {
        RequestSpecification req = authorized(token).contentType(ContentType.JSON);
        if (key != null) req.header("Idempotency-Key", key);
        return req.body(body).post("/bill-payments");
    }

    public static String freshIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}
