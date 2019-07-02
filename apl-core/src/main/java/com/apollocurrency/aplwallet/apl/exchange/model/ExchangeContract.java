package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ExchangeContract {

    private Long id;
    private Long orderId;
    private Long counterOrderId;
    /**
     * Hash from secret key. sha256(key)
     */
    private String secretHash;

    public ExchangeContract(DexContractAttachment dexContractAttachment) {
        this.orderId = dexContractAttachment.getOrderId();
        this.counterOrderId = dexContractAttachment.getCounterOrderId();
        this.secretHash = dexContractAttachment.getSecretHash();
    }

}