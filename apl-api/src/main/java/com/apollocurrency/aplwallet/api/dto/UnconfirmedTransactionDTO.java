/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.dto.utils.JacksonUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
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
    private String transaction;//unsigned long of transactionId
    @JsonDeserialize(using = UnconfirmedTransactionDTO.AttachmentDeserializer.class)
    private Map<?, ?> attachment;
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


    static class AttachmentDeserializer extends JsonDeserializer<LinkedHashMap<?, ?>> {
        @Override
        public LinkedHashMap<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectCodec oc = p.getCodec();
            JsonNode root = oc.readTree(p);
            return JacksonUtil.parseJsonNodeGraph(root);
        }
    }

}
