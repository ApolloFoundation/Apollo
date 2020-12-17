/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpWriteBuffer;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 *
 */
@EqualsAndHashCode
public abstract class AbstractAppendix implements Appendix {

    private final byte version;

    AbstractAppendix(JSONObject attachmentData) {
        this.version = ((Number) attachmentData.get("version." + getAppendixName())).byteValue();
    }

    @Deprecated
    AbstractAppendix(ByteBuffer buffer) {
        this.version = buffer.get();
    }

    AbstractAppendix(RlpReader reader) {
        this.version = reader.read()[0];
    }

    AbstractAppendix(int version) {
        this.version = (byte) version;
    }

    AbstractAppendix() {
        this.version = getVersion() > 0 ? getVersion() : 1;
    }

    public abstract String getAppendixName();

    @Override
    public final int getSize() {
        return getMySize() + (version > 0 ? 1 : 0);
    }

    @Override
    public final int getFullSize() {
        return getMyFullSize() + (version > 0 ? 1 : 0);
    }

    public abstract int getMySize();

    public int getMyFullSize() {
        return getMySize();
    }

    /**
     * @deprecated use {@link #putBytes(RlpWriteBuffer)}
     */
    @Deprecated(since = "TransactionV3")
    @Override
    public final void putBytes(ByteBuffer buffer) {
        if (version > 0) {
            buffer.put(version);
        }
        putMyBytes(buffer);
    }

    /**
     * @deprecated use {@link #putMyBytes(RlpWriteBuffer)}
     */
    @Deprecated(since = "TransactionV3")
    public abstract void putMyBytes(ByteBuffer buffer);

    @Override
    public final void putBytes(RlpWriteBuffer buffer) {
        buffer.write(version);

        putMyBytes(buffer);
    }

    public void putMyBytes(RlpWriteBuffer buffer){
        throw new UnsupportedOperationException("Unsupported RLP writer for appendix=" + getAppendixName());
    }

    @Override
    public final JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version." + getAppendixName(), getVersion());
        putMyJSON(json);
        return json;
    }

    public abstract void putMyJSON(JSONObject json);

    @Override
    public byte getVersion() {
        return version;
    }

    public boolean verifyVersion() {
        return version == 1;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction, long oneAPL) {
        return Fee.NONE;
    }

    public void validateAtFinish(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (!isPhased(transaction)) {
            return;
        }
        performFullValidation(transaction, blockHeight);
    }

    @Override
    public final boolean isPhased(Transaction transaction) {
        return isPhasable() && transaction.getPhasing() != null;
    }

}
