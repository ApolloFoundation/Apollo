package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.api.request.GetEthBalancesRequest;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.http.post.TransactionResponse;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingApprovalResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.CustomRequestWrapper;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachmentV2;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderCancelAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletBalanceInfo;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexTradeDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.TransferTransactionInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.WalletsBalance;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import com.apollocurrency.aplwallet.apl.exchange.utils.DexCurrencyValidator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StackTraceUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
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
    private DexOrderDao dexOrderDao;
    private DexOrderTable dexOrderTable;
    private DexContractTable dexContractTable;
    private PhasingApprovedResultTable phasingApprovedResultTable;
    private DexContractDao dexContractDao;
    private TransactionProcessor transactionProcessor;
    private SecureStorageService secureStorageService;
    private MandatoryTransactionDao mandatoryTransactionDao;

    private DexTradeDao dexTradeDao;

    private DexOrderTransactionCreator dexOrderTransactionCreator;
    private TimeService timeService;
    private Blockchain blockchain;
    private PhasingPollService phasingPollService;
    private IDexMatcherInterface dexMatcherService;

    @Inject
    public DexService(EthereumWalletService ethereumWalletService, DexOrderDao dexOrderDao, DexOrderTable dexOrderTable, TransactionProcessor transactionProcessor,
                      DexSmartContractService dexSmartContractService, SecureStorageService secureStorageService, DexContractTable dexContractTable,
                      DexOrderTransactionCreator dexOrderTransactionCreator, TimeService timeService, DexContractDao dexContractDao, Blockchain blockchain, PhasingPollServiceImpl phasingPollService,
                      IDexMatcherInterface dexMatcherService, DexTradeDao dexTradeDao, PhasingApprovedResultTable phasingApprovedResultTable, MandatoryTransactionDao mandatoryTransactionDao) {
        this.ethereumWalletService = ethereumWalletService;
        this.dexOrderDao = dexOrderDao;
        this.dexOrderTable = dexOrderTable;
        this.transactionProcessor = transactionProcessor;
        this.dexSmartContractService = dexSmartContractService;
        this.secureStorageService = secureStorageService;
        this.dexContractTable = dexContractTable;
        this.dexTradeDao = dexTradeDao;
        this.dexOrderTransactionCreator = dexOrderTransactionCreator;
        this.timeService = timeService;
        this.dexContractDao = dexContractDao;
        this.blockchain = blockchain;
        this.phasingPollService = phasingPollService;
        this.dexMatcherService = dexMatcherService;
        this.phasingApprovedResultTable = phasingApprovedResultTable;
        this.mandatoryTransactionDao = mandatoryTransactionDao;
    }


    @Transactional
    public DexOrder getOrder(Long transactionId) {
        return dexOrderTable.getByTxId(transactionId);
    }

    @Transactional
    public List<DexTradeEntry> getTradeInfoForPeriod(Integer start, Integer finish,
                                                     Byte pairCurrency, Integer offset, Integer limit) {
        return dexTradeDao.getDexEntriesForInterval(start, finish, pairCurrency, offset, limit);
    }

    @Transactional
    public void saveDexTradeEntry(DexTradeEntry dexTradeEntry) {
        dexTradeDao.saveDexTradeEntry(dexTradeEntry);
    }

    /**
     * Use dexOfferTable for insert, to be sure that everything in one transaction.
     */
    public void saveOrder(DexOrder order) {
        order.setHeight(this.blockchain.getHeight()); // new height value
        if (log.isTraceEnabled()) {
            log.trace("Save order {} at height {} : {} ", order.getId(), order.getHeight(), StackTraceUtils.lastNStacktrace(3));
        }
        dexOrderTable.insert(order);
    }

    @Transactional
    public void saveDexContract(ExchangeContract exchangeContract) {
        exchangeContract.setHeight(this.blockchain.getHeight()); // new height value
        dexContractTable.insert(exchangeContract);
    }

    @Transactional
    public List<ExchangeContract> getDexContracts(DexContractDBRequest dexContractDBRequest) {
        return dexContractDao.getAll(dexContractDBRequest);
    }

    @Transactional
    public List<ExchangeContract> getDexContractsByCounterOrderId(Long counterOrderId) {
        return dexContractTable.getAllByCounterOrder(counterOrderId);
    }

    @Transactional
    public ExchangeContract getDexContractByOrderId(Long orderId) {
        return dexContractTable.getByOrder(orderId);
    }

    @Transactional
    public ExchangeContract getDexContractByCounterOrderId(Long counterOrderId) {
        return dexContractTable.getByCounterOrder(counterOrderId);
    }

    /**
     * Use jdbc.
     *
     * @param dexContractDBRequest
     * @return
     */
    @Deprecated
    @Transactional
    public ExchangeContract getDexContract(DexContractDBRequest dexContractDBRequest) {
        return dexContractDao.get(dexContractDBRequest);
    }

    @Transactional
    public ExchangeContract getDexContractByOrderAndCounterOrder(Long orderId, Long counterOrderId) {
        return dexContractTable.getByOrderAndCounterOrder(orderId, counterOrderId);
    }

    @Transactional(readOnly = true)
    public ExchangeContract getDexContractById(long id) {
        return dexContractTable.getById(id);
    }

    @Transactional(readOnly = true)
    public List<DexOrder> getOrders(DexOrderDBRequest dexOrderDBRequest) {
        return dexOrderDao.getOrders(dexOrderDBRequest);
    }

    public WalletsBalance getBalances(GetEthBalancesRequest getBalancesRequest) {
        List<String> eth = getBalancesRequest.ethAddresses;
        List<EthWalletBalanceInfo> ethWalletsBalance = new ArrayList<>();

        for (String ethAddress : eth) {
            ethWalletsBalance.add(ethereumWalletService.balanceInfo(ethAddress));
        }

        return new WalletsBalance(ethWalletsBalance);
    }

    public List<ExchangeOrder> getHistory(String account, String pair, String type) {
        return new ArrayList<>();
    }


    public String withdraw(long accountId, String secretPhrase, String fromAddress, String toAddress, BigDecimal amount, DexCurrencies currencies, Long transferFee) throws AplException.ExecutiveProcessException {
        if (currencies != null && currencies.isEthOrPax()) {
            return ethereumWalletService.transfer(secretPhrase, accountId, fromAddress, toAddress, amount, transferFee, currencies);
        } else {
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + currencies.getCurrencyCode());
        }
    }


    public void closeOverdueOrders(Integer time) throws AplException.ExecutiveProcessException {
        long start = System.currentTimeMillis();
        List<DexOrder> orders = dexOrderTable.getOverdueOrders(time);
        log.trace(">> closeOverdueOrders() size=[{}] = {} ms by finish_time < {}",
                orders.size(), System.currentTimeMillis() - start, time);

        for (DexOrder order : orders) {
            log.debug("Order expired, orderId: {}", order.getId());
            order.setStatus(OrderStatus.EXPIRED);
            saveOrder(order);
            refundFrozenAplForOrderIfWeCan(order);

            reopenIncomeOrders(order.getId());

        }
        log.trace("<< closeOverdueOrders() = total {} ms", System.currentTimeMillis() - start);
    }

    public void closeOverdueContracts(Integer time) throws AplException.ExecutiveProcessException {
        List<ExchangeContract> contracts = dexContractTable.getOverdueContractsStep1and2(time);

        for (ExchangeContract contract : contracts) {
            DexOrder order = getOrder(contract.getOrderId());
            DexOrder counterOrder = getOrder(contract.getCounterOrderId());

            closeOverdueContract(order, time);
            closeOverdueContract(counterOrder, time);
            contract.setContractStatus(ExchangeContractStatus.STEP_4);
            order.setHeight(this.blockchain.getHeight()); // new height value
            dexContractTable.insert(contract);
        }
    }

    public void closeOverdueContract(DexOrder order, Integer time) throws AplException.ExecutiveProcessException {
        if (order.getStatus() != OrderStatus.EXPIRED && order.getStatus() != OrderStatus.CLOSED && order.getStatus() != OrderStatus.CANCEL) {
            if (order.getFinishTime() > time) {
                order.setStatus(OrderStatus.OPEN);
                saveOrder(order);
            } else {
                order.setStatus(OrderStatus.EXPIRED);
                saveOrder(order);
                refundFrozenAplForOrderIfWeCan(order);
                reopenIncomeOrders(order.getId());
            }

        } else {
            log.debug("Skip closing order {} in status {}", order.getId(), order.getStatus());
        }
    }

    public void refundFrozenAplForOrderIfWeCan(DexOrder order) throws AplException.ExecutiveProcessException {
        if (DexCurrencyValidator.haveFreezeOrRefundApl(order)) {
            refundAPLFrozenMoney(order);
        }
    }


    private void refundAPLFrozenMoney(DexOrder order) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.checkHaveFreezeOrRefundApl(order);

        //Return APL.
        Account account = Account.getAccount(order.getAccountId());
        account.addToUnconfirmedBalanceATM(LedgerEvent.DEX_REFUND_FROZEN_MONEY, order.getId(), order.getOrderAmount());
    }

    public String refundEthPaxFrozenMoney(String passphrase, DexOrder order) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.checkHaveFreezeOrRefundEthOrPax(order);

        //Check if deposit exist.
        String ethAddress = DexCurrencyValidator.isEthOrPaxAddress(order.getFromAddress()) ? order.getFromAddress() : order.getToAddress();
        if (!dexSmartContractService.isDepositForOrderExist(ethAddress, order.getId())) {
            log.warn("Eth/Pax deposit is not exist. Perhaps refund process was called before. OrderId: {}", order.getId());
            return "";
        }

        String txHash = dexSmartContractService.withdraw(passphrase, order.getAccountId(), order.getFromAddress(), new BigInteger(Long.toUnsignedString(order.getId())), null);

        if (txHash == null) {
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }
        return txHash;
    }

    public String refundEthPaxFrozenMoney(String passphrase, Long accountId, Long orderId, String address) throws AplException.ExecutiveProcessException {
        //Check if deposit exist.
        if (!dexSmartContractService.isDepositForOrderExist(address, orderId)) {
            log.warn("Eth/Pax deposit is not exist. Perhaps refund process was called before. OrderId: {}", orderId);
            return "";
        }

        String txHash = dexSmartContractService.withdraw(passphrase, accountId, address, new BigInteger(Long.toUnsignedString(orderId)), null);

        if (txHash == null) {
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }
        return txHash;
    }

    public String freezeEthPax(String passphrase, DexOrder order) throws ExecutionException, AplException.ExecutiveProcessException {
        String txHash;

        DexCurrencyValidator.checkHaveFreezeOrRefundEthOrPax(order);

        BigDecimal haveToPay = EthUtil.atmToEth(order.getOrderAmount()).multiply(order.getPairRate());
        txHash = dexSmartContractService.deposit(passphrase, order.getId(), order.getAccountId(), order.getFromAddress(), EthUtil.etherToWei(haveToPay), null, order.getPairCurrency());


        if (txHash == null) {
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }

        return txHash;
    }

    public void cancelOffer(DexOrder order) {
        order.setStatus(OrderStatus.CANCEL);
        saveOrder(order);
    }

    /**
     * @param cancelTrId can be null if we just want to check are there any unconfirmed transactions for this order.
     */
    public boolean isThereAnotherCancelUnconfirmedTx(Long orderId, Long cancelTrId) {
        try (DbIterator<UnconfirmedTransaction> tx = transactionProcessor.getAllUnconfirmedTransactions()) {
            while (tx.hasNext()) {
                UnconfirmedTransaction unconfirmedTransaction = tx.next();
                if (TransactionType.TYPE_DEX == unconfirmedTransaction.getTransaction().getType().getType() &&
                        TransactionType.SUBTYPE_DEX_ORDER_CANCEL == unconfirmedTransaction.getTransaction().getType().getSubtype()) {
                    DexOrderCancelAttachment dexOrderCancelAttachment = (DexOrderCancelAttachment) unconfirmedTransaction.getTransaction().getAttachment();

                    if (dexOrderCancelAttachment.getOrderId() == orderId &&
                            !Objects.equals(unconfirmedTransaction.getTransaction().getId(), cancelTrId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void broadcast(Transaction transaction) {
        try {
            transactionProcessor.broadcast(transaction);
        }
        catch (AplException.ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transfer money with approve for APL/ETH/PAX
     *
     * @return Tx hash id/link and non-broadcasted apl transaction if created
     */
    public TransferTransactionInfo transferMoneyWithApproval(CreateTransactionRequest createTransactionRequest, DexOrder order, String toAddress, long contractId, byte[] secretHash, ExchangeContractStatus contractStatus) throws AplException.ExecutiveProcessException {
        TransferTransactionInfo result = new TransferTransactionInfo();

        if (DexCurrencyValidator.isEthOrPaxAddress(toAddress)) {
            if (dexSmartContractService.isUserTransferMoney(order.getFromAddress(), order.getId())) {
                throw new AplException.ExecutiveProcessException("User has already started exchange process with another user. OrderId: " + order.getId());
            }

            if (dexSmartContractService.isDepositForOrderExist(order.getFromAddress(), order.getId())) {
                String txHash = dexSmartContractService.initiate(createTransactionRequest.getPassphrase(), createTransactionRequest.getSenderAccount().getId(),
                        order.getFromAddress(), order.getId(), secretHash, toAddress, contractStatus.timeOfWaiting(), null);
                result.setTxId(txHash);
            } else {
                throw new AplException.ExecutiveProcessException("There is no deposit(frozen money) for order. OrderId: " + order.getId());
            }

        } else { // just send apl  transaction with phasing (secret)

            createTransactionRequest.setRecipientId(Convert.parseAccountId(toAddress));
            // set amount into attachment to apply balance changes on attachment level
            //            createTransactionRequest.setAmountATM(offer.getOrderAmount());
            createTransactionRequest.setDeadlineValue("1440");

            createTransactionRequest.setFeeATM(Constants.ONE_APL * 3);
            PhasingParams phasingParams = new PhasingParams((byte) 5, 0, 1, 0, (byte) 0, null);
            PhasingAppendixV2 phasing = new PhasingAppendixV2(-1, timeService.getEpochTime() + contractStatus.timeOfWaiting(), phasingParams, null, secretHash, (byte) 2);
            createTransactionRequest.setPhased(true);
            createTransactionRequest.setPhasing(phasing);
            createTransactionRequest.setAttachment(new DexControlOfFrozenMoneyAttachment(contractId, order.getOrderAmount()));
            createTransactionRequest.setBroadcast(false);
            createTransactionRequest.setValidate(false);
            try {
                Transaction transaction = dexOrderTransactionCreator.createTransaction(createTransactionRequest);
                String txId = transaction != null ? Long.toUnsignedString(transaction.getId()) : null;
                result.setTransaction(transaction);
                result.setTxId(txId);
            } catch (AplException.ValidationException | ParameterException e) {
                LOG.error(e.getMessage(), e);
                throw new AplException.ExecutiveProcessException(e.getMessage());
            }

        }
        return result;
    }

    public boolean txExists(long aplTxId) {
        return blockchain.hasTransaction(aplTxId);
    }

    /**
     * Approve money transfer for APL and ETH/PAX.
     *
     * @param passphrase    User passphrase.
     * @param userAccountId User accountId. (On the APL blockchain)
     * @param userOrderId   User orderId.
     * @param txId          Id of the transaction with money transfer for this "User". (Which account we mentioned.)
     * @param secret        Secret to approve transfer.
     * @return true - was approved, false - wasn't approved.
     * @throws AplException.ExecutiveProcessException
     */
    public boolean approveMoneyTransfer(String passphrase, Long userAccountId, Long userOrderId, String txId, long contractId, byte[] secret) throws AplException.ExecutiveProcessException {
        try {
            DexOrder userOffer = getOrder(userOrderId);

            CreateTransactionRequest templatTransactionRequest = CreateTransactionRequest
                    .builder()
                    .passphrase(passphrase)
                    .publicKey(Account.getPublicKey(userAccountId))
                    .senderAccount(Account.getAccount(userAccountId))
                    .keySeed(Crypto.getKeySeed(Helper2FA.findAplSecretBytes(userAccountId, passphrase)))
                    .deadlineValue("1440")
                    .feeATM(Constants.ONE_APL * 2)
                    .broadcast(true)
                    .recipientId(0L)
                    .ecBlockHeight(0)
                    .ecBlockId(0L)
                    .build();

            if (DexCurrencyValidator.isEthOrPaxAddress(txId)) {

                boolean approved = dexSmartContractService.approve(passphrase, secret, userOffer.getToAddress(), userAccountId);

                if (!approved) {
                    throw new AplException.ExecutiveProcessException("Transaction:" + txId + " was not approved. (Eth/Pax)");
                }

                log.debug("Transaction:" + txId + " was approved. (Eth/Pax)");

                DexCloseOrderAttachment closeOrderAttachment = new DexCloseOrderAttachment(contractId);
                templatTransactionRequest.setAttachment(closeOrderAttachment);

                Transaction respCloseOffer = dexOrderTransactionCreator.createTransaction(templatTransactionRequest);
                log.debug("Order:" + userOffer.getId() + " was closed. TxId:" + respCloseOffer.getId() + " (Eth/Pax)");

            } else {

                Transaction transaction = blockchain.getTransaction(Long.parseUnsignedLong(txId));
                List<byte[]> txHash = new ArrayList<>();
                txHash.add(transaction.getFullHash());

                Attachment attachment = new MessagingPhasingVoteCasting(txHash, secret);
                templatTransactionRequest.setAttachment(attachment);

                Transaction respApproveTx = dexOrderTransactionCreator.createTransaction(templatTransactionRequest);
                log.debug("Transaction:" + txId + " was approved. TxId: " + respApproveTx.getId() + " (Apl)");
//              order will be closed automatically
            }
        } catch (Exception ex) {
            throw new AplException.ExecutiveProcessException(ex.getMessage());
        }

        return true;
    }

    @Transactional
    public JSONStreamAware createOffer(CustomRequestWrapper requestWrapper, Account account, DexOrder order) throws ParameterException, AplException.ValidationException, AplException.ExecutiveProcessException, ExecutionException {
        DexOrder counterOffer = dexMatcherService.findCounterOffer(order);
        String freezeTx = null;
        JSONStreamAware response = new JSONObject();

        if (counterOffer != null) {
            if (counterOffer.getAccountId().equals(order.getAccountId())) {
                throw new ParameterException(JSONResponses.DEX_SELF_ORDER_MATCHING_DENIED);
            }
            // 1. Create order.

            order.setStatus(OrderStatus.PENDING);
            CreateTransactionRequest createOfferTransactionRequest = HttpRequestToCreateTransactionRequestConverter
                    .convert(requestWrapper, account, 0L, 0L, new DexOrderAttachmentV2(order), false);
            Transaction offerTx = dexOrderTransactionCreator.createTransaction(createOfferTransactionRequest);
            order.setId(offerTx.getId());

            // 2. Create contract.
            DexContractAttachment contractAttachment = new DexContractAttachment(order.getId(), counterOffer.getId(), null, null, null, null, ExchangeContractStatus.STEP_1, Constants.DEX_MIN_CONTRACT_TIME_WAITING_TO_REPLY);
            TransactionResponse transactionResponse = dexOrderTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, contractAttachment, false);
            response = transactionResponse.getJson();
            Transaction contractTx = transactionResponse.getTx();
            MandatoryTransaction offerMandatoryTx = new MandatoryTransaction(offerTx, null, null);
            MandatoryTransaction contractMandatoryTx = new MandatoryTransaction(contractTx, offerMandatoryTx.getFullHash(), null);
            mandatoryTransactionDao.insert(offerMandatoryTx);
            mandatoryTransactionDao.insert(contractMandatoryTx);
            transactionProcessor.broadcast(offerMandatoryTx.getTransaction());
//  ??      transactionProcessor.broadcastWhenConfirmed(contractTx, offerTx);
            // will be broadcasted by DexOrderProcessor when offer will be confirmed
            //            transactionProcessor.broadcast(contractMandatoryTx.getTransaction());
        } else {
            CreateTransactionRequest createOfferTransactionRequest = HttpRequestToCreateTransactionRequestConverter
                    .convert(requestWrapper, account, 0L, 0L, new DexOrderAttachmentV2(order), true);
            Transaction offerTx = dexOrderTransactionCreator.createTransaction(createOfferTransactionRequest);
            order.setId(offerTx.getId());
        }

        if (order.getPairCurrency().isEthOrPax() && order.getType().isBuy()) {
            String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(requestWrapper, true));
            freezeTx = freezeEthPax(passphrase, order);
            log.debug("Create order - frozen money, accountId: {}, offerId: {}", account.getId(), order.getId());
        }
        if (freezeTx != null) {
            ((JSONObject) response).put("frozenTx", freezeTx);
        }

        return response;
    }


    public boolean isTxApproved(byte[] secretHash, String transferTxId) throws AplException.ExecutiveProcessException {
        if (DexCurrencyValidator.isEthOrPaxAddress(transferTxId)) {
            SwapDataInfo swapDataInfo = dexSmartContractService.getSwapData(secretHash);
            return swapDataInfo != null && swapDataInfo.getSecret() != null;
        } else {
            PhasingPollResult phasingResult = phasingPollService.getResult(Long.parseUnsignedLong(transferTxId));
            return phasingResult != null && phasingApprovedResultTable.get(phasingResult.getId()) != null;
        }
    }

    public byte[] getSecretIfTxApproved(byte[] secretHash, String transferTxId) throws AplException.ExecutiveProcessException {

        if (DexCurrencyValidator.isEthOrPaxAddress(transferTxId)) {
            SwapDataInfo swapDataInfo = dexSmartContractService.getSwapData(secretHash);
            if (swapDataInfo != null && swapDataInfo.getSecret() != null) {
                return swapDataInfo.getSecret();
            }
        } else {
            PhasingPollResult phasingPoll = phasingPollService.getResult(Long.parseUnsignedLong(transferTxId));
            if (phasingPoll == null) {
                return null;
            }

            PhasingApprovalResult phasingApprovalResult = phasingApprovedResultTable.get(phasingPoll.getId());
            if (phasingApprovalResult == null) {
                return null;
            }
            long approvedTx = phasingApprovalResult.getApprovedTx();

            Transaction transaction = blockchain.getTransaction(approvedTx);
            MessagingPhasingVoteCasting voteCasting = (MessagingPhasingVoteCasting) transaction.getAttachment();
            return voteCasting.getRevealedSecret();
        }

        return null;
    }

    public boolean hasConfirmations(ExchangeContract contract, DexOrder dexOrder) {
        if (dexOrder.getType() == OrderType.BUY) {
            return hasAplConfirmations(Convert.parseUnsignedLong(contract.getTransferTxId()), Constants.DEX_APL_NUMBER_OF_CONFIRMATIONS);
        } else if (dexOrder.getPairCurrency().isEthOrPax()) { // for now this check is useless, but for future can be used to separate other currencies
            return ethereumWalletService.getNumberOfConfirmations(contract.getTransferTxId()) >= Constants.DEX_ETH_NUMBER_OF_CONFIRMATIONS;
        } else {
            throw new IllegalArgumentException("Unable to calculate number of confirmations for paired currency - " + dexOrder.getPairCurrency());
        }
    }
    
    public boolean hasConfirmations(DexOrder dexOrder){
        log.debug("DexService: HasConfirmations reached");
        if (dexOrder.getType() == OrderType.BUY) {
         log.debug("desService: HasConfirmations reached");
            return hasAplConfirmations(dexOrder.getId(), Constants.DEX_APL_NUMBER_OF_CONFIRMATIONS);
        } 
        else if (dexOrder.getPairCurrency().isEthOrPax()){
            log.debug("Just a sell Sell Order, add eth confirmations check here...");
            return true;
        }
        
        log.debug("hasConfirmations2: just returning true here...");
        return true;
    }

    public boolean hasAplConfirmations(long txId, int confirmations) {
        int currentHeight = blockchain.getHeight();
        int requiredTxHeight = currentHeight - confirmations;
        return blockchain.hasTransaction(txId, requiredTxHeight);
    }

    public void reopenPendingOrders(int height, int time) throws AplException.ExecutiveProcessException {
        if (height % 10 == 0 ) { // every ten blocks
            List<DexOrder> pendingOrders = dexOrderTable.getPendingOrdersWithoutContracts(height - Constants.DEX_NUMBER_OF_PENDING_ORDER_CONFIRMATIONS);
            for (DexOrder pendingOrder : pendingOrders) {
                if (pendingOrder.getFinishTime() > time) {
                    pendingOrder.setStatus(OrderStatus.OPEN);
                } else {
                    pendingOrder.setStatus(OrderStatus.EXPIRED);
                    refundFrozenAplForOrderIfWeCan(pendingOrder);
                }
                saveOrder(pendingOrder);
            }
        }
    }



    public DexOrder closeOrder(long orderId) {
        DexOrder order = getOrder(orderId);
        order.setStatus(OrderStatus.CLOSED);
        saveOrder(order);
        return order;
    }

    public void finishExchange(long transactionId, long orderId) {
        DexOrder order = closeOrder(orderId);

        ExchangeContract exchangeContract = getDexContractByOrderId(order.getId());

        if (exchangeContract == null) {
            exchangeContract = getDexContractByCounterOrderId(order.getId());
        }

        Block lastBlock = blockchain.getLastBlock();

        DexTradeEntry dexTradeEntry = DexTradeEntry.builder()
                .transactionID(transactionId)
                .senderOfferID(exchangeContract.getSender())
                .receiverOfferID(exchangeContract.getRecipient())
                .senderOfferType((byte) order.getType().ordinal())
                .senderOfferCurrency((byte) order.getOrderCurrency().ordinal())
                .senderOfferAmount(order.getOrderAmount())
                .pairCurrency((byte) order.getPairCurrency().ordinal())
                .pairRate(order.getPairRate())
                .finishTime(lastBlock.getTimestamp())
                .height(lastBlock.getHeight())
                .build();

        saveDexTradeEntry(dexTradeEntry);
    }
    
    public boolean flushSecureStorage(Long accountID, String passPhrase) {                
        return secureStorageService.flushAccountKeys( accountID, passPhrase);        
    }
    


    public void reopenIncomeOrders(Long orderId) {
        closeContracts(dexContractTable.getAllByCounterOrder(orderId));
    }

    public void closeContracts(List<ExchangeContract> contractsForReopen) {
        for (ExchangeContract contract : contractsForReopen) {

            contract.setContractStatus(ExchangeContractStatus.STEP_4);
            saveDexContract(contract);
            log.debug("Contract was closed. ContractId: {}", contract.getId());

            //Reopen order.
            DexOrder order = getOrder(contract.getOrderId());
            if (!order.getStatus().isClosedOrExpiredOrCancel()) {
                order.setStatus(OrderStatus.OPEN);
                saveOrder(order);
                log.debug("Order was reopened. OrderId: {}", order.getId());
            }
        }
    }


    public void onPhasedTxReleased(@Observes @TxEvent(TxEventType.RELEASE_PHASED_TRANSACTION) Transaction transaction) {
        if (transaction.getType() == DEX.DEX_TRANSFER_MONEY_TRANSACTION) {
            List<PhasingVote> votes = phasingPollService.getVotes(transaction.getId());
            log.debug("Found {} votes, pick latest", votes.size());
            phasingApprovedResultTable.insert(new PhasingApprovalResult(blockchain.getHeight(), transaction.getId(), votes.get(0).getVoteId()));
        }
    }

    public boolean isExpired(Transaction tx) {
        return timeService.getEpochTime() > tx.getExpiration();
    }


    public List<UserEthDepositInfo> getUserFilledDeposits(String user) throws AplException.ExecutiveProcessException {
        return dexSmartContractService.getUserFilledDeposits(user);
    }

    public List<UserEthDepositInfo> getUserFilledOrders(String user) throws AplException.ExecutiveProcessException {
        return dexSmartContractService.getUserFilledOrders(user);
    }

}
