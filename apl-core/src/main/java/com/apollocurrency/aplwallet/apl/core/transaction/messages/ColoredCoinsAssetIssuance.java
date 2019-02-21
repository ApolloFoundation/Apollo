/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.ColoredCoins;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class ColoredCoinsAssetIssuance extends AbstractAttachment {
    
    final String name;
    final String description;
    final long quantityATU;
    final byte decimals;

    public ColoredCoinsAssetIssuance(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
            this.quantityATU = buffer.getLong();
            this.decimals = buffer.get();
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public ColoredCoinsAssetIssuance(JSONObject attachmentData) {
        super(attachmentData);
        this.name = (String) attachmentData.get("name");
        this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
        this.quantityATU = attachmentData.containsKey("quantityATU") ? Convert.parseLong(attachmentData.get("quantityATU")) : Convert.parseLong(attachmentData.get("quantityQNT"));
        this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
    }

    public ColoredCoinsAssetIssuance(String name, String description, long quantityATU, byte decimals) {
        this.name = name;
        this.description = Convert.nullToEmpty(description);
        this.quantityATU = quantityATU;
        this.decimals = decimals;
    }

    @Override
    int getMySize() {
        return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        byte[] name = Convert.toBytes(this.name);
        byte[] description = Convert.toBytes(this.description);
        buffer.put((byte) name.length);
        buffer.put(name);
        buffer.putShort((short) description.length);
        buffer.put(description);
        buffer.putLong(quantityATU);
        buffer.put(decimals);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("name", name);
        attachment.put("description", description);
        attachment.put("quantityATU", quantityATU);
        attachment.put("decimals", decimals);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoins.ASSET_ISSUANCE;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getQuantityATU() {
        return quantityATU;
    }

    public byte getDecimals() {
        return decimals;
    }
    
}
