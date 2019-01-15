package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDTO {
    public String senderRS;
    public String assetTransfer;
    public String sender;
    public String recipientRS;
    public String recipient;
    public String quantityATU;
    public String asset;
    public Long height;
    public Long timestamp;
}
