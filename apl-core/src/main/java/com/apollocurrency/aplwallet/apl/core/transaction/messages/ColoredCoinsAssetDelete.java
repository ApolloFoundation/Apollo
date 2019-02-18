/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.ColoredCoins;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class ColoredCoinsAssetDelete extends AbstractAttachment {
    
    final long assetId;
    final long quantityATU;

    public ColoredCoinsAssetDelete(ByteBuffer buffer) {
        super(buffer);
        this.assetId = buffer.getLong();
        this.quantityATU = buffer.getLong();
    }

    public ColoredCoinsAssetDelete(JSONObject attachmentData) {
        super(attachmentData);
        this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.quantityATU = attachmentData.containsKey("quantityATU") ? Convert.parseLong(attachmentData.get("quantityATU")) : Convert.parseLong(attachmentData.get("quantityQNT"));
        ;
    }

    public ColoredCoinsAssetDelete(long assetId, long quantityATU) {
        this.assetId = assetId;
        this.quantityATU = quantityATU;
    }

    @Override
    int getMySize() {
        return 8 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(assetId);
        buffer.putLong(quantityATU);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("asset", Long.toUnsignedString(assetId));
        attachment.put("quantityATU", quantityATU);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoins.ASSET_DELETE;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getQuantityATU() {
        return quantityATU;
    }
    
}
