/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountControlPhasingTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountLedgerTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.alias.AliasOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.alias.AliasTable;
import com.apollocurrency.aplwallet.apl.core.app.Poll;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.app.mint.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.monetary.Exchange;
import com.apollocurrency.aplwallet.apl.core.monetary.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.dao.state.order.AskOrderTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.order.BidOrderTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.TradeTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author silaev-firstbridge
 */
@Singleton
public class TableRegistryInitializer {
    @Inject
    private DatabaseManager databaseManager;
    @Inject
    private PhasingPollTable phasingPollTable;
    @Inject
    private DGSGoodsTable dgsGoodsTable;
    @Inject
    private TransactionProcessor transactionProcessor;
    @Inject
    private DGSTagTable dgsTagTable;
    @Inject
    private DexContractTable dexContractTable;
    @Inject
    private DataTagDao dataTagDao;
    @Inject
    private PhasingPollLinkedTransactionTable phasingPollLinkedTransactionTable;
    @Inject
    private TaggedDataTimestampDao taggedDataTimestampDao;
    @Inject
    private DGSFeedbackTable dgsFeedbackTable;
    @Inject
    private AccountGuaranteedBalanceTable accountGuaranteedBalanceTable;
    @Inject
    private PrunableMessageTable prunableMessageTable;
    @Inject
    private ReferencedTransactionDao referencedTransactionDao;
    @Inject
    private DexOrderTable dexOrderTable;
    @Inject
    private TaggedDataExtendDao taggedDataExtendDao;
    @Inject
    private PhasingVoteTable phasingVoteTable;
    @Inject
    private PhasingApprovedResultTable phasingApprovedResultTable;
    @Inject
    private TaggedDataDao taggedDataDao;
    @Inject
    private DGSPurchaseTable dgsPurchaseTable;
    @Inject
    private PhasingPollVoterTable phasingPollVoterTable;
    @Inject
    private DGSPublicFeedbackTable dgsPublicFeedbackTable;
    @Inject
    private PhasingPollResultTable phasingPollResultTable;
    @Inject
    private AccountTable accountTable;
    @Inject
    private AccountLedgerTable accountLedgerTable;
    @Inject
    private AccountCurrencyTable accountCurrencyTable;
    @Inject
    private AccountLeaseTable accountLeaseTable;
    @Inject
    private AccountAssetTable accountAssetTable;
    @Inject
    private AccountPropertyTable accountPropertyTable;
    @Inject
    private AccountInfoTable accountInfoTable;
    @Inject
    private AliasTable aliasTable;
    @Inject
    private AliasOfferTable aliasOfferTable;
    @Inject
    private AskOrderTable askOrderTable;
    @Inject
    private BidOrderTable bidOrderTable;
    @Inject
    private TradeTable tradeTable;
    @Inject
    private AccountControlPhasingTable accountControlPhasingTable;

    @PostConstruct
    public void init() {
        transactionProcessor.init();

        Poll.init();
        Vote.init();
        Currency.init();
        CurrencyFounder.init();
        CurrencyBuyOffer.init();
        CurrencySellOffer.init();
        CurrencyMint.init();
        CurrencyTransfer.init();
        Exchange.init();
        ExchangeRequest.init();
        Shuffling.init();
        ShufflingParticipant.init();
        CurrencyExchangeOffer.init();
        AssetTransfer.init(databaseManager);
    }
}
