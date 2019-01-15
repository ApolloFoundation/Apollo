package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentResponse extends ResponseBase {
    @ApiModelProperty(value = "Transaction entity")
    public TransactionDTO transactionJSON;
    @ApiModelProperty
    public String unsignedTransactionBytes;
    @ApiModelProperty(value = "Transaction id")
    public String transaction;
    @ApiModelProperty
    public String fullHash;
    @ApiModelProperty
    public String transactionBytes;
    @ApiModelProperty
    public String signatureHash;
    @ApiModelProperty
    public Boolean broadcasted;
}
