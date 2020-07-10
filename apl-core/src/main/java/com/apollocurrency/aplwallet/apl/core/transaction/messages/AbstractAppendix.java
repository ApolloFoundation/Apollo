/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;

/**
 *
 */
public abstract class AbstractAppendix implements Appendix {

    private static Blockchain blockchain;// = CDI.current().select(Blockchain.class).get();
    private static PhasingPollService phasingPollService;// = CDI.current().select(PhasingPollService.class).get();
    private static BlockchainConfig blockchainConfig;// = CDI.current().select(BlockchainConfig.class).get();
    private static volatile TimeService timeService;// = CDI.current().select(TimeService.class).get();
    private static PrunableMessageService messageService;// = CDI.current().select(PrunableMessageService.class).get();
    private static ShufflingService shufflingService;

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

    public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
    }

    public abstract boolean isPhasable();

    @Override
    public final boolean isPhased(Transaction transaction) {
        return isPhasable() && transaction.getPhasing() != null;
    }

    Blockchain lookupBlockchain() {
        if (blockchain == null) {
            blockchain = CDI.current().select(BlockchainImpl.class).get();
        }
        return blockchain;
    }

    public BlockchainConfig lookupBlockchainConfig(){
        if ( blockchainConfig == null) {
            blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        }
        return blockchainConfig;
    }

    public static PhasingPollService lookupPhasingPollService(){
        if ( phasingPollService == null) {
            phasingPollService = CDI.current().select(PhasingPollService.class).get();
        }
        return phasingPollService;
    }

    public static TimeService lookupTimeService(){
        if ( timeService == null) {
            timeService = CDI.current().select(TimeService.class).get();
        }
        return timeService;
    }

    public static PrunableMessageService lookupMessageService(){
        if ( messageService == null) {
            messageService = CDI.current().select(PrunableMessageService.class).get();
        }
        return messageService;
    }

    public static ShufflingService lookupShufflingService(){
        if ( shufflingService == null) {
            shufflingService = CDI.current().select(ShufflingService.class).get();
        }
        return shufflingService;
    }

}
