/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.util.Search;

public class TaggedData {

    private final long id;
    private DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;
    private final String tags;
    private final String[] parsedTags;
    private final byte[] data;
    private final String type;
    private final String channel;
    private final boolean isText;
    private final String filename;
    private int transactionTimestamp;
    private int blockTimestamp;
    private int height;

    public TaggedData(Transaction transaction, TaggedDataAttachment attachment, int blockTimestamp, int height) {
        this.id = transaction.getId();
//        this.dbKey = taggedDataKeyFactory.newKey(this.id);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.tags = attachment.getTags();
        this.parsedTags = Search.parseTags(tags, 3, 20, 5);
        this.data = attachment.getData();
        this.type = attachment.getType();
        this.channel = attachment.getChannel();
        this.isText = attachment.isText();
        this.filename = attachment.getFilename();
        this.blockTimestamp = blockTimestamp;
        this.transactionTimestamp = transaction.getTimestamp();
        this.height = height;
    }

    public TaggedData(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.tags = rs.getString("tags");
        this.parsedTags = DbUtils.getArray(rs, "parsed_tags", String[].class);
        this.data = rs.getBytes("data");
        this.type = rs.getString("type");
        this.channel = rs.getString("channel");
        this.isText = rs.getBoolean("is_text");
        this.filename = rs.getString("filename");
        this.blockTimestamp = rs.getInt("block_timestamp");
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
        this.height = rs.getInt("height");
    }

    public TaggedData(long id, DbKey dbKey, long accountId, String name, String description, String tags, String[] parsedTags, byte[] data, String type, String channel, boolean isText, String filename) {
        this.id = id;
        this.dbKey = dbKey;
        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.parsedTags = parsedTags;
        this.data = data;
        this.type = type;
        this.channel = channel;
        this.isText = isText;
        this.filename = filename;
    }

    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
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

    public String[] getParsedTags() {
        return parsedTags;
    }

    public byte[] getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public boolean isText() {
        return isText;
    }

    public String getFilename() {
        return filename;
    }

    public int getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public void setTransactionTimestamp(int transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public void setBlockTimestamp(int blockTimestamp) {
        this.blockTimestamp = blockTimestamp;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaggedData that = (TaggedData) o;
        return id == that.id &&
                accountId == that.accountId &&
                isText == that.isText &&
                transactionTimestamp == that.transactionTimestamp &&
                blockTimestamp == that.blockTimestamp &&
                height == that.height &&
                Objects.equals(dbKey, that.dbKey) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(tags, that.tags) &&
                Arrays.equals(parsedTags, that.parsedTags) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(type, that.type) &&
                Objects.equals(channel, that.channel) &&
                Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, dbKey, accountId, name, description, tags, type, channel, isText, filename, transactionTimestamp, blockTimestamp, height);
        result = 31 * result + Arrays.hashCode(parsedTags);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

}
