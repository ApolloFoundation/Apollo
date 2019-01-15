package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class CreateTransactionResponse extends ResponseBase {
    @ApiModelProperty("Transaction Signature Hash")
    public String signatureHash;
    @ApiModelProperty("Transaction transaction Info")
    public TransactionInfo transactionJSON;
    @ApiModelProperty("Unsigned Transaction Bytes")
    public String unsignedTransactionBytes;
    @ApiModelProperty("Transaction broadcasted")
    public Boolean broadcasted;
    @ApiModelProperty("Transaction Bytes")
    public String transactionBytes;
    @ApiModelProperty("Transaction full hash")
    public String fullHash;
    @ApiModelProperty("Transaction id as string")
    public String transaction;

}
