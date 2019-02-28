/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import java.util.Objects;

public class ShardingSettings {
    private static final int DEFAULT_SHARDING_FREQUENCY = 500_000;
    private static final boolean DEFAULT_SHARDING_ENABLED = false;

    private boolean enabled;
    private int frequency;

    public ShardingSettings(boolean enabled, int frequency) {
        this.enabled = enabled;
        this.frequency = frequency;
    }

    public ShardingSettings(boolean enabled) {
        this(enabled, DEFAULT_SHARDING_FREQUENCY);
    }

    public ShardingSettings() {
        this(DEFAULT_SHARDING_ENABLED);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getFrequency() {
        return frequency;
    }

    public ShardingSettings copy() {
        return new ShardingSettings(enabled, frequency);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShardingSettings)) return false;
        ShardingSettings that = (ShardingSettings) o;
        return enabled == that.enabled &&
                frequency == that.frequency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, frequency);
    }
}
