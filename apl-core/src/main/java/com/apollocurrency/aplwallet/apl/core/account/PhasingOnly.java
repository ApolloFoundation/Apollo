/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.app.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SetPhasingOnly;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;

/**
 *
 * @author al
 */
public final class PhasingOnly {
    
    private BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

    public static PhasingOnly get(long accountId) {
        return AccountRestrictions.phasingControlTable.getBy(new DbClause.LongClause("account_id", accountId).and(new DbClause.ByteClause("voting_model", DbClause.Op.NE, VoteWeighting.VotingModel.NONE.getCode())));
    }

    public static int getCount() {
        return AccountRestrictions.phasingControlTable.getCount();
    }

    public static DbIterator<PhasingOnly> getAll(int from, int to) {
        return AccountRestrictions.phasingControlTable.getAll(from, to);
    }

    public static void set(Account senderAccount, SetPhasingOnly attachment) {
        PhasingParams phasingParams = attachment.getPhasingParams();
        if (phasingParams.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.NONE) {
            //no voting - remove the control
            senderAccount.removeControl(Account.ControlType.PHASING_ONLY);
            PhasingOnly phasingOnly = get(senderAccount.getId());
            phasingOnly.phasingParams = phasingParams;
            AccountRestrictions.phasingControlTable.delete(phasingOnly);
            unset(senderAccount);
        } else {
            senderAccount.addControl(Account.ControlType.PHASING_ONLY);
            PhasingOnly phasingOnly = get(senderAccount.getId());
            if (phasingOnly == null) {
                phasingOnly = new PhasingOnly(senderAccount.getId(), phasingParams, attachment.getMaxFees(), attachment.getMinDuration(), attachment.getMaxDuration());
            } else {
                phasingOnly.phasingParams = phasingParams;
                phasingOnly.maxFees = attachment.getMaxFees();
                phasingOnly.minDuration = attachment.getMinDuration();
                phasingOnly.maxDuration = attachment.getMaxDuration();
            }
            AccountRestrictions.phasingControlTable.insert(phasingOnly);
        }
    }

    static void unset(Account account) {
        account.removeControl(Account.ControlType.PHASING_ONLY);
        PhasingOnly phasingOnly = get(account.getId());
        AccountRestrictions.phasingControlTable.delete(phasingOnly);
    }
    final DbKey dbKey;
    private final long accountId;
    private PhasingParams phasingParams;
    private long maxFees;
    private short minDuration;
    private short maxDuration;

    
    PhasingOnly(long accountId, PhasingParams params, long maxFees, short minDuration, short maxDuration) {
        this.accountId = accountId;
        dbKey = AccountRestrictions.phasingControlDbKeyFactory.newKey(this.accountId);
        phasingParams = params;
        this.maxFees = maxFees;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    PhasingOnly(ResultSet rs, DbKey dbKey) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.dbKey = dbKey;
        Long[] whitelist = DbUtils.getArray(rs, "whitelist", Long[].class);
        phasingParams = new PhasingParams(rs.getByte("voting_model"), rs.getLong("holding_id"), rs.getLong("quorum"), rs.getLong("min_balance"), rs.getByte("min_balance_model"), whitelist == null ? Convert.EMPTY_LONG : Convert.toArray(whitelist));
        this.maxFees = rs.getLong("max_fees");
        this.minDuration = rs.getShort("min_duration");
        this.maxDuration = rs.getShort("max_duration");
    }

    public long getAccountId() {
        return accountId;
    }

    public PhasingParams getPhasingParams() {
        return phasingParams;
    }

    public long getMaxFees() {
        return maxFees;
    }

    public short getMinDuration() {
        return minDuration;
    }

    public short getMaxDuration() {
        return maxDuration;
    }

    void checkTransaction(Transaction transaction) throws AplException.AccountControlException {
        if (maxFees > 0 && Math.addExact(transaction.getFeeATM(), PhasingPoll.getSenderPhasedTransactionFees(transaction.getSenderId())) > maxFees) {
            throw new AplException.AccountControlException(String.format("Maximum total fees limit of %f %s exceeded", ((double) maxFees) / Constants.ONE_APL, blockchainConfig.getCoinSymbol()));
        }
        if (transaction.getType() == Messaging.PHASING_VOTE_CASTING) {
            return;
        }
        try {
            phasingParams.checkApprovable();
        } catch (AplException.NotCurrentlyValidException e) {
            //LOG.debug("Account control no longer valid: " + e.getMessage());
            return;
        }
        PhasingAppendix phasingAppendix = transaction.getPhasing();
        if (phasingAppendix == null) {
            throw new AplException.AccountControlException("Non-phased transaction when phasing account control is enabled");
        }
        if (!phasingParams.equals(phasingAppendix.getParams())) {
            throw new AplException.AccountControlException("Phasing parameters mismatch phasing account control. Expected: " + phasingParams.toString() + " . Actual: " + phasingAppendix.getParams().toString());
        }
        int duration = phasingAppendix.getFinishHeight() - blockchain.getHeight();
        if ((maxDuration > 0 && duration > maxDuration) || (minDuration > 0 && duration < minDuration)) {
            throw new AplException.AccountControlException("Invalid phasing duration " + duration);
        }
    }

    void save(Connection con) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_control_phasing " + "(account_id, whitelist, voting_model, quorum, min_balance, holding_id, min_balance_model, " + "max_fees, min_duration, max_duration, height, latest) KEY (account_id, height) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.accountId);
            DbUtils.setArrayEmptyToNull(pstmt, ++i, Convert.toArray(phasingParams.getWhitelist()));
            pstmt.setByte(++i, phasingParams.getVoteWeighting().getVotingModel().getCode());
            DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getQuorum());
            DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getVoteWeighting().getMinBalance());
            DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getVoteWeighting().getHoldingId());
            pstmt.setByte(++i, phasingParams.getVoteWeighting().getMinBalanceModel().getCode());
            pstmt.setLong(++i, this.maxFees);
            pstmt.setShort(++i, this.minDuration);
            pstmt.setShort(++i, this.maxDuration);
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }
    
}
