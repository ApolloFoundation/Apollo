/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.util.service.ElGamalEncryptor;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ElGamalEncryptorProducer {
    private final ElGamalEncryptor elGamalEncryptor;

    @Inject
    public ElGamalEncryptorProducer(TaskDispatchManager dispatchManager) {
        this.elGamalEncryptor = new ElGamalEncryptor(dispatchManager);
    }

    @PostConstruct
    public void init() {
        elGamalEncryptor.init();
    }

    @Produces
    public ElGamalEncryptor getElGamalEncryptor() {
        return elGamalEncryptor;
    }
}
