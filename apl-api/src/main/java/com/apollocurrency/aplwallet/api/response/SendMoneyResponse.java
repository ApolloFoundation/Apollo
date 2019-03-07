package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Information about transaction's broadcast operation")
//@ApiModel
public class SendMoneyResponse extends ResponseBase {
    //@ApiModelProperty("Transaction full hash")
    public String fullHash;
    //@ApiModelProperty("Transaction id as string")
    public String transaction;

    @Override
    public String toString() {
        return "SendMoneyResponse{" +
                "fullHash='" + fullHash + '\'' +
                ", transaction='" + transaction + '\'' +
                '}';
    }
}
