/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"type", "adaptiveForgingSettings"})
public class Consensus {
    private Type type;
    private AdaptiveForgingSettings adaptiveForgingSettings;

    public Consensus(Type type, AdaptiveForgingSettings adaptiveForgingSettings) {
        this.type = type;
        this.adaptiveForgingSettings = adaptiveForgingSettings;
    }

    public Consensus() {
        this(Type.POS, new AdaptiveForgingSettings());
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
    public String toString() {
        return "Consensus{" +
                "type=" + type +
                ", adaptiveForgingSettings=" + adaptiveForgingSettings +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Consensus)) return false;
        Consensus consensus = (Consensus) o;
        return type == consensus.type &&
                Objects.equals(adaptiveForgingSettings, consensus.adaptiveForgingSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, adaptiveForgingSettings);
    }
}
