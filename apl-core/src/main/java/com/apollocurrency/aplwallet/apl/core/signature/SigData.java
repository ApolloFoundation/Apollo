/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
class SigData implements Signature {
    private final byte[] signature;
    private boolean verified = false;

    public SigData(byte[] signature) {
        this.signature = Objects.requireNonNull(signature);
    }

    void setVerified(boolean verified) {
        this.verified = verified;
    }

    @Override
    public boolean isVerified() {
        return verified;
    }

    @Override
    public byte[] bytes() {
        return signature;
    }

    @Override
    public int getSize() {
        return signature.length;
    }

    @Override
    public String getJsonString() {
        return Convert.toHexString(signature);
    }

    static class Parser implements SignatureParser {
        /**
         * Parse the byte array and build the sig object
         *
         * @param bytes input data array
         * @return the sig object
         */
        @Override
        public Signature parse(byte[] bytes) {
            return parse(ByteBuffer.wrap(bytes));
        }

        /**
         * Parse the byte array and build the sig object
         *
         * @param buffer input data array
         * @return the sig object
         */
        @Override
        public Signature parse(ByteBuffer buffer) {
            SigData sigData;
            try {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                byte[] signature = new byte[ECDSA_SIGNATURE_SIZE];
                buffer.get(signature);
                sigData = new SigData(signature);
            } catch (BufferUnderflowException e) {
                String message = "Can't parse signature bytes, cause: " + e.getMessage();
                log.error(message);
                throw new SignatureParseException(message);
            }
            return sigData;
        }

        @Override
        public int calcDataSize(int count) {
            return ECDSA_SIGNATURE_SIZE;
        }

        @Override
        public byte[] bytes(Signature signature) {
            ByteBuffer buffer = ByteBuffer.allocate(calcDataSize(1));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(signature.bytes());
            return buffer.array();
        }

    }
}
