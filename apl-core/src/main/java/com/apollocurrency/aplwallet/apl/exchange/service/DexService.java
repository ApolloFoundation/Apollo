package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.api.request.GetEthBalancesRequest;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferCancelAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletBalanceInfo;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferTable;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.WalletsBalance;
import com.apollocurrency.aplwallet.apl.exchange.utils.DexCurrencyValidator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Slf4j
@Singleton
public class DexService {
    private static final Logger LOG = LoggerFactory.getLogger(DexService.class);

    private EthereumWalletService ethereumWalletService;
    private DexSmartContractService dexSmartContractService;
    private DexOfferDao dexOfferDao;
    private DexOfferTable dexOfferTable;
    private DexContractTable dexContractTable;
    private DexContractDao dexContractDao;
    private TransactionProcessorImpl transactionProcessor;
    private SecureStorageService secureStorageService;
    private DexOfferTransactionCreator dexOfferTransactionCreator;
    private EpochTime timeService;
    private Blockchain blockchain;
    private PhasingPollServiceImpl phasingPollService;


    @Inject
    public DexService(EthereumWalletService ethereumWalletService, DexOfferDao dexOfferDao, DexOfferTable dexOfferTable, TransactionProcessorImpl transactionProcessor,
                      DexSmartContractService dexSmartContractService, SecureStorageServiceImpl secureStorageService, DexContractTable dexContractTable,
                      DexOfferTransactionCreator dexOfferTransactionCreator, EpochTime timeService, DexContractDao dexContractDao, Blockchain blockchain, PhasingPollServiceImpl phasingPollService) {
        this.ethereumWalletService = ethereumWalletService;
        this.dexOfferDao = dexOfferDao;
        this.dexOfferTable = dexOfferTable;
        this.transactionProcessor = transactionProcessor;
        this.dexSmartContractService = dexSmartContractService;
        this.secureStorageService = secureStorageService;
        this.dexContractTable = dexContractTable;
        this.dexOfferTransactionCreator = dexOfferTransactionCreator;
        this.timeService = timeService;
        this.dexContractDao = dexContractDao;
        this.blockchain = blockchain;
        this.phasingPollService = phasingPollService;
    }


    @Transactional
    public DexOffer getOfferByTransactionId(Long transactionId){
        return dexOfferDao.getByTransactionId(transactionId);
    }

    @Transactional
    public DexOffer getOfferById(Long id){
        return dexOfferDao.getById(id);
    }

    /**
     * Use dexOfferTable for insert, to be sure that everything in one transaction.
     */
    @Transactional
    public void saveOffer (DexOffer offer){
        dexOfferTable.insert(offer);
    }

    @Transactional
    public void saveDexContract(ExchangeContract exchangeContract){
        dexContractTable.insert(exchangeContract);
    }

    @Transactional
    public List<ExchangeContract> getDexContracts(DexContractDBRequest dexContractDBRequest){
        return dexContractDao.getAll(dexContractDBRequest);
    }

    @Transactional
    public ExchangeContract getDexContract(DexContractDBRequest dexContractDBRequest){
        return dexContractDao.get(dexContractDBRequest);
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
            } catch (AplException.ExecutiveProcessException ex){
                LOG.error(ex.getMessage(), ex);
                //TODO take a look ones again do we need this throw exception there.
//                throw new RuntimeException(ex);
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

        String txHash = dexSmartContractService.withdraw(passphrase, offer.getAccountId(), offer.getFromAddress(), new BigInteger(Long.toUnsignedString(offer.getTransactionId())), null, offer.getPairCurrency());

        if(txHash==null){
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }
        return txHash;
    }

    public String freezeEthPax(String passphrase, DexOffer offer) throws ExecutionException, AplException.ExecutiveProcessException {
        String txHash;

        DexCurrencyValidator.checkHaveFreezeOrRefundEthOrPax(offer);

        BigDecimal haveToPay = EthUtil.aplToEth(offer.getOfferAmount()).multiply(offer.getPairRate());
        txHash = dexSmartContractService.deposit(passphrase, offer.getTransactionId(), offer.getAccountId(), offer.getFromAddress(), EthUtil.etherToWei(haveToPay), null, offer.getPairCurrency());


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
     * Transfer money with approve for APL/ETH/PAX
     * @return Tx hash id/link
     */
    public String transferMoneyWithApproval(CreateTransactionRequest createTransactionRequest, DexOffer offer, String toAddress, byte [] secretHash, ExchangeContractStatus contractStatus) throws AplException.ExecutiveProcessException {
        String transactionStr = null;

        if(offer.getType().isSell()) {
            createTransactionRequest.setDeadlineValue("1440");
            createTransactionRequest.setFeeATM(Constants.ONE_APL * 3);
            PhasingParams phasingParams = new PhasingParams((byte) 5, 0, 1, 0, (byte) 0, null);
            PhasingAppendixV2 phasing = new PhasingAppendixV2(-1, timeService.getEpochTime() + contractStatus.timeOfWaiting(), phasingParams, null, secretHash, (byte) 2);
            createTransactionRequest.setPhased(true);
            createTransactionRequest.setPhasing(phasing);

            createTransactionRequest.setAttachment(new DexControlOfFrozenMoneyAttachment(offer.getTransactionId(), false));
            try {
                Transaction transaction = dexOfferTransactionCreator.createTransaction(createTransactionRequest);
                transactionStr = transaction != null ? Long.toUnsignedString(transaction.getId()) : null;
            } catch (AplException.ValidationException | ParameterException e) {
                LOG.error(e.getMessage(), e);
                //TODO
            }
        } else if(offer.getType().isBuy() && offer.getPairCurrency().isEthOrPax()){
            BigDecimal haveToPay = EthUtil.aplToEth(offer.getOfferAmount()).multiply(offer.getPairRate());
            String token = null;

            if(offer.getPairCurrency().isPax()){
                token = ethereumWalletService.PAX_CONTRACT_ADDRESS;
            }

            if(contractStatus.isStep1()) {
                transactionStr = dexSmartContractService.depositAndInitiate(createTransactionRequest.getPassphrase(), createTransactionRequest.getSenderAccount().getId(),
                        offer.getFromAddress(), offer.getTransactionId(),
                        EthUtil.etherToWei(haveToPay),
                        secretHash, toAddress, contractStatus.timeOfWaiting(),
                        null, token);
            } else if (contractStatus.isStep2()){
                transactionStr = dexSmartContractService.initiate(createTransactionRequest.getPassphrase(), createTransactionRequest.getSenderAccount().getId(),
                        offer.getFromAddress(), offer.getTransactionId(), secretHash, toAddress, contractStatus.timeOfWaiting(), null);
            }

        }
        return transactionStr;
    }


    public boolean approveMoneyTransfer(String passphrase, Long accountId, Long orderId, String txId, byte[] secret) throws AplException.ExecutiveProcessException {

        DexOffer offer = getOfferByTransactionId(orderId);


        if(offer.getType().isSell()){

            dexSmartContractService.approve(passphrase, secret, offer.getToAddress(), accountId);

            log.info("Transaction:" + txId +" was approved.");
        } else if(offer.getType().isBuy()){
            try {
                Transaction transaction = blockchain.getTransaction(Long.parseLong(txId));
                List<byte[]> txHash = new ArrayList<>();
                txHash.add(transaction.getFullHash());

                Attachment attachment = new MessagingPhasingVoteCasting(txHash, secret);

                CreateTransactionRequest createTransactionRequest = CreateTransactionRequest
                        .builder()
                        .passphrase(passphrase)
                        .publicKey(Account.getPublicKey(accountId))
                        .senderAccount(Account.getAccount(accountId))
                        .keySeed(Crypto.getKeySeed(Helper2FA.findAplSecretBytes(accountId, passphrase)))
                        .deadlineValue("1440")
                        .feeATM(Constants.ONE_APL * 2)
                        .broadcast(true)
                        .recipientId(0L)
                        .ecBlockHeight(0)
                        .ecBlockId(0L)
                        .attachment(attachment)
                        .build();

                Transaction transactionResp = dexOfferTransactionCreator.createTransaction(createTransactionRequest);

                log.info("Transaction:" + txId +" was approved. TxId: " + transactionResp.getId());
            } catch (Exception ex){
                throw new AplException.ExecutiveProcessException(ex.getMessage());
            }
        }

        return true;
    }

    public boolean isTxApproved(byte[] secretHash, OfferType offerType, DexCurrencies dexCurrencies, String transferTxId) throws AplException.ExecutiveProcessException {
        return getSecretIfTxApproved(secretHash, offerType, dexCurrencies, transferTxId) != null;
    }

    public byte[] getSecretIfTxApproved(byte[] secretHash, OfferType offerType, DexCurrencies dexCurrencies, String transferTxId) throws AplException.ExecutiveProcessException {

        if(offerType.isBuy() && dexCurrencies.isEthOrPax()) {
            SwapDataInfo swapDataInfo = dexSmartContractService.getSwapData(secretHash);
            if(swapDataInfo!=null && swapDataInfo.getSecret()!= null) {
                return swapDataInfo.getSecret();
            }
        } else if(offerType.isSell()) {
            PhasingPoll phasingPoll = phasingPollService.getPoll(Long.parseLong(transferTxId));
            return phasingPoll.getHashedSecret();
        }

        return null;
    }


}
