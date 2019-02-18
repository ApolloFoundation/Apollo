/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

public interface AccountGenerator {
    GeneratedAccount generate(String passphrase);
}
