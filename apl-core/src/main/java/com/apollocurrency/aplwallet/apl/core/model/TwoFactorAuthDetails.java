/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TwoFactorAuthDetails {
    private String qrCodeUrl;
    private String secret;
    private Status2FA status2FA;

    @JsonCreator
    public TwoFactorAuthDetails(@JsonProperty("qrCodeUrl") String qrCodeUrl, @JsonProperty("secret") String secret,
                                @JsonProperty("status2FA") Status2FA
                                    status2FA) {
        this.qrCodeUrl = qrCodeUrl;
        this.secret = secret;
        this.status2FA = status2FA;
    }

    public TwoFactorAuthDetails() {
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

    public Status2FA getStatus2Fa() {
        return status2FA;
    }

    public void setStatus2Fa(Status2FA status2Fa) {
        this.status2FA = status2Fa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TwoFactorAuthDetails)) return false;
        TwoFactorAuthDetails that = (TwoFactorAuthDetails) o;
        return Objects.equals(qrCodeUrl, that.qrCodeUrl) &&
            Objects.equals(secret, that.secret) &&
            Objects.equals(status2FA, that.status2FA);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qrCodeUrl, secret, status2FA);
    }
}
