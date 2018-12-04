/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

public interface AccountGenerator {
    GeneratedAccount generate(String passphrase);
}
