/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import java.util.Objects;

public class Asset {
    private long assetId;
    private long accountId;
    private String name;
    private String description;
    private long initialQuantityATU;
    private long quantityATU;
    private byte decimals;

    public Asset() {
    }

    @Override
    public String toString() {
        return "Asset{" +
                "assetId=" + assetId +
                ", accountId=" + accountId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", initialQuantityATU=" + initialQuantityATU +
                ", quantityATU=" + quantityATU +
                ", decimals=" + decimals +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset)) return false;
        Asset asset = (Asset) o;
        return assetId == asset.assetId &&
                accountId == asset.accountId &&
                initialQuantityATU == asset.initialQuantityATU &&
                quantityATU == asset.quantityATU &&
                decimals == asset.decimals &&
                Objects.equals(name, asset.name) &&
                Objects.equals(description, asset.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId, accountId, name, description, initialQuantityATU, quantityATU, decimals);
    }

    public long getAssetId() {
        return assetId;
    }

    public void setAssetId(long assetId) {
        this.assetId = assetId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getInitialQuantityATU() {
        return initialQuantityATU;
    }

    public void setInitialQuantityATU(long initialQuantityATU) {
        this.initialQuantityATU = initialQuantityATU;
    }

    public long getQuantityATU() {
        return quantityATU;
    }

    public void setQuantityATU(long quantityATU) {
        this.quantityATU = quantityATU;
    }

    public byte getDecimals() {
        return decimals;
    }

    public void setDecimals(byte decimals) {
        this.decimals = decimals;
    }
}
