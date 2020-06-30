/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.app.shuffling.ShufflingData;
import com.apollocurrency.aplwallet.apl.core.app.shuffling.ShufflingParticipantEvent;
import com.apollocurrency.aplwallet.apl.core.app.shuffling.ShufflingParticipantState;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingParticipantTable;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class ShufflingServiceImpl implements ShufflingService {

    private ShufflingDataTable shufflingDataTable;
    private ShufflingParticipantTable participantTable;
    private BlockchainConfig blockchainConfig;
    private TimeService timeService;
    private Blockchain blockchain;

    private static final Listeners<ShufflingParticipant, ShufflingParticipantEvent> listeners = new Listeners<>();

    @Inject
    public ShufflingServiceImpl(ShufflingDataTable shufflingDataTable,
                                ShufflingParticipantTable participantTable,
                                BlockchainConfig blockchainConfig,
                                TimeService timeService,
                                Blockchain blockchain) {
        this.shufflingDataTable = shufflingDataTable;
        this.participantTable = participantTable;
        this.blockchainConfig = blockchainConfig;
        this.timeService = timeService;
        this.blockchain = blockchain;
    }

    @Override
    public byte[][] getData(long shufflingId, long accountId) {
        return shufflingDataTable.getData(shufflingId, accountId);
    }

    @Override
    public void restoreData(long shufflingId, long accountId, byte[][] data, int timestamp, int height) {
        shufflingDataTable.restoreData(shufflingId, accountId, data, timestamp, height);
    }

    @Override
    public void setData(ShufflingParticipant participant, byte[][] data, int timestamp) {
        if (data != null && timeService.getEpochTime() - timestamp < blockchainConfig.getMaxPrunableLifetime() && getData(participant.getShufflingId(), participant.getAccountId()) == null) {
            shufflingDataTable.insert(new ShufflingData(participant.getShufflingId(), participant.getAccountId(), data, timestamp, blockchain.getHeight()));
        }
    }

    @Override
    public DbIterator<ShufflingParticipant> getParticipants(long shufflingId) {
        return participantTable.getParticipants(shufflingId);
    }

    @Override
    public ShufflingParticipant getParticipant(long shufflingId, long accountId) {
        return participantTable.getParticipant(shufflingId, accountId);
    }

    @Override
    public ShufflingParticipant getLastParticipant(long shufflingId) {
        return participantTable.getLastParticipant(shufflingId);
    }

    @Override
    public void addParticipant(long shufflingId, long accountId, int index) {
        ShufflingParticipant participant = new ShufflingParticipant(shufflingId, accountId, index, blockchain.getHeight());
        participantTable.addParticipant(participant);

        listeners.notify(participant, ShufflingParticipantEvent.PARTICIPANT_REGISTERED);
    }

    @Override
    public int getVerifiedCount(long shufflingId) {
        return participantTable.getVerifiedCount(shufflingId);
    }

    @Override
    public void changeStatusToProcessed(ShufflingParticipant participant, byte[] dataTransactionFullHash, byte[] dataHash) {
        if (participant.getDataTransactionFullHash() != null) {
            throw new IllegalStateException("dataTransactionFullHash already set");
        }
        participant.setState(ShufflingParticipantState.PROCESSED);
        participant.setDataTransactionFullHash(dataTransactionFullHash);
        if (dataHash != null) {
            if (participant.getDataHash() != null) {
                throw new IllegalStateException("dataHash already set");
            }
            participant.setDataHash(dataHash);
        }
        participant.setHeight(blockchain.getHeight());
        participantTable.insert(participant);

        listeners.notify(participant, ShufflingParticipantEvent.PARTICIPANT_PROCESSED);
    }

    @Override
    public void changeStatusToVerified(ShufflingParticipant participant) {
        participant.setState(ShufflingParticipantState.VERIFIED);
        participant.setHeight(blockchain.getHeight());
        participantTable.insert(participant);

        listeners.notify(participant, ShufflingParticipantEvent.PARTICIPANT_VERIFIED);
    }

    @Override
    public void changeStatusToCancel(ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds) {
        if (participant.getKeySeeds().length > 0) {
            throw new IllegalStateException("keySeeds already set");
        }
        participant.setBlameData(blameData);
        participant.setKeySeeds(keySeeds);
        participant.setState(ShufflingParticipantState.CANCELLED);
        participant.setHeight(blockchain.getHeight());
        participantTable.insert(participant);

        listeners.notify(participant, ShufflingParticipantEvent.PARTICIPANT_CANCELLED);
    }

    @Override
    public ShufflingParticipant getPreviousParticipant(ShufflingParticipant participant) {
        if (participant.getIndex() == 0) {
            return null;
        }

        return participantTable.getPreviousParticipant(participant);
    }

    @Override
    public boolean delete(ShufflingParticipant participant) {
        return participantTable.deleteAtHeight(participant, blockchain.getHeight());
    }

    @Override
    public void setNextAccountId(ShufflingParticipant participant, long nextAccountId) {
        if (participant.getNextAccountId() != 0) {
            throw new IllegalStateException("nextAccountId already set to " + Long.toUnsignedString(participant.getNextAccountId()));
        }
        participant.setNextAccountId(nextAccountId);
        participant.setHeight(blockchain.getHeight());

        participantTable.insert(participant);
    }


}
