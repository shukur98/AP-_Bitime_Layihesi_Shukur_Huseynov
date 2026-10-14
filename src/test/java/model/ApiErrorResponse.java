package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiErrorResponse {
    public String error;
    public String code;
    public List<Detail> details;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Detail {
        public String field;
        public String message;
        public Detail() {}
    }
    public ApiErrorResponse() {}
}
