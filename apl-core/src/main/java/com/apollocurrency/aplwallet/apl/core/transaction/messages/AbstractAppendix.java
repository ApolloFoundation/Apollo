/*
 * Copyright © 2018-2019 Apollo Foundation
 */


package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

/**
 *
 */
public abstract class AbstractAppendix implements Appendix {

    private final byte version;

    AbstractAppendix(JSONObject attachmentData) {
        version = ((Long) attachmentData.get("version." + getAppendixName())).byteValue();
    }

    AbstractAppendix(ByteBuffer buffer) {
        version = buffer.get();
    }

    AbstractAppendix(int version) {
        this.version = (byte) version;
    }

    AbstractAppendix() {
        this.version = 1;
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

    abstract int getMySize();

    int getMyFullSize() {
        return getMySize();
    }

    @Override
    public final void putBytes(ByteBuffer buffer) {
        if (version > 0) {
            buffer.put(version);
        }
        putMyBytes(buffer);
    }

    abstract void putMyBytes(ByteBuffer buffer);

    @Override
    public final JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version." + getAppendixName(), version);
        putMyJSON(json);
        return json;
    }

    abstract void putMyJSON(JSONObject json);

    @Override
    public final byte getVersion() {
        return version;
    }

    public boolean verifyVersion() {
        return version == 1;
    }

    @Override
    public int getBaselineFeeHeight() {
        return 1;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return Fee.NONE;
    }

    @Override
    public int getNextFeeHeight() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Fee getNextFee(Transaction transaction) {
        return getBaselineFee(transaction);
    }

    public void validateAtFinish(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (!isPhased(transaction)) {
            return;
        }
        validate(transaction, blockHeight);
    }

    public abstract void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

    public void loadPrunable(Transaction transaction) {
        loadPrunable(transaction, false);
    }

    public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {}

    public abstract boolean isPhasable();

    @Override
    public final boolean isPhased(Transaction transaction) {
        return isPhasable() && transaction.getPhasing() != null;
    }

}
