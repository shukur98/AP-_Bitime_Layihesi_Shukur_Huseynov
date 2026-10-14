package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.Config;

/** GRAD-204 safe checks; successful FROZEN -> ACTIVE and BLOCKED are in opt-in workflow. */
public class CardUnblockTest extends BaseApiTest {
    @Test @DisplayName("G204 unknown card -> 404 NOT_FOUND")
    void unknownCard() {
        ApiAssertHelper.error(ApiHelper.authorized(user1Token())
                .post("/cards/vcd_zzzzzzzz/unblock"),404,"NOT_FOUND");
    }

    @Test @DisplayName("G204 ACTIVE -> 409 NOT_FROZEN (if ACTIVE fixture provided)")
    void activeCardCannotBeUnblocked() {
        String id=helper.TestFixtures.activeCard(user1Token());
        Assumptions.assumeFalse(id.isEmpty(),"Set QA_ACTIVE_CARD_ID to a known ACTIVE card");
        Response r=ApiHelper.authorized(user1Token()).post("/cards/"+id+"/unblock");
        ApiAssertHelper.error(r,409,"NOT_FROZEN");
    }

    @Test @DisplayName("G204 missing authorization -> 401")
    void noAuthorization() {
        Response r=io.restassured.RestAssured.given().spec(ApiHelper.baseSpec())
                .post("/cards/vcd_zzzzzzzz/unblock");
        ApiAssertHelper.status(r,401);
    }
}
