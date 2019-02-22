/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
        this.quantityATU = attachmentData.containsKey("quantityATU") ? Convert.parseLong(attachmentData.get("quantityATU")) : Convert.parseLong(attachmentData.get("quantityQNT"));
        this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
    }

    public ColoredCoinsOrderPlacementAttachment(long assetId, long quantityATU, long priceATM) {
        this.assetId = assetId;
        this.quantityATU = quantityATU;
        this.priceATM = priceATM;
    }

    @Override
    int getMySize() {
        return 8 + 8 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(assetId);
        buffer.putLong(quantityATU);
        buffer.putLong(priceATM);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
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
