/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dgs.SellerDbClause;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsListing;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Search;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DGSGoods {
        private final long id;
        private DbKey dbKey;
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
    private int height;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public DGSGoods(Transaction transaction, DigitalGoodsListing attachment, int timestamp) {
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

    public DbKey getDbKey() {
        return dbKey;
    }

    public DGSGoods(long id, DbKey dbKey, long sellerId, String name, String description, String tags, String[] parsedTags, int timestamp, boolean hasImage, int quantity, long priceATM, boolean delisted, int height) {
        this.id = id;
        this.dbKey = dbKey;
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
        this.height = height;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
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

        public void changeQuantity(int deltaQuantity) {
            if (quantity == 0 && deltaQuantity > 0) {
                Tag.add(this);
            }
            quantity += deltaQuantity;
            if (quantity < 0) {
                quantity = 0;
            } else if (quantity > Constants.MAX_DGS_LISTING_QUANTITY) {
                quantity = Constants.MAX_DGS_LISTING_QUANTITY;
            }
            if (quantity == 0) {
                Tag.delist(this);
            }
            goodsTable.insert(this);
        }

        public long getPriceATM() {
            return priceATM;
        }

        private void changePrice(long priceATM) {
            this.priceATM = priceATM;
            goodsTable.insert(this);
        }

        public boolean isDelisted() {
            return delisted;
        }

        private void setDelisted(boolean delisted) {
            this.delisted = delisted;
            if (this.quantity > 0) {
                Tag.delist(this);
            }
            goodsTable.insert(this);
        }

        public String[] getParsedTags() {
            return parsedTags;
        }

        public boolean hasImage() {
            return hasImage;
        }

    }
}
