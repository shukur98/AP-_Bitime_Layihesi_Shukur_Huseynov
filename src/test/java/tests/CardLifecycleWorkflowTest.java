package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import helper.AuthHelper;
import io.restassured.response.Response;
import model.BillPaymentRequest;
import model.CardStateResponse;
import model.VirtualCardRequest;
import model.VirtualCardResponse;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.Config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GRAD-201/202/203/204 + GRAD-302 integration.
 * Destructive, SEQUENTIAL, single-run workflow: 4 cards, blocks, and sandbox payment.
 * It is not safe to re-run in the same sandbox without resetting it; no delete-card API exists.
 */
public class CardLifecycleWorkflowTest extends BaseApiTest {

    @Test @DisplayName("G201-204 AC chain: create, read, transitions, payment, replay and security")
    void freshSandboxCardLifecycle() {
        Assumptions.assumeTrue(Config.enabled("qa.cardWorkflow"),
                "Enable with -Dqa.cardWorkflow=true ONLY on a new disposable sandbox");
        String accountId=Config.get("QA_CARD_ACCOUNT_ID");
        Assumptions.assumeFalse(accountId.isEmpty(),"Set QA_CARD_ACCOUNT_ID to user1-owned AZN account");
        // An explicit account enables the workflow; no credentials/tokens are hardcoded.
        String token=user1Token();

        // GRAD-201 AC-1/2: 3 initially ACTIVE cards, all with a 3-year expiry and default limits.
        VirtualCardResponse a=create(token,accountId,"100.00","QA-main-A");
        VirtualCardResponse b=create(token,accountId,"20.00","QA-state-B");
        VirtualCardResponse c=create(token,accountId,"5.00","QA-state-C");

        // GRAD-201 AC-7: cannot exceed 3 ACTIVE/FROZEN cards.
        Response fourthAttempt=ApiHelper.post(token,"/cards",
                new VirtualCardRequest(accountId,new BigDecimal("5.00"),"QA-fourth-attempt"));
        ApiAssertHelper.error(fourthAttempt,422,"CARD_LIMIT_REACHED");

        // GRAD-202 AC-1: immediate GET matches the POST card details.
        VirtualCardResponse details=card(token,a.id);
        assertCardEqual(a,details);

        // GRAD-202 AC-3: other user must not discover a card belonging to user1.
        if(AuthHelper.configured(2)) {
            ApiAssertHelper.error(ApiHelper.authorized(user2Token()).get("/cards/"+a.id),404,"NOT_FOUND");
        }

        // GRAD-203 AC-1/3: ACTIVE -> FROZEN -> BLOCKED (escalation to STOLEN).
        CardStateResponse bFrozen=block(token,b.id,"TEMPORARY","FROZEN");
        assertEquals("TEMPORARY",bFrozen.blockReason);
        assertNotNull(bFrozen.blockedAt);
        assertEquals("FROZEN",card(token,b.id).status);
        CardStateResponse bBlocked=block(token,b.id,"STOLEN","BLOCKED");
        assertEquals("STOLEN",bBlocked.blockReason);
        ApiAssertHelper.error(ApiHelper.authorized(token).post("/cards/"+b.id+"/unblock"),
                409,"PERMANENTLY_BLOCKED");

        // GRAD-203 AC-2: ACTIVE -> BLOCKED directly (LOST).
        CardStateResponse cBlocked=block(token,c.id,"LOST","BLOCKED");
        assertEquals("LOST",cBlocked.blockReason);
        ApiAssertHelper.error(ApiHelper.post(token,"/cards/"+c.id+"/block",Map.of("reason","STOLEN")),
                409,"CARD_BLOCKED");

        // GRAD-201 AC-8: BLOCKED cards are excluded from limit.
        VirtualCardResponse d=create(token,accountId,"5.00","QA-after-block-D");
        assertEquals("ACTIVE",d.status);
        // GRAD-203 AC-2: ACTIVE -> BLOCKED directly with STOLEN.
        block(token,d.id,"STOLEN","BLOCKED");

        // GRAD-203 AC-1/4/7: ACTIVE -> FROZEN; repeat freeze fails; payment forbidden.
        CardStateResponse aFrozen=block(token,a.id,"TEMPORARY","FROZEN");
        assertEquals("TEMPORARY",aFrozen.blockReason);
        ApiAssertHelper.error(ApiHelper.post(token,"/cards/"+a.id+"/block",
                Map.of("reason","TEMPORARY")),409,"ALREADY_FROZEN");
        VirtualCardResponse frozenDetail=card(token,a.id);
        assertEquals("FROZEN",frozenDetail.status);
        assertEquals("TEMPORARY",frozenDetail.blockReason);
        BillPaymentRequest payment=new BillPaymentRequest(a.id,"citynet","CN123456",new BigDecimal("50.00"));
        ApiAssertHelper.error(ApiHelper.paymentPost(token,payment,ApiHelper.freshIdempotencyKey()),
                422,"CARD_NOT_ACTIVE");

        // GRAD-204 AC-1/2: unfreeze resets block metadata, second unfreeze rejected.
        Response unfrozen=ApiHelper.authorized(token).post("/cards/"+a.id+"/unblock");
        ApiAssertHelper.status(unfrozen,200);
        ApiAssertHelper.schema(unfrozen,"card-state.schema.json");
        CardStateResponse active=unfrozen.as(CardStateResponse.class);
        assertEquals("ACTIVE",active.status);
        assertNull(active.blockReason);
        assertNull(active.blockedAt);
        ApiAssertHelper.error(ApiHelper.authorized(token).post("/cards/"+a.id+"/unblock"),
                409,"NOT_FROZEN");

        // GRAD-204 AC-4 / GRAD-202 AC-2: newly activated card pays CityNet 50 + fee .50.
        String key=ApiHelper.freshIdempotencyKey();
        Response paid=ApiHelper.paymentPost(token,payment,key);
        ApiAssertHelper.status(paid,201);
        assertEquals("SUCCESS",paid.jsonPath().getString("status"));
        assertMoney("50.50",paid.jsonPath().getString("total"));
        assertEquals("/bill-payments/"+paid.jsonPath().getString("id"),paid.getHeader("Location"));
        VirtualCardResponse after=card(token,a.id);
        assertEquals(0,a.availableBalance.subtract(new BigDecimal("50.50")).compareTo(after.availableBalance));
        assertEquals(0,a.spentToday.add(new BigDecimal("50.50")).compareTo(after.spentToday));

        // GRAD-302 AC-4 / GRAD-202: same key/body replays without a second debit.
        Response replay=ApiHelper.paymentPost(token,payment,key);
        ApiAssertHelper.status(replay,200);
        assertEquals(paid.jsonPath().getString("id"),replay.jsonPath().getString("id"));
        assertEquals("true",replay.getHeader("Idempotent-Replay"));
        VirtualCardResponse afterReplay=card(token,a.id);
        assertEquals(0,after.availableBalance.compareTo(afterReplay.availableBalance));
        assertEquals(0,after.spentToday.compareTo(afterReplay.spentToday));

        // GRAD-203/204 AC-2/3: LOST is permanent, cannot re-block/unblock or pay.
        block(token,a.id,"LOST","BLOCKED");
        ApiAssertHelper.error(ApiHelper.authorized(token).post("/cards/"+a.id+"/unblock"),
                409,"PERMANENTLY_BLOCKED");
        ApiAssertHelper.error(ApiHelper.post(token,"/cards/"+a.id+"/block",
                Map.of("reason","TEMPORARY")),409,"CARD_BLOCKED");
        ApiAssertHelper.error(ApiHelper.paymentPost(token,payment,ApiHelper.freshIdempotencyKey()),
                422,"CARD_NOT_ACTIVE");
    }

    private static VirtualCardResponse create(String token,String account,String initial,String label) {
        Response r=ApiHelper.post(token,"/cards",
                new VirtualCardRequest(account,new BigDecimal(initial),label+"-"+UUID.randomUUID().toString().substring(0,6)));
        ApiAssertHelper.status(r,201);
        ApiAssertHelper.schema(r,"virtual-card.schema.json");
        VirtualCardResponse card=r.as(VirtualCardResponse.class);
        assertNotNull(card.id);
        assertEquals("/cards/"+card.id,r.getHeader("Location"));
        assertEquals(account,card.accountId);
        assertEquals("AZN",card.currency,"QA_CARD_ACCOUNT_ID must refer to an AZN account");
        assertEquals("ACTIVE",card.status);
        assertMoney(initial,card.availableBalance);
        assertMoney("0",card.spentToday);
        assertMoney("500",card.limits.dailyOnline);
        assertMoney("300",card.limits.perTransaction);
        assertNotNull(card.maskedPan);
        assertTrue(card.maskedPan.matches("4169 73\\*\\* \\*{4} \\d{4}"));
        String expiry=LocalDate.now(ZoneId.of("Asia/Baku")).plusYears(3)
                .format(DateTimeFormatter.ofPattern("MM/yy"));
        assertEquals(expiry,card.expiry);
        return card;
    }

    private static VirtualCardResponse card(String token,String id) {
        Response r=ApiHelper.authorized(token).get("/cards/"+id);
        ApiAssertHelper.status(r,200);
        ApiAssertHelper.schema(r,"virtual-card.schema.json");
        return r.as(VirtualCardResponse.class);
    }

    private static CardStateResponse block(String token,String id,String reason,String status) {
        Response r=ApiHelper.post(token,"/cards/"+id+"/block",Map.of("reason",reason));
        ApiAssertHelper.status(r,200);
        ApiAssertHelper.schema(r,"card-state.schema.json");
        CardStateResponse result=r.as(CardStateResponse.class);
        assertEquals(id,result.id);
        assertEquals(status,result.status);
        assertEquals(reason,result.blockReason);
        assertNotNull(result.blockedAt);
        return result;
    }

    private static void assertCardEqual(VirtualCardResponse expected,VirtualCardResponse actual) {
        assertEquals(expected.id,actual.id);
        assertEquals(expected.accountId,actual.accountId);
        assertEquals(expected.status,actual.status);
        assertEquals(expected.maskedPan,actual.maskedPan);
        assertEquals(expected.expiry,actual.expiry);
        assertMoney(expected.availableBalance.toPlainString(),actual.availableBalance);
        assertMoney(expected.spentToday.toPlainString(),actual.spentToday);
    }

    private static void assertMoney(String expected,BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0,new BigDecimal(expected).compareTo(actual));
    }
    private static void assertMoney(String expected,Object actual) {
        assertNotNull(actual);
        assertEquals(0,new BigDecimal(expected).compareTo(new BigDecimal(String.valueOf(actual))));
    }
}
