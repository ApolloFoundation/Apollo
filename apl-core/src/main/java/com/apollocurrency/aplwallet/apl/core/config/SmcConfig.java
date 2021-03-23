/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.smc.blockchain.crypt.CryptoLibProvider;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.fuel.FuelPriceLimitProvider;
import com.apollocurrency.smc.contract.fuel.StaticFuelPriceLimitProvider;
import com.apollocurrency.smc.polyglot.LanguageContext;
import com.apollocurrency.smc.polyglot.LanguageContextFactory;
import com.apollocurrency.smc.polyglot.security.AllowFullHostAccessPolicy;
import com.apollocurrency.smc.polyglot.security.AllowHostClassLoadingPolicy;
import com.apollocurrency.smc.polyglot.security.DenyGlobalObjectsPolicy;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class SmcConfig {

    @Produces
    public FuelPriceLimitProvider createDefaultFuelProvider() {
        final BigInteger fuelLimit = BigInteger.valueOf(20000);
        final BigInteger fuelPrice = BigInteger.valueOf(1000000);
        return new StaticFuelPriceLimitProvider(fuelPrice, fuelLimit);
    }

    @Produces
    public LanguageContext createLanguageContext() {
        return LanguageContextFactory.createJSContext(
            new DenyGlobalObjectsPolicy(),
            new AllowFullHostAccessPolicy(),
            new AllowHostClassLoadingPolicy());
    }

    @Produces
    @ApplicationScoped
    public HashSumProvider createHashSumProvider() {
        return new HashSumProvider() {
            @Override
            public byte[] sha256(byte[] input) {
                return Crypto.sha256().digest(input);
            }

            @Override
            public String sha256(String input) {
                return Convert.toHexString(Crypto.sha256().digest(Convert.toBytes(input)));
            }

            @Override
            public byte[] sha3(byte[] input) {
                return Crypto.sha3().digest(input);
            }

            @Override
            public String sha3(String input) {
                return Convert.toHexString(Crypto.sha3().digest(Convert.toBytes(input)));
            }
        };
    }

    @Produces
    @ApplicationScoped
    public CryptoLibProvider createCryptoLibProvider() {
        return new CryptoLibProvider() {
            @Override
            public byte[] getPublicKey(byte[] secretPhrase, byte[] nonce, byte[] salt) {
                return Crypto.getPublicKey(Crypto.getKeySeed(secretPhrase, nonce, salt));
            }

            @Override
            public byte[] getKeySeed(byte[] secretPhrase, byte[] nonce, byte[] salt) {
                return Crypto.getKeySeed(secretPhrase, nonce, salt);
            }

            @Override
            public byte[] getPublicKey(byte[] secretPhrase) {
                return Crypto.getPublicKey(Crypto.sha256().digest(secretPhrase));
            }

            @Override
            public boolean verify(byte[] signature, byte[] message, byte[] publicKey) {
                return Crypto.verify(signature, message, publicKey);
            }

            @Override
            public byte[] sign(String message, byte[] secretPhrase) {
                return Crypto.sign(Convert.toBytes(message), Crypto.sha256().digest(secretPhrase));
            }

            @Override
            public byte[] sign(byte[] message, byte[] secretPhrase) {
                return Crypto.sign(message, Crypto.sha256().digest(secretPhrase));
            }

            @Override
            public String generateAccount(String secretPhrase) {
                return Convert.defaultRsAccount(Convert.getId(Crypto.getPublicKey(secretPhrase)));
            }
        };
    }
}
