package imp;

import com.thoughtworks.gauge.Step;
import helper.ApiAssertHelper;
import helper.ApiHelper;
import helper.AuthHelper;
import io.restassured.response.Response;
import model.BeneficiaryRequest;
import model.BeneficiaryListResponse;
import org.junit.jupiter.api.Assertions;
import utils.Config;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Executable, NON-MUTATING Gauge smoke/negative scenarios for ALL ten stories.
 * Stateful positive journeys are implemented in JUnit CardLifecycleWorkflowTest
 * and BeneficiaryTest, and are opt-in to avoid dirtying the user's sandbox.
 * One token is cached for each user for the lifetime of the Gauge JVM.
 */
public class FinalProjectGaugeSteps {
    private static final String ABSENT_BENEFICIARY = "ben_zzzzzzzz";
    private static final String ABSENT_CARD = "vcd_zzzzzzzz";

    private String token() {
        if (!AuthHelper.configured(1)) {
            throw new AssertionError("Configure QA_TOKEN_USER1 or QA_USER1_EMAIL/QA_USER1_PASSWORD before running Gauge");
        }
        return AuthHelper.token(1);
    }

    @Step("G101 short fullName is rejected")
    public void g101ShortName() {
        Response r = ApiHelper.post(token(), "/beneficiaries",
                new BeneficiaryRequest("A", "AZ41AIIB00000000000000900001", "AZN", null));
        ApiAssertHelper.validation(r, "fullName");
    }

    @Step("G101 invalid IBAN is rejected")
    public void g101InvalidIban() {
        Response r = ApiHelper.post(token(), "/beneficiaries",
                new BeneficiaryRequest("Valid Name", "AZ123", "AZN", null));
        ApiAssertHelper.validation(r, "iban");
    }

    @Step("G102 beneficiary list matches contract")
    public void g102ListContract() {
        Response r = ApiHelper.get(token(), "/beneficiaries", Map.of());
        ApiAssertHelper.status(r, 200);
        ApiAssertHelper.schema(r, "beneficiary-list.schema.json");
        BeneficiaryListResponse list = r.as(BeneficiaryListResponse.class);
        Assertions.assertEquals(list.total, list.items.size());
    }

    @Step("G102 invalid one character search is rejected")
    public void g102ShortQuery() {
        ApiAssertHelper.validation(ApiHelper.get(token(), "/beneficiaries", Map.of("q", "a")), "q");
    }

    @Step("G102 unknown search returns empty list")
    public void g102NoResults() {
        Response r = ApiHelper.get(token(), "/beneficiaries",
                Map.of("q", "QA_NoBeneficiary_Fixture_Impossible_98765"));
        ApiAssertHelper.status(r, 200);
        BeneficiaryListResponse list = r.as(BeneficiaryListResponse.class);
        Assertions.assertEquals(0, list.total);
        Assertions.assertTrue(list.items.isEmpty());
    }

    @Step("G103 deleting an unknown beneficiary returns NOT_FOUND")
    public void g103UnknownDelete() {
        Response r = ApiHelper.authorized(token()).delete("/beneficiaries/" + ABSENT_BENEFICIARY);
        ApiAssertHelper.error(r, 404, "NOT_FOUND");
    }

    @Step("G201 initialAmount <value> fails validation")
    public void g201AmountBoundary(String value) {
        String account = Config.get("QA_CARD_ACCOUNT_ID", "acc_002");
        Object amount = value.equals("text-50") ? "50" : new BigDecimal(value);
        Response r = ApiHelper.post(token(), "/cards", Map.of("accountId", account, "initialAmount", amount));
        ApiAssertHelper.validation(r, "initialAmount");
    }

    @Step("G201 unknown account returns ACCOUNT_NOT_FOUND")
    public void g201UnknownAccount() {
        Response r = ApiHelper.post(token(), "/cards", Map.of("accountId", "acc_999", "initialAmount", 10));
        ApiAssertHelper.error(r, 404, "ACCOUNT_NOT_FOUND");
    }

    @Step("G202 unknown card is hidden as NOT_FOUND")
    public void g202UnknownCard() {
        Response r = ApiHelper.authorized(token()).get("/cards/" + ABSENT_CARD);
        ApiAssertHelper.error(r, 404, "NOT_FOUND");
    }

    @Step("G203 invalid block reason returns validation error")
    public void g203InvalidReason() {
        Response r = ApiHelper.post(token(), "/cards/" + ABSENT_CARD + "/block", Map.of("reason", "BROKEN"));
        ApiAssertHelper.validation(r, "reason");
    }

    @Step("G203 unknown card cannot be blocked")
    public void g203UnknownCard() {
        Response r = ApiHelper.post(token(), "/cards/" + ABSENT_CARD + "/block", Map.of("reason", "TEMPORARY"));
        ApiAssertHelper.error(r, 404, "NOT_FOUND");
    }

    @Step("G204 unknown card cannot be unblocked")
    public void g204UnknownCard() {
        Response r = ApiHelper.authorized(token()).post("/cards/" + ABSENT_CARD + "/unblock");
        ApiAssertHelper.error(r, 404, "NOT_FOUND");
    }

    @Step("G301 catalog returns six billers")
    public void g301Catalog() {
        Response r = ApiHelper.get(token(), "/billers", Map.of());
        ApiAssertHelper.status(r, 200);
        ApiAssertHelper.schema(r, "biller-list.schema.json");
        Assertions.assertEquals(6, r.jsonPath().getInt("total"));
        Assertions.assertEquals(6, r.jsonPath().getList("items").size());
    }

    @Step("G301 MOBILE category returns Azercell and Bakcell")
    public void g301Mobile() {
        Response r = ApiHelper.get(token(), "/billers", Map.of("category", "MOBILE"));
        ApiAssertHelper.status(r, 200);
        Assertions.assertEquals(2, r.jsonPath().getInt("total"));
        java.util.List<String> ids = r.jsonPath().getList("items.id");
        Assertions.assertTrue(ids.contains("azercell") && ids.contains("bakcell"));
    }

    @Step("G301 GAS category is rejected")
    public void g301Gas() {
        ApiAssertHelper.validation(ApiHelper.get(token(), "/billers", Map.of("category", "GAS")), "category");
    }

    private Map<String, Object> paymentBody(Object amount) {
        return Map.of("cardId", ABSENT_CARD, "billerId", "azerisiq", "subscriberNo", "1234567890", "amount", amount);
    }

    @Step("G302 missing Idempotency-Key returns required error")
    public void g302MissingKey() {
        ApiAssertHelper.error(ApiHelper.paymentPost(token(), paymentBody(10), null), 400, "IDEMPOTENCY_KEY_REQUIRED");
    }

    @Step("G302 short Idempotency-Key returns invalid error")
    public void g302ShortKey() {
        ApiAssertHelper.error(ApiHelper.paymentPost(token(), paymentBody(10), "1234567"), 400, "IDEMPOTENCY_KEY_INVALID");
    }

    @Step("G302 string amount is rejected")
    public void g302StringAmount() {
        ApiAssertHelper.validation(ApiHelper.paymentPost(token(), paymentBody("10"), ApiHelper.freshIdempotencyKey()), "amount");
    }

    @Step("G303 default payment history has page one and limit ten")
    public void g303DefaultHistory() {
        Response r = ApiHelper.get(token(), "/bill-payments", Map.of());
        ApiAssertHelper.status(r, 200);
        ApiAssertHelper.schema(r, "payment-history.schema.json");
        Assertions.assertEquals(1, r.jsonPath().getInt("page"));
        Assertions.assertEquals(10, r.jsonPath().getInt("limit"));
    }

    @Step("G303 invalid <field> query <value> is rejected")
    public void g303InvalidQuery(String field, String value) {
        ApiAssertHelper.validation(ApiHelper.get(token(), "/bill-payments", Map.of(field, value)), field);
    }

    @Step("G303 reversed dates are rejected")
    public void g303ReversedDates() {
        ApiAssertHelper.validation(ApiHelper.get(token(), "/bill-payments",
                Map.of("from", "2026-10-12", "to", "2026-10-11")), "from");
    }
}
