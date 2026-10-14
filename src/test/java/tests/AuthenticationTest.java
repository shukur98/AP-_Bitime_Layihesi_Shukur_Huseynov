package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

public class AuthenticationTest {
    @Test
    void requestWithoutTokenReturns401() {
        Response r=RestAssured.given().spec(ApiHelper.baseSpec()).get("/billers");
        ApiAssertHelper.status(r,401);
    }

    @Test
    void invalidTokenReturns401() {
        Response r=ApiHelper.authorized("invalid-token").get("/billers");
        ApiAssertHelper.status(r,401);
    }
}
