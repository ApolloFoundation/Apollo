package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.api.request.GetEthBalancesRequest;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferCancelAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletBalanceInfo;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexTradeDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.WalletsBalance;
import com.apollocurrency.aplwallet.apl.exchange.utils.DexCurrencyValidator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DexService {
    private static final Logger LOG = LoggerFactory.getLogger(DexService.class);

    private EthereumWalletService ethereumWalletService;
    private DexSmartContractService dexSmartContractService;
    private DexOfferDao dexOfferDao;
    private DexOfferTable dexOfferTable;
    private DexContractTable dexContractTable;
    private TransactionProcessorImpl transactionProcessor;
    private SecureStorageService secureStorageService;

    private DexTradeDao dexTradeDao;

    private DexOfferTransactionCreator dexOfferTransactionCreator;
    private TimeService timeService;

    @Inject
    public DexService(EthereumWalletService ethereumWalletService, DexOfferDao dexOfferDao, DexOfferTable dexOfferTable, TransactionProcessorImpl transactionProcessor,
                      DexSmartContractService dexSmartContractService, SecureStorageServiceImpl secureStorageService, DexContractTable dexContractTable,
                      DexOfferTransactionCreator dexOfferTransactionCreator, TimeService timeService, DexTradeDao dexTradeDao) {

        this.ethereumWalletService = ethereumWalletService;
        this.dexOfferDao = dexOfferDao;
        this.dexOfferTable = dexOfferTable;
        this.transactionProcessor = transactionProcessor;
        this.dexSmartContractService = dexSmartContractService;
        this.secureStorageService = secureStorageService;
        this.dexContractTable = dexContractTable;

        this.dexTradeDao = dexTradeDao;

        this.dexOfferTransactionCreator = dexOfferTransactionCreator;
        this.timeService = timeService;

    }


    @Transactional
    public DexOffer getOfferByTransactionId(Long transactionId){
        return dexOfferDao.getByTransactionId(transactionId);
    }

    @Transactional
    public DexOffer getOfferById(Long id){
        return dexOfferDao.getById(id);
    }
    
    @Transactional
    public List<DexTradeEntry> getTradeInfoForPeriod( Integer start, Integer finish, 
            Byte pairCurrency, Integer offset, Integer limit) {
        return dexTradeDao.getDexEntriesForInterval(start, finish, pairCurrency, offset, limit);        
    }
    
    @Transactional
    public void saveDexTradeEntry( DexTradeEntry dexTradeEntry) {
        dexTradeDao.saveDexTradeEntry(dexTradeEntry);
    }

    /**
     * Use dexOfferTable for insert, to be sure that everything in one transaction.
     */
    public void saveOffer (DexOffer offer){
        dexOfferTable.insert(offer);
    }

    public void saveDexContract(ExchangeContract exchangeContract){
        dexContractTable.insert(exchangeContract);
    }

    @Transactional
    public List<DexOffer> getOffers(DexOfferDBRequest dexOfferDBRequest){
        return dexOfferDao.getOffers(dexOfferDBRequest);
    }

    @Transactional
    public List<DexOffer> getOffersForMatching(DexOfferDBMatchingRequest dexOfferDBMatchingRequest){
        return dexOfferDao.getOffersForMatching(dexOfferDBMatchingRequest);
    }

    public WalletsBalance getBalances(GetEthBalancesRequest getBalancesRequest){
        List<String> eth = getBalancesRequest.ethAddresses;
        List<EthWalletBalanceInfo> ethWalletsBalance = new ArrayList<>();

        for (String ethAddress : eth) {
            ethWalletsBalance.add(ethereumWalletService.balanceInfo(ethAddress));
        }

        // New Crypto currencies.

        return new WalletsBalance(ethWalletsBalance);
    }

    public List<ExchangeOrder> getHistory(String account, String pair, String type){
        return new ArrayList<>();
    }


    public String withdraw(long accountId, String secretPhrase, String fromAddress,  String toAddress, BigDecimal amount, DexCurrencies currencies, Long transferFee) throws AplException.ExecutiveProcessException{
        if (currencies != null && currencies.isEthOrPax()) {
            return ethereumWalletService.transfer(secretPhrase, accountId, fromAddress, toAddress, amount, transferFee, currencies);
        } else {
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + currencies.getCurrencyCode());
        }
    }


    public void closeOverdueOrders(Integer time){
        List<DexOffer> offers = dexOfferDao.getOverdueOrders(time);

        for (DexOffer offer : offers) {
            try {
                offer.setStatus(OfferStatus.EXPIRED);
                dexOfferTable.insert(offer);

                refundFrozenMoneyForOffer(offer);
            } catch (Exception ex){
                LOG.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }

    }

    public void refundFrozenMoneyForOffer(DexOffer offer) throws AplException.ExecutiveProcessException {
        if(DexCurrencyValidator.haveFreezeOrRefundApl(offer)){
            refundAPLFrozenMoney(offer);
        } else if(DexCurrencyValidator.haveFreezeOrRefundEthOrPax(offer)) {
            String passphrase = secureStorageService.getUserPassPhrase(offer.getAccountId());
            if(StringUtils.isNotBlank(passphrase)) {
                refundEthPaxFrozenMoney(passphrase, offer);
            }
        }
    }

    public void refundAPLFrozenMoney(DexOffer offer) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.checkHaveFreezeOrRefundApl(offer);

        //Return APL.
        Account account = Account.getAccount(offer.getAccountId());
        account.addToUnconfirmedBalanceATM(LedgerEvent.DEX_REFUND_FROZEN_MONEY, offer.getTransactionId(), offer.getOfferAmount());
    }

    public String refundEthPaxFrozenMoney(String passphrase, DexOffer offer) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.checkHaveFreezeOrRefundEthOrPax(offer);

        BigDecimal haveToPay = EthUtil.aplToEth(offer.getOfferAmount()).multiply(offer.getPairRate());
        String txHash = dexSmartContractService.withdraw(passphrase, offer.getAccountId(), offer.getFromAddress(), EthUtil.etherToWei(haveToPay), null, offer.getPairCurrency());

        if(txHash==null){
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }
        return txHash;
    }

    public String freezeEthPax(String passphrase, DexOffer offer) throws ExecutionException, AplException.ExecutiveProcessException {
        String txHash;

        DexCurrencyValidator.checkHaveFreezeOrRefundEthOrPax(offer);

        BigDecimal haveToPay = EthUtil.aplToEth(offer.getOfferAmount()).multiply(offer.getPairRate());
        txHash = dexSmartContractService.deposit(passphrase, offer.getAccountId(), offer.getFromAddress(), EthUtil.etherToWei(haveToPay), null, offer.getPairCurrency());


        if(txHash==null){
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }

        return txHash;
    }

    public void cancelOffer(DexOffer offer) {
        offer.setStatus(OfferStatus.CANCEL);
        saveOffer(offer);
    }

    /**
     * @param cancelTrId  can be null if we just want to check are there any unconfirmed transactions for this order.
     */
    public boolean isThereAnotherCancelUnconfirmedTx(Long orderId, Long cancelTrId){
        try(DbIterator<UnconfirmedTransaction>  tx = transactionProcessor.getAllUnconfirmedTransactions()) {
            while (tx.hasNext()) {
                UnconfirmedTransaction unconfirmedTransaction = tx.next();
                if (TransactionType.TYPE_DEX == unconfirmedTransaction.getTransaction().getType().getType() &&
                        TransactionType.SUBTYPE_DEX_OFFER_CANCEL == unconfirmedTransaction.getTransaction().getType().getSubtype()) {
                    DexOfferCancelAttachment dexOfferCancelAttachment = (DexOfferCancelAttachment) unconfirmedTransaction.getTransaction().getAttachment();

                    if(dexOfferCancelAttachment.getTransactionId() == orderId &&
                            !Objects.equals(unconfirmedTransaction.getTransaction().getId(),cancelTrId)){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     *
     * @return Tx hash id/link
     */
    public String transferMoneyWithApproval(CreateTransactionRequest createTransactionRequest, DexOffer offer, byte [] secretHash){
        //TODO Case APL
        createTransactionRequest.setDeadlineValue("1440");
        createTransactionRequest.setFeeATM(Constants.ONE_APL * 3);
        PhasingParams phasingParams = new PhasingParams((byte) 5, 0, 1, 0, (byte) 0, null);
        PhasingAppendixV2 phasing = new PhasingAppendixV2(-1, timeService.getEpochTime() + Constants.DEX_TIME_OF_WAITING_TX_WITH_APPROVAL_STEP_1, phasingParams, null, secretHash, (byte) 2);
        createTransactionRequest.setPhased(true);
        createTransactionRequest.setPhasing(phasing);

        createTransactionRequest.setAttachment(new DexControlOfFrozenMoneyAttachment(offer.getTransactionId() != null ? offer.getTransactionId() : 0 , false));
        Transaction transaction = null;
        try {
            transaction = dexOfferTransactionCreator.createTransaction(createTransactionRequest);

        } catch (AplException.ValidationException e) {
            LOG.error(e.getMessage(), e);
            //TODO
        } catch (ParameterException e) {
            LOG.error(e.getMessage(), e);
            //TODO
        }


        //TODO Case ETH/PAX

        return transaction != null ? Long.toUnsignedString(transaction.getId()) : null;
    }


}
