/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsListing;
import com.apollocurrency.aplwallet.apl.util.Search;

public class DGSGoods extends VersionedDerivedEntity {
    private final long id;
    private final long sellerId;
    private final String name;
    private final String description;
    private final String tags;
    private final String[] parsedTags;
    private final int timestamp;
    private final boolean hasImage;
    private int quantity;
    private long priceATM;
    private boolean delisted;


    public DGSGoods(Transaction transaction, DigitalGoodsListing attachment, int timestamp) {
        super(null, transaction.getHeight());
        this.id = transaction.getId();
        this.sellerId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.tags = attachment.getTags();
        this.parsedTags = Search.parseTags(this.tags, 3, 20, 3);
        this.quantity = attachment.getQuantity();
        this.priceATM = attachment.getPriceATM();
        this.delisted = false;
        this.timestamp = timestamp;//blockchain.getLastBlockTimestamp();
        this.hasImage = transaction.getPrunablePlainMessage() != null;
    }


    public DGSGoods(Long dbId, Integer height, long id, long sellerId, String name, String description, String tags, String[] parsedTags, int timestamp, boolean hasImage, int quantity, long priceATM, boolean delisted) {
        super(dbId, height);
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.parsedTags = parsedTags;
        this.timestamp = timestamp;
        this.hasImage = hasImage;
        this.quantity = quantity;
        this.priceATM = priceATM;
        this.delisted = delisted;
    }

    public long getId() {
        return id;
    }

    public long getSellerId() {
        return sellerId;
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

    public int getTimestamp() {
        return timestamp;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPriceATM(long priceATM) {
        this.priceATM = priceATM;
    }

    public long getPriceATM() {
        return priceATM;
    }

    public boolean isDelisted() {
        return delisted;
    }

    public void setDelisted(boolean delisted) {
        this.delisted = delisted;
    }

    public String[] getParsedTags() {
        return parsedTags;
    }

    public boolean hasImage() {
        return hasImage;
    }
}
