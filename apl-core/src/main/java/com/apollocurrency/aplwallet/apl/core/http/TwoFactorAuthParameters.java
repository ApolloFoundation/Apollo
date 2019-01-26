package com.apollocurrency.aplwallet.apl.core.http;

public class TwoFactorAuthParameters {

    long accountId;
    String passphrase;
    String secretPhrase;

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

    public TwoFactorAuthParameters(long accountId, String passphrase, String secretPhrase) {
        this.accountId = accountId;
        this.passphrase = passphrase;
        this.secretPhrase = secretPhrase;
    }

}
