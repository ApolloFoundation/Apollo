package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.account.AccountRestrictions;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountLedgerTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.alias.dao.AliasOfferTable;
import com.apollocurrency.aplwallet.apl.alias.dao.AliasTable;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.app.Poll;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.app.Trade;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.app.mint.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageTable;
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
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
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

    @PostConstruct
    public void init() {
        transactionProcessor.init();

        Asset.init();
        Order.init();
        Poll.init();
        Trade.init();
        AssetDelete.init();
        AssetDividend.init();
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
        AccountRestrictions.init();
        AssetTransfer.init(databaseManager);
    }
}
