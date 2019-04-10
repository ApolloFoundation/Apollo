/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxBlocksDiffCounter {
    private static final Logger log = LoggerFactory.getLogger(MaxBlocksDiffCounter.class);

    private int period;
    private int value;
    private long lastResetTime = System.currentTimeMillis() / (1000 * 60);

    public MaxBlocksDiffCounter(int period) {
        this.period = period;
    }

    public void update(int currentBlockDiff) {
        value = Math.max(value, currentBlockDiff);
        log.info("MAX Blocks diff for last {}h is {} blocks", period, value);
        long currentTime = System.currentTimeMillis() / 1000 / 60;
        if (currentTime - lastResetTime >= period * 60) {
            lastResetTime = currentTime;
            value = currentBlockDiff;
        }
    }
}
