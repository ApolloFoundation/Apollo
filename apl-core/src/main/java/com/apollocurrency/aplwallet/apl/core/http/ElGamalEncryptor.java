/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import io.firstbridge.cryptolib.dataformat.FBElGamalKeyPair;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author alukin@gmail.com
 */
@Singleton
public class ElGamalEncryptor {

    private byte[] privateKey;
    private byte[] publicKey;
    private FBElGamalKeyPair elGamalKeyPair;
    private TaskDispatchManager taskDispatchManager;

    @Inject
    public ElGamalEncryptor(TaskDispatchManager dispatchManager) {
        taskDispatchManager = dispatchManager;
    }

    @PostConstruct
    public final void init() {
        taskDispatchManager.newBackgroundDispatcher("KeyGenerator")
                .schedule(Task.builder()
                        .name("KeyGenerationTask")
                        .delay(15 * 60 * 1000) // 15 min
                        .task(this::generateKeys)
                        .build());
    }

    private synchronized void generateKeys() {
        byte[] keyBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(keyBytes);
        byte[] keySeed = Crypto.getKeySeed(keyBytes);
        privateKey = Crypto.getPrivateKey(keySeed);
        publicKey = Crypto.getPublicKey(keySeed);
        elGamalKeyPair = Crypto.getElGamalKeyPair();
    }

    public synchronized byte[] getServerPublicKey() {
        return publicKey;
    }

    public synchronized byte[] getServerPrivateKey() {
        return privateKey;
    }

    public synchronized FBElGamalKeyPair getServerElGamalPublicKey() {
        return elGamalKeyPair;
    }

    public synchronized String elGamalDecrypt(String cryptogramm) {
        return Crypto.elGamalDecrypt(cryptogramm, elGamalKeyPair);
    }

}
