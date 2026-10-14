package tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import helper.ApiHelper;
import helper.AuthHelper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.Config;
import utils.AzerbaijaniCollation;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Full 117-request CallMan scenario replay, translated into RestAssured/JUnit dynamic tests.
 * One disposable, freshly logged-in sandbox, a fixed order, and shared resource IDs are REQUIRED.
 * Invoke ONLY via scripts/run-117-restassured.sh --new-sandbox, never with ordinary mvn test.
 *
 * Assertions: exact HTTP statuses, business error codes, generated ID semantics,
 * response shape/status/location, and 143 machine-translated source assertions,
 * with additional handwritten invariants for key stateful flows.
 * Postman-specific JavaScript expressions are not executed; see docs/CALLMAN_117_MAPPING.csv.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class FullCallManWorkflowIT {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern VAR = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9]*)}}");
    private final Map<String,String> vars = new HashMap<>();
    private final Map<String,Integer> paymentsByDate = new HashMap<>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private int successfulPayments;

    @TestFactory
    Stream<DynamicTest> executeCallManCollectionInOrder() throws Exception {
        assumeTrue("https://api.anarabbas.com".equals(Config.apiUrl()),
                "117-case state-changing suite is restricted to the documented teacher lab");
        assumeTrue(Config.enabled("qa.fullWorkflow") && Config.enabled("QA_FULL_FRESH"),
                "Use scripts/run-117-restassured.sh --new-sandbox (isolated destructive workflow)");
        assumeTrue(Config.get("QA_TOKEN_USER1").isBlank() && Config.get("QA_TOKEN_USER2").isBlank(),
                "Explicit tokens could point to populated sandboxes. Unset QA_TOKEN_USER1/2.");
        initVariables();
        JsonNode rows;
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("workflow/callman-117.json")) {
            assertNotNull(stream, "Missing 117-row source manifest");
            rows = JSON.readTree(stream);
        }
        assertEquals(117, rows.size(), "Source CallMan collection row count changed");
        List<DynamicTest> cases = new ArrayList<>();
        for (JsonNode row : rows) {
            int position = row.path("number").asInt();
            cases.add(DynamicTest.dynamicTest(String.format("%03d / %s", position, row.path("name").asText()), () -> {
                assumeTrue(!stopped.get(), "Previous dependent workflow step failed; do not contaminate sandbox");
                try { runRow(row); }
                catch (AssertionError | RuntimeException ex) { stopped.set(true); throw ex; }
            }));
        }
        return cases.stream();
    }

    @BeforeAll
    static void enforceIsolatedWorkflow() {
        assertEquals("https://api.anarabbas.com", Config.apiUrl(), "Do not run the destructive workflow on other hosts");
        assertTrue(Config.enabled("qa.fullWorkflow") && Config.enabled("QA_FULL_FRESH"),
                "Use the opt-in scripts/run-117-restassured.sh --new-sandbox wrapper");
        assertTrue(Config.get("QA_TOKEN_USER1").isBlank() && Config.get("QA_TOKEN_USER2").isBlank(),
                "Cannot prove sandbox isolation if manually supplied tokens override login");
        assertFalse(Config.enabled("QA_SESSION_PERSIST"), "Full workflow should not reuse persistent sandbox tokens");
    }

    private void initVariables() {
        vars.put("baseUrl", Config.apiUrl());
        vars.put("notExistCard", "vcd_zzzzzzzz");
        vars.put("todayBaku", LocalDate.now(ZoneId.of("Asia/Baku")).toString());
        for (int i=1; i<=12; i++) vars.put("benIban"+i, deterministicAzIban(i));
        String formatted = vars.get("benIban2");
        vars.put("benIban2Formatted", (formatted.substring(0,4)+" "+formatted.substring(4,8)+" "+formatted.substring(8)).toLowerCase(java.util.Locale.ROOT));
        for (int i=1; i<=32; i++) vars.put("payKey"+i, UUID.randomUUID().toString());
    }

    /** Identical deterministic 900000+i account number algorithm from CallMan AUTH-01. */
    private static String deterministicAzIban(int index) {
        String account = String.format("%020d", 900000 + index);
        String moved = "AIIB"+account+"AZ00";
        int remainder = 0;
        for (char ch : moved.toCharArray()) {
            String digits = ch>='A' && ch<='Z' ? String.valueOf(ch-'A'+10) : String.valueOf(ch);
            for (char d : digits.toCharArray()) remainder=(remainder*10+d-'0')%97;
        }
        return "AZ" + String.format("%02d", 98-remainder) + "AIIB" + account;
    }

    private String interpolate(String template) {
        Matcher matcher = VAR.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key=matcher.group(1);
            String value=vars.get(key);
            if (value==null || value.isBlank()) throw new AssertionError("Missing workflow variable {{"+key+"}}; check producer step");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void runRow(JsonNode row) throws Exception {
        int number = row.path("number").asInt();
        // AUTH-01/02 are executed by the exact same AuthHelper as standard JUnit/Gauge.
        // This guarantees one login per user and preserves the newly created sandbox.
        if (number == 1 || number == 2) {
            int user = number;
            String token = AuthHelper.token(user);
            assertNotNull(token, "Login did not provide token for user"+user);
            assertTrue(token.length()>10, "Login returned suspiciously short token");
            vars.put("tokenUser"+user, token);
            return;
        }
        String path = interpolate(row.path("url").asText()).replace(Config.apiUrl(), "");
        RequestSpecification request = io.restassured.RestAssured.given().spec(ApiHelper.baseSpec());
        for (JsonNode header : row.path("headers")) {
            String name=header.path("key").asText();
            String value=interpolate(header.path("value").asText());
            request.header(name,value);
        }
        if (!row.path("body").isNull() && !row.path("body").asText().isEmpty()) {
            request.contentType(ContentType.JSON).body(interpolate(row.path("body").asText()));
        }
        Response r;
        switch (row.path("method").asText()) {
            case "GET": r=request.get(path); break;
            case "POST": r=request.post(path); break;
            case "DELETE": r=request.delete(path); break;
            default: throw new AssertionError("Unsupported source HTTP method: "+row.path("method").asText());
        }
        int expected = row.path("expectedStatus").asInt();
        assertEquals(expected, r.statusCode(), "CallMan #"+number+" HTTP status: "+row.path("name").asText()
                + "; response="+summarize(r.asString()));
        String errorCode=row.path("errorCode").asText("");
        if (!errorCode.isEmpty()) assertEquals(errorCode, r.jsonPath().getString("code"),
                "CallMan #"+number+" business error code");
        if (expected == 204) assertTrue(r.asString().isEmpty(), "204 response must have no body");
        if (expected >= 200 && expected < 300 && expected != 204) {
            assertNotNull(r.getHeader("Content-Type"), "Success response should have Content-Type");
        }
        JsonNode payload = null;
        if (expected != 204 && !r.asString().isBlank()) payload = JSON.readTree(r.asString());
        if (payload != null) {
            structuralChecks(row.path("assertions"), payload, r, number);
            businessInvariants(number, row, payload, r);
        }
        saveOutput(number, payload, expected);
    }

    private void structuralChecks(JsonNode checks, JsonNode body, Response response, int number) {
        for (JsonNode check : checks) {
            String kind=check.path("kind").asText();
            String path=check.path("path").asText();
            JsonNode actual = jsonPath(body, path);
            String hint="#"+number+"/"+kind+"/"+path;
            switch (kind) {
                case "equals":
                    JsonNode expected=check.path("value");
                    assertTrue(sameValue(actual,expected), hint+" expected "+expected+" got "+actual);
                    break;
                case "exists": assertFalse(actual.isMissingNode() || actual.isNull(),hint); break;
                case "length": assertTrue(actual.isArray() || actual.isTextual(),hint+" not array/string");
                    assertEquals(check.path("value").asInt(), actual.isArray()?actual.size():actual.asText().length(),hint);break;
                case "variable": assertEquals(vars.get(check.path("variable").asText()), actual.asText(),hint);break;
                case "validationField":
                    String field=check.path("field").asText();
                    JsonNode details=body.path("details");
                    boolean present=false;
                    for (JsonNode item:details) if (field.equals(item.path("field").asText())) present=true;
                    assertTrue(present,hint+" error details should mention "+field);break;
                case "header": assertEquals(check.path("value").asText(),response.getHeader(check.path("header").asText()),hint);break;
                case "headerVar": assertEquals(vars.get(check.path("variable").asText()),response.getHeader(check.path("header").asText()),hint);break;
                default: throw new AssertionError("Unknown migrated check type: "+kind);
            }
        }
    }

    /**
     * Minimal source-CallMan expression evaluator for dot-separated fields and JS .length.
     * A collection assertion such as items.length must NOT be interpreted as
     * a JSON field named length.
     * Keep this method in sync with WorkflowManifestTest's supported grammar.
     */
    static JsonNode jsonPath(JsonNode node,String path) {
        if (path.isBlank()) return node;
        for (String segment : path.split("\\.")) {
            if ("length".equals(segment) && (node.isArray() || node.isTextual())) {
                node = IntNode.valueOf(node.isArray() ? node.size() : node.asText().length());
            } else {
                node = node.path(segment);
            }
        }
        return node;
    }
    private static boolean sameValue(JsonNode actual,JsonNode expected) {
        if (actual.isNumber() && expected.isNumber()) return actual.decimalValue().compareTo(expected.decimalValue())==0;
        return actual.equals(expected);
    }
    private static String summarize(String body) {
        if (body==null) return "<null>";
        return body.length()>240 ? body.substring(0,240)+"..." : body;
    }

    private void businessInvariants(int n, JsonNode spec, JsonNode body, Response response) {
        String url=spec.path("url").asText();
        int status=response.statusCode();
        if (status==201 && url.contains("/beneficiaries")) {
            assertTrue(body.path("id").asText().matches("ben_.{8}"),"New beneficiary id");
            assertEquals("AZN",body.path("currency").asText());
            assertFalse(body.path("iban").asText().contains(" "),"Normalized IBAN must not contain spaces");
            assertTrue(body.path("ibanMasked").isTextual(),"Masked IBAN missing");
            assertEquals("/beneficiaries/"+body.path("id").asText(),response.getHeader("Location"));
        }
        if (status==201 && url.endsWith("/cards")) {
            assertTrue(body.path("id").asText().startsWith("vcd_"));
            assertEquals("ACTIVE",body.path("status").asText());
            assertEquals("500",body.path("limits").path("dailyOnline").decimalValue().stripTrailingZeros().toPlainString());
            assertEquals("300",body.path("limits").path("perTransaction").decimalValue().stripTrailingZeros().toPlainString());
            assertEquals("/cards/"+body.path("id").asText(),response.getHeader("Location"));
        }
        if (status==201 && url.endsWith("/bill-payments")) {
            assertEquals("SUCCESS",body.path("status").asText());
            assertTrue(body.path("receiptNo").asText().startsWith("RCP-"));
            assertEquals("/bill-payments/"+body.path("id").asText(),response.getHeader("Location"));
            successfulPayments++;
            String date = Instant.parse(body.path("createdAt").asText()).atZone(ZoneId.of("Asia/Baku"))
                    .toLocalDate().toString();
            paymentsByDate.merge(date,1,Integer::sum);
        }
        if (n==6) assertEquals(vars.get("benIban2"),body.path("iban").asText(),"IBAN normalization");
        if (n==8) assertTrue(hasValue(body.path("items"), "id", vars.get("benId1")),
                "G102 list must include the newly created beneficiary #1");
        if (n==9) assertTrue(hasValue(body.path("items"), "fullName", "Nigar Əliyeva"),
                "G102 case-insensitive name search must return Nigar Əliyeva");
        if (n==10) assertTrue(hasValue(body.path("items"), "nickname", "Anam"),
                "G102 nickname search must find Anam");
        if (n==11) {
            for(JsonNode item:body.path("items")) assertEquals("AZN",item.path("currency").asText(),
                    "G102 currency filter must exclude non-AZN beneficiaries");
            assertEquals(body.path("total").asInt(),body.path("items").size());
        }
        if (n==28) {
            assertEquals(10,body.path("items").size(),"Azerbaijani sort dataset");
            List<String> names = new ArrayList<>();
            for(JsonNode item:body.path("items")) {
                assertTrue(item.path("fullName").isTextual(),
                        "G102 sorted result requires a textual fullName");
                names.add(item.path("fullName").asText());
            }
            // Source CallMan: names.slice().sort((a,b) => a.localeCompare(b, "az"))
            // Do not use JDK Collator(az-AZ) here: some JDK locale data places Ə after Z.
            List<String> sorted = new ArrayList<>(names);
            sorted.sort(AzerbaijaniCollation.BY_ALPHABET);
            assertEquals(sorted, names, "G102 response must follow Azerbaijani primary alphabet order");
        }
        if (n==29 || n==104) assertEquals(0,body.path("items").size(),
                "CallMan empty result must have empty items array");
        if (n==31) assertFalse(hasValue(body.path("items"),"id",vars.get("benId1")),
                "Deleted beneficiary must not appear in G103 list");
        if (n==34) assertTrue(hasValue(body.path("items"),"id",vars.get("benId2")),
                "Deleting another beneficiary must not delete beneficiary #2");
        if (n>=59 && n<=62) {
            String[][] known = {
                {"azerisiq","azersu","azercell","bakcell","citynet","aztelekom"},
                {"azercell","bakcell"}, {"aztelekom","citynet"}, {"azerisiq","azersu"}
            };
            java.util.Set<String> expectedIds = new java.util.HashSet<>(java.util.Arrays.asList(known[n-59]));
            java.util.Set<String> actualIds = new java.util.HashSet<>();
            for(JsonNode item:body.path("items")) {
                assertTrue(actualIds.add(item.path("id").asText()),"Duplicate biller in catalog");
                if(n==59) {
                    for(String field:new String[]{"subscriberPattern","minAmount","maxAmount","fee"})
                        assertFalse(item.path(field).isMissingNode() || item.path(field).isNull(),
                                "Biller missing field: "+field);
                    assertFalse(item.path("subscriberPattern").asText().isBlank(),
                            "Biller must provide a nonempty subscriberPattern");
                }
            }
            assertEquals(expectedIds,actualIds,"G301 catalog/filter provider IDs");
        }
        if (n>=42 && n<=44) {
            assertTrue(body.path("maskedPan").asText().matches("^4169 73\\*\\* \\*{4} \\d{4}$"),
                    "G201 card PAN must be masked");
            String expiry = java.time.LocalDate.now(ZoneId.of("Asia/Baku")).plusYears(3)
                    .format(java.time.format.DateTimeFormatter.ofPattern("MM/yy"));
            assertEquals(expiry,body.path("expiry").asText(),"Card expiry must be 3 years ahead (Baku date)");
        }
        if (n==64) {
            // The source CallMan script checks exact first receipt sequence.
            assertTrue(body.path("receiptNo").asText().matches("RCP-\\d{8}-000001"),
                    "First receipt number must match RCP-YYYYMMDD-000001");
        }
        if (n==65) {
            assertEquals(vars.get("firstPaymentId"),body.path("id").asText(),"Idempotency must return original payment");
            assertEquals(vars.get("firstReceiptNo"),body.path("receiptNo").asText());
            assertEquals("true",response.getHeader("Idempotent-Replay"));
        }
        if (n==66) {
            assertMoney("749.50",body.path("availableBalance"));
            assertMoney("50.50",body.path("spentToday"));
        }
        if (n==101) {
            BigDecimal spent=body.path("spentToday").decimalValue();
            BigDecimal balance=body.path("availableBalance").decimalValue();
            assertTrue(spent.compareTo(new BigDecimal("50.5"))>0,"Payments must have debited card A");
            assertEquals(0,balance.add(spent).compareTo(new BigDecimal("800")),"Card balance accounting");
        }
        if (n==102 || n==103) {
            assertEquals(18,body.path("total").asInt(),"18-payment fixture from source CallMan flow");
            assertEquals(2,body.path("totalPages").asInt());
            assertEquals(n==102?10:8,body.path("items").size());
            assertNewestFirst(body.path("items"));
            if(n==102) for(JsonNode item:body.path("items")) {
                for(String field:new String[]{"id","receiptNo","billerId","createdAt"})
                    assertTrue(item.path(field).isTextual() && !item.path(field).asText().isEmpty(),
                            "Payment history missing textual "+field);
                assertTrue(item.path("total").isNumber(),"Payment history total must be numeric");
            }
        }
        if (n==114) assertEquals(paymentsByDate.getOrDefault(vars.get("todayBaku"),0),body.path("total").asInt(),"Baku-day inclusive filter");
        if (n==115) {
            assertEquals(5,body.path("total").asInt());
            for(JsonNode item:body.path("items")) assertEquals("citynet",item.path("billerId").asText());
        }
        if (n==116) {
            assertEquals(2,body.path("total").asInt());
            List<String> ids=new ArrayList<>();
            for(JsonNode item:body.path("items")) ids.add(item.path("id").asText());
            ids.sort(Comparator.naturalOrder());
            List<String> expected=new ArrayList<>();
            expected.add(vars.get("cardBPayment1"));expected.add(vars.get("cardBPayment2"));
            expected.sort(Comparator.naturalOrder());
            assertEquals(expected,ids,"Exactly two Card B payments");
        }
        if (n==117) {
            assertEquals(0,body.path("total").asInt(),"User2 payment history must be isolated");
            assertEquals(0,body.path("items").size());
        }
    }

    private static boolean hasValue(JsonNode items, String field, String value) {
        if (!items.isArray() || value == null) return false;
        for(JsonNode item:items) if(value.equals(item.path(field).asText())) return true;
        return false;
    }

    private static void assertMoney(String amount, JsonNode actual) {
        assertTrue(actual.isNumber(),"Expected numeric money; got "+actual);
        assertEquals(0,new BigDecimal(amount).compareTo(actual.decimalValue()));
    }
    private static void assertNewestFirst(JsonNode items) {
        Instant previous=null;
        for(JsonNode item:items) {
            Instant created=Instant.parse(item.path("createdAt").asText());
            if(previous!=null) assertFalse(previous.isBefore(created),"History order must be newest first");
            previous=created;
        }
    }
    private void saveOutput(int n,JsonNode body,int status) {
        if (body==null || status<200 || status>=300) return;
        switch(n) {
            case 5: vars.put("benId1",body.path("id").asText());break;
            case 6: vars.put("benId2",body.path("id").asText());break;
            case 7: vars.put("benId3",body.path("id").asText());break;
            case 35: vars.put("benId11",body.path("id").asText());break;
            case 42: vars.put("cardA",body.path("id").asText());break;
            case 43: vars.put("cardB",body.path("id").asText());break;
            case 44: vars.put("cardC",body.path("id").asText());break;
            case 52: vars.put("cardD",body.path("id").asText());break;
            case 58: vars.put("cardUSD",body.path("id").asText());break;
            case 64: vars.put("firstPaymentId",body.path("id").asText()); vars.put("firstReceiptNo",body.path("receiptNo").asText());break;
            case 91:vars.put("cardBPayment1",body.path("id").asText());break;
            case 92:vars.put("cardBPayment2",body.path("id").asText());break;
            default: break;
        }
    }
}
