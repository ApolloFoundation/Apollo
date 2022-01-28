/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.aplwallet.apl.smc.vm.PriceProvider;
import com.apollocurrency.aplwallet.apl.smc.vm.SMCOperationPriceProvider;
import com.apollocurrency.smc.blockchain.BlockchainIntegratorFactory;
import com.apollocurrency.smc.blockchain.crypt.Digest;
import com.apollocurrency.smc.blockchain.crypt.DigestWrapper;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.fuel.Chargeable;
import com.apollocurrency.smc.contract.fuel.OperationPrice;
import com.apollocurrency.smc.contract.vm.SMCFreeExecutionMode;
import com.apollocurrency.smc.contract.vm.SMCPaidExecutionMode;
import com.apollocurrency.smc.polyglot.config.JsLimitsConfig;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import com.apollocurrency.smc.polyglot.engine.ExecutionMode;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.LanguageContextFactory;
import com.apollocurrency.smc.polyglot.security.AllowHostClassLoadingPolicy;
import com.apollocurrency.smc.polyglot.security.DenyGlobalObjectsPolicy;
import com.apollocurrency.smc.polyglot.security.ExplicitHostAccessPolicy;
import lombok.Getter;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class SmcConfig {

    private final PriceProvider priceProvider = SMCOperationPriceProvider.getInstance();

    public static LanguageContext getSmcLanguageContext() {
        return LanguageContextFactory.createJSContext(
            new DenyGlobalObjectsPolicy(),
            new ExplicitHostAccessPolicy(),
            new AllowHostClassLoadingPolicy()
        );
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

    @Produces
    public LanguageContext createLanguageContext() {
        return getSmcLanguageContext();
    }

    public SmcContext asContext(int height, Chargeable chargeable, final BlockchainIntegratorFactory integrator) {
        return new AbstractContext(height, chargeable, integrator) {
            @Override
            protected ExecutionMode getExecutionMode(int height, Chargeable chargeable) {
                return new SMCPaidExecutionMode(loadPrice(height), chargeable, true, true, false);
            }
        };
    }

    public SmcContext asViewContext(int height, Chargeable chargeable, final BlockchainIntegratorFactory integrator) {
        return new AbstractContext(height, chargeable, integrator) {
            @Override
            protected ExecutionMode getExecutionMode(int height, Chargeable chargeable) {
                return new SMCFreeExecutionMode(loadPrice(height), chargeable, true, true, false);
            }
        };
    }

    public OperationPrice loadPrice(int height) {
        return priceProvider.getPrice(height);
    }

    private abstract class AbstractContext implements SmcContext {
        private final int height;
        private final Chargeable chargeable;
        @Getter
        private final BlockchainIntegratorFactory integratorFactory;

        public AbstractContext(int height, Chargeable chargeable, BlockchainIntegratorFactory integratorFactory) {
            this.height = height;
            this.chargeable = chargeable;
            this.integratorFactory = integratorFactory;
        }

        protected abstract ExecutionMode getExecutionMode(int height, Chargeable chargeable);

        @Override
        public ExecutionEnv getExecutionEnv() {
            return ExecutionEnv.builder()
                .mode(getExecutionMode(height, chargeable))
                .config(new JsLimitsConfig())
                .build();
        }

        @Override
        public LanguageContext getLanguageContext() {
            return createLanguageContext();
        }

        @Override
        public OperationPrice getPrice() {
            return loadPrice(height);
        }
    }
}
