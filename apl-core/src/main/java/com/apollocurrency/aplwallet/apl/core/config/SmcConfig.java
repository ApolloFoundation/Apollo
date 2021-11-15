/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.blockchain.crypt.Digest;
import com.apollocurrency.smc.blockchain.crypt.DigestWrapper;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.vm.SMCPaidExecutionMode;
import com.apollocurrency.smc.contract.vm.operation.OperationPrice;
import com.apollocurrency.smc.contract.vm.operation.SMCOperationPrice;
import com.apollocurrency.smc.polyglot.config.JsLimitsConfig;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.LanguageContextFactory;
import com.apollocurrency.smc.polyglot.security.AllowFullHostAccessPolicy;
import com.apollocurrency.smc.polyglot.security.AllowHostClassLoadingPolicy;
import com.apollocurrency.smc.polyglot.security.DenyGlobalObjectsPolicy;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class SmcConfig {

    @Produces
    public LanguageContext createLanguageContext() {
        return getLanguageContext();
    }

    @Produces
    public ExecutionEnv createExecutionEnv() {
        return getExecutionEnv();
    }

    @Produces
    @Singleton
    public HashSumProvider createHashSumProvider() {
        return new HashSumProvider() {
            @Override
            public Digest sha256() {
                return new DigestWrapper(Crypto.sha256());
            }

            @Override
            public byte[] sha256(byte[] input) {
                return Crypto.sha256().digest(input);
            }

            @Override
            public String sha256(String input) {
                return Convert.toHexString(Crypto.sha256().digest(Convert.toBytes(input)));
            }

            @Override
            public Digest sha3() {
                return new DigestWrapper(Crypto.sha3());
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

    public static SmcContext asContext(final BlockchainIntegrator integrator) {
        return new SmcContext() {
            @Override
            public BlockchainIntegrator getIntegrator() {
                return integrator;
            }

            @Override
            public ExecutionEnv getExecutionEnv() {
                return SmcConfig.getExecutionEnv();
            }

            @Override
            public LanguageContext getLanguageContext() {
                return SmcConfig.getLanguageContext();
            }
        };
    }

    private static LanguageContext getLanguageContext() {
        return LanguageContextFactory.createJSContext(
            new DenyGlobalObjectsPolicy(),
            new AllowFullHostAccessPolicy(),
            new AllowHostClassLoadingPolicy());
    }

    private static OperationPrice getAplPrice() {
        return SMCOperationPrice.getInstance();
    }

    private static ExecutionEnv getExecutionEnv() {
        return ExecutionEnv.builder()
            .mode(new SMCPaidExecutionMode(getAplPrice()))
            .price(getAplPrice())
            .config(new JsLimitsConfig())
            .build();
    }

}
