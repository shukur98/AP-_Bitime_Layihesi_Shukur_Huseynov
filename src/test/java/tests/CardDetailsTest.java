package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import model.VirtualCardResponse;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.Config;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** GRAD-202 — non-mutating read-only card detail/security checks. */
public class CardDetailsTest extends BaseApiTest {
    private String existingCard() {
        String id=helper.TestFixtures.activeCard(user1Token());
        Assumptions.assumeFalse(id.isEmpty(),"QA_ACTIVE_CARD_ID must be user1-owned");
        return id;
    }

    @Test @DisplayName("G202 / GET own card returns 200 and full contract")
    void ownCardDetails() {
        String id=existingCard();
        Response r=ApiHelper.authorized(user1Token()).get("/cards/"+id);
        ApiAssertHelper.status(r,200);
        ApiAssertHelper.schema(r,"virtual-card.schema.json");
        VirtualCardResponse c=r.as(VirtualCardResponse.class);
        assertEquals(id,c.id);
        assertNotNull(c.currency);
        assertNotNull(c.status);
        assertNotNull(c.spentToday);
        assertTrue(c.availableBalance.compareTo(BigDecimal.ZERO)>=0);
        assertEquals(0,c.limits.dailyOnline.compareTo(new BigDecimal("500")));
        assertEquals(0,c.limits.perTransaction.compareTo(new BigDecimal("300")));
        assertTrue(c.maskedPan.matches("4169 73\\*\\* \\*{4} \\d{4}"));
    }

    @Test @DisplayName("G202 AC-3 / other user sees 404 NOT_FOUND")
    void crossUserCardHidden() {
        String id=existingCard();
        Response r=ApiHelper.authorized(user2Token()).get("/cards/"+id);
        ApiAssertHelper.error(r,404,"NOT_FOUND");
    }

    @Test @DisplayName("G202 / unknown id returns 404")
    void unknownCardId() {
        Response r=ApiHelper.authorized(user1Token()).get("/cards/vcd_zzzzzzzz");
        ApiAssertHelper.error(r,404,"NOT_FOUND");
    }
}
