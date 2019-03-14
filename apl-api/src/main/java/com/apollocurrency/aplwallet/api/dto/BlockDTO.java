package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockDTO {
    public String previousBlockHash;
    public Long payloadLength;
    public String totalAmountATM;
    public String generationSignature;
    public String generator;
    public String generatorPublicKey;
    public String baseTarget;
    public String payloadHash;
    public String generatorRS;
    public Long numberOfTransactions;
    public String blockSignature;
    public TransactionDTO[] transactions;
    public Long version;
    public String totalFeeATM;
    public String previousBlock;
    public String cumulativeDifficulty;
    public String block;
    public Long height;
    public Long timestamp;
    public String nextBlock;
    private String protocol;
    public Long requestProcessingTime;
    public Long timeout;


}
