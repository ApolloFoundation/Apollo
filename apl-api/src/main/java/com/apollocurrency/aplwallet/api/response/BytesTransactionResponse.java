
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;


/**
 * List of unconfirmed transactions (OR they Ids)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel(description = "Transaction bytes data)")
//@ApiModel
public class BytesTransactionResponse extends ResponseBase{
    //@ApiModelProperty("Confirmation number. It's difference between transaction height and current blockhchain height")
    public Integer confirmations;
    //@ApiModelProperty("Transaction's bytes representation")
    public String transactionBytes;
    //@ApiModelProperty("Transaction's unsigned bytes representation")
    public String unsignedTransactionBytes;
    //@ApiModelProperty(value = "Optional Prunable attachment data as Json", allowEmptyValue = true)
    public JsonNode prunableAttachmentJSON;

    @Override
    public String toString() {
        return "BytesTransactionResponse{" +
                "confirmations=" + confirmations +
                ", transactionBytes=[" + (transactionBytes != null ? transactionBytes.length() : -1) + ']' +
                ", unsignedTransactionBytes=[" + (unsignedTransactionBytes != null ? unsignedTransactionBytes.length() : -1) + ']' +
                ", prunableAttachmentJSON=[" + (prunableAttachmentJSON !=null ? prunableAttachmentJSON.size() : -1) + ']' +
                '}';
    }
}

