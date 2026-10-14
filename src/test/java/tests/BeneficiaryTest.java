package tests;

import helper.ApiAssertHelper;
import helper.ApiHelper;
import io.restassured.response.Response;
import model.BeneficiaryRequest;
import model.BeneficiaryResponse;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import utils.Config;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BeneficiaryTest extends BaseApiTest {
    private String iban() {
        String value=Config.get("QA_TEST_IBAN");
        if (value.isEmpty() && helper.TestFixtures.enabled()) return helper.TestFixtures.unusedIban();
        Assumptions.assumeFalse(value.isEmpty(),"Set QA_TEST_IBAN to a valid, unused sandbox IBAN");
        return value;
    }

    @ParameterizedTest(name="G101 invalid fullName: {0}")
    @CsvSource({"A", "Ali12"})
    void rejectInvalidNames(String badName) {
        Response r=ApiHelper.post(user1Token(),"/beneficiaries",
                new BeneficiaryRequest(badName,iban(),"AZN",null));
        ApiAssertHelper.validation(r,"fullName");
    }

    @Test
    void invalidIbanRejected() {
        Response r=ApiHelper.post(user1Token(),"/beneficiaries",
                new BeneficiaryRequest("Valid Name","AZ123","AZN",null));
        ApiAssertHelper.validation(r,"iban");
    }

    @Test
    void createThenDeleteThenDeleteAgainG101G103() {
        Assumptions.assumeTrue(Config.enabled("qa.mutationTests"),
                "State-changing tests require -Dqa.mutationTests=true and a fresh sandbox IBAN");
        String nickname="QA"+UUID.randomUUID().toString().substring(0,8);
        Response created=ApiHelper.post(user1Token(),"/beneficiaries",
                new BeneficiaryRequest("Automation Test",iban(),"AZN",nickname));
        ApiAssertHelper.status(created,201);
        BeneficiaryResponse dto=created.as(BeneficiaryResponse.class);
        assertNotNull(dto.id);
        assertNotNull(dto.ibanMasked);
        assertTrue(dto.ibanMasked.contains("*"),"IBAN must be masked in response");
        try {
            // G103: cross-user access must be hidden with 404.
            if (helper.AuthHelper.configured(2)) {
                Response foreignDelete=ApiHelper.authorized(user2Token())
                        .delete("/beneficiaries/"+dto.id);
                ApiAssertHelper.status(foreignDelete,404);
            }
        } finally {
            Response deleted=ApiHelper.authorized(user1Token())
                    .delete("/beneficiaries/"+dto.id);
            ApiAssertHelper.status(deleted,204);
            assertTrue(deleted.asString().isEmpty(),"204 body must be empty");
        }
        Response again=ApiHelper.authorized(user1Token())
                .delete("/beneficiaries/"+dto.id);
        ApiAssertHelper.status(again,404);
    }
}
