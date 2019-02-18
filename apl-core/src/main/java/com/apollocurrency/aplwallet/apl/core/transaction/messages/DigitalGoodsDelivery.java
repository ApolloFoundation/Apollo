/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.DigitalGoods;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public class DigitalGoodsDelivery extends AbstractAttachment {
    
    final long purchaseId;
    EncryptedData goods;
    final long discountATM;
    final boolean goodsIsText;

    public DigitalGoodsDelivery(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        this.purchaseId = buffer.getLong();
        int length = buffer.getInt();
        goodsIsText = length < 0;
        if (length < 0) {
            length &= Integer.MAX_VALUE;
        }
        try {
            this.goods = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_DGS_GOODS_LENGTH);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
        this.discountATM = buffer.getLong();
    }

    public DigitalGoodsDelivery(JSONObject attachmentData) {
        super(attachmentData);
        this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
        this.goods = new EncryptedData(Convert.parseHexString((String) attachmentData.get("goodsData")), Convert.parseHexString((String) attachmentData.get("goodsNonce")));
        this.discountATM = attachmentData.containsKey("discountATM") ? Convert.parseLong(attachmentData.get("discountATM")) : Convert.parseLong(attachmentData.get("discountNQT"));
        this.goodsIsText = Boolean.TRUE.equals(attachmentData.get("goodsIsText"));
    }

    public DigitalGoodsDelivery(long purchaseId, EncryptedData goods, boolean goodsIsText, long discountATM) {
        this.purchaseId = purchaseId;
        this.goods = goods;
        this.discountATM = discountATM;
        this.goodsIsText = goodsIsText;
    }

    @Override
    int getMySize() {
        return 8 + 4 + goods.getSize() + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(purchaseId);
        buffer.putInt(goodsIsText ? goods.getData().length | Integer.MIN_VALUE : goods.getData().length);
        buffer.put(goods.getData());
        buffer.put(goods.getNonce());
        buffer.putLong(discountATM);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("purchase", Long.toUnsignedString(purchaseId));
        attachment.put("goodsData", Convert.toHexString(goods.getData()));
        attachment.put("goodsNonce", Convert.toHexString(goods.getNonce()));
        attachment.put("discountATM", discountATM);
        attachment.put("goodsIsText", goodsIsText);
    }

    @Override
    public final TransactionType getTransactionType() {
        return DigitalGoods.DELIVERY;
    }

    public final long getPurchaseId() {
        return purchaseId;
    }

    public final EncryptedData getGoods() {
        return goods;
    }

    final void setGoods(EncryptedData goods) {
        this.goods = goods;
    }

    public int getGoodsDataLength() {
        return goods.getData().length;
    }

    public final long getDiscountATM() {
        return discountATM;
    }

    public final boolean goodsIsText() {
        return goodsIsText;
    }
    
}
