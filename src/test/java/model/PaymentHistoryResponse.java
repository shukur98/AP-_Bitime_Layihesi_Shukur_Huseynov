package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/** Maps HTTP GET /bill-payments, including pagination metadata. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentHistoryResponse {
    public int page;
    public int limit;
    public int total;
    public int totalPages;
    public List<PaymentItem> items = new ArrayList<>();
    public PaymentHistoryResponse() {}
}
