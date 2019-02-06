package com.apollocurrency.aplwallet.apl.crypto.fbc;

import io.firstbridge.cryptolib.FBCryptoFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Purpose of this meta-factory is to create crypto factory
 * with set of parameters consistent for certain crypto system.
 * All we need to guess parameters is X.509 certificate or public key
 * @author alukin@gmail.com
 */
public class CryptoMetaFacroty {
   public FBCryptoFactory createFacrory(PublicKey pubKey){
       return null;
   } 
   public FBCryptoFactory createFacrory(X509Certificate cert){
       return null;
   }    
}
