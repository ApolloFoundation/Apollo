package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter @Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class UnconfirmedTransactionDTO extends BaseDTO {
    private Byte type;
    private Byte subtype;
    private Boolean phased;
    private Integer timestamp;
    private Short deadline;
    private String senderPublicKey;
    private String recipient;
    private String recipientRS;
    private String amountATM;
    private String feeATM;
    private String referencedTransactionFullHash;
    private String signature;
    private String signatureHash;
    private String fullHash;
    private String transaction;
    private Map attachment;
    private String sender;
    private String senderRS;

    private Integer height;
    private Byte version;

    private String ecBlockId;
    private Integer ecBlockHeight;

    public UnconfirmedTransactionDTO(UnconfirmedTransactionDTO o) {
        this.type = o.type;
        this.subtype = o.subtype;
        this.phased = o.phased;
        this.timestamp = o.timestamp;
        this.deadline = o.deadline;
        this.senderPublicKey = o.senderPublicKey;
        this.recipient = o.recipient;
        this.recipientRS = o.recipientRS;
        this.amountATM = o.amountATM;
        this.feeATM = o.feeATM;
        this.referencedTransactionFullHash = o.referencedTransactionFullHash;
        this.signature = o.signature;
        this.signatureHash = o.signatureHash;
        this.fullHash = o.fullHash;
        this.transaction = o.transaction;
        this.attachment = o.attachment;
        this.sender = o.sender;
        this.senderRS = o.senderRS;
        this.height = o.height;
        this.version = o.version;
        this.ecBlockId = o.ecBlockId;
        this.ecBlockHeight = o.ecBlockHeight;
    }
}
