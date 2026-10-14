package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import model.PaymentHistoryResponse;
import model.PaymentItem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import utils.Config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** GRAD-303: date filtering, sorting, pagination and BVA. */
public class PaymentHistoryTest extends BaseApiTest {
    private Response get(Map<String, ?> params) {
        return ApiHelper.get(user1Token(), "/bill-payments", params);
    }

    @Test @DisplayName("G303 AC-1: default page=1, limit=10, newest first")
    void defaultPaginationAndSort() {
        Response r=get(Map.of());
        ApiAssertHelper.status(r, 200);
        ApiAssertHelper.schema(r, "payment-history.schema.json");
        PaymentHistoryResponse history=r.as(PaymentHistoryResponse.class);
        assertEquals(1, history.page);
        assertEquals(10, history.limit);
        assertEquals((history.total + 9) / 10, history.totalPages);
        assertTrue(history.items.size() <= 10);
        assertSortedDescending(history.items);
    }

    @ParameterizedTest(name="G303 AC-4 valid limit={0}")
    @ValueSource(ints={1,50})
    void validLimitBoundary(int limit) {
        Response r=get(Map.of("limit",limit));
        ApiAssertHelper.status(r,200);
        PaymentHistoryResponse h=r.as(PaymentHistoryResponse.class);
        assertEquals(limit,h.limit);
        assertEquals((h.total+limit-1)/limit,h.totalPages);
        assertTrue(h.items.size()<=limit);
    }

    @ParameterizedTest(name="G303 AC-4 invalid {0}={1}")
    @CsvSource({"limit,0", "limit,51", "page,0", "page,-1", "page,abc"})
    void invalidPagination(String field,String value) {
        Response r=get(Map.of(field,value));
        ApiAssertHelper.validation(r,field);
    }

    @Test @DisplayName("G303 AC-5: invalid month -> 400")
    void invalidMonth() {
        ApiAssertHelper.validation(get(Map.of("from","2026-13-01")),"from");
    }

    @Test @DisplayName("G303 AC-6: from later than to -> 400")
    void invertedDates() {
        ApiAssertHelper.validation(get(Map.of("from","2026-10-12","to","2026-10-11")),"from");
    }

    @Test @DisplayName("G303 AC-3: out of range page -> 200 []")
    void nonexistentPage() {
        Response r=get(Map.of("page",999999));
        ApiAssertHelper.status(r,200);
        ApiAssertHelper.schema(r,"payment-history.schema.json");
        assertTrue(r.as(PaymentHistoryResponse.class).items.isEmpty());
    }

    @Test @DisplayName("G303 AC-8: billerId filter contains only matching records")
    void billerFilter() {
        Response r=get(Map.of("billerId","citynet","limit",50));
        ApiAssertHelper.status(r,200);
        PaymentHistoryResponse h=r.as(PaymentHistoryResponse.class);
        assertTrue(h.items.stream().allMatch(x->"citynet".equals(x.billerId)));
        assertEquals((h.total+49)/50,h.totalPages);
        // total is the size AFTER filtering, not the current page's item count.
        int count=0;
        for(int page=1;page<=h.totalPages;page++) {
            Response pageR=get(Map.of("billerId","citynet","limit",50,"page",page));
            ApiAssertHelper.status(pageR,200);
            count+=pageR.as(PaymentHistoryResponse.class).items.size();
        }
        assertEquals(h.total,count,"Filtered total must equal the complete filtered dataset count");
    }

    @Test @DisplayName("G303 AC-7: today is interpreted in Asia/Baku")
    void inclusiveToday() {
        String today=LocalDate.now(ZoneId.of("Asia/Baku")).toString();
        Response r=get(Map.of("from",today,"to",today,"limit",50));
        ApiAssertHelper.status(r,200);
        PaymentHistoryResponse h=r.as(PaymentHistoryResponse.class);
        assertTrue(h.items.stream().allMatch(p->
                Instant.parse(p.createdAt).atZone(ZoneId.of("Asia/Baku")).toLocalDate().toString().equals(today)));
    }

    @Test @DisplayName("G303 AC-2: seeded 18 payments, page 2 returns 8")
    void exactEighteenFixture() {
        Assumptions.assumeTrue(Config.enabled("qa.fixture18"),"Requires a known 18-payment fixture");
        Response r=get(Map.of("page",2,"limit",10));
        ApiAssertHelper.status(r,200);
        PaymentHistoryResponse h=r.as(PaymentHistoryResponse.class);
        assertEquals(18,h.total,"Fixture must have exactly 18 payments");
        assertEquals(2,h.totalPages);
        assertEquals(8,h.items.size());
    }

    private static void assertSortedDescending(List<PaymentItem> items) {
        for(int i=1;i<items.size();i++) {
            Instant before=Instant.parse(items.get(i-1).createdAt);
            Instant after=Instant.parse(items.get(i).createdAt);
            assertFalse(before.isBefore(after),"Newest payment should be first, index="+i);
        }
    }
}
