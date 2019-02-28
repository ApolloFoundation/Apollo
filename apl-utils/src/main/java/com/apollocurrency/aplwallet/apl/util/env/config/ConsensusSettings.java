/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"type", "adaptiveForgingSettings"})
public class ConsensusSettings {
    private Type type;
    private AdaptiveForgingSettings adaptiveForgingSettings;

    public ConsensusSettings(Type type, AdaptiveForgingSettings adaptiveForgingSettings) {
        this.type = type == null ? Type.POS : type;
        this.adaptiveForgingSettings = adaptiveForgingSettings == null ? new AdaptiveForgingSettings() : adaptiveForgingSettings;
    }

    public ConsensusSettings(Type type) {
        this(type, null);
    }

    public ConsensusSettings(AdaptiveForgingSettings adaptiveForgingSettings) {
        this(null, adaptiveForgingSettings);
    }

    public ConsensusSettings() {
        this(null, null);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AdaptiveForgingSettings getAdaptiveForgingSettings() {
        return adaptiveForgingSettings;
    }

    public void setAdaptiveForgingSettings(AdaptiveForgingSettings adaptiveForgingSettings) {
        this.adaptiveForgingSettings = adaptiveForgingSettings;
    }

    public enum Type {
        POS
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsensusSettings)) return false;
        ConsensusSettings that = (ConsensusSettings) o;
        return type == that.type &&
                Objects.equals(adaptiveForgingSettings, that.adaptiveForgingSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, adaptiveForgingSettings);
    }

    public ConsensusSettings copy() {
        return new ConsensusSettings(type, adaptiveForgingSettings.copy());
    }
    @Override
    public String toString() {
        return "ConsensusSettings{" +
                "type=" + type +
                ", adaptiveForgingSettings=" + adaptiveForgingSettings +
                '}';
    }

}
