package imp;

import com.thoughtworks.gauge.Step;
import helper.ApiAssertHelper;
import helper.ApiHelper;
import helper.AuthHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.Collections;

/** Thin Gauge @Step adapter: mirrors teacher's imp -> helper pattern. */
public class ApiRequestImp {
    private static final ThreadLocal<Response> LAST = new ThreadLocal<>();

    @Step("Tokenli GET <path> gonder")
    public void authorizedGet(String path) {
        LAST.set(ApiHelper.get(AuthHelper.token(1), path, Collections.emptyMap()));
    }

    @Step("Tokenli GET <path> query <param> = <value> gonder")
    public void authorizedGetWithQuery(String path,String param,String value) {
        LAST.set(ApiHelper.get(AuthHelper.token(1),path,Collections.singletonMap(param,value)));
    }

    @Step("HTTP status <code> olmalidir")
    public void checkStatus(int code) {
        ApiAssertHelper.status(require(),code);
    }

    @Step("Error code <code> olmalidir")
    public void checkErrorCode(String code) {
        Assertions.assertEquals(code,require().jsonPath().getString("code"));
    }

    @Step("Response <schemaName> schema-ya uygun olmalidir")
    public void checkSchema(String schemaName) {
        ApiAssertHelper.schema(require(),schemaName);
    }

    @Step("Validation field <field> olmalidir")
    public void checkValidationField(String field) {
        ApiAssertHelper.validation(require(),field);
    }

    private Response require() {
        Response r=LAST.get();
        if(r==null) throw new AssertionError("No request has been sent in this scenario");
        return r;
    }

    public static void clear() { LAST.remove(); }
}
