package tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Offline contract tests; never connects to the API or mutates teacher sandbox. */
class WorkflowManifestTest {
    private static final Pattern TEMPLATE=Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9]*)}}");

    private JsonNode rows() throws Exception {
        try (InputStream file=getClass().getClassLoader().getResourceAsStream("workflow/callman-117.json")) {
            assertNotNull(file,"CallMan manifest missing");
            return new ObjectMapper().readTree(file);
        }
    }

    @Test void everyCallmanRequestHasMigratedHttpMethodAndStatus() throws Exception {
        JsonNode rows=rows();
        assertEquals(117,rows.size(),"CallMan collection must contain 117 numbered requests");
        for(int i=0;i<rows.size();i++) {
            JsonNode row=rows.get(i);
            assertEquals(i+1,row.path("number").asInt());
            assertFalse(row.path("name").asText().isBlank());
            assertTrue(Set.of("GET","POST","DELETE").contains(row.path("method").asText()));
            assertTrue(row.path("url").asText().startsWith("{{baseUrl}}/"));
            assertTrue(row.path("expectedStatus").asInt()>=200);
            assertTrue(row.path("expectedStatus").asInt()<500);
            assertTrue(row.path("sourcePmTestBlocks").asInt()>=1);
        }
    }

    @Test void allRequestVariablesHaveProducersOrKnownSetupValues() throws Exception {
        Set<String> available=new HashSet<>(Set.of("baseUrl","notExistCard","todayBaku","user1Email",
                "user1Password","user2Email","user2Password","benIban2Formatted"));
        for(int i=1;i<=12;i++) available.add("benIban"+i);
        for(int i=1;i<=32;i++) available.add("payKey"+i);
        JsonNode rows=rows();
        for(JsonNode row:rows) {
            StringBuilder content=new StringBuilder(row.path("url").asText());
            content.append(row.path("body").asText());
            for(JsonNode header:row.path("headers")) content.append(header.path("value").asText());
            Matcher matcher=TEMPLATE.matcher(content);
            while(matcher.find()) assertTrue(available.contains(matcher.group(1)),
                    "CallMan request #"+row.path("number").asInt()+" missing producer for "+matcher.group(1));
            int n=row.path("number").asInt();
            if(n==1)available.add("tokenUser1");
            if(n==2)available.add("tokenUser2");
            if(n==5)available.add("benId1");
            if(n==6)available.add("benId2");
            if(n==7)available.add("benId3");
            if(n==35)available.add("benId11");
            if(n==42)available.add("cardA");
            if(n==43)available.add("cardB");
            if(n==44)available.add("cardC");
            if(n==52)available.add("cardD");
            if(n==58)available.add("cardUSD");
            if(n==64){ available.add("firstPaymentId"); available.add("firstReceiptNo"); }
            if(n==91)available.add("cardBPayment1");
            if(n==92)available.add("cardBPayment2");
        }
    }
}
