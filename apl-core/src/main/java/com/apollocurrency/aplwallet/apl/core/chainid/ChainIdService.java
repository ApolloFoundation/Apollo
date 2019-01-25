/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import java.io.IOException;
import java.util.List;
/**
 * Extract all available chains and provide current active chain
 * @see Chain
 */
public interface ChainIdService {
    /**
     * Retrieve current active chain.
     * @return current active chain
     * @throws IOException when IO error occurred
     */
    Chain getActiveChain() throws IOException;

    /**
     * Retrive all available chains including active and not active
     * @return list of available chains
     * @throws IOException whein IO error occurred
     */
    List<Chain> getAll() throws IOException;
}
