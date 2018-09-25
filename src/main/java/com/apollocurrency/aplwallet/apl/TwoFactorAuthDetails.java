/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.util.Objects;

public class TwoFactorAuthDetails {
    private String qrCodeUrl;
    private String secret;

    public TwoFactorAuthDetails(String qrCodeUrl, String secret) {
        this.qrCodeUrl = qrCodeUrl;
        this.secret = secret;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TwoFactorAuthDetails)) return false;
        TwoFactorAuthDetails that = (TwoFactorAuthDetails) o;
        return Objects.equals(qrCodeUrl, that.qrCodeUrl) &&
                Objects.equals(secret, that.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qrCodeUrl, secret);
    }
}
