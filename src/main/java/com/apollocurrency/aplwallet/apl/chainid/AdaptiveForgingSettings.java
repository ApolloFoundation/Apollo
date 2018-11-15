/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.chainid;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"enabled", "adaptiveBlockTime", "maxAdaptiveBlockTimeLimit", "minAdaptiveBlockTimeLimit", "numberOfTransactions"})
public class AdaptiveForgingSettings {
    private boolean enabled;
    private int adaptiveBlockTime;
    private int maxAdaptiveBlockTimeLimit;
    private int minAdaptiveBlockTimeLimit;
    private int numberOfTransactions;

    public AdaptiveForgingSettings() {
        this(false, 60, 53, 67, 0);
    }

    public AdaptiveForgingSettings(boolean enabled, int adaptiveBlockTime, int maxAdaptiveBlockTimeLimit, int minAdaptiveBlockTimeLimit, int numberOfTransactions) {
        this.enabled = enabled;
        this.adaptiveBlockTime = adaptiveBlockTime;
        this.maxAdaptiveBlockTimeLimit = maxAdaptiveBlockTimeLimit;
        this.minAdaptiveBlockTimeLimit = minAdaptiveBlockTimeLimit;
        this.numberOfTransactions = numberOfTransactions;
    }

    public int getNumberOfTransactions() {
        return numberOfTransactions;
    }

    public void setNumberOfTransactions(int numberOfTransactions) {
        this.numberOfTransactions = numberOfTransactions;
    }

    public int getAdaptiveBlockTime() {
        return adaptiveBlockTime;
    }

    public void setAdaptiveBlockTime(int adaptiveBlockTime) {
        this.adaptiveBlockTime = adaptiveBlockTime;
    }

    public int getMaxAdaptiveBlockTimeLimit() {
        return maxAdaptiveBlockTimeLimit;
    }

    public void setMaxAdaptiveBlockTimeLimit(int maxAdaptiveBlockTimeLimit) {
        this.maxAdaptiveBlockTimeLimit = maxAdaptiveBlockTimeLimit;
    }

    public int getMinAdaptiveBlockTimeLimit() {
        return minAdaptiveBlockTimeLimit;
    }

    public void setMinAdaptiveBlockTimeLimit(int minAdaptiveBlockTimeLimit) {
        this.minAdaptiveBlockTimeLimit = minAdaptiveBlockTimeLimit;
    }

    public int getEmptyBlockTime() {
        return adaptiveBlockTime;
    }

    public void setEmptyBlockTime(int adaptiveBlockTime) {
        this.adaptiveBlockTime = adaptiveBlockTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AdaptiveForgingSettings{" +
                "enabled=" + enabled +
                ", adaptiveBlockTime=" + adaptiveBlockTime +
                ", maxAdaptiveBlockTimeLimit=" + maxAdaptiveBlockTimeLimit +
                ", minAdaptiveBlockTimeLimit=" + minAdaptiveBlockTimeLimit +
                ", numberOfTransactions=" + numberOfTransactions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdaptiveForgingSettings)) return false;
        AdaptiveForgingSettings that = (AdaptiveForgingSettings) o;
        return enabled == that.enabled &&
                adaptiveBlockTime == that.adaptiveBlockTime &&
                maxAdaptiveBlockTimeLimit == that.maxAdaptiveBlockTimeLimit &&
                minAdaptiveBlockTimeLimit == that.minAdaptiveBlockTimeLimit &&
                numberOfTransactions == that.numberOfTransactions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, adaptiveBlockTime, maxAdaptiveBlockTimeLimit, minAdaptiveBlockTimeLimit, numberOfTransactions);
    }
}
