/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
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

    AbstractAppendix(ByteBuffer buffer) {
        this.version = buffer.get();
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

    @Override
    public final void putBytes(ByteBuffer buffer) {
        if (version > 0) {
            buffer.put(version);
        }
        putMyBytes(buffer);
    }

    public abstract void putMyBytes(ByteBuffer buffer);

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
        validate(transaction, blockHeight);
    }

    public abstract void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

    public abstract boolean isPhasable();

    @Override
    public final boolean isPhased(Transaction transaction) {
        return isPhasable() && transaction.getPhasing() != null;
    }

}
