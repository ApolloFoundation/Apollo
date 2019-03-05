/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.DigitalGoods;
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
public final class DigitalGoodsListing extends AbstractAttachment {
    
    final String name;
    final String description;
    final String tags;
    final int quantity;
    final long priceATM;

    public DigitalGoodsListing(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.name = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH);
            this.tags = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_TAGS_LENGTH);
            this.quantity = buffer.getInt();
            this.priceATM = buffer.getLong();
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public DigitalGoodsListing(JSONObject attachmentData) {
        super(attachmentData);
        this.name = (String) attachmentData.get("name");
        this.description = (String) attachmentData.get("description");
        this.tags = (String) attachmentData.get("tags");
        this.quantity = ((Long) attachmentData.get("quantity")).intValue();
        this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
    }

    public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceATM) {
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.quantity = quantity;
        this.priceATM = priceATM;
    }

    @Override
    int getMySize() {
        return 2 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 2 + Convert.toBytes(tags).length + 4 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        byte[] nameBytes = Convert.toBytes(name);
        buffer.putShort((short) nameBytes.length);
        buffer.put(nameBytes);
        byte[] descriptionBytes = Convert.toBytes(description);
        buffer.putShort((short) descriptionBytes.length);
        buffer.put(descriptionBytes);
        byte[] tagsBytes = Convert.toBytes(tags);
        buffer.putShort((short) tagsBytes.length);
        buffer.put(tagsBytes);
        buffer.putInt(quantity);
        buffer.putLong(priceATM);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("name", name);
        attachment.put("description", description);
        attachment.put("tags", tags);
        attachment.put("quantity", quantity);
        attachment.put("priceATM", priceATM);
    }

    @Override
    public TransactionType getTransactionType() {
        return DigitalGoods.LISTING;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getPriceATM() {
        return priceATM;
    }
    
}
