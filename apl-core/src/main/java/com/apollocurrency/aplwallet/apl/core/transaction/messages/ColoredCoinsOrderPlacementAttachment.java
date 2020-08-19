/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public abstract class ColoredCoinsOrderPlacementAttachment extends AbstractAttachment {

    final long assetId;
    final long quantityATU;
    final long priceATM;

    public ColoredCoinsOrderPlacementAttachment(ByteBuffer buffer) {
        super(buffer);
        this.assetId = buffer.getLong();
        this.quantityATU = buffer.getLong();
        this.priceATM = buffer.getLong();
    }

    public ColoredCoinsOrderPlacementAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.quantityATU = Convert.parseLong(attachmentData.get("quantityATU"));
        this.priceATM = Convert.parseLong(attachmentData.get("priceATM"));
    }

    public ColoredCoinsOrderPlacementAttachment(long assetId, long quantityATU, long priceATM) {
        this.assetId = assetId;
        this.quantityATU = quantityATU;
        this.priceATM = priceATM;
    }

    @Override
    public int getMySize() {
        return 8 + 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(assetId);
        buffer.putLong(quantityATU);
        buffer.putLong(priceATM);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("asset", Long.toUnsignedString(assetId));
        attachment.put("quantityATU", quantityATU);
        attachment.put("priceATM", priceATM);
    }

    public long getAssetId() {
        return assetId;
    }

    public long getQuantityATU() {
        return quantityATU;
    }

    public long getPriceATM() {
        return priceATM;
    }

}
