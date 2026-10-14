package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/** GRAD-102 list/search/filter response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeneficiaryListResponse {
    public int total;
    public List<BeneficiaryResponse> items = new ArrayList<>();
    public BeneficiaryListResponse() {}
}
