
/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import io.firstbridge.cryptolib.dataformat.FBElGamalKeyPair;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;

/**
 *
 * @author alukin@gmail.com
 */
@Singleton
public class ElGamalEncryptor {

    private byte[] privateKey;
    private byte[] publicKey;
    private FBElGamalKeyPair elGamalKeyPair;
    //TODO: well, it is just "fuse", may be it should be removed
    private volatile static boolean threadStarted = false;
    
    public ElGamalEncryptor() {
    }
    
    @PostConstruct
    public final void init() {
      if(!threadStarted){  
        serverKeysGenerator.setDaemon(true);
        serverKeysGenerator.start();
        threadStarted = true;
      }
    }
//TODO: remove this as soon as Al Gamal is ready!    
    private  Thread serverKeysGenerator = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (API.class) {
                byte[] keyBytes = new byte[32];
                Crypto.getSecureRandom().nextBytes(keyBytes);
                byte[] keySeed = Crypto.getKeySeed(keyBytes);
                privateKey = Crypto.getPrivateKey(keySeed);
                publicKey = Crypto.getPublicKey(keySeed);

                elGamalKeyPair = Crypto.getElGamalKeyPair();

            }
            try {
                TimeUnit.MINUTES.sleep(15);
            } catch (InterruptedException e) {
                return;
            }
        }
    });

    public synchronized byte[] getServerPublicKey() {
        return publicKey;
    }

    public synchronized byte[] getServerPrivateKey() {
        return privateKey;
    }

    public synchronized FBElGamalKeyPair getServerElGamalPublicKey() {
        return elGamalKeyPair;
    }

    public String elGamalDecrypt(String cryptogramm) {
        return Crypto.elGamalDecrypt(cryptogramm, elGamalKeyPair);
    }

}
