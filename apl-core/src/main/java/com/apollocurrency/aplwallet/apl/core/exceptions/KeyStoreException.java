 /*
  * Copyright © 2019 Apollo Foundation
  */

package com.apollocurrency.aplwallet.apl.core.exceptions;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;

public class KeyStoreException extends Exception{

    public KeyStoreService.Status status;

    public KeyStoreException(KeyStoreService.Status status) {
        super(status.message);
        this.status = status;
    }

    public KeyStoreException(Throwable throwable,  KeyStoreService.Status status) {
        super(throwable.getMessage(), throwable);
        this.status = status;
    }
}
