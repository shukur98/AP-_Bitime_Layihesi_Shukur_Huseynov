package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** GRAD-301 AC-1..AC-3, including response contract and data consistency. */
public class BillerCatalogTest extends BaseApiTest {
    private Response list(Map<String, ?> params) {
        return ApiHelper.get(user1Token(),"/billers",params);
    }

    @Test @DisplayName("G301 AC-1: six billers with subscriber pattern, min/max and fee")
    void allSixBillers() {
        Response r=list(Map.of());
        ApiAssertHelper.status(r,200);
        ApiAssertHelper.schema(r,"biller-list.schema.json");
        List<String> ids=r.jsonPath().getList("items.id");
        assertEquals(6,r.jsonPath().getInt("total"));
        assertEquals(6,ids.size());
        assertEquals(Set.of("azerisiq","azersu","azercell","bakcell","citynet","aztelekom"),new HashSet<>(ids));
        assertEquals(6,new HashSet<>(ids).size(),"Provider IDs must be unique");
    }

    @Test @DisplayName("G301 AC-2: only Azercell and Bakcell in MOBILE")
    void mobileOnly() {
        Response r=list(Map.of("category","MOBILE"));
        ApiAssertHelper.status(r,200);
        assertEquals(2,r.jsonPath().getInt("total"));
        List<String> ids=r.jsonPath().getList("items.id");
        assertEquals(Set.of("azercell","bakcell"),new HashSet<>(ids));
        List<String> categories=r.jsonPath().getList("items.category");
        assertTrue(categories.stream().allMatch("MOBILE"::equals));
    }

    @Test @DisplayName("G301 AC-3: GAS not a supported category")
    void invalidGasCategory() {
        ApiAssertHelper.validation(list(Map.of("category","GAS")),"category");
    }
}
