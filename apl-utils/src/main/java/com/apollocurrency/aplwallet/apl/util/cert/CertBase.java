package com.apollocurrency.aplwallet.apl.util.cert;

import io.firstbridge.cryptolib.FBCryptoAsym;
import io.firstbridge.cryptolib.FBCryptoParams;
import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.impl.AsymJCEImpl;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Base class for certificate and CSR
 * @author alukin@gmail.com
 */
public class CertBase {
   protected PublicKey pubKey = null;
   protected PrivateKey pvtKey = null;
    
     public boolean checkKeys(PrivateKey pvtk) {
        boolean res = false;
        try {
            String test = "Lazy Fox jumps ofver snoopy dog";
            FBCryptoAsym ac = new AsymJCEImpl(FBCryptoParams.createDefault());
            ac.setAsymmetricKeys(pubKey, pvtk, pubKey);
            byte[] enc = ac.encryptAsymmetric(test.getBytes());
            byte[] dec = ac.decryptAsymmetric(enc);
            String test_res = new String(dec);           
            res = test.compareTo(test_res)==0;
        } catch (InvalidKeyException | CryptoNotValidException ex) {
        }
        return res;
    }
     
    public void setPrivateKey(PrivateKey pvtKey) {
        this.pvtKey = pvtKey;
    }


    public PublicKey getPublicKey() {
        return pubKey;
    }

    public PrivateKey getPrivateKey() {
        return pvtKey;
    }     
}
