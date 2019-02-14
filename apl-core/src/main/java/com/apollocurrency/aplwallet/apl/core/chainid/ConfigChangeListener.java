/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.util.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class ConfigChangeListener implements Listener<Block> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigChangeListener.class);

    private final Set<Integer> targetHeights;
    private final BlockchainConfigUpdater configUpdater;

    public ConfigChangeListener(Set<Integer> targetHeights, BlockchainConfigUpdater configUpdater) {
        this.targetHeights = targetHeights;
        this.configUpdater = configUpdater;
        String stringConstantsChangeHeights =
                targetHeights.stream().map(Object::toString).collect(Collectors.joining(
                        ","));
        LOG.debug("Constants updates at heights: {}",
                stringConstantsChangeHeights.isEmpty() ? "none" : stringConstantsChangeHeights);
    }

    @Override
    public void notify(Block block) {
        int currentHeight = block.getHeight();
        if (targetHeights.contains(currentHeight)) {
            LOG.info("Updating chain config at height {}", currentHeight);
            configUpdater.updateToHeight(currentHeight, true);
            LOG.info("New config applied at height: {}", currentHeight);
        }
    }
}