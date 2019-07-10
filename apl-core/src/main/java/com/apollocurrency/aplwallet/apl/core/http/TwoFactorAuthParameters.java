/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.api.dto.Status2FA;

public class TwoFactorAuthParameters {

    long accountId;
    String passphrase;
    String secretPhrase;
    Status2FA status2FA;
    Integer code2FA;

    public static void requireSecretPhraseOrPassphrase(TwoFactorAuthParameters params2FA) throws ParameterException {
        if (!params2FA.isPassphrasePresent() && !params2FA.isSecretPhrasePresent()) {
            throw new ParameterException(JSONResponses.either("secretPhrase", "passphrase"));
        }
    }
    public long getAccountId() {
        return accountId;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public String getSecretPhrase() {
        return secretPhrase;
    }

    public boolean isSecretPhrasePresent() {
        return secretPhrase != null;
    }

    public boolean isPassphrasePresent() {
        return passphrase != null;
    }

    public Status2FA getStatus2FA() {
        return status2FA;
    }

    public void setStatus2FA(Status2FA status2FA) {
        this.status2FA = status2FA;
    }

    public Integer getCode2FA() {
        return code2FA;
    }

    public void setCode2FA(Integer code2FA) {
        this.code2FA = code2FA;
    }

    public TwoFactorAuthParameters(long accountId, String passphrase, String secretPhrase) {
        this.accountId = accountId;
        this.passphrase = passphrase;
        this.secretPhrase = secretPhrase;
    }

}
