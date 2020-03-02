/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TwoFactorAuthParameters {

    private final long accountId;
    private final String passphrase;
    private final String secretPhrase;
    @Setter
    private Status2FA status2FA;
    @Setter
    private Integer code2FA;

    public TwoFactorAuthParameters(long accountId, String passphrase, String secretPhrase) {
        this.accountId = accountId;
        this.passphrase = passphrase;
        this.secretPhrase = secretPhrase;
    }

    public boolean isSecretPhrasePresent() {
        return secretPhrase != null;
    }

    public boolean isPassphrasePresent() {
        return passphrase != null;
    }

}
