package tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Offline regressions for CallMan JS property semantics migrated to Java. */
class WorkflowAssertionTranslationTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void step8ItemsLengthIsArraySizeNotAJsonObjectProperty() throws Exception {
        JsonNode response = JSON.readTree("{\"total\":3,\"items\":[{\"id\":\"a\"},{\"id\":\"b\"},{\"id\":\"c\"}]}");
        assertEquals(3, FullCallManWorkflowIT.jsonPath(response, "items.length").asInt());
        assertTrue(FullCallManWorkflowIT.jsonPath(response, "items.length").isNumber());
        assertEquals(3, FullCallManWorkflowIT.jsonPath(response, "total").asInt());
    }

    @Test void otherSourceLengthCasesWorkForFilteredAndPagedResponses() throws Exception {
        for (int size : new int[]{0, 2, 6, 8, 9, 10}) {
            com.fasterxml.jackson.databind.node.ArrayNode items = JSON.createArrayNode();
            for (int i = 0; i < size; i++) items.add(i);
            com.fasterxml.jackson.databind.node.ObjectNode response = JSON.createObjectNode();
            response.set("items", items);
            assertEquals(size, FullCallManWorkflowIT.jsonPath(response, "items.length").asInt());
        }
        JsonNode text = JSON.readTree("{\"name\":\"CallMan\"}");
        assertEquals(7, FullCallManWorkflowIT.jsonPath(text, "name.length").asInt());
    }

    @Test void nestedFieldsAndMissingFieldsPreserveTheirOriginalSemantics() throws Exception {
        JsonNode response = JSON.readTree("{\"limits\":{\"dailyOnline\":500},\"items\":[]}");
        assertEquals(500, FullCallManWorkflowIT.jsonPath(response, "limits.dailyOnline").asInt());
        assertTrue(FullCallManWorkflowIT.jsonPath(response, "limits.notThere").isMissingNode());
        assertEquals(0, FullCallManWorkflowIT.jsonPath(response, "items.length").asInt());
    }
}
