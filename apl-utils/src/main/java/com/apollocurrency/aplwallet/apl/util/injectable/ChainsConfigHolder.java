/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainUtils;

import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;

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
