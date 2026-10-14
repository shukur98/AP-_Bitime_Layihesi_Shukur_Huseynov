package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import utils.Config;

import java.util.HashMap;
import java.util.Map;

/** Negative boundary checks: avoid card creation during unattended regression. */
public class CardBoundaryTest extends BaseApiTest {
    @ParameterizedTest(name="G201 initialAmount rejected: {0}")
    @CsvSource({"0.99", "1000.01", "10.555", "'50'"})
    void invalidAmount(String amount) {
        String account=helper.TestFixtures.accountId();
        Assumptions.assumeFalse(account.isEmpty(),"QA_CARD_ACCOUNT_ID needed for card tests");
        Map<String,Object> body=new HashMap<>();
        body.put("accountId",account);
        body.put("initialAmount",amount.equals("50") ? amount : new java.math.BigDecimal(amount));
        Response r=ApiHelper.post(user1Token(),"/cards",body);
        ApiAssertHelper.validation(r,"initialAmount");
    }
}
