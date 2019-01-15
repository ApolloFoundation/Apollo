package com.apollocurrency.aplwallet.api.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel("List of scheduled transaction with optional attachment data")
@ApiModel
public class GetAccountResponse extends ResponseBase {
    public String balanceNQT;
    public String forgedBalanceNQT;
    public String accountRS;
    public String publicKey;
    public String unconfirmedBalanceNQT;
    public String account;
    public long numberOfBlocks;
    public long requestProcessingTime;
    public String name;
}
