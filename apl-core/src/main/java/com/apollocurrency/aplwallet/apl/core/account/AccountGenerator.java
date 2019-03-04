/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;

public interface AccountGenerator {
    AplWalletKey generateApl();
    EthWalletKey generateEth();

}
