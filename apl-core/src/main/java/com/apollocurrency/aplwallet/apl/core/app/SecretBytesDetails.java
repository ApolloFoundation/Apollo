/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.service.appdata.KeyStoreService;

import java.util.Arrays;
import java.util.Objects;

public class SecretBytesDetails {
    private byte[] secretBytes;
    private KeyStoreService.Status extractStatus;

    public SecretBytesDetails(byte[] secretBytes, KeyStoreService.Status extractStatus) {
        this.secretBytes = secretBytes;
        this.extractStatus = extractStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecretBytesDetails)) return false;
        SecretBytesDetails that = (SecretBytesDetails) o;
        return Arrays.equals(secretBytes, that.secretBytes) &&
            extractStatus == that.extractStatus;
    }

    public byte[] getSecretBytes() {
        return secretBytes;
    }

    public KeyStoreService.Status getExtractStatus() {
        return extractStatus;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(extractStatus);
        result = 31 * result + Arrays.hashCode(secretBytes);
        return result;
    }


}
