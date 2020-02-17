package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
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
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyExchangeOffer;
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

    @PostConstruct
    public void init() {
        transactionProcessor.init();

        Alias.init();
        Asset.init();
        Order.init();
        Poll.init();
        Trade.init();
        AssetDelete.init();
        AssetDividend.init();
        Vote.init();
        Currency.init();
        CurrencyBuyOffer.init();
        CurrencySellOffer.init();
        CurrencyMint.init();
        CurrencyTransfer.init();
        Exchange.init();
        ExchangeRequest.init();
        Shuffling.init();
        ShufflingParticipant.init();
        CurrencyExchangeOffer.init();
        /*
        the following are initialized in AplCore:
        AccountPropertyTable
        AccountInfoTable
        ShufflingParticipant
        AssetTransfer
        AccountRestrictions
        AccountLedgerTable
        AccountCurrencyTable
        AccountLeaseTable
        AccountAssetTable
        TODO: refactor after merging Feature/apl 724 refactor account class
        */
    }
}
