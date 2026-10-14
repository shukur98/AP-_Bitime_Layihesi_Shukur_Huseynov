package model;

import java.math.BigDecimal;

/** POST /bill-payments request DTO. */
public class BillPaymentRequest {
    public String cardId;
    public String billerId;
    public String subscriberNo;
    public BigDecimal amount;
    public BillPaymentRequest() {}
    public BillPaymentRequest(String cardId, String billerId, String subscriberNo, BigDecimal amount) {
        this.cardId=cardId; this.billerId=billerId; this.subscriberNo=subscriberNo; this.amount=amount;
    }
}
