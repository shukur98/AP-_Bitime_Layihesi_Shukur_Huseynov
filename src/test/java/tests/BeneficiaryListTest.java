package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import model.BeneficiaryListResponse;
import model.BeneficiaryResponse;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import utils.AzerbaijaniCollation;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** GRAD-102 — GET /beneficiaries: list, search, filters, locale sort, errors. */
public class BeneficiaryListTest extends BaseApiTest {
    private Response list(Map<String, ?> params) {
        String token = user1Token();
        helper.TestFixtures.ensureBeneficiaries(token);
        return ApiHelper.get(token, "/beneficiaries", params);
    }

    @Test @DisplayName("G102 / default list has consistent total, items and schema")
    void listAndSchema() {
        Response r=list(Map.of());
        ApiAssertHelper.status(r,200);
        ApiAssertHelper.schema(r,"beneficiary-list.schema.json");
        BeneficiaryListResponse dto=r.as(BeneficiaryListResponse.class);
        assertNotNull(dto.items);
        assertEquals(dto.total,dto.items.size(),"GET /beneficiaries returns all saved beneficiaries");
        for(BeneficiaryResponse item:dto.items) {
            assertNotNull(item.id);
            assertNotNull(item.fullName);
            assertNotNull(item.currency);
        }
    }

    @Test @DisplayName("G102 / search by existing name ignores case")
    void searchByNameCaseInsensitive() {
        BeneficiaryResponse sample=sample();
        String firstWord=sample.fullName.strip().split("\\s+")[0];
        Assumptions.assumeTrue(firstWord.length()>=2,"Fixture must have a name with >= 2 characters");
        String query=firstWord.toUpperCase(Locale.forLanguageTag("az-AZ"));
        Response r=list(Map.of("q",query));
        ApiAssertHelper.status(r,200);
        BeneficiaryListResponse dto=r.as(BeneficiaryListResponse.class);
        assertTrue(dto.items.stream().anyMatch(x->sample.id.equals(x.id)),
                "Known beneficiary must be found by an upper-cased name query");
    }

    @Test @DisplayName("G102 / search by nickname")
    void searchByNickname() {
        Response baseline=list(Map.of());
        ApiAssertHelper.status(baseline,200);
        List<BeneficiaryResponse> items=baseline.as(BeneficiaryListResponse.class).items;
        BeneficiaryResponse sample=items.stream().filter(x->x.nickname!=null && x.nickname.length()>=2)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(sample!=null,"At least one nickname fixture required");
        Response r=list(Map.of("q",sample.nickname));
        ApiAssertHelper.status(r,200);
        BeneficiaryListResponse dto=r.as(BeneficiaryListResponse.class);
        assertTrue(dto.items.stream().anyMatch(x->sample.id.equals(x.id)));
    }

    @Test @DisplayName("G102 / allowed currency filtering")
    void filterByCurrency() {
        Response r=list(Map.of("currency","AZN"));
        ApiAssertHelper.status(r,200);
        BeneficiaryListResponse dto=r.as(BeneficiaryListResponse.class);
        assertEquals(dto.items.size(),dto.total);
        assertTrue(dto.items.stream().allMatch(x->"AZN".equals(x.currency)));
    }

    @ParameterizedTest(name="G102 invalid {0}={1} -> 400")
    @CsvSource({"q,a", "currency,GBP"})
    void rejectInvalidQueries(String field,String value) {
        ApiAssertHelper.validation(list(Map.of(field,value)),field);
    }

    @Test @DisplayName("G102 / search with no matches returns 200 + []")
    void emptySearch() {
        Response r=list(Map.of("q","QA_NoBeneficiary_Fixture_Impossible_98765"));
        ApiAssertHelper.status(r,200);
        BeneficiaryListResponse dto=r.as(BeneficiaryListResponse.class);
        assertEquals(0,dto.total);
        assertTrue(dto.items.isEmpty());
    }

    @Test @DisplayName("G102 / fullName asc uses Azerbaijani alphabet")
    void sortByAzerbaijaniAlphabet() {
        Response r=list(Map.of("sort","fullName","order","asc"));
        ApiAssertHelper.status(r,200);
        List<String> names=r.as(BeneficiaryListResponse.class).items.stream()
                .map(x->x.fullName).collect(Collectors.toList());
        Assumptions.assumeTrue(names.size()>=2,"At least 2 seeded beneficiaries required");
        List<String> sorted=new ArrayList<>(names);
        sorted.sort(AzerbaijaniCollation.BY_ALPHABET);
        assertEquals(sorted,names,"Sort order should match Azerbaijani collation");
    }

    private BeneficiaryResponse sample() {
        Response r=list(Map.of());
        ApiAssertHelper.status(r,200);
        List<BeneficiaryResponse> items=r.as(BeneficiaryListResponse.class).items;
        Assumptions.assumeTrue(items!=null && !items.isEmpty(),
                "To verify name search, seed at least one beneficiary in isolated QA sandbox");
        return items.get(0);
    }
}
