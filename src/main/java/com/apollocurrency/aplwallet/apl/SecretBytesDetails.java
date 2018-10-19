/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.util.Arrays;
import java.util.Objects;

public class SecretBytesDetails {
    private byte[] secretBytes;
    private ExtractStatus extractStatus;

    public SecretBytesDetails(byte[] secretBytes, ExtractStatus extractStatus) {
        this.secretBytes = secretBytes;
        this.extractStatus = extractStatus;
    }

    public SecretBytesDetails() {
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

    public ExtractStatus getExtractStatus() {
        return extractStatus;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(extractStatus);
        result = 31 * result + Arrays.hashCode(secretBytes);
        return result;
    }

    public enum ExtractStatus {
        NOT_FOUND,
        DUPLICATE_FOUND,
        BAD_CREDENTIALS,
        READ_ERROR,
        DECRYPTION_ERROR,
        OK
    }
}
