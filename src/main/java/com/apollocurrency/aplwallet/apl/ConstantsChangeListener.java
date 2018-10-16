/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.util.Map;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.util.Listener;

public class ConstantsChangeListener implements Listener<Block> {

    private Map<Integer, BlockchainProperties> propertiesMap;
    private Set<Integer> targetHeights;

    public ConstantsChangeListener(Map<Integer, BlockchainProperties> propertiesMap) {
        this.propertiesMap = propertiesMap;
        targetHeights = propertiesMap.keySet();
    }

    @Override
    public void notify(Block block) {
        int currentHeight = block.getHeight();
        if (targetHeights.contains(currentHeight)) {
            Constants.updateConstants(propertiesMap.get(currentHeight));
            targetHeights.remove(currentHeight);
        }
    }
}
