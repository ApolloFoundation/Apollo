package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "Block of blockchain representation")
public class BlockDTO {
    @ApiModelProperty(value = "Hash value of previous block in HEX represenation", allowEmptyValue = false)
    public String previousBlockHash;
    public Long payloadLength;
    public String totalAmountNQT;
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
    public String totalFeeNQT;
    public String previousBlock;
    public String cumulativeDifficulty;
    public String block;
    public Long height;
    public Long timestamp;
    public String nextBlock;
    private String protocol;

}
