/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.prunable;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataAttachment;
import com.apollocurrency.aplwallet.apl.util.Search;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

public class TaggedData extends VersionedDerivedEntity {

    private final long id;
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

    public TaggedData(Transaction transaction, TaggedDataAttachment attachment, int blockTimestamp, int height) {
        super(null, height);
        this.id = transaction.getId();
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
        setHeight(height);
    }

    public TaggedData(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        setDbKey(dbKey);
        this.id = rs.getLong("id");
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
    }

    public TaggedData(long id, DbKey dbKey, long accountId, String name, String description, String tags, String[] parsedTags,
                      byte[] data, String type, String channel, boolean isText, String filename) {
        super(null, null);
        setDbKey(dbKey);
        this.id = id;
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

    public void setTransactionTimestamp(int transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    public void setBlockTimestamp(int blockTimestamp) {
        this.blockTimestamp = blockTimestamp;
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
            getHeight() == that.getHeight() &&
            Objects.equals(this.getDbKey(), that.getDbKey()) &&
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
        int result = Objects.hash(id, getDbKey(), accountId, name, description, tags, type, channel, isText, filename,
            transactionTimestamp, blockTimestamp, getHeight());
        result = 31 * result + Arrays.hashCode(parsedTags);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TaggedData{");
        sb.append("id=").append(id);
        sb.append(", dbKey=").append(getDbKey());
        sb.append(", accountId=").append(accountId);
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", tags='").append(tags).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", channel='").append(channel).append('\'');
        sb.append(", isText=").append(isText);
        sb.append(", filename='").append(filename).append('\'');
        sb.append(", transactionTimestamp=").append(transactionTimestamp);
        sb.append(", blockTimestamp=").append(blockTimestamp);
        sb.append(", height=").append(getHeight());
        sb.append(", latest=").append(isLatest());
        sb.append('}');
        return sb.toString();
    }
}
