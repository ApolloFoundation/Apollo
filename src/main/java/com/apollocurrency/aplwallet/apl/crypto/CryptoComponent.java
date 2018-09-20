package com.apollocurrency.aplwallet.apl.crypto;


import com.apollocurrency.aplwallet.apl.crypto.asymmetric.AsymmetricKeyGenerator;
import com.apollocurrency.aplwallet.apl.crypto.asymmetric.PublicKeyEncoder;
import com.apollocurrency.aplwallet.apl.crypto.asymmetric.SharedKeyCalculator;
import com.apollocurrency.aplwallet.apl.crypto.asymmetric.signature.Signer;
import com.apollocurrency.aplwallet.apl.crypto.legacy.KeyGenerator;

/**
 * Class to configure crypto-components
 * Add cryptography related components here
 */
public class CryptoComponent {

    private static final PublicKeyEncoder PUBLIC_KEY_ENCODER_INSTANCE = new com.apollocurrency.aplwallet.apl.crypto.legacy.PublicKeyEncoder();
    private static final AsymmetricKeyGenerator ASYMMETRIC_KEY_GENERATOR = new KeyGenerator();
    private static final Signer SIGNER = new com.apollocurrency.aplwallet.apl.crypto.legacy.Signer();
    private static final SharedKeyCalculator SHARED_KEY_CALCULATOR = new com.apollocurrency.aplwallet.apl.crypto.legacy.SharedKeyCalculator();
    private static final DigestCalculator DIGEST_CALCULATOR = new com.apollocurrency.aplwallet.apl.crypto.legacy.DigestCalculator();

    public static PublicKeyEncoder getPublicKeyEncoder() {
        return PUBLIC_KEY_ENCODER_INSTANCE;
    }

    public static AsymmetricKeyGenerator getKeyGenerator() {
        return ASYMMETRIC_KEY_GENERATOR;
    }

    public static Signer getSigner() {
        return SIGNER;
    }

    public static SharedKeyCalculator getSharedKeyCalculator() {
        return SHARED_KEY_CALCULATOR;
    }

    public static DigestCalculator getDigestCalculator() {
        return DIGEST_CALCULATOR;
    }

}
