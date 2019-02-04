package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChainUtils {

    public static Chain getActiveChain(Map<UUID, Chain> chains) {
        Objects.requireNonNull(chains, "Chains cannot be null");
        List<Chain> activeChains = chains.values().stream().filter(Chain::isActive).collect(Collectors.toList());
        if (activeChains.isEmpty()) {
            throw new RuntimeException("No active chain found");
        }
        if (activeChains.size() > 1) {
            throw new RuntimeException("Only one active chain should exist. Found " + activeChains.size() + " active chains.");
        }
        return activeChains.get(0);
    }
}
