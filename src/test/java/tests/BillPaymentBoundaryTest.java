package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import model.BillPaymentRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import utils.Config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** GRAD-302: rejected-request BVA and Idempotency-Key headers. */
public class BillPaymentBoundaryTest extends BaseApiTest {
    private String activeCard() {
        String card=helper.TestFixtures.activeCard(user1Token());
        Assumptions.assumeFalse(card.isEmpty(),"Set a user1-owned ACTIVE card in QA_ACTIVE_CARD_ID");
        return card;
    }
    private String subscriber() { return Config.get("QA_VALID_SUBSCRIBER_NO","1234567890"); }
    private BillPaymentRequest validBody(BigDecimal amount) {
        return new BillPaymentRequest(activeCard(),"azerisiq",subscriber(),amount);
    }

    @Test
    void missingIdempotencyKey400() {
        Response r=ApiHelper.paymentPost(user1Token(),validBody(new BigDecimal("10.00")),null);
        ApiAssertHelper.error(r,400,"IDEMPOTENCY_KEY_REQUIRED");
    }

    @Test
    void shortIdempotencyKey400() {
        Response r=ApiHelper.paymentPost(user1Token(),validBody(new BigDecimal("10.00")),"1234567");
        ApiAssertHelper.error(r,400,"IDEMPOTENCY_KEY_INVALID");
    }

    @Test
    void amountAsString400() {
        Map<String,Object> body=new HashMap<>();
        body.put("cardId",activeCard());
        body.put("billerId","azerisiq");
        body.put("subscriberNo",subscriber());
        body.put("amount","10");
        Response r=ApiHelper.paymentPost(user1Token(),body,ApiHelper.freshIdempotencyKey());
        ApiAssertHelper.validation(r,"amount");
    }

    @ParameterizedTest(name="G302 AzerCell amount below/above bounds: {0}")
    @ValueSource(strings={"0.49","200.01"})
    void azercellBoundaryAmount422(String invalidAmount) {
        BillPaymentRequest body=new BillPaymentRequest(activeCard(),"azercell","501234567",new BigDecimal(invalidAmount));
        Response r=ApiHelper.paymentPost(user1Token(),body,ApiHelper.freshIdempotencyKey());
        ApiAssertHelper.error(r,422,"AMOUNT_OUT_OF_RANGE");
    }

    @Test
    void invalidSubscriberRejectedBeforeCardLookup() {
        // Documented evaluation order: INVALID_SUBSCRIBER precedes CARD_NOT_FOUND.
        BillPaymentRequest body = new BillPaymentRequest("vcd_zzzzzzzz", "azercell", "551234567", new BigDecimal("10.00"));
        Response r=ApiHelper.paymentPost(user1Token(),body,ApiHelper.freshIdempotencyKey());
        ApiAssertHelper.error(r,422,"INVALID_SUBSCRIBER");
        org.junit.jupiter.api.Assertions.assertNotNull(r.jsonPath().getString("expected"));
    }

    @Test
    void unknownBillerRejected() {
        BillPaymentRequest body=new BillPaymentRequest("vcd_zzzzzzzz","unknown-service",subscriber(),new BigDecimal("10.00"));
        ApiAssertHelper.error(ApiHelper.paymentPost(user1Token(),body,ApiHelper.freshIdempotencyKey()),404,"BILLER_NOT_FOUND");
    }

    @Test
    void unknownCardRejected() {
        BillPaymentRequest body=new BillPaymentRequest("vcd_zzzzzzzz","azerisiq",subscriber(),new BigDecimal("10.00"));
        ApiAssertHelper.error(ApiHelper.paymentPost(user1Token(),body,ApiHelper.freshIdempotencyKey()),404,"CARD_NOT_FOUND");
    }

    @Test
    void tooLongIdempotencyKeyRejected() {
        BillPaymentRequest body=new BillPaymentRequest("vcd_zzzzzzzz","azerisiq",subscriber(),new BigDecimal("10.00"));
        ApiAssertHelper.error(ApiHelper.paymentPost(user1Token(),body,"a".repeat(65)),400,"IDEMPOTENCY_KEY_INVALID");
    }

    @Test
    void perTransactionAmount300Point01Rejected() {
        // The utility biller allows amounts <= 500; limit checking is applied to total.
        BillPaymentRequest body=new BillPaymentRequest("vcd_zzzzzzzz","azerisiq",subscriber(),new BigDecimal("300.01"));
        // CARD_NOT_FOUND happens before PER_TRANSACTION_LIMIT, therefore a live card fixture is necessary.
        String id=helper.TestFixtures.activeCard(user1Token());
        Assumptions.assumeFalse(id.isEmpty(),"Set QA_ACTIVE_CARD_ID to a user1-owned ACTIVE card");
        body.cardId=id;
        ApiAssertHelper.error(ApiHelper.paymentPost(user1Token(),body,ApiHelper.freshIdempotencyKey()),422,"PER_TRANSACTION_LIMIT");
    }
}
