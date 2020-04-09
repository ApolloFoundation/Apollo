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
    private final byte[] publicKey;
    @Setter
    private Status2FA status2FA;
    @Setter
    private Integer code2FA;

    public TwoFactorAuthParameters(long accountId, String passphrase, String secretPhrase) {
        this(accountId, passphrase, secretPhrase, null);
    }

    public TwoFactorAuthParameters(long accountId, String passphrase, String secretPhrase, byte[] publicKey) {
        this.accountId = accountId;
        this.passphrase = passphrase;
        this.secretPhrase = secretPhrase;
        this.publicKey = publicKey;
    }

    public boolean isSecretPhrasePresent() {
        return secretPhrase != null;
    }

    public boolean isPassphrasePresent() {
        return passphrase != null;
    }

    public boolean isPublicKeyPresent() {
        return publicKey != null;
    }

}
