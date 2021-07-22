/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.io.WriteBuffer;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 */
@EqualsAndHashCode
public abstract class AbstractAppendix implements Appendix {

    private final byte version;

    AbstractAppendix(JSONObject attachmentData) {
        this.version = ((Number) attachmentData.get("version." + getAppendixName())).byteValue();
    }

    @Deprecated(since = "TransactionV3")
    AbstractAppendix(ByteBuffer buffer) {
        this.version = buffer.get();
    }

    AbstractAppendix(RlpReader reader) {
        this.version = reader.readByte();
    }

    AbstractAppendix(int version) {
        this.version = (byte) version;
    }

    AbstractAppendix() {
        this.version = getVersion() > 0 ? getVersion() : 1;
    }

    public abstract void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

    public abstract void putMyBytes(ByteBuffer buffer);

    public abstract void putMyJSON(JSONObject json);

    public abstract int getMySize();

    public abstract boolean isPhasable();

    public abstract String getAppendixName();

    @Override
    public final int getSize() {
        return getMySize() + (version > 0 ? 1 : 0);
    }

    @Override
    public final int getFullSize() {
        return getMyFullSize() + (version > 0 ? 1 : 0);
    }


    public int getMyFullSize() {
        return getMySize();
    }

    @Override
    public void putBytes(WriteBuffer buffer) {
        int size = getSize();
        if (size > 0) {
            ByteBuffer appBuffer = ByteBuffer.allocate(size);
            appBuffer.order(ByteOrder.LITTLE_ENDIAN);
            putBytes(appBuffer);

            buffer.write(appBuffer.array());
        }
    }

    /**
     * @deprecated use {@link #putBytes(RlpList.RlpListBuilder)}
     */
    @Deprecated(since = "TransactionV3")
    @Override
    public final void putBytes(ByteBuffer buffer) {
        if (version > 0) {
            buffer.put(version);
        }
        putMyBytes(buffer);
    }


    @Override
    public final void putBytes(RlpList.RlpListBuilder builder) {
        RlpList.RlpListBuilder attachment = RlpList.builder()
            .add( getAppendixFlag() )
            .add(version);

        putMyBytes(attachment);

        builder.add(attachment.build());
    }

    public void putMyBytes(RlpList.RlpListBuilder builder){
        throw new UnsupportedOperationException("Unsupported RLP writer for appendix=" + getAppendixName());
    }

    @Override
    public final JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version." + getAppendixName(), getVersion());
        putMyJSON(json);
        return json;
    }


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
        performStateIndependentValidation(transaction, blockHeight);
        performStateDependentValidation(transaction, blockHeight);
    }

    @Override
    public final boolean isPhased(Transaction transaction) {
        return isPhasable() && transaction.getPhasing() != null;
    }

}
