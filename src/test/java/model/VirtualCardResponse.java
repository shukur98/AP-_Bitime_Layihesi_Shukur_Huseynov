package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** GRAD-201/202 card detail; enough fields for BVA, state and balances. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualCardResponse {
    public String id;
    public String accountId;
    public String label;
    public String maskedPan;
    public String expiry;
    public String currency;
    public String status;
    public BigDecimal availableBalance;
    public Limits limits;
    public String blockReason;
    public String blockedAt;
    public String createdAt;
    public BigDecimal spentToday;
    public VirtualCardResponse() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Limits {
        public BigDecimal dailyOnline;
        public BigDecimal perTransaction;
        public Limits() {}
    }
}
