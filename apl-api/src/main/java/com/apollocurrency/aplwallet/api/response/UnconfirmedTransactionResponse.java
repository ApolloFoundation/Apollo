
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * List of unconfirmed transactions (OR they Ids)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "List of unconfirmed transaction Ids (OR data with optional attachment data)")
@ApiModel
public class UnconfirmedTransactionResponse extends ResponseBase{
    @ApiModelProperty(value = "Id List of unconfirmed transactions", allowEmptyValue = true)
    public List<Long> unconfirmedTransactionIds;
    @ApiModelProperty(value = "List of unconfirmed transactions (three fields are missing comparing to confirmed Tr)", allowEmptyValue = true)
    public List<TransactionInfo> unconfirmedTransactions;

    @Override
    public String toString() {
        return "UnconfirmedTransactionResponse{" +
                "unconfirmedTransactionIds=[" + (unconfirmedTransactionIds != null ? unconfirmedTransactionIds.size() : -1 ) +
                "]}";
    }
}

