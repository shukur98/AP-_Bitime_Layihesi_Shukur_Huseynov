package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Read-only DTO for one GRAD-303 history item. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentItem {
    public String id;
    public String receiptNo;
    public String billerId;
    public String subscriberNo;
    public BigDecimal amount;
    public BigDecimal fee;
    public BigDecimal total;
    public String status;
    public String createdAt;

    public PaymentItem() {}
}
