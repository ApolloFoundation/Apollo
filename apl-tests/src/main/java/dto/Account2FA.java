/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Account2FA extends BasicAccount {
    @JsonAlias("errorDescription")
    private TwoFactorAuthService.Status2FA status;

    @JsonCreator
    public Account2FA(@JsonProperty(value = "account", required = true) String account,
                      @JsonProperty("status") TwoFactorAuthService.Status2FA status2FA) {
        super(account);
        this.status = status2FA;
    }

    public TwoFactorAuthService.Status2FA getStatus2FA() {
        return status;
    }

    public void setStatus2FA(TwoFactorAuthService.Status2FA status2FA) {
        this.status = status2FA;
    }

    public TwoFactorAuthService.Status2FA getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Account2FA{" +
                "status=" + status +
                ", id=" + getAccountRS() +
                '}';
    }
}
