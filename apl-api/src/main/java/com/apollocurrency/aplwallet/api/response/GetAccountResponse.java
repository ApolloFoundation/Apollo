package com.apollocurrency.aplwallet.api.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
//@ApiModel("List of scheduled transaction with optional attachment data")
@ApiModel
public class GetAccountResponse extends ResponseBase {
    public String balanceATM;
    public String forgedBalanceATM;
    public String accountRS;
    public String publicKey;
    public String unconfirmedBalanceATM;
    public String account;
    public long numberOfBlocks;
    public long requestProcessingTime;
    public String name;
    public boolean is2FA;
}
