
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author al
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Single transaction data optional attachment data included")
@ApiModel
public class TransactionInfo extends ResponseBase{
    @ApiModelProperty("Transaction type")
    public Byte type;
    @ApiModelProperty("Transaction subtype")
    public Byte subtype;
    @ApiModelProperty("True if it's phased")
    public Boolean phased;
    @ApiModelProperty("Transaction timestamp value")
    public Integer timestamp;
    @ApiModelProperty("Transaction deadline value")
    public Short deadline;
    @ApiModelProperty("Sender/Account public key bytes representation")
    public String senderPublicKey;
    @ApiModelProperty("Transaction's recipient Id)")
    public String recipient;
    @ApiModelProperty("Transaction recipient ID in Reed-Solomon format")
    public String recipientRS;
    @ApiModelProperty("Transaction amount")
    public String amountATM;
    @ApiModelProperty("Transaction free amount")
    public String feeATM;
    @ApiModelProperty("Transaction reference full hash")
    public String referencedTransactionFullHash;
    @ApiModelProperty("Transaction signature")
    public String signature;
    @ApiModelProperty("Transaction signature hash")
    public String signatureHash;
    @ApiModelProperty("Transaction full hash")
    public String fullHash;
    @ApiModelProperty("Transaction Id as String")
    public String transaction;
    @ApiModelProperty("Transaction version")
    public Byte version;
    @ApiModelProperty("Transaction sender Id as String")
    public String sender;
    @ApiModelProperty("Transaction sender Id in Reed-Solomon format")
    public String senderRS;
    @ApiModelProperty("Transaction height")
    public Long height;
    @ApiModelProperty("Transaction ec block Id")
    public String ecBlockId;
    @ApiModelProperty("Transaction ec block height")
    public Long ecBlockHeight;
    @ApiModelProperty(value = "Optional. Transaction parent block Id as String if it's confirmed", allowEmptyValue = true)
    public String block;
    @ApiModelProperty(value = "Optional. Confirmations number. It's a difference between transaction height and current block height", allowEmptyValue = true)
    public Integer confirmations;
    @ApiModelProperty(value = "Optional. Transaction timestamp", allowEmptyValue = true)
    public Integer blockTimestamp;
    @ApiModelProperty(value = "Optional. Transaction index", allowEmptyValue = true)
    public Short transactionIndex;
    @ApiModelProperty(value = "Optional. Phasing poll approved field", allowEmptyValue = true)
    public Boolean approved;
    @ApiModelProperty(value = "Optional. Phasing poll result field", allowEmptyValue = true)
    public String result;
    @ApiModelProperty(value = "Optional. Phasing poll execution Height field", allowEmptyValue = true)
    public Integer executionHeight;
    @ApiModelProperty(value = "Optional. Attachment field as json structure", allowEmptyValue = true)
    public JsonNode attachment;

    @Override
    public String toString() {
        return "TransactionInfo{" +
                "type=" + type +
                ", subtype=" + subtype +
                ", timestamp=" + timestamp +
                ", recipient='" + recipient + '\'' +
                ", recipientRS='" + recipientRS + '\'' +
                ", amountATM='" + amountATM + '\'' +
                ", feeATM='" + feeATM + '\'' +
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

