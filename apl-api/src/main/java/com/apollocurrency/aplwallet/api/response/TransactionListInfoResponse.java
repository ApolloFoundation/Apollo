
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;


import java.util.List;

/**
 * List of TransactionInfo instances with Transaction data + attachments
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "List of transaction data with optional attachment data")
//@ApiModel
public class TransactionListInfoResponse extends ResponseBase {
    //@ApiModelProperty(value = "Transaction list with all info", allowEmptyValue = true)
    public List<TransactionInfo> transactions;
    //@ApiModelProperty(value = "Transaction list with all info", allowEmptyValue = true)
    public List<TransactionInfo> unconfirmedTransactions;

    @Override
    public String toString() {
        return "TransactionListInfoResponse{" +
                "transactions=[" + (transactions != null ? transactions.size() : -1 ) +
                "]}";
    }
}

