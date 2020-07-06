/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;

public interface MonetaryCurrencyMintingService {

    static Set<HashFunction> acceptedHashFunctions =
        Collections.unmodifiableSet(EnumSet.of(HashFunction.SHA256, HashFunction.SHA3, HashFunction.SCRYPT, HashFunction.Keccak25));

    boolean meetsTarget(long accountId, Currency currency, MonetarySystemCurrencyMinting attachment);

    boolean meetsTarget(byte[] hash, byte[] target);

    byte[] getHash(byte algorithm, long nonce, long currencyId, long units, long counter, long accountId);

    byte[] getHash(HashFunction hashFunction, long nonce, long currencyId, long units, long counter, long accountId);

    byte[] getTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply);

    byte[] getTarget(BigInteger numericTarget);

    BigInteger getNumericTarget(Currency currency, long units);

    BigInteger getNumericTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply);

}
