/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.chainid;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"enabled", "emptyBlockTime"})
public class AdaptiveForgingSettings {
    private boolean enabled;
    private int emptyBlockTime;

    public AdaptiveForgingSettings() {
        this(false, 60);
    }

    public AdaptiveForgingSettings(boolean enabled, int emptyBlockTime) {
        this.enabled = enabled;
        this.emptyBlockTime = emptyBlockTime;
    }

    public int getEmptyBlockTime() {
        return emptyBlockTime;
    }

    public void setEmptyBlockTime(int emptyBlockTime) {
        this.emptyBlockTime = emptyBlockTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdaptiveForgingSettings)) return false;
        AdaptiveForgingSettings that = (AdaptiveForgingSettings) o;
        return enabled == that.enabled &&
                emptyBlockTime == that.emptyBlockTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, emptyBlockTime);
    }

    @Override
    public String toString() {
        return "AdaptiveForgingSettings{" +
                "enabled=" + enabled +
                ", emptyBlockTime=" + emptyBlockTime +
                '}';
    }
}
