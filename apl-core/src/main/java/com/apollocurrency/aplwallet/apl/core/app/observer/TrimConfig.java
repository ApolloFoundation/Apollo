/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;


import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TrimConfig {
    public static final int DEFAULT_TRIM_DELAY = 2000;
    private final int trimFrequency;
    private final int trimDelay;
    private final int defaultTrimDelay;

    @Inject
    public TrimConfig(@Property(value = "apl.trimProcessingDelay", defaultValue = "500") int trimDelay,
                      @Property(value = "apl.trimFrequency", defaultValue = "1000") int trimFrequency) {
        this(trimDelay, trimFrequency, DEFAULT_TRIM_DELAY);
    }

    public TrimConfig(int trimDelay, int trimFrequency, int defaultTrimDelay) {
        this.trimFrequency = trimFrequency;
        this.trimDelay = trimDelay;
        this.defaultTrimDelay = defaultTrimDelay;
    }

    public int getTrimFrequency() {
        return trimFrequency;
    }

    public int getTrimDelay() {
        return trimDelay;
    }

    public int getDefaultTrimDelay() {
        return defaultTrimDelay;
    }
}
