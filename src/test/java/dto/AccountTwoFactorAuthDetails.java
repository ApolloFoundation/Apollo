/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Objects;

public class AccountTwoFactorAuthDetails extends TwoFactorAuthDetails {
    private long account;
    private String accountRS;

    @JsonSetter("account")
    public void setStringAccount(String account) {
        this.account = Convert.parseAccountId(account);
    }
    public AccountTwoFactorAuthDetails(String qrCodeUrl, String secret, long account, String accountRS) {
        super(qrCodeUrl, secret);
        this.account = account;
        this.accountRS = accountRS;
    }

    public AccountTwoFactorAuthDetails() {
        super(null, null);
    }

    public long getAccount() {
        return account;
    }

    public void setAccount(long account) {
        this.account = account;
    }

    public String getAccountRS() {
        return accountRS;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountTwoFactorAuthDetails)) return false;
        if (!super.equals(o)) return false;
        AccountTwoFactorAuthDetails that = (AccountTwoFactorAuthDetails) o;
        return account == that.account &&
                Objects.equals(accountRS, that.accountRS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), account, accountRS);
    }
}
