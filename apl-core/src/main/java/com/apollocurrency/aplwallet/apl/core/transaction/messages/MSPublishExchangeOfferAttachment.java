/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class MSPublishExchangeOfferAttachment extends AbstractAttachment implements MonetarySystemAttachment {

    final long currencyId;
    final long buyRateATM;
    final long sellRateATM;
    final long totalBuyLimit;
    final long totalSellLimit;
    final long initialBuySupply;
    final long initialSellSupply;
    final int expirationHeight;

    public MSPublishExchangeOfferAttachment(ByteBuffer buffer) {
        super(buffer);
        this.currencyId = buffer.getLong();
        this.buyRateATM = buffer.getLong();
        this.sellRateATM = buffer.getLong();
        this.totalBuyLimit = buffer.getLong();
        this.totalSellLimit = buffer.getLong();
        this.initialBuySupply = buffer.getLong();
        this.initialSellSupply = buffer.getLong();
        this.expirationHeight = buffer.getInt();
    }

    public MSPublishExchangeOfferAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        this.buyRateATM = Convert.parseLong(attachmentData.get("buyRateATM"));
        this.sellRateATM = Convert.parseLong(attachmentData.get("sellRateATM"));
        this.totalBuyLimit = Convert.parseLong(attachmentData.get("totalBuyLimit"));
        this.totalSellLimit = Convert.parseLong(attachmentData.get("totalSellLimit"));
        this.initialBuySupply = Convert.parseLong(attachmentData.get("initialBuySupply"));
        this.initialSellSupply = Convert.parseLong(attachmentData.get("initialSellSupply"));
        this.expirationHeight = ((Number) attachmentData.get("expirationHeight")).intValue();
    }

    public MSPublishExchangeOfferAttachment(long currencyId, long buyRateATM, long sellRateATM, long totalBuyLimit, long totalSellLimit, long initialBuySupply, long initialSellSupply, int expirationHeight) {
        this.currencyId = currencyId;
        this.buyRateATM = buyRateATM;
        this.sellRateATM = sellRateATM;
        this.totalBuyLimit = totalBuyLimit;
        this.totalSellLimit = totalSellLimit;
        this.initialBuySupply = initialBuySupply;
        this.initialSellSupply = initialSellSupply;
        this.expirationHeight = expirationHeight;
    }

    @Override
    public int getMySize() {
        return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(currencyId);
        buffer.putLong(buyRateATM);
        buffer.putLong(sellRateATM);
        buffer.putLong(totalBuyLimit);
        buffer.putLong(totalSellLimit);
        buffer.putLong(initialBuySupply);
        buffer.putLong(initialSellSupply);
        buffer.putInt(expirationHeight);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("currency", Long.toUnsignedString(currencyId));
        attachment.put("buyRateATM", buyRateATM);
        attachment.put("sellRateATM", sellRateATM);
        attachment.put("totalBuyLimit", totalBuyLimit);
        attachment.put("totalSellLimit", totalSellLimit);
        attachment.put("initialBuySupply", initialBuySupply);
        attachment.put("initialSellSupply", initialSellSupply);
        attachment.put("expirationHeight", expirationHeight);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_PUBLISH_EXCHANGE_OFFER;
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }

    public long getBuyRateATM() {
        return buyRateATM;
    }

    public long getSellRateATM() {
        return sellRateATM;
    }

    public long getTotalBuyLimit() {
        return totalBuyLimit;
    }

    public long getTotalSellLimit() {
        return totalSellLimit;
    }

    public long getInitialBuySupply() {
        return initialBuySupply;
    }

    public long getInitialSellSupply() {
        return initialSellSupply;
    }

    public int getExpirationHeight() {
        return expirationHeight;
    }

}
