
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author al
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Single transaction data optional attachment data included")
@ApiModel
public class TransactionVerifiedInfo extends TransactionInfo {
    // additional fields
    @ApiModelProperty(value = "True if Transaction is verified, empty otherwise", allowEmptyValue = true)
    public Boolean verify;
    @ApiModelProperty(value = "False if Transaction is not valid, empty otherwise", allowEmptyValue = true)
    public Boolean validate;

    public TransactionVerifiedInfo() {
    }

    /**
     * Copy constructor
     * @param info parent instance
     */
    public TransactionVerifiedInfo(TransactionInfo info) {
        this.type = info.type;
        this.subtype = info.subtype;
        this.phased = info.phased;
        this.timestamp = info.timestamp;
        this.deadline = info.deadline;
        this.senderPublicKey = info.senderPublicKey;
        this.recipient = info.recipient;
        this.recipientRS = info.recipientRS;
        this.amountNQT = info.amountNQT;
        this.feeNQT = info.feeNQT;
        this.referencedTransactionFullHash = info.referencedTransactionFullHash;
        this.signature = info.signature;
        this.signatureHash = info.signatureHash;
        this.fullHash = info.fullHash;
        this.transaction = info.transaction;
        this.version = info.version;
        this.sender = info.sender;
        this.senderRS = info.senderRS;
        this.height = info.height;
        this.ecBlockId = info.ecBlockId;
        this.ecBlockHeight = info.ecBlockHeight;
        this.block = info.block;
        this.confirmations = info.confirmations;
        this.blockTimestamp = info.blockTimestamp;
        this.transactionIndex = info.transactionIndex;
        this.approved = info.approved;
        this.result = info.result;
        this.executionHeight = info.executionHeight;
        this.attachment = info.attachment;
    }

    @Override
    public String toString() {
        return "TransactionVerifiedInfo{" +
                "type=" + type +
                ", subtype=" + subtype +
                ", timestamp=" + timestamp +
                ", recipient='" + recipient + '\'' +
                ", recipientRS='" + recipientRS + '\'' +
                ", amountNQT='" + amountNQT + '\'' +
                ", feeNQT='" + feeNQT + '\'' +
                ", fullHash='" + fullHash + '\'' +
                ", transaction='" + transaction + '\'' +
                ", sender='" + sender + '\'' +
                ", senderRS='" + senderRS + '\'' +
                ", height=" + height +
                ", block='" + block + '\'' +
                ", confirmations=" + confirmations +
                ", transactionIndex=" + transactionIndex +
                ", approved=" + approved +
                '}';
    }
}

