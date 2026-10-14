package model;

import java.math.BigDecimal;

/** GRAD-201 request; label may be omitted (null) by the client. */
public class VirtualCardRequest {
    public String accountId;
    public BigDecimal initialAmount;
    public String label;
    public VirtualCardRequest() {}
    public VirtualCardRequest(String accountId, BigDecimal initialAmount, String label) {
        this.accountId=accountId;
        this.initialAmount=initialAmount;
        this.label=label;
    }
}
