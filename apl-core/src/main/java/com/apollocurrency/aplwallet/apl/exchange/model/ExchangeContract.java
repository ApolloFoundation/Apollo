package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class ExchangeContract {

    private Long id;
    private Long orderId;
    private Long counterOrderId;
    private Long sender;
    private Long recipient;
    private ExchangeContractStatus contractStatus;
    /**
     * Hash from secret key. sha256(key)
     */
    private byte[] secretHash;
    private String transferTxId;
    private String counterTransferTxId;
    /**
     * Encrypted secret key to have able to restore secret.
     */
    private byte[] encryptedSecret;
//    private Integer finishTime;

    public ExchangeContract(Long senderId, Long recipientId, DexContractAttachment dexContractAttachment) {
        this.orderId = dexContractAttachment.getOrderId();
        this.counterOrderId = dexContractAttachment.getCounterOrderId();
        this.sender = senderId;
        this.recipient = recipientId;

        this.secretHash = dexContractAttachment.getSecretHash();
        this.encryptedSecret = dexContractAttachment.getEncryptedSecret();
        this.transferTxId = dexContractAttachment.getTransferTxId();
        this.counterTransferTxId = dexContractAttachment.getCounterTransferTxId();
        this.contractStatus = dexContractAttachment.getContractStatus();
//        this.finishTime = dexContractAttachment.getFinishTime();
    }

}