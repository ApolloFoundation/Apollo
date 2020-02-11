package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.api.request.GetEthBalancesRequest;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.cache.DexOrderFreezingCacheConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.http.post.TransactionResponse;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingApprovalResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.CustomRequestWrapper;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachmentV2;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletBalanceInfo;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.DexConfig;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.model.AddressEthDepositsInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.AddressEthExpiredSwaps;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequestForTrading;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderWithFreezing;
import com.apollocurrency.aplwallet.apl.exchange.model.EthDepositInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.EthDepositsWithOffset;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.ExpiredSwap;
import com.apollocurrency.aplwallet.apl.exchange.model.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderFreezing;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.TransferTransactionInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.UserAddressesWithOffset;
import com.apollocurrency.aplwallet.apl.exchange.model.WalletsBalance;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import com.apollocurrency.aplwallet.apl.exchange.utils.DexCurrencyValidator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StackTraceUtils;
import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.apollocurrency.aplwallet.apl.util.cache.CacheType;
import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    private LoadingCache<Long, OrderFreezing> orderFreezingCache;

    private DexOrderTransactionCreator dexOrderTransactionCreator;
    private TimeService timeService;
    private Blockchain blockchain;
    private PhasingPollService phasingPollService;
    private IDexMatcherInterface dexMatcherService;
    private BlockchainConfig blockchainConfig;
    private DexConfig dexConfig;

    private Integer MAX_PAGES_FOR_SEARCH = 10;
    @Inject
    public DexService(EthereumWalletService ethereumWalletService, DexOrderDao dexOrderDao, DexOrderTable dexOrderTable, TransactionProcessor transactionProcessor,
                      DexSmartContractService dexSmartContractService, SecureStorageService secureStorageService, DexContractTable dexContractTable,
                      DexOrderTransactionCreator dexOrderTransactionCreator, TimeService timeService, DexContractDao dexContractDao, Blockchain blockchain, PhasingPollService phasingPollService,
                      IDexMatcherInterface dexMatcherService, PhasingApprovedResultTable phasingApprovedResultTable, MandatoryTransactionDao mandatoryTransactionDao,
                      BlockchainConfig blockchainConfig,
                      @CacheProducer
                      @CacheType(DexOrderFreezingCacheConfig.CACHE_NAME) Cache<Long, OrderFreezing> cache, DexConfig dexConfig) {
        this.ethereumWalletService = ethereumWalletService;
        this.dexOrderDao = dexOrderDao;
        this.dexOrderTable = dexOrderTable;
        this.transactionProcessor = transactionProcessor;
        this.dexSmartContractService = dexSmartContractService;
        this.secureStorageService = secureStorageService;
        this.dexContractTable = dexContractTable;
        this.dexOrderTransactionCreator = dexOrderTransactionCreator;
        this.timeService = timeService;
        this.dexContractDao = dexContractDao;
        this.blockchain = blockchain;
        this.phasingPollService = phasingPollService;
        this.dexMatcherService = dexMatcherService;
        this.phasingApprovedResultTable = phasingApprovedResultTable;
        this.mandatoryTransactionDao = mandatoryTransactionDao;
        this.orderFreezingCache = (LoadingCache<Long, OrderFreezing>) cache;
        this.blockchainConfig = blockchainConfig;
        this.dexConfig = dexConfig;
    }


    @Transactional
    public DexOrder getOrder(Long transactionId) {
        return dexOrderTable.getByTxId(transactionId);
    }

    /**
     * returns unsorted list of orders for the specific time interval
     *
     */
    @Transactional(readOnly = true)
    public List<DexOrder> getOrdersForTrading(DexOrderDBRequestForTrading dexOrderDBRequestForTrading ) {
        return dexOrderDao.getOrdersForTrading(dexOrderDBRequestForTrading);
    }


    /**
     * Use dexOfferTable for insert, to be sure that everything in one transaction.
     */
    @Transactional
    public void saveOrder(DexOrder order) {
        order.setHeight(this.blockchain.getHeight()); // new height value
        if (log.isTraceEnabled()) {
            log.trace("Save order {} at height {} : {} ", order.getId(), order.getHeight(), StackTraceUtils.lastNStacktrace(3));
        }
        dexOrderTable.insert(order);
    }

    @Transactional
    public void saveDexContract(ExchangeContract exchangeContract) {
        log.debug("Create new contract: id:{}, orderId:{}, counterOrderId:{}, contractStatus:{}",
            exchangeContract.getId(), exchangeContract.getOrderId(), exchangeContract.getCounterOrderId(), exchangeContract.getContractStatus());
        exchangeContract.setHeight(this.blockchain.getHeight()); // new height value
        dexContractTable.insert(exchangeContract);
    }


    public List<ExchangeContract> getDexContracts(DexContractDBRequest dexContractDBRequest) {
        return dexContractDao.getAll(dexContractDBRequest);
    }

    public List<ExchangeContract> getDexContracts(DexContractDBRequest dexContractDBRequest, List<ExchangeContractStatus> exchangeContractStatuses) {
        //TODO use list of ExchangeContractStatus for jdbi instead of list of Integer
        return dexContractDao.getAllWithMultipleStatuses(dexContractDBRequest,
            exchangeContractStatuses
            .stream()
            .map(ExchangeContractStatus::ordinal)
            .collect(Collectors.toList())
        );
    }


    public List<ExchangeContract> getDexContractsByCounterOrderId(Long counterOrderId) {
        return dexContractTable.getAllByCounterOrder(counterOrderId);
    }


    public ExchangeContract getDexContractByOrderId(Long orderId) {
        return dexContractTable.getLastByOrder(orderId);
    }


    public ExchangeContract getDexContractByCounterOrderId(Long counterOrderId) {
        return dexContractTable.getLastByCounterOrder(counterOrderId);
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
    public ExchangeContract getDexContract(DexContractDBRequest dexContractDBRequest, List<ExchangeContractStatus> exchangeContractStatuses) {
        return dexContractDao.getWithMultipleStatuses(dexContractDBRequest,
            exchangeContractStatuses
                .stream()
                .map(ExchangeContractStatus::ordinal)
                .collect(Collectors.toList())
        );
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
    public List<DexOrderWithFreezing> getOrdersWithFreezing(DexOrderDBRequest dexOrderDBRequest) {
        List<DexOrderWithFreezing> orders;
        if (dexOrderDBRequest.getHasFrozenMoney() != null) {
            List<DexOrder> fetchedOrders;
            orders = new ArrayList<>();
            do {
                fetchedOrders = dexOrderDao.getOrders(dexOrderDBRequest, dexOrderDBRequest.getSortBy(), dexOrderDBRequest.getSortOrder());
                Integer offset = dexOrderDBRequest.getOffset();
                if (offset != null) {
                    dexOrderDBRequest.setOffset(dexOrderDBRequest.getOffset() + dexOrderDBRequest.getLimit());
                } else {
                    dexOrderDBRequest.setOffset(dexOrderDBRequest.getLimit());
                }
                List<DexOrderWithFreezing> dexOrderWithFreezings = mapToOrdersWithFreezing(fetchedOrders);
                dexOrderWithFreezings
                        .stream()
                        .filter(o -> o.isHasFrozenMoney() == dexOrderDBRequest.getHasFrozenMoney())
                        .limit(dexOrderDBRequest.getLimit() - orders.size())
                        .forEach(orders::add);
            }
            while (orders.size() < dexOrderDBRequest.getLimit() && fetchedOrders.size() == dexOrderDBRequest.getLimit());
        } else {
            List<DexOrder> fetchedOrders = dexOrderDao.getOrders(dexOrderDBRequest,
                dexOrderDBRequest.getSortBy(), dexOrderDBRequest.getSortOrder());
            orders = mapToOrdersWithFreezing(fetchedOrders);
        }
        return orders;
    }

    @Transactional(readOnly = true)
    public DexOrderWithFreezing getOrderWithFreezing(Long orderId) {
        DexOrder order = getOrder(orderId);

        return mapToOrdersWithFreezing(order);
    }


    /**
     * Sorted by height. It's important for a cash.
     *
     * @param dexOrderDBRequest
     */
    @Transactional(readOnly = true)
    public List<DexOrder> getOrders(DexOrderDBRequest dexOrderDBRequest) {
        return dexOrderDao.getOrders(dexOrderDBRequest,
            dexOrderDBRequest.getSortBy(), dexOrderDBRequest.getSortOrder())
                .stream()
            .sorted(Comparator.comparingLong(DexOrder::getDbId))
                .collect(Collectors.toList());
    }


    private List<DexOrderWithFreezing> mapToOrdersWithFreezing(List<DexOrder> orders) {
        return orders.stream().map(this::mapToOrdersWithFreezing).collect(Collectors.toList());
    }

    private DexOrderWithFreezing mapToOrdersWithFreezing(DexOrder order) {
        if (order == null) {
            return null;
        }

        return new DexOrderWithFreezing(order, order.getType() == OrderType.SELL || orderFreezingCache.getUnchecked(order.getId()).isHasFrozenMoney());
    }

    public WalletsBalance getBalances(GetEthBalancesRequest getBalancesRequest) {
        List<String> eth = getBalancesRequest.ethAddresses;
        List<EthWalletBalanceInfo> ethWalletsBalance = new ArrayList<>();

        for (String ethAddress : eth) {
            ethWalletsBalance.add(ethereumWalletService.balanceInfo(ethAddress));
        }

        return new WalletsBalance(ethWalletsBalance);
    }


    public String withdraw(long accountId, String secretPhrase, String fromAddress, String toAddress, BigDecimal amount, DexCurrency currencies, Long transferFee) throws AplException.ExecutiveProcessException {
        if (currencies != null && currencies.isEthOrPax()) {
            return ethereumWalletService.transfer(secretPhrase, accountId, fromAddress, toAddress, amount, transferFee, currencies);
        } else {
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + currencies.getCurrencyCode());
        }
    }


    /**
     * Can close only Open orders.
     *
     * @param time
     * @throws AplException.ExecutiveProcessException
     */
    public void closeOverdueOrders(Integer time) throws AplException.ExecutiveProcessException {
        long start = System.currentTimeMillis();
        List<DexOrder> orders = dexOrderTable.getOverdueOrders(time)
            .stream()
            .filter(order -> order.getHeight() < blockchain.getHeight())
            .collect(Collectors.toList());

        log.trace(">> closeOverdueOrders() size=[{}] = {} ms by finish_time < {}",
            orders.size(), System.currentTimeMillis() - start, time);

        for (DexOrder order : orders) {
            log.debug("Order expired, orderId: {}", order.getId());
            order.setStatus(OrderStatus.EXPIRED);
            saveOrder(order);

            tryRefundApl(order);

            reopenIncomeOrders(order.getId());
        }
        log.trace("<< closeOverdueOrders() = total {} ms", System.currentTimeMillis() - start);
    }

    /**
     * Use closeExpiredContractsStep1_2_3
     */
    @Deprecated
    public void closeExpiredContractsStep1_2(Integer time) throws AplException.ExecutiveProcessException {
        closeExpiredContracts(time, dexContractTable.getOverdueContractsStep1and2(time));
    }

    public void closeExpiredContractsStep1_2_3(Integer time) throws AplException.ExecutiveProcessException {
        closeExpiredContracts(time, dexContractTable.getOverdueContractsStep1_2_3(time));
    }

    public void closeExpiredContracts(Integer time, List<ExchangeContract> contracts) throws AplException.ExecutiveProcessException{
        for (ExchangeContract contract : contracts) {
            closeContract(contract, time);
        }
    }



    public void closeContract(ExchangeContract contract, Integer time) throws AplException.ExecutiveProcessException {
        DexOrder order        = getOrder(contract.getOrderId());

        //Close current and all income contracts/orders.
        handleExpiredContractOrder(order, time, contract);
        // In the Step1 user didn't answer yet, so it can have another offers. This order on this step is Open.
        if (!contract.getContractStatus().isStep1()) {
            DexOrder counterOrder = getOrder(contract.getCounterOrderId());
            handleExpiredContractOrder(counterOrder, time, contract);
        }
        contract.setContractStatus(ExchangeContractStatus.STEP_4);
        saveDexContract(contract);

    }


    public void handleExpiredContractOrder(DexOrder order, Integer time, ExchangeContract contract) throws AplException.ExecutiveProcessException {
        if (!order.getStatus().isClosedOrExpiredOrCancel()) {
            OrderStatus phasingOrderStatus = getOrderStatusAccordingPhasingTx(order, contract);
            if (phasingOrderStatus != null) {
                order.setStatus(phasingOrderStatus);
            } else if (order.getFinishTime() > time) {
                order.setStatus(OrderStatus.OPEN);
            } else {
                order.setStatus(OrderStatus.EXPIRED);
                tryRefundApl(order);
            }
            log.debug("Save expired contract's order as {} at height {}", order, blockchain.getHeight());
            saveOrder(order);
        } else {
            log.debug("Skip closing order {} in status {}", order.getId(), order.getStatus());
        }
    }

    private OrderStatus getOrderStatusAccordingPhasingTx(DexOrder order, ExchangeContract contract) {
        OrderStatus orderStatus = null;
        if (blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() != null && blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() < blockchain.getHeight()) {
            if (contract.getContractStatus() != ExchangeContractStatus.STEP_1) {
                Long id = contract.getPhasingIdForOrder(order);
                if (id != null) {
                    PhasingPoll poll = phasingPollService.getPoll(id);
                    PhasingPollResult result = phasingPollService.getResult(id);
                    boolean finishedPhasing = result != null && result.isApproved();
                    boolean phasingInProgress = poll != null && (poll.getFinishHeight() > blockchain.getHeight() || poll.getFinishTime() > blockchain.getLastBlockTimestamp()) && result == null;

                    if (finishedPhasing) {
                        orderStatus = OrderStatus.EXPIRED;
                    } else if (phasingInProgress) {
                        orderStatus = OrderStatus.PHASING_RESULT_PENDING;
                    }
                }
            }
        }
        return orderStatus;
    }


    private void doRefundApl(DexOrder order) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.requireAplRefundable(order);

        //Return APL.
        Account account = Account.getAccount(order.getAccountId());
        account.addToUnconfirmedBalanceATM(LedgerEvent.DEX_REFUND_FROZEN_MONEY, order.getId(), order.getOrderAmount());
    }
    public void tryRefundApl(DexOrder order) throws AplException.ExecutiveProcessException {
        if (DexCurrencyValidator.haveFreezeOrRefundApl(order)) {
           doRefundApl(order);
        }
    }

    public String refundEthPaxFrozenMoney(String passphrase, DexOrder order, boolean check) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.requireEthOrPaxRefundable(order);

        //Check if deposit exist.
        if(check) {
            String ethAddress = DexCurrencyValidator.isEthOrPaxAddress(order.getFromAddress()) ? order.getFromAddress() : order.getToAddress();
            if (!dexSmartContractService.isDepositForOrderExist(ethAddress, order.getId())) {
                log.warn("Eth/Pax deposit is not exist. Perhaps refund process was called before. OrderId: {}", order.getId());
                return "";
            }
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

    public String freezeEthPax(String passphrase, DexOrder order) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.requireEthOrPaxRefundable(order);
        BigDecimal haveToPay = EthUtil.atmToEth(order.getOrderAmount()).multiply(order.getPairRate());
        return freezeEthPax(passphrase, order.getAccountId(), order.getId(), order.getPairCurrency(), EthUtil.etherToWei(haveToPay), order.getFromAddress());
    }

    public String freezeEthPax(String passphrase, long accountId, long orderId,  DexCurrency currency, BigInteger amountWei, String walletAddress) throws AplException.ExecutiveProcessException {
        String txHash = dexSmartContractService.deposit(passphrase, orderId, accountId, walletAddress, amountWei, null, currency);
        if (txHash == null) {
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }

        return txHash;
    }

    public void cancelOffer(DexOrder order) {
        order.setStatus(OrderStatus.CANCEL);
        saveOrder(order);
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
    public TransferTransactionInfo transferMoneyWithApproval(CreateTransactionRequest createTransactionRequest, DexOrder order, String toAddress, long contractId, byte[] secretHash, int transferWithApprovalDuration) throws AplException.ExecutiveProcessException {
        TransferTransactionInfo result = new TransferTransactionInfo();

        if (DexCurrencyValidator.isEthOrPaxAddress(toAddress)) {
            if (dexSmartContractService.isDepositForOrderExist(order.getFromAddress(), order.getId())) {

                String txHash = dexSmartContractService.initiate(createTransactionRequest.getPassphrase(), createTransactionRequest.getSenderAccount().getId(),
                        order.getFromAddress(), order.getId(), secretHash, toAddress, transferWithApprovalDuration / 60, null);
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
            PhasingAppendixV2 phasing = new PhasingAppendixV2(-1, timeService.getEpochTime() + transferWithApprovalDuration, phasingParams, null, secretHash, (byte) 2);
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

    public Transaction transferApl(HttpServletRequest req, Account account, long contractId, long recipient, long amountAtm, int phasingDuration, byte[] secretHash) throws ParameterException, AplException.ValidationException {
        PhasingParams phasingParams = new PhasingParams((byte) 5, 0, 1, 0, (byte) 0, null);
        PhasingAppendixV2 phasing = new PhasingAppendixV2(-1, timeService.getEpochTime() + phasingDuration, phasingParams, null, secretHash, (byte) 2);
        CreateTransactionRequest request = HttpRequestToCreateTransactionRequestConverter.convert(req, account, recipient, 0, new DexControlOfFrozenMoneyAttachment(contractId, amountAtm), false);
        request.setPhasing(phasing);
        request.setPhased(true);
        return dexOrderTransactionCreator.createTransaction(request);
    }

    public boolean depositExists(Long orderId, String address) {
        return dexSmartContractService.isDepositForOrderExist(address, orderId);
    }

    public boolean swapIsRefundable(byte[] hash, String walletAddress) throws AplException.ExecutiveProcessException {
        SwapDataInfo swapData = dexSmartContractService.getSwapData(hash);
        return swapData.getTimeStart() != 0 && swapData.getAddressFrom().equalsIgnoreCase(walletAddress) && timeService.systemTime() > swapData.getTimeDeadLine();
    }
    public boolean swapIsRedeemable(byte[] hash, String address) throws AplException.ExecutiveProcessException {
        SwapDataInfo swapData = dexSmartContractService.getSwapData(hash);
        return swapData.getTimeStart() != 0 && swapData.getAddressTo().equalsIgnoreCase(address) && timeService.systemTime() < swapData.getTimeDeadLine();
    }

    public String initiate(String passphrase, DexOrder order, String toAddress, int durationMinutes, byte[] secretHash) throws AplException.ExecutiveProcessException {
        return dexSmartContractService.initiate(passphrase, order.getAccountId(), order.getFromAddress(), order.getId(), secretHash, toAddress, durationMinutes, null);
    }

    public String redeem(String passphrase, String walletAddress, long accountId, byte[] secret) throws AplException.ExecutiveProcessException {
        return dexSmartContractService.approve(passphrase, secret, walletAddress, accountId);
    }

    public String refundAndWithdraw(String passphrase, String walletAddress, long accountId, byte[] secretHash) throws AplException.ExecutiveProcessException {
        return dexSmartContractService.refundAndWithdraw(secretHash, passphrase, walletAddress, accountId, false);
    }

    public String refund(String passphrase, String walletAddress, long accountId, byte[] secretHash) throws AplException.ExecutiveProcessException {
        return dexSmartContractService.refund(secretHash, passphrase, walletAddress, accountId, false);
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

                boolean approved = dexSmartContractService.approve(passphrase, secret, userOffer.getToAddress(), userAccountId) != null;

                if (!approved) {
                    throw new AplException.ExecutiveProcessException("Transaction:" + txId + " was not approved. (Eth/Pax)");
                }

                log.debug("Transaction:" + txId + " was approved. (Eth/Pax)");

                DexCloseOrderAttachment closeOrderAttachment = new DexCloseOrderAttachment(contractId);
                templatTransactionRequest.setAttachment(closeOrderAttachment);

                //TODO use it for move in to another status.
                Transaction respCloseOffer = dexOrderTransactionCreator.createTransaction(templatTransactionRequest);
                log.debug("Order:" + userOffer.getId() + " was closed. TxId:" + respCloseOffer.getId() + " (Eth/Pax)");

            } else {
                Transaction transaction = blockchain.getTransaction(Long.parseUnsignedLong(txId));
                Integer finishTime = ((PhasingAppendixV2) transaction.getPhasing()).getFinishTime();
                if (finishTime < blockchain.getLastBlockTimestamp()) {
                    log.error("Phasing transaction is finished. TrId: {}, OrderId:{} ", transaction.getId(), userOffer.getId());
                    return false;
                }

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
    public JSONStreamAware createOffer(CustomRequestWrapper requestWrapper, Account account, DexOrder order) throws ParameterException, AplException.ExecutiveProcessException, AplException.ValidationException {
        DexOrder counterOffer = dexMatcherService.findCounterOffer(order);
        String freezeTx = null;
        JSONStreamAware response = new JSONObject();
        Transaction orderTx;
        Transaction contractTx = null;

        if (counterOffer != null) {
            if (counterOffer.getAccountId().equals(order.getAccountId())) {
                throw new ParameterException(JSONResponses.DEX_SELF_ORDER_MATCHING_DENIED);
            }
            // 1. Create order.

            order.setStatus(OrderStatus.PENDING);
            CreateTransactionRequest createOfferTransactionRequest = HttpRequestToCreateTransactionRequestConverter
                    .convert(requestWrapper, account, 0L, 0L, new DexOrderAttachmentV2(order), false);
            orderTx = dexOrderTransactionCreator.createTransaction(createOfferTransactionRequest);
            order.setId(orderTx.getId());

            // 2. Create contract.
            DexContractAttachment contractAttachment = new DexContractAttachment(order.getId(), counterOffer.getId(), null, null, null, null, ExchangeContractStatus.STEP_1, dexConfig.getMinAtomicSwapDuration());
            TransactionResponse transactionResponse = dexOrderTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, contractAttachment, false);
            contractTx = transactionResponse.getTx();

            MandatoryTransaction offerMandatoryTx = new MandatoryTransaction(orderTx, null, null);
            MandatoryTransaction contractMandatoryTx = new MandatoryTransaction(contractTx, offerMandatoryTx.getFullHash(), null);
            mandatoryTransactionDao.insert(offerMandatoryTx);
            mandatoryTransactionDao.insert(contractMandatoryTx);
            transactionProcessor.broadcast(offerMandatoryTx.getTransaction());
            transactionProcessor.broadcastWhenConfirmed(contractTx, orderTx);
        } else {
            CreateTransactionRequest createOfferTransactionRequest = HttpRequestToCreateTransactionRequestConverter
                    .convert(requestWrapper, account, 0L, 0L, new DexOrderAttachmentV2(order), true);
            orderTx = dexOrderTransactionCreator.createTransaction(createOfferTransactionRequest);
            order.setId(orderTx.getId());
        }

        //Freeze ETH or PAX
        if (order.getPairCurrency().isEthOrPax() && order.getType().isBuy()) {
            String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(requestWrapper, true));
            freezeTx = freezeEthPax(passphrase, order);
            log.debug("Create order - frozen money, accountId: {}, offerId: {}", account.getId(), order.getId());
        }

        ((JSONObject) response).put("order", orderTx.getJSONObject());
        if (contractTx != null) {
            ((JSONObject) response).put("contract", contractTx.getJSONObject());
        }
        if (freezeTx != null) {
            ((JSONObject) response).put("frozenTx", freezeTx);
        }

        return response;
    }

    public Transaction createDexOrderTransaction(HttpServletRequest request,
                                                 Account account,
                                                 String fromAddress,
                                                 String toAddress,
                                                 int duration,
                                                 OrderType orderType,
                                                 BigDecimal pairRate,
                                                 OrderStatus orderStatus,
                                                 long orderAmountAtm,
                                                 DexCurrency pairCurrency
                                                 ) throws ParameterException, AplException.ValidationException {
        DexOrder dexOrder = new DexOrder(null, account.getId(), fromAddress, toAddress, orderType, orderStatus, DexCurrency.APL, orderAmountAtm, pairCurrency, pairRate, timeService.getEpochTime() + duration);
        CreateTransactionRequest transactionRequest = HttpRequestToCreateTransactionRequestConverter.convert(request, account, 0, 0, new DexOrderAttachmentV2(dexOrder), true);
        return dexOrderTransactionCreator.createTransaction(transactionRequest);
    }

    public Transaction sendContractStep1Transaction(HttpServletRequest request, Account account, DexOrder order, DexOrder counterOrder, int timeToReply) throws ParameterException, AplException.ValidationException {
        DexContractAttachment contractAttachment = new DexContractAttachment(order.getId(), counterOrder.getId(), null, null, null, null, ExchangeContractStatus.STEP_1, timeToReply);
        CreateTransactionRequest transactionRequest = HttpRequestToCreateTransactionRequestConverter.convert(request, account, 0, 0, contractAttachment, true);
        return dexOrderTransactionCreator.createTransaction(transactionRequest);
    }

    public Transaction sendContractStep2Transaction(HttpServletRequest request, Account account, byte[] secretHash, byte[] encryptedSecret, String counterTransferTx, int timeToReply, ExchangeContract contract) throws ParameterException, AplException.ValidationException {
        DexContractAttachment contractAttachment = new DexContractAttachment(contract);
        contractAttachment.setContractStatus(ExchangeContractStatus.STEP_2);
        contractAttachment.setTimeToReply(timeToReply);
        contractAttachment.setEncryptedSecret(encryptedSecret);
        contractAttachment.setSecretHash(secretHash);
        contractAttachment.setCounterTransferTxId(counterTransferTx);
        CreateTransactionRequest transactionRequest = HttpRequestToCreateTransactionRequestConverter.convert(request, account, 0, 0, contractAttachment, true);
        return dexOrderTransactionCreator.createTransaction(transactionRequest);
    }
    public Transaction sendContractStep3Transaction(HttpServletRequest request, Account account, String transferTx, int timeToReply, ExchangeContract contract) throws ParameterException, AplException.ValidationException {
        DexContractAttachment contractAttachment = new DexContractAttachment(contract.getOrderId(), contract.getCounterOrderId(), null, transferTx, null, null, ExchangeContractStatus.STEP_3, timeToReply);
        CreateTransactionRequest transactionRequest = HttpRequestToCreateTransactionRequestConverter.convert(request, account, 0, 0, contractAttachment, true);
        return dexOrderTransactionCreator.createTransaction(transactionRequest);
    }


    public boolean isTxApproved(byte[] secretHash, String transferTxId) throws AplException.ExecutiveProcessException {
        if(transferTxId == null){
            return false;
        }

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
            return hasAplConfirmations(Convert.parseUnsignedLong(contract.getTransferTxId()), dexConfig.getAplConfirmations());
        } else if (dexOrder.getPairCurrency().isEthOrPax()) { // for now this check is useless, but for future can be used to separate other currencies
            return ethereumWalletService.getNumberOfConfirmations(contract.getTransferTxId()) >= dexConfig.getEthConfirmations();
        } else {
            throw new IllegalArgumentException("Unable to calculate number of confirmations for paired currency - " + dexOrder.getPairCurrency());
        }
    }

    public boolean hasConfirmations(DexOrder dexOrder){
        log.debug("DexService: HasConfirmations reached");
        if (dexOrder.getType() == OrderType.BUY) {
         log.debug("desService: HasConfirmations reached");
            return hasAplConfirmations(dexOrder.getId(), dexConfig.getAplConfirmations());
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

    /**
     * orders in the status Pending doesn't have phasing transaction.
     * @throws AplException.ExecutiveProcessException
     */
    public void reopenPendingOrders(int height, int time) throws AplException.ExecutiveProcessException {
        if (height % 10 == 0 ) { // every ten blocks
            List<DexOrder> pendingOrders = dexOrderTable.getPendingOrdersWithoutContracts(height - Constants.DEX_NUMBER_OF_PENDING_ORDER_CONFIRMATIONS);
            for (DexOrder pendingOrder : pendingOrders) {
                if (pendingOrder.getFinishTime() > time) {
                    pendingOrder.setStatus(OrderStatus.OPEN);
                } else {
                    pendingOrder.setStatus(OrderStatus.EXPIRED);
                    tryRefundApl(pendingOrder);
                }
                saveOrder(pendingOrder);
            }
        }
    }



    public DexOrder closeOrder(long orderId) {
        DexOrder order = getOrder(orderId);
        order.setStatus(OrderStatus.CLOSED);
        // set an actual finish time of the order starting from the dex2.0 height
        if (blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() != null &&
                blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() < blockchain.getLastBlock().getHeight()) {
            order.setFinishTime(blockchain.getLastBlockTimestamp());
        }
        saveOrder(order);
        return order;
    }

    public boolean flushSecureStorage(Long accountID, String passPhrase) {
        return secureStorageService.flushAccountKeys( accountID, passPhrase);
    }



    public void reopenIncomeOrders(Long orderId) {
        closeContractsReopenOrders(dexContractTable.getAllByCounterOrder(orderId));
    }

    public void closeContractsReopenOrders(List<ExchangeContract> contractsForReopen) {
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


    public void onPhasedTxReleased(@Observes @TxEvent(TxEventType.RELEASE_PHASED_TRANSACTION) Transaction transaction) throws AplException.ExecutiveProcessException {
        if (transaction.getType() == DEX.DEX_TRANSFER_MONEY_TRANSACTION) {
            List<PhasingVote> votes = phasingPollService.getVotes(transaction.getId());
            log.debug("Found {} votes, pick latest", votes.size());
            phasingApprovedResultTable.insert(new PhasingApprovalResult(blockchain.getHeight(), transaction.getId(), votes.get(0).getVoteId()));
            ExchangeContract contract = getDexContractById(((DexControlOfFrozenMoneyAttachment) transaction.getAttachment()).getContractId());
            if (blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() != null &&
                blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() < blockchain.getHeight()) {
                if (contract.getContractStatus().isStep4()) {

                    DexOrder order = getOrder(contract.getOrderId());
                    DexOrder counterOrder = getOrder(contract.getCounterOrderId());
                    DexOrder aplOrder = order.getType().isSell() ? order : counterOrder;
                    if (aplOrder.getStatus() == OrderStatus.PHASING_RESULT_PENDING) {
                        log.debug("PHASING_RESULT_PENDING RELEASED, order {}, contract {}, phasing {}", aplOrder.getId(), contract.getId(), transaction.getId());
                        aplOrder.setStatus(OrderStatus.EXPIRED);
                        saveOrder(aplOrder);
                    } else {
                        log.debug("NOT A PHASING_RESULT_PENDING ORDER (release), skip processing order {}, contract {}, phasing {}", aplOrder.getId(), contract.getId(), transaction.getId());
                    }
                }
            }
        }
    }

    public void onPhasedTxReject(@Observes @TxEvent(TxEventType.REJECT_PHASED_TRANSACTION) Transaction transaction) throws AplException.ExecutiveProcessException {
        if (blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() != null &&
                blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() < blockchain.getHeight()) {

            if (transaction.getType() == DEX.DEX_TRANSFER_MONEY_TRANSACTION) {
                long contractId = ((DexControlOfFrozenMoneyAttachment) transaction.getAttachment()).getContractId();
                ExchangeContract exchangeContract = getDexContractById(contractId);

                if (!exchangeContract.getContractStatus().isStep4()) {
                    closeContract(exchangeContract, blockchain.getLastBlockTimestamp());
                } else {
                    DexOrder order = getOrder(exchangeContract.getOrderId());
                    DexOrder counterOrder = getOrder(exchangeContract.getCounterOrderId());
                    DexOrder aplOrder = order.getType().isSell() ? order : counterOrder;
                    if (aplOrder.getStatus() == OrderStatus.PHASING_RESULT_PENDING) {
                        log.debug("PHASING_RESULT_PENDING REJECTED, order {}, contract {}, phasing {}", aplOrder.getId(), exchangeContract.getId(), transaction.getId());
                        if (aplOrder.getFinishTime() > blockchain.getLastBlockTimestamp()) {
                            aplOrder.setStatus(OrderStatus.OPEN);
                        } else {
                            aplOrder.setStatus(OrderStatus.EXPIRED);
                            tryRefundApl(aplOrder);
                        }
                        saveOrder(aplOrder);
                    } else {
                        log.debug("NOT A PHASING_RESULT_PENDING ORDER (reject), skip processing order {}, contract {}, phasing {}", aplOrder.getId(), exchangeContract.getId(), transaction.getId());
                    }
                }
            }
        }
    }

    public DexOrder getLastOrderBeforeTimestamp(DexCurrency currency, int timestamp) {
        int epochTime = Convert2.toEpochTime((long) timestamp * 1000);
        return dexOrderDao.getLastClosedOrderBeforeTimestamp(currency, epochTime);
    }

    public void broadcastWhenConfirmed(Transaction tx, Transaction uncTx) {
        transactionProcessor.broadcastWhenConfirmed(tx, uncTx);
    }

    public EthDepositsWithOffset getUserActiveDeposits(String user, long offset, long limit) throws AplException.ExecutiveProcessException {
        return dexSmartContractService.getUserActiveDeposits(user, offset, limit);
    }

    public EthDepositsWithOffset getUserFilledOrders(String user, long offset, long limit) throws AplException.ExecutiveProcessException {
        return dexSmartContractService.getUserFilledOrders(user, offset, limit);
    }

    //TODO implement some abstract function to download all data.
    public List<EthDepositInfo> getAllUserFilledOrders(String user) throws AplException.ExecutiveProcessException {
        List<EthDepositInfo> ethDepositInfos = new ArrayList<>();
        int offset = 0;
        int limit = 100;

        for (int i = 0; i < MAX_PAGES_FOR_SEARCH; i++) {
            EthDepositsWithOffset ethDepositsWithOffset = dexSmartContractService.getUserFilledOrders(user, offset, limit);
            if (CollectionUtils.isEmpty(ethDepositsWithOffset.getDeposits())) {
                break;
            }
            ethDepositInfos.addAll(ethDepositsWithOffset.getDeposits());

            if (ethDepositsWithOffset.getDeposits().size() < limit) {
                break;
            }
            offset += limit;
        }
        return ethDepositInfos;
    }

    public List<EthDepositInfo> getAllUserActiveDeposits(String user) throws AplException.ExecutiveProcessException {
        List<EthDepositInfo> ethDepositInfos = new ArrayList<>();
        int offset = 0;
        int limit = 100;

        for (int i = 0; i < MAX_PAGES_FOR_SEARCH; i++) {
            EthDepositsWithOffset ethDepositsWithOffset = dexSmartContractService.getUserActiveDeposits(user, offset, limit);
            if (CollectionUtils.isEmpty(ethDepositsWithOffset.getDeposits())) {
                break;
            }
            ethDepositInfos.addAll(ethDepositsWithOffset.getDeposits());

            if (ethDepositsWithOffset.getDeposits().size() < limit) {
                break;
            }
            offset += limit;
        }
        return ethDepositInfos;
    }

    public List<ExchangeContract> getContractsByAccountOrderWithStatus(long accountId, long orderId, byte status) {
        return dexContractDao.getAllForAccountOrder(accountId, orderId, status, status);
    }

    public List<ExchangeContract> getContractsByAccountOrderFromStatus(long accountId, long orderId, byte fromStatus) {
        return dexContractDao.getAllForAccountOrder(accountId, orderId, fromStatus, ExchangeContractStatus.values().length - 1);
    }

    public List<ExchangeContract> getVersionedContractsByAccountOrder(long accountId, long orderId) {
        return dexContractDao.getAllVersionedForAccountOrder(accountId, orderId, 0, ExchangeContractStatus.values().length - 1);
    }

    public List<String> getAllUsers() throws AplException.ExecutiveProcessException {
        List<String> addresses = new ArrayList<>();
        int offset = 0;
        int limit = 100;

        for (int i = 0; i < MAX_PAGES_FOR_SEARCH; i++) {
            UserAddressesWithOffset userAddressesWithOffset = dexSmartContractService.getUserAddresses(offset, limit);
            if (CollectionUtils.isEmpty(userAddressesWithOffset.getAddresses())) {
                break;
            }
            addresses.addAll(userAddressesWithOffset.getAddresses());

            if (userAddressesWithOffset.getAddresses().size() < limit) {
                break;
            }
            offset += limit;
        }

        return addresses;
    }

    public List<AddressEthDepositsInfo> getAllFilledOrders() throws AplException.ExecutiveProcessException {
        List<AddressEthDepositsInfo> addressEthDepositsInfos = new ArrayList<>();
        List<String> addresses = getAllUsers();

        for (String address : addresses) {
            try {
                List<EthDepositInfo> ethDepositInfos = getAllUserFilledOrders(address);
                addressEthDepositsInfos.add(new AddressEthDepositsInfo(address, ethDepositInfos));
            } catch (Exception ex) {
                log.warn(ex.getMessage());
            }

        }

        return addressEthDepositsInfos;
    }

    public List<AddressEthExpiredSwaps> getAllExpiredSwaps() throws AplException.ExecutiveProcessException {
        List<AddressEthExpiredSwaps> addressEthExpiredSwaps = new ArrayList<>();
        List<String> addresses = getAllUsers();

        for (String address : addresses) {
            try {
                List<ExpiredSwap> expiredSwaps = dexSmartContractService.getExpiredSwaps(address);
                addressEthExpiredSwaps.add(new AddressEthExpiredSwaps(address, expiredSwaps));
            } catch (Exception ex) {
                log.warn(ex.getMessage());
            }
        }

        return addressEthExpiredSwaps;
    }

    public List<AddressEthDepositsInfo> getAllActiveDeposits() throws AplException.ExecutiveProcessException {
        List<AddressEthDepositsInfo> addressEthExpiredSwaps = new ArrayList<>();
        List<String> addresses = getAllUsers();

        for (String address : addresses) {
            try {
                List<EthDepositInfo> expiredSwaps = getAllUserActiveDeposits(address);
                addressEthExpiredSwaps.add(new AddressEthDepositsInfo(address, expiredSwaps));
            } catch (Exception ex) {
                log.warn(ex.getMessage());
            }
        }

        return addressEthExpiredSwaps;
    }



}
