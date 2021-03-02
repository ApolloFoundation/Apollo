/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;


import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrimConfig {
    private final int trimFrequency;
    private final int trimDelay;

    @Inject
    public TrimConfig(@Property(value = "apl.trimProcessingDelay", defaultValue = "500") int trimDelay,
                      @Property(value = "apl.trimFrequency", defaultValue = "1000") int trimFrequency) {
        this.trimFrequency = trimFrequency;
        this.trimDelay = trimDelay;
    }

    public int getTrimFrequency() {
        return trimFrequency;
    }

    public int getTrimDelay() {
        return trimDelay;
    }
}
