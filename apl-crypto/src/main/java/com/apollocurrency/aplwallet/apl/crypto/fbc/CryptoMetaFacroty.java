package com.apollocurrency.aplwallet.apl.crypto.fbc;

import io.firstbridge.cryptolib.FBCryptoFactory;
import io.firstbridge.cryptolib.FBCryptoParams;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

/**
 * Purpose of this meta-factory is to create crypto factory
 * with set of parameters consistent for certain crypto system.
 * All we need to guess parameters is X.509 certificate or public key
 * @author alukin@gmail.com
 */
public class CryptoMetaFacroty {
   public static FBCryptoFactory createFacrory(PublicKey pubKey){
       FBCryptoParams params;
       String algo = pubKey.getAlgorithm();
       if("RSA".equalsIgnoreCase(algo)){
           RSAPublicKey rpk = (RSAPublicKey)pubKey;
           int bitLength = rpk.getModulus().bitLength();
           params = FBCryptoParams.createRSAn(bitLength);
       }else if("EC".equalsIgnoreCase(algo)){
           params=FBCryptoParams.createDefault();
       }else{
           params=FBCryptoParams.createDefault();           
       }
       return FBCryptoFactory.create(params);
   } 
   
   public static FBCryptoFactory createFacrory(X509Certificate cert){
       PublicKey pk = cert.getPublicKey();
       return createFacrory(pk);
   }    
}
