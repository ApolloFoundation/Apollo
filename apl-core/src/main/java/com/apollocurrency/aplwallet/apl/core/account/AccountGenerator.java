/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;

public interface AccountGenerator {
    /**
     * Generate new account with random key.
     * @return AplWallet
     */
    AplWalletKey generateApl();

    /**
     * Generate new account with predefined secret key.
     * @return AplWallet
     */
    AplWalletKey generateApl(byte[] secretBytes);

    /**
     * Generate new account with random key.
     * @return EthWallet
     */
    EthWalletKey generateEth();

}
