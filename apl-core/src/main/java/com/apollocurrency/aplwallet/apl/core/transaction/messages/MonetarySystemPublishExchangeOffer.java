/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MonetarySystemPublishExchangeOffer extends AbstractAttachment implements MonetarySystemAttachment {
    
    final long currencyId;
    final long buyRateATM;
    final long sellRateATM;
    final long totalBuyLimit;
    final long totalSellLimit;
    final long initialBuySupply;
    final long initialSellSupply;
    final int expirationHeight;

    public MonetarySystemPublishExchangeOffer(ByteBuffer buffer) {
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

    public MonetarySystemPublishExchangeOffer(JSONObject attachmentData) {
        super(attachmentData);
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        this.buyRateATM = attachmentData.containsKey("buyRateATM") ? Convert.parseLong(attachmentData.get("buyRateATM")) : Convert.parseLong(attachmentData.get("buyRateNQT"));
        this.sellRateATM = attachmentData.containsKey("sellRateATM") ? Convert.parseLong(attachmentData.get("sellRateATM")) : Convert.parseLong(attachmentData.get("sellRateNQT"));
        this.totalBuyLimit = Convert.parseLong(attachmentData.get("totalBuyLimit"));
        this.totalSellLimit = Convert.parseLong(attachmentData.get("totalSellLimit"));
        this.initialBuySupply = Convert.parseLong(attachmentData.get("initialBuySupply"));
        this.initialSellSupply = Convert.parseLong(attachmentData.get("initialSellSupply"));
        this.expirationHeight = ((Long) attachmentData.get("expirationHeight")).intValue();
    }

    public MonetarySystemPublishExchangeOffer(long currencyId, long buyRateATM, long sellRateATM, long totalBuyLimit, long totalSellLimit, long initialBuySupply, long initialSellSupply, int expirationHeight) {
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
    int getMySize() {
        return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
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
    void putMyJSON(JSONObject attachment) {
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
    public TransactionType getTransactionType() {
        return MonetarySystem.PUBLISH_EXCHANGE_OFFER;
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
