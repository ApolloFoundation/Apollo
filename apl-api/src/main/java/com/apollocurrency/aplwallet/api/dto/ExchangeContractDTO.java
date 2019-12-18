package com.apollocurrency.aplwallet.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeContractDTO {
    private String id;
    private String orderId;
    private String counterOrderId;
    private String sender;
    private String recipient;
    private byte contractStatus;
    private String secretHash;
    private String transferTxId;
    private String counterTransferTxId;
    private String encryptedSecret;
    private Integer deadlineToReply;
    private int height;
}
