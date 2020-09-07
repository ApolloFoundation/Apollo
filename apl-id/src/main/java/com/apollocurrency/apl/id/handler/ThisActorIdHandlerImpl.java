/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ExtCSR;
import com.apollocurrency.apl.id.cert.CertException;
import com.apollocurrency.apl.id.cert.CertKeyPersistence;
import com.apollocurrency.apl.id.cert.ExtCert;
import io.firstbridge.cryptolib.AsymKeysHolder;
import io.firstbridge.cryptolib.CryptoFactory;
import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.CryptoSignature;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class ThisActorIdHandlerImpl implements ThisActorIdHandler {
    BigInteger myApolloId;
    KeyPair myKeys;
    ExtCert myCert;
    CryptoFactory cryptoFactory;
    
    @Override
    public BigInteger getActorId() {
       return myApolloId;
    }

    @Override
    public ExtCert getCertHelper() {
        return myCert;
    }

    @Override
    public byte[] sign(byte[] message) {
        byte[] res = null;
        CryptoSignature signer = cryptoFactory.getCryptoSiganture();
        AsymKeysHolder kh = new AsymKeysHolder(myKeys.getPublic(), myKeys.getPrivate(), null);
        signer.setKeys(kh);
        try {
            res = signer.sign(message);
        } catch (CryptoNotValidException ex) {
            log.error("Can not sign message with my node private key", ex);
        }
        return res;
    }

    @Override
    public void generateSelfSignedCert() {
        Properties prop = new Properties();
        ExtCSR csr = new ExtCSR();
     //   csr.
    }

    @Override
    public boolean loadCertAndKey(Path baseDir) {
        boolean res = false;
        try {
            myCert = CertKeyPersistence.loadPEMFromPath(baseDir);
            res=true;
        } catch (CertException ex) {            
        } catch (IOException ex) {         
        }
        if(myCert==null){
            generateSelfSignedCert();            
        }
        return res;
    }

    @Override
    public boolean saveAll(Path baseDir) {
        boolean res=false;
        //TODO: implement
        return res;
    }
    
    
}
