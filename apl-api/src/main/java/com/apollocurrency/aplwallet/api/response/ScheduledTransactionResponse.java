
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;


import java.util.ArrayList;
import java.util.List;

/**
 * List of scheduled transactions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel("List of scheduled transaction with optional attachment data")
//@ApiModel
public class ScheduledTransactionResponse extends ResponseBase{

    //@ApiModelProperty(value = "List of scheduled transactions (three fields are missing comparing to confirmed Tr)")
    public List<TransactionInfo> scheduledTransactions = new ArrayList<>(0);

    @Override
    public String toString() {
        return "ScheduledTransactionResponse{" +
                "scheduledTransactions=[" + (scheduledTransactions != null ? scheduledTransactions.size() : -1 ) +
                "]}";
    }
}

