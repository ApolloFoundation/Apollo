/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.util.env.config.ChainUtils;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;

import java.util.Map;
import java.util.UUID;
import javax.inject.Singleton;

/**
 * Holds all available chains
 */
@Singleton
public class ChainsConfigHolder {
    private Map<UUID, Chain> chains;

    public ChainsConfigHolder(Map<UUID, Chain> chains) {
        this.chains = chains;
    }

    public ChainsConfigHolder() {
    }

    public Map<UUID, Chain> getChains() {
        return chains;
    }

    public void setChains(Map<UUID, Chain> chains) {
        this.chains = chains;
    }

    public Chain getActiveChain() {
        return ChainUtils.getActiveChain(chains);
    }
}
