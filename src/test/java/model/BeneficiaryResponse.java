package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** POST /beneficiaries response DTO (minimal stable fields). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeneficiaryResponse {
    public String id;
    public String fullName;
    public String iban;
    public String ibanMasked;
    public String currency;
    public String nickname;
    public String createdAt;
    public BeneficiaryResponse() {}
}
