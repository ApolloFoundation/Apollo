/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.id.handler;

import com.apollocurrency.apl.id.cert.ApolloCertificate;
import io.firstbridge.cryptolib.AsymKeysHolder;
import io.firstbridge.cryptolib.CryptoFactory;
import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.CryptoSignature;
import java.math.BigInteger;
import java.security.KeyPair;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class ThisNodeIdHandlerImpl implements ThisNodeIdHandler {
    BigInteger myApolloId;
    KeyPair myKeys;
    ApolloCertificate myCert;
    CryptoFactory cryptoFactory;
    
    @Override
    public BigInteger getApolloId() {
       return myApolloId;
    }

    @Override
    public ApolloCertificate getCertificate() {
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
    public CertKeyHolder generateSelfSignedCert() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
