/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class UnencryptedDigitalGoodsDelivery extends DigitalGoodsDelivery implements Encryptable {
    
    final byte[] goodsToEncrypt;
    final byte[] recipientPublicKey;

    public UnencryptedDigitalGoodsDelivery(JSONObject attachmentData) {
        super(attachmentData);
        setGoods(null);
        String goodsToEncryptString = (String) attachmentData.get("goodsToEncrypt");
        this.goodsToEncrypt = goodsIsText() ? Convert.toBytes(goodsToEncryptString) : Convert.parseHexString(goodsToEncryptString);
        this.recipientPublicKey = Convert.parseHexString((String) attachmentData.get("recipientPublicKey"));
    }

    public UnencryptedDigitalGoodsDelivery(long purchaseId, byte[] goodsToEncrypt, boolean goodsIsText, long discountATM, byte[] recipientPublicKey) {
        super(purchaseId, null, goodsIsText, discountATM);
        this.goodsToEncrypt = goodsToEncrypt;
        this.recipientPublicKey = recipientPublicKey;
    }

    @Override
    int getMySize() {
        if (getGoods() == null) {
            return 8 + 4 + EncryptedData.getEncryptedSize(getPlaintext()) + 8;
        }
        return super.getMySize();
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        if (getGoods() == null) {
            throw new AplException.NotYetEncryptedException("Goods not yet encrypted");
        }
        super.putMyBytes(buffer);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        if (getGoods() == null) {
            attachment.put("goodsToEncrypt", goodsIsText() ? Convert.toString(goodsToEncrypt) : Convert.toHexString(goodsToEncrypt));
            attachment.put("recipientPublicKey", Convert.toHexString(recipientPublicKey));
            attachment.put("purchase", Long.toUnsignedString(getPurchaseId()));
            attachment.put("discountATM", getDiscountATM());
            attachment.put("goodsIsText", goodsIsText());
        } else {
            super.putMyJSON(attachment);
        }
    }

    @Override
    public void encrypt(byte[] keySeed) {
        setGoods(EncryptedData.encrypt(getPlaintext(), keySeed, recipientPublicKey));
    }

    @Override
    public int getGoodsDataLength() {
        return EncryptedData.getEncryptedDataLength(getPlaintext());
    }

    private byte[] getPlaintext() {
        return Convert.compress(goodsToEncrypt);
    }
    
}
