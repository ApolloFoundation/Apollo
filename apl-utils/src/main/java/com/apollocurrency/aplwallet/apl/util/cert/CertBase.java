package com.apollocurrency.aplwallet.apl.util.cert;

import io.firstbridge.cryptolib.AsymCrypto;
import io.firstbridge.cryptolib.AsymKeysHolder;
import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.CryptoParams;
import io.firstbridge.cryptolib.impl.ecc.AsymJCEIESImpl;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Base class for certificate and CSR
 *
 * @author alukin@gmail.com
 */
public class CertBase {

    protected PublicKey pubKey = null;
    protected PrivateKey pvtKey = null;

    public boolean checkKeys(PrivateKey pvtk) {
        boolean res = false;
        try {
            String test = "Lazy Fox jumps ofver snoopy dog";
            AsymCrypto ac = new AsymJCEIESImpl(CryptoParams.createDefault());
            AsymKeysHolder kn = new AsymKeysHolder(pubKey, pvtk, pubKey);
            ac.setKeys(kn);
            byte[] enc = ac.encryp(test.getBytes());
            byte[] dec = ac.decrypt(enc);
            String test_res = new String(dec);
            res = test.compareTo(test_res) == 0;
        } catch (CryptoNotValidException ex) {
        }
        return res;
    }

    public PublicKey getPublicKey() {
        return pubKey;
    }

    public PrivateKey getPrivateKey() {
        return pvtKey;
    }

    public void setPrivateKey(PrivateKey pvtKey) {
        this.pvtKey = pvtKey;
    }
}
