package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.APL;
import com.apollocurrency.aplwallet.api.dto.EthWalletInfo;
import com.apollocurrency.aplwallet.api.dto.auth.Status2FA;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class Account2FAResponse extends ResponseBase {
    private String publicKey;
    private long id;
    private String accountRS;
    private String account;
    private String secretBytes;
    private List<EthWalletInfo> eth;
    private APL apl;
    private String passphrase;
    private Status2FA status;
}
