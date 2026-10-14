package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

/** GRAD-203 safe negative-path checks. Positive state machine is in opt-in CardLifecycleWorkflowTest. */
public class CardBlockTest extends BaseApiTest {
    private static final String PATH="/cards/vcd_zzzzzzzz/block";

    @ParameterizedTest(name="G203 invalid reason: {0}")
    @ValueSource(strings={"BROKEN","invalid",""})
    void invalidReason(String reason) {
        ApiAssertHelper.validation(ApiHelper.post(user1Token(),PATH, Map.of("reason",reason)),"reason");
    }

    @Test @DisplayName("G203 missing reason -> 400 VALIDATION_ERROR")
    void missingReason() {
        ApiAssertHelper.validation(ApiHelper.post(user1Token(),PATH,Map.of()),"reason");
    }

    @Test @DisplayName("G203 reason valid but card id missing -> 404 NOT_FOUND")
    void missingCard() {
        ApiAssertHelper.error(ApiHelper.post(user1Token(),PATH,Map.of("reason","TEMPORARY")),404,"NOT_FOUND");
    }

    @Test @DisplayName("G203 omitted JSON body -> 400 INVALID_JSON")
    void missingBody() {
        Response r=ApiHelper.authorized(user1Token()).post(PATH);
        ApiAssertHelper.error(r,400,"INVALID_JSON");
    }
}
