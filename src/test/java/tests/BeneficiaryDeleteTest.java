package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GRAD-103: read-only negative checks; successful DELETE is in BeneficiaryTest gated workflow. */
public class BeneficiaryDeleteTest extends BaseApiTest {
    @Test @DisplayName("G103 unknown beneficiary -> 404 NOT_FOUND")
    void unknownBeneficiary() {
        Response r=ApiHelper.authorized(user1Token()).delete("/beneficiaries/ben_zzzzzzzz");
        ApiAssertHelper.error(r,404,"NOT_FOUND");
    }

    @Test @DisplayName("G103 no token -> 401")
    void missingAuthorization() {
        Response r=io.restassured.RestAssured.given().spec(ApiHelper.baseSpec())
                .delete("/beneficiaries/ben_zzzzzzzz");
        ApiAssertHelper.status(r,401);
    }
}
