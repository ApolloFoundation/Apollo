/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.shuffling.dao.ShufflingDataTable;
import com.apollocurrency.aplwallet.apl.core.shuffling.dao.ShufflingParticipantTable;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingData;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;
import org.slf4j.Logger;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShufflingParticipantServiceImpl implements ShufflingParticipantService {
    private static final Logger LOG = getLogger(ShufflingParticipantServiceImpl.class);

    private BlockchainConfig blockchainConfig;
    private Blockchain blockchain;
    private volatile EpochTime timeService;
    private ShufflingParticipantTable shufflingParticipantTable;
    private ShufflingDataTable shufflingDataTable;

    @Inject
    public ShufflingParticipantServiceImpl(BlockchainConfig blockchainConfig, Blockchain blockchain, EpochTime timeService, ShufflingParticipantTable shufflingParticipantTable, ShufflingDataTable shufflingDataTable) {
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.shufflingParticipantTable = shufflingParticipantTable;
        this.shufflingDataTable = shufflingDataTable;
    }


    @Override
    public List<ShufflingParticipant> getParticipants(long shufflingId) {
        return shufflingParticipantTable.getParticipants(shufflingId);
    }

    @Override
    public ShufflingParticipant getParticipant(long shufflingId, long accountId) {
        return shufflingParticipantTable.get(shufflingId, accountId);
    }

    @Override
    public ShufflingParticipant getLastParticipant(long shufflingId) {
        return shufflingParticipantTable.getLast(shufflingId);
    }

    @Override
    public void addParticipant(long shufflingId, long accountId, int index) {
        ShufflingParticipant participant = new ShufflingParticipant(shufflingId, accountId, index, blockchain.getHeight());
        shufflingParticipantTable.insert(participant);
    }

    @Override
    public int getVerifiedCount(long shufflingId) {
        return shufflingParticipantTable.getVerifiedCount(shufflingId);
    }

    @Override
    public void setNextAccountId(ShufflingParticipant participant, long nextAccountId) {
        if (participant.getNextAccountId() != 0) {
            throw new IllegalStateException("nextAccountId already set to " + Long.toUnsignedString(participant.getNextAccountId()));
        }
        participant.setNextAccountId(nextAccountId);
        participant.setHeight(blockchain.getHeight());
        shufflingParticipantTable.insert(participant);
    }

    // caller must update database
    private void setState(ShufflingParticipant participant, ParticipantState state) {
        if (!participant.getState().canBecome(state)) {
            throw new IllegalStateException(String.format("Shuffling participant in state %s cannot go to state %s", participant.getState(), state));
        }
        participant.setState(state);
        LOG.debug("Shuffling participant {} changed state to {}", Long.toUnsignedString(participant.getAccountId()), participant.getState());
    }

    @Override
    public byte[][] getData(ShufflingParticipant participant) {
        return getData(participant.getShufflingId(), participant.getAccountId());
    }

    @Override
    public byte[][] getData(long shufflingId, long accountId) {
        ShufflingData shufflingData = shufflingDataTable.get(shufflingId, accountId);
        return shufflingData != null ? shufflingData.getData() : null;
    }

    @Override
    public void setData(ShufflingParticipant participant, byte[][] data, int timestamp) {
        if (data != null && timeService.getEpochTime() - timestamp < blockchainConfig.getMaxPrunableLifetime() && getData(participant) == null) {
            shufflingDataTable.insert(new ShufflingData(null, blockchain.getHeight(), participant.getShufflingId(), participant.getAccountId(), data, timestamp));
        }
    }

    @Override
    public void restoreData(long shufflingId, long accountId, byte[][] data, int timestamp, int height) {
        if (data != null && getData(shufflingId, accountId) == null) {
            shufflingDataTable.insert(new ShufflingData(null, height, shufflingId, accountId, data, timestamp));
        }
    }

    @Override
    public void cancel(ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds) {
        if (participant.getKeySeeds().length > 0) {
            throw new IllegalStateException("keySeeds already set");
        }
        participant.setBlameData(blameData);
        participant.setKeySeeds(keySeeds);
        setState(participant, ParticipantState.CANCELLED);
        participant.setHeight(blockchain.getHeight());
        shufflingParticipantTable.insert(participant);
    }

    @Override
    public void setProcessed(ShufflingParticipant participant, byte[] dataTransactionFullHash, byte[] dataHash) {
        if (participant.getDataTransactionFullHash() != null) {
            throw new IllegalStateException("dataTransactionFullHash already set");
        }
        setState(participant, ParticipantState.PROCESSED);
        participant.setDataTransactionFullHash(dataTransactionFullHash);
        if (dataHash != null) {
            setDataHash(participant, dataHash);
        }
        participant.setHeight(blockchain.getHeight());
        shufflingParticipantTable.insert(participant);
    }

    private void setDataHash(ShufflingParticipant participant, byte[] dataHash) {
        if (participant.getDataHash() != null) {
            throw new IllegalStateException("data hash already set");
        }
        participant.setDataHash(dataHash);
    }

    @Override
    public ShufflingParticipant getPreviousParticipant(ShufflingParticipant participant) {
        if (participant.getIndex() == 0) {
            return null;
        }
        return shufflingParticipantTable.getByIndex(participant.getShufflingId(), participant.getIndex() - 1);
    }

    @Override
    public void verify(ShufflingParticipant participant) {
        setState(participant, ParticipantState.VERIFIED);
        participant.setHeight(blockchain.getHeight());
        shufflingParticipantTable.insert(participant);
    }

    @Override
    public void delete(ShufflingParticipant participant) {
        participant.setHeight(blockchain.getHeight());
        shufflingParticipantTable.delete(participant);
    }

}
