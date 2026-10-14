package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal GRAD-203/204 state-transition response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardStateResponse {
    public String id;
    public String status;
    public String blockReason;
    public String blockedAt;
    public CardStateResponse() {}
}
