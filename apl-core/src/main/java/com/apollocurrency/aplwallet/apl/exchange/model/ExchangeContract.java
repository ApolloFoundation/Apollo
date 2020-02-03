/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExchangeContract extends VersionedDerivedEntity {

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
    private Integer deadlineToReply;

    public ExchangeContract(Long transactionId, Long senderId, Long recipientId, Integer deadlineToReply, DexContractAttachment dexContractAttachment) {
        super(null, null);
        this.id = transactionId;
        this.orderId = dexContractAttachment.getOrderId();
        this.counterOrderId = dexContractAttachment.getCounterOrderId();
        this.sender = senderId;
        this.recipient = recipientId;

        this.secretHash = dexContractAttachment.getSecretHash();
        this.encryptedSecret = dexContractAttachment.getEncryptedSecret();
        this.transferTxId = dexContractAttachment.getTransferTxId();
        this.counterTransferTxId = dexContractAttachment.getCounterTransferTxId();
        this.contractStatus = dexContractAttachment.getContractStatus();
        this.deadlineToReply = deadlineToReply;
    }

    public ExchangeContract(Long dbId, Long transactionId, Long senderId, Long recipientId, Integer deadlineToReply,
                            DexContractAttachment dexContractAttachment, int height, boolean latest) {
        super(dbId, height);
        this.id = transactionId;
        this.orderId = dexContractAttachment.getOrderId();
        this.counterOrderId = dexContractAttachment.getCounterOrderId();
        this.sender = senderId;
        this.recipient = recipientId;

        this.secretHash = dexContractAttachment.getSecretHash();
        this.encryptedSecret = dexContractAttachment.getEncryptedSecret();
        this.transferTxId = dexContractAttachment.getTransferTxId();
        this.counterTransferTxId = dexContractAttachment.getCounterTransferTxId();
        this.contractStatus = dexContractAttachment.getContractStatus();
        this.deadlineToReply = deadlineToReply;
        this.setLatest(latest);
    }

    @Builder(builderMethodName = "builder")
    public ExchangeContract(Long dbId, Long id, Long orderId, Long counterOrderId, Long sender,
                            Long recipient, ExchangeContractStatus contractStatus, byte[] secretHash,
                            String transferTxId, String counterTransferTxId, byte[] encryptedSecret, Integer deadlineToReply,
                            Integer height, boolean latest) {
        super(dbId, height);
        this.id = id;
        this.orderId = orderId;
        this.counterOrderId = counterOrderId;
        this.sender = sender;
        this.recipient = recipient;
        this.contractStatus = contractStatus;
        this.secretHash = secretHash;
        this.transferTxId = transferTxId;
        this.counterTransferTxId = counterTransferTxId;
        this.encryptedSecret = encryptedSecret;
        this.deadlineToReply = deadlineToReply;
        this.setLatest(latest);
    }

    public Long getPhasingIdForOrder(DexOrder order) {
        Long txId = null;
        if (order.getType() == OrderType.SELL) {
            if (getOrderId().equals(order.getId()) && getTransferTxId() != null) {
                txId = Long.parseUnsignedLong(getTransferTxId());
            } else if (getCounterOrderId().equals(order.getId()) && getCounterOrderId() != null) {
                txId = Long.parseUnsignedLong(getCounterTransferTxId());
            }
        }
        return txId;
    }
}