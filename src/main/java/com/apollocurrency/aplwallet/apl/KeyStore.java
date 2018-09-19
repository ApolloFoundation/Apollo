package com.apollocurrency.aplwallet.apl;

import java.io.IOException;

public interface KeyStore {
    byte[] getPrivateKey(String passphrase, long accountId) throws IOException;

    void savePrivateKey(String passphrase, byte[] privateKey) throws IOException;
}
