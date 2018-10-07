/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthDetails;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class TwoFactorAuthAccountDetails {
    @JsonUnwrapped
    private BasicAccount account;
    @JsonUnwrapped
    private TwoFactorAuthDetails details;

    public TwoFactorAuthAccountDetails(BasicAccount account, TwoFactorAuthDetails details) {
        this.account = account;
        this.details = details;
    }

    public TwoFactorAuthAccountDetails() {
    }

    public BasicAccount getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = new BasicAccount(account);
    }

    public void setAccount(BasicAccount account) {
        this.account = account;
    }

    public TwoFactorAuthDetails getDetails() {
        return details;
    }

    public void setDetails(TwoFactorAuthDetails details) {
        this.details = details;
    }
}
