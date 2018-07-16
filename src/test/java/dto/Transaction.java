/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package dto;

import com.apollocurrency.aplwallet.apl.TransactionType;

import java.util.Objects;

import static util.TestUtil.fromATM;

/**
 * Simple DTO object for {@link com.apollocurrency.aplwallet.apl.TransactionImpl}
 */
public class Transaction {
    private String senderPublicKey;
    private String signature;
    private Long feeATM;
    private Long transactionIndex;
    private Byte type;
    private Long confirmations;
    private String fullHash;
    private Long version;
    private Boolean phased;
    private String ecBlockId;
    private String signatureHash;
    //attachments should present, but ignoring for now
    private String senderRS;
    private Byte subtype;
    private Long amountATM;
    private String sender; //senderId
    private Long ecBlockHeight;
    private String block;//blockId
    private Long blockTimestamp; //time in seconds since genesis block till this transaction block
    private Long deadline;
    private String transaction; //transactionId
    private Long timestamp; //time in seconds since genesis block till this transaction
    private Long height;
    private String recipientRS;

    @Override
    public String toString() {
        return "Transaction{" +
                "type=" + TransactionType.findTransactionType(type,subtype) +
                "amountATM=" + fromATM(amountATM) +
                ", feeATM=" + fromATM(feeATM) +
                ", senderRS='" + senderRS + '\'' +
                ", height=" + height +
                ", recipientRS='" + recipientRS + '\'' +
                '}';
    }

    public Long getBlockTimestamp() {
        return blockTimestamp;
    }

    public void setBlockTimestamp(Long blockTimestamp) {
        this.blockTimestamp = blockTimestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderPublicKey() {
        return senderPublicKey;
    }

    public void setSenderPublicKey(String senderPublicKey) {
        this.senderPublicKey = senderPublicKey;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getFeeATM() {
        return feeATM;
    }

    public void setFeeATM(Long feeATM) {
        this.feeATM = feeATM;
    }

    public Long getTransactionIndex() {
        return transactionIndex;
    }

    public void setTransactionIndex(Long transactionIndex) {
        this.transactionIndex = transactionIndex;
    }

    public Byte getType() {
        return type;
    }

    public void setType(Byte type) {
        this.type = type;
    }

    public Long getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(Long confirmations) {
        this.confirmations = confirmations;
    }

    public String getFullHash() {
        return fullHash;
    }

    public void setFullHash(String fullHash) {
        this.fullHash = fullHash;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getPhased() {
        return phased;
    }

    public void setPhased(Boolean phased) {
        this.phased = phased;
    }

    public String getEcBlockId() {
        return ecBlockId;
    }

    public void setEcBlockId(String ecBlockId) {
        this.ecBlockId = ecBlockId;
    }

    public String getSignatureHash() {
        return signatureHash;
    }

    public void setSignatureHash(String signatureHash) {
        this.signatureHash = signatureHash;
    }

    public String getSenderRS() {
        return senderRS;
    }

    public void setSenderRS(String senderRS) {
        this.senderRS = senderRS;
    }

    public Byte getSubtype() {
        return subtype;
    }

    public void setSubtype(Byte subtype) {
        this.subtype = subtype;
    }

    public Long getAmountATM() {
        return amountATM;
    }

    public void setAmountATM(Long amountATM) {
        this.amountATM = amountATM;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Long getEcBlockHeight() {
        return ecBlockHeight;
    }

    public void setEcBlockHeight(Long ecBlockHeight) {
        this.ecBlockHeight = ecBlockHeight;
    }


    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public String getRecipientRS() {
        return recipientRS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(getSenderPublicKey(), that.getSenderPublicKey()) &&
                Objects.equals(getSignature(), that.getSignature()) &&
                Objects.equals(getFeeATM(), that.getFeeATM()) &&
                Objects.equals(getTransactionIndex(), that.getTransactionIndex()) &&
                Objects.equals(getType(), that.getType()) &&
                Objects.equals(getConfirmations(), that.getConfirmations()) &&
                Objects.equals(getFullHash(), that.getFullHash()) &&
                Objects.equals(getVersion(), that.getVersion()) &&
                Objects.equals(getPhased(), that.getPhased()) &&
                Objects.equals(getEcBlockId(), that.getEcBlockId()) &&
                Objects.equals(getSignatureHash(), that.getSignatureHash()) &&
                Objects.equals(getSenderRS(), that.getSenderRS()) &&
                Objects.equals(getSubtype(), that.getSubtype()) &&
                Objects.equals(getAmountATM(), that.getAmountATM()) &&
                Objects.equals(getSender(), that.getSender()) &&
                Objects.equals(getEcBlockHeight(), that.getEcBlockHeight()) &&
                Objects.equals(getBlock(), that.getBlock()) &&
                Objects.equals(getBlockTimestamp(), that.getBlockTimestamp()) &&
                Objects.equals(getDeadline(), that.getDeadline()) &&
                Objects.equals(getTransaction(), that.getTransaction()) &&
                Objects.equals(getTimestamp(), that.getTimestamp()) &&
                Objects.equals(getHeight(), that.getHeight()) &&
                Objects.equals(getRecipientRS(), that.getRecipientRS());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getSenderPublicKey(), getSignature(), getFeeATM(), getTransactionIndex(), getType(), getConfirmations(), getFullHash(), getVersion(), getPhased(), getEcBlockId(), getSignatureHash(), getSenderRS(), getSubtype(), getAmountATM(), getSender(), getEcBlockHeight(), getBlock(), getBlockTimestamp(), getDeadline(), getTransaction(), getTimestamp(), getHeight(), getRecipientRS());
    }

    public void setRecipientRS(String recipientRS) {
        this.recipientRS = recipientRS;
    }

    public boolean isNull() {
        return signature == null || signatureHash == null || fullHash == null;
    }

    public boolean isPrivate() {
        return type == TransactionType.Payment.PRIVATE.getType() && subtype == TransactionType.Payment.PRIVATE.getSubtype();
    }

    public boolean isOwnedBy(String accountRs) {
        Objects.requireNonNull(accountRs);
        return accountRs.equalsIgnoreCase(recipientRS) || accountRs.equalsIgnoreCase(senderRS);
    }
}
