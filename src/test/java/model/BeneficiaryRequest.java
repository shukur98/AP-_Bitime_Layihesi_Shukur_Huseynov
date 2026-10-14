package model;

/** POST /beneficiaries request DTO. */
public class BeneficiaryRequest {
    public String fullName;
    public String iban;
    public String currency;
    public String nickname;

    public BeneficiaryRequest() {}
    public BeneficiaryRequest(String fullName, String iban, String currency, String nickname) {
        this.fullName = fullName;
        this.iban = iban;
        this.currency = currency;
        this.nickname = nickname;
    }
}
