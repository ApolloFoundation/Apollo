package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ArrayNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDTO {
    public String signature;
    public Long transactionIndex;
    public Integer type;
    public Boolean phased;
    public String ecBlockID;
    public String signatureHash;
    // TODO : Annotation added for test
    @JsonIgnore
    public ArrayNode attachment;
    public Integer subtype;
    public String block;
    public Long blockTimestamp;
    public Long deadline;
    public Long timestamp;
    public Long height;
    public Long confirmations;
    public String fullHash;
    public Long version;
    public String amountATM;
    public String sender;
    public String senderRS;
    public Object recipient;
    public Object recipientRS;
    public String feeATM;
    public Long ecBlockHeight;
    public String transaction;
    public String encryptedTransaction;

    public String senderPublicKey;
    public String amountNQT;
    public String feeNQT;
    public String ecBlockId;
    public String referencedTransactionFullHash;
}
