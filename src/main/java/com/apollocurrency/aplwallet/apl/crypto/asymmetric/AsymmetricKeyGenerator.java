package com.apollocurrency.aplwallet.apl.crypto.asymmetric;

import java.security.KeyPair;

/**
 * Interface for AsymmetricKeyGenerator component
 */
public interface AsymmetricKeyGenerator {

    KeyPair generateKeyPair(String secretPhrase);

}
