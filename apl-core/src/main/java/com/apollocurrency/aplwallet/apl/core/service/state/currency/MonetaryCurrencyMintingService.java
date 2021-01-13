/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public interface MonetaryCurrencyMintingService {

    Set<HashFunction> acceptedHashFunctions =
        Collections.unmodifiableSet(EnumSet.of(HashFunction.SHA256, HashFunction.SHA3, HashFunction.SCRYPT, HashFunction.Keccak25));

    static boolean meetsTarget(byte[] hash, byte[] target) {
        for (int i = hash.length - 1; i >= 0; i--) {
            if ((hash[i] & 0xff) > (target[i] & 0xff)) {
                return false;
            }
            if ((hash[i] & 0xff) < (target[i] & 0xff)) {
                return true;
            }
        }
        return true;
    }

    static byte[] getHash(HashFunction hashFunction, long nonce, long currencyId, long units, long counter, long accountId) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(nonce);
        buffer.putLong(currencyId);
        buffer.putLong(units);
        buffer.putLong(counter);
        buffer.putLong(accountId);
        return hashFunction.hash(buffer.array());
    }

    boolean meetsTarget(long accountId, Currency currency, MonetarySystemCurrencyMinting attachment);

    byte[] getHash(byte algorithm, long nonce, long currencyId, long units, long counter, long accountId);

    byte[] getTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply);

    byte[] getTarget(BigInteger numericTarget);

    BigInteger getNumericTarget(Currency currency, long units);

    BigInteger getNumericTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply);

}
