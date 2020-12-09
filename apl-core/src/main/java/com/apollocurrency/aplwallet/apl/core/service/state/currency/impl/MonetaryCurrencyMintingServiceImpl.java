/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.MonetaryCurrencyMintingService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.Arrays;

@Singleton
public class MonetaryCurrencyMintingServiceImpl implements MonetaryCurrencyMintingService {

    private CurrencyService currencyService;

    public MonetaryCurrencyMintingServiceImpl() {
    }

    private CurrencyService lookupCurrencyService() {
        if (this.currencyService == null) {
            this.currencyService = CDI.current().select(CurrencyService.class).get();
        }
        return currencyService;
    }


    @Override
    public boolean meetsTarget(long accountId, Currency currency, MonetarySystemCurrencyMinting attachment) {
        byte[] hash = getHash(currency.getAlgorithm(), attachment.getNonce(), attachment.getCurrencyId(), attachment.getUnits(),
            attachment.getCounter(), accountId);

        Currency currencyFull = this.localSupplyDependency(currency);

        byte[] target = getTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(),
            attachment.getUnits(),
            currencyFull.getCurrencySupply().getCurrentSupply() - currency.getReserveSupply(),
            currency.getMaxSupply() - currency.getReserveSupply());
        return MonetaryCurrencyMintingService.meetsTarget(hash, target);
    }

    private Currency localSupplyDependency(Currency currency) {
        CurrencySupply currencySupply = lookupCurrencyService().loadCurrencySupplyByCurrency(currency); // load dependency
        if (currencySupply != null) {
            currency.setCurrencySupply(currencySupply);
        }
        return currency;
    }

    @Override
    public byte[] getHash(byte algorithm, long nonce, long currencyId, long units, long counter, long accountId) {
        HashFunction hashFunction = HashFunction.getHashFunction(algorithm);
        return MonetaryCurrencyMintingService.getHash(hashFunction, nonce, currencyId, units, counter, accountId);
    }

    @Override
    public byte[] getTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply) {
        return getTarget(getNumericTarget(min, max, units, currentMintableSupply, totalMintableSupply));
    }

    @Override
    public byte[] getTarget(BigInteger numericTarget) {
        byte[] targetRowBytes = numericTarget.toByteArray();
        if (targetRowBytes.length == 32) {
            return reverse(targetRowBytes);
        }
        byte[] targetBytes = new byte[32];
        Arrays.fill(targetBytes, 0, 32 - targetRowBytes.length, (byte) 0);
        System.arraycopy(targetRowBytes, 0, targetBytes, 32 - targetRowBytes.length, targetRowBytes.length);
        return reverse(targetBytes);
    }

    @Override
    public BigInteger getNumericTarget(Currency currency, long units) {
        Currency currencyFull = this.localSupplyDependency(currency);
        return getNumericTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(), units,
            currencyFull.getCurrencySupply().getCurrentSupply() - currency.getReserveSupply(),
            currency.getMaxSupply() - currency.getReserveSupply());
    }

    @Override
    public BigInteger getNumericTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply) {
        if (min < 1 || max > 255) {
            throw new IllegalArgumentException(String.format("Min: %d, Max: %d, allowed range is 1 to 255", min, max));
        }
        int exp = (int) (256 - min - ((max - min) * currentMintableSupply) / totalMintableSupply);
        return BigInteger.valueOf(2).pow(exp).subtract(BigInteger.ONE).divide(BigInteger.valueOf(units));
    }

    private byte[] reverse(byte[] b) {
        for (int i = 0; i < b.length / 2; i++) {
            byte temp = b[i];
            b[i] = b[b.length - i - 1];
            b[b.length - i - 1] = temp;
        }
        return b;
    }

    private byte[] reverseXor(byte[] b) {
        for (int i = 0; i < b.length / 2; i++) {
            b[i] ^= b[b.length - i - 1];
            b[b.length - i - 1] ^= b[i];
            b[i] ^= b[b.length - i - 1];
        }
        return b;
    }


}
