/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"enabled", "adaptiveBlockTime", "maxAdaptiveBlockTimeLimit", "minAdaptiveBlockTimeLimit", "numberOfTransactions"})
public class AdaptiveForgingSettings {
    private static final boolean DEFAULT_ENABLED = false;
    private static final int DEFAULT_ADAPTIVE_BLOCK_TIME = 60;
    private static final int DEFAULT_NUMBER_OF_TRANSACTIONS = 0;
    private boolean enabled;
    private int adaptiveBlockTime;
    private int numberOfTransactions;

    @JsonCreator
    public AdaptiveForgingSettings() {
        this(DEFAULT_ENABLED, DEFAULT_ADAPTIVE_BLOCK_TIME, DEFAULT_NUMBER_OF_TRANSACTIONS);
    }

    public AdaptiveForgingSettings(boolean enabled, int adaptiveBlockTime, int numberOfTransactions) {
        this.enabled = enabled;
        this.adaptiveBlockTime = adaptiveBlockTime;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AdaptiveForgingSettings copy() {
        return new AdaptiveForgingSettings(enabled, adaptiveBlockTime, numberOfTransactions);
    }

    @Override
    public String toString() {
        return "AdaptiveForgingSettings{" +
                "enabled=" + enabled +
                ", adaptiveBlockTime=" + adaptiveBlockTime +
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
                numberOfTransactions == that.numberOfTransactions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, adaptiveBlockTime, numberOfTransactions);
    }
}
