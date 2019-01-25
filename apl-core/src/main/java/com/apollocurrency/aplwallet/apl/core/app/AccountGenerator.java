/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

public interface AccountGenerator {
    GeneratedAccount generate(String passphrase);
}
