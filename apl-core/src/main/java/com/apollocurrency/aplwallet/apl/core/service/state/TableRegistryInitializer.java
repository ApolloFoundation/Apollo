/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.TradeTable;
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
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetDeleteTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetDividendTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTransferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyBuyOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyFounderTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyMintTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySellOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTransferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeRequestTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.order.AskOrderTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.order.BidOrderTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.VoteTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingParticipantTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
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
    private AccountControlPhasingTable accountControlPhasingTable;
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
    private PollTable pollTable;
    @Inject
    private PollResultTable pollResultTable;
    @Inject
    private CurrencyBuyOfferTable currencyBuyOfferTable;
    @Inject
    private CurrencySellOfferTable currencySellOfferTable;
    @Inject
    private CurrencyMintTable currencyMintTable;
    @Inject
    private CurrencyTransferTable currencyTransferTable;
    @Inject
    private ExchangeTable exchangeTable;
    @Inject
    private ExchangeRequestTable exchangeRequestTable;
    @Inject
    private AssetTable assetTable;
    @Inject
    private AssetDeleteTable assetDeleteTable;
    @Inject
    private AssetDividendTable assetDividendTable;
    @Inject
    private AssetTransferTable assetTransferTable;
    @Inject
    private VoteTable voteTable;
    @Inject
    private CurrencyTable currencyTable;
    @Inject
    private CurrencySupplyTable currencySupplyTable;
    @Inject
    private CurrencyFounderTable currencyFounderTable;
    @Inject
    private ShufflingParticipantTable participantTable;
    @Inject
    private ShufflingDataTable shufflingDataTable;
    @Inject
    private ShufflingTable shufflingTable;
    @Inject
    private UnconfirmedTransactionTable unconfirmedTransactionTable;

    @PostConstruct
    public void init() {
        transactionProcessor.init();
    }
}
