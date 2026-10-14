package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import utils.Config;

import java.util.Map;

/** GRAD-203/204: validation/auth/unknown-id are read-only (no card state mutation). */
public class CardStateValidationTest extends BaseApiTest {
    private String card() {
        String id=helper.TestFixtures.activeCard(user1Token());
        Assumptions.assumeFalse(id.isEmpty(),"QA_ACTIVE_CARD_ID required");
        return id;
    }

    @ParameterizedTest(name="G203 invalid reason {0} -> 400")
    @ValueSource(strings={"BROKEN",""})
    void invalidReason(String reason) {
        String id=card();
        Object body=reason.isEmpty()? Map.of() : Map.of("reason",reason);
        Response r=ApiHelper.post(user1Token(),"/cards/"+id+"/block",body);
        ApiAssertHelper.validation(r,"reason");
    }

    @Test @DisplayName("G203 / body omitted returns 400 INVALID_JSON")
    void omittedJsonBody() {
        String id=card();
        Response r=ApiHelper.authorized(user1Token()).post("/cards/"+id+"/block");
        ApiAssertHelper.error(r,400,"INVALID_JSON");
    }

    @Test @DisplayName("G203 / non-existent card: 404 NOT_FOUND")
    void unknownCardBlock() {
        Response r=ApiHelper.post(user1Token(),"/cards/vcd_zzzzzzzz/block",Map.of("reason","TEMPORARY"));
        ApiAssertHelper.error(r,404,"NOT_FOUND");
    }

    @Test @DisplayName("G204 / non-existent card: 404 NOT_FOUND")
    void unknownCardUnblock() {
        Response r=ApiHelper.authorized(user1Token()).post("/cards/vcd_zzzzzzzz/unblock");
        ApiAssertHelper.error(r,404,"NOT_FOUND");
    }
}
