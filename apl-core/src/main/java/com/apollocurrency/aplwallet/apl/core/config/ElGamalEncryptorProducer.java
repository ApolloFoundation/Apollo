/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.service.ElGamalEncryptor;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Produce CDI-managed instance of the {@link ElGamalEncryptor}
 * @author Andrii Boiarskyi
 * @see ElGamalEncryptor
 * @see TaskDispatchManager
 * @since 1.48.4
 */
@Singleton
public class ElGamalEncryptorProducer {
    @Inject
    TaskDispatchManager dispatchManager;


    @Produces
    @Singleton
    public ElGamalEncryptor elGamalEncryptor() {
        ElGamalEncryptor encryptor = new ElGamalEncryptor(dispatchManager);
        encryptor.init();
        return encryptor;
    }
}
