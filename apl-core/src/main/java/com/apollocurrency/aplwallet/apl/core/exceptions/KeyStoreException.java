 /*
  * Copyright © 2019 Apollo Foundation
  */

package com.apollocurrency.aplwallet.apl.core.exceptions;

import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStore;

public class KeyStoreException extends Exception{

    public VaultKeyStore.Status status;

    public KeyStoreException(VaultKeyStore.Status status) {
        super(status.message);
        this.status = status;
    }

    public KeyStoreException(Throwable throwable,  VaultKeyStore.Status status) {
        super(throwable.getMessage(), throwable);
        this.status = status;
    }
}
