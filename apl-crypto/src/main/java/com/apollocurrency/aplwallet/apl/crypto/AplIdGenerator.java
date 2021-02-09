/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.crypto;

import java.math.BigInteger;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class AplIdGenerator {

    public interface IdGenerator {

        BigInteger getIdByHash(byte[] hash);

        BigInteger getId(byte[] data);

    }

    public static final IdGenerator ACCOUNT = new ToLongGenerator();
    public static final IdGenerator TRANSACTION = new ToLongGenerator();
    public static final IdGenerator BLOCK = new ToLongGenerator();

    static class ToLongGenerator implements IdGenerator {
        @Override
        public BigInteger getIdByHash(byte[] hash) {
            return new BigInteger(1, new byte[]{hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
        }

        @Override
        public BigInteger getId(byte[] data) {
            Objects.requireNonNull(data);
            byte[] hash = Crypto.sha256().digest(data);
            return getIdByHash(hash);
        }
    }
}
