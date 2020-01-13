package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockchainEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockchainEventType;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.exception.NotValidTransactionException;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderHeightId;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.TransferTransactionInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import com.apollocurrency.aplwallet.apl.exchange.utils.DexCurrencyValidator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_1;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_2;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_3;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_MAX_TIME_OF_ATOMIC_SWAP;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_MAX_TIME_OF_ATOMIC_SWAP_WITH_BIAS;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_OFFER_PROCESSOR_DELAY;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_IN_PARAMETER;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_OK;

@Slf4j
@Singleton
public class DexOrderProcessor {
    private static final int ORDERS_SELECT_SIZE = 30;

    private static final String SERVICE_NAME = "DexOrderProcessor";
    private static final String BACKGROUND_SERVICE_NAME = SERVICE_NAME + "-background";
    private static final int BACKGROUND_THREADS_NUMBER = 10;
    private static final int SWAP_EXPIRATION_OFFSET = 60; // in seconds

    private final SecureStorageService secureStorageService;
    private final DexService dexService;
    private final TransactionValidator validator;
    private final DexOrderTransactionCreator dexOrderTransactionCreator;
    private final MandatoryTransactionDao mandatoryTransactionDao;
    private final IDexValidator dexValidator;
    private final DexSmartContractService dexSmartContractService;
    private final EthereumWalletService ethereumWalletService;
    private final TaskDispatchManager taskDispatchManager;
    private final PhasingPollService phasingPollService;
    private TaskDispatcher taskDispatcher;
    private TimeService timeService;
    private ExecutorService backgroundExecutor;

    private volatile boolean processorEnabled = true;
    @Getter
    private boolean initialized = false;
    private Blockchain blockchain;

    private final Map<Long, OrderHeightId> accountCancelOrderMap = new HashMap<>();
    private final Map<Long, OrderHeightId> accountExpiredOrderMap = new HashMap<>();
    private final Set<String> expiredSwaps = ConcurrentHashMap.newKeySet();

    @Inject
    public DexOrderProcessor(SecureStorageService secureStorageService, TransactionValidator validator, DexService dexService,
                             DexOrderTransactionCreator dexOrderTransactionCreator, DexValidationServiceImpl dexValidationServiceImpl,
                             DexSmartContractService dexSmartContractService, EthereumWalletService ethereumWalletService,
                             MandatoryTransactionDao mandatoryTransactionDao, TaskDispatchManager taskDispatchManager, TimeService timeService,
                             Blockchain blockchain, PhasingPollService phasingPollService) {
        this.secureStorageService = secureStorageService;
        this.dexService = dexService;
        this.dexOrderTransactionCreator = dexOrderTransactionCreator;
        this.dexValidator = dexValidationServiceImpl;
        this.mandatoryTransactionDao = mandatoryTransactionDao;
        this.dexSmartContractService = dexSmartContractService;
        this.ethereumWalletService = ethereumWalletService;
        this.validator = validator;
        this.timeService = timeService;
        this.taskDispatchManager = Objects.requireNonNull(taskDispatchManager, "Task dispatch manager is NULL.");
        this.blockchain = blockchain;
        this.phasingPollService = phasingPollService;
    }

    @PostConstruct
    public void init(){
        taskDispatcher = taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME);
        Runnable task = () -> {
            if (processorEnabled) {
                long start = System.currentTimeMillis();
                log.info("{}: start", BACKGROUND_SERVICE_NAME);
                try {
                    processContracts();
                } catch (Throwable e) {
                    log.warn("DexOrderProcessor error", e);
                }
                log.info("{}: finish in {} ms", BACKGROUND_SERVICE_NAME, System.currentTimeMillis() - start);
            } else {
                log.debug("Contracts processor is suspended.");
            }
        };

        Task dexOrderProcessorTask = Task.builder()
                .name(SERVICE_NAME)
                .delay((int) TimeUnit.MINUTES.toMillis(DEX_OFFER_PROCESSOR_DELAY))
                .initialDelay((int) TimeUnit.MINUTES.toMillis(DEX_OFFER_PROCESSOR_DELAY))
                .task(task)
                .build();

        taskDispatcher.schedule(dexOrderProcessorTask);

        log.debug("{} initialized. Periodical task configuration: initDelay={} milliseconds, delay={} milliseconds",
                dexOrderProcessorTask.getName(),
                dexOrderProcessorTask.getInitialDelay(),
                dexOrderProcessorTask.getDelay());
        backgroundExecutor = Executors.newFixedThreadPool(BACKGROUND_THREADS_NUMBER, new NamedThreadFactory(BACKGROUND_SERVICE_NAME));
        initialized = true;
    }

    public void onResumeBlockchainEvent(@Observes @BlockchainEvent(BlockchainEventType.RESUME_DOWNLOADING) BlockchainConfig cfg) {
        resumeContractProcessor();
    }

    public void onSuspendBlockchainEvent(@Observes @BlockchainEvent(BlockchainEventType.SUSPEND_DOWNLOADING) BlockchainConfig cfg) {
        suspendContractProcessor();
    }

    public void suspendContractProcessor(){
        taskDispatcher.suspend();
    }

    public void resumeContractProcessor() {
        taskDispatcher.resume();
    }

    @PreDestroy
    public void shutdown() {
        backgroundExecutor.shutdown();
    }


    private void processContracts() {
        if (secureStorageService.isEnabled()) {

            List<Long> accounts = secureStorageService.getAccounts();

            for (Long account : accounts) {
                try {
                    //TODO run this 3 functions not every time. (improve performance)
                    processCancelOrders(account);
                    processExpiredOrders(account);
                    refundDepositsForLostOrders(account);
                    refundExpiredAtomicSwaps(account);

                    processContractsForUserStep1(account);
                    processContractsForUserStep2(account);

                    processIncomeContractsForUserStep3(account);
                    processOutcomeContractsForUserStep3(account);
                    processMandatoryTransactions();
                } catch (Throwable e) {
                    log.error("DexOrderProcessor error, user:" + account, e);
                }
            }
        }
    }

    /**
     * Processing contracts with status step_1.
     *
     * @param accountId
     */
    @Transactional
    private void processContractsForUserStep1(Long accountId) {
        Set<Long> processedOrders = new HashSet<>();

        List<ExchangeContract> contracts = dexService.getDexContracts(DexContractDBRequest.builder()
                .recipient(accountId)
                .status(STEP_1.ordinal())
                .build());

        for (ExchangeContract contract : contracts) {
            try {
                // Process only first order for one user order. (Per processing)
                if (processedOrders.contains(contract.getCounterOrderId())) {
                    continue;
                }

                DexOrder order = dexService.getOrder(contract.getOrderId());
                DexOrder counterOrder = dexService.getOrder(contract.getCounterOrderId());

                if (contract.getCounterTransferTxId() != null) {
                    log.debug("DexContract has been already created.(Step-1) ExchangeContractId:{}", contract.getId());
                    continue;
                }

                if (!counterOrder.getStatus().isOpen()) {
                    log.debug("Exit point 1: Order is in the status: {}, not valid now.", counterOrder.getStatus());
                    continue;
                }

                if (!isContractStep1Valid(contract)) {
                    log.debug("Exit point 2: Order is in the status: {}, not valid now.", counterOrder.getStatus());
                    continue;
                }

                //Generate secret X
                byte[] secretX = new byte[32];
                Crypto.getSecureRandom().nextBytes(secretX);
                byte[] secretHash = Crypto.sha256().digest(secretX);
                String passphrase = secureStorageService.getUserPassPhrase(accountId);
                byte[] encryptedSecretX = Crypto.aesGCMEncrypt(secretX, Crypto.sha256().digest(Convert.toBytes(passphrase)));

                CreateTransactionRequest transferMoneyReq = buildRequest(passphrase, accountId, null, null);

                log.debug("DexOfferProcessor Step-1. User transfer money. accountId:{}, offer {}, counterOffer {}.", accountId, order.getId(), counterOrder.getId());

                TransferTransactionInfo transferTxInfo = dexService.transferMoneyWithApproval(transferMoneyReq, counterOrder, order.getToAddress(), contract.getId(), secretHash, DEX_MAX_TIME_OF_ATOMIC_SWAP);

                log.debug("DexOfferProcessor Step-1. User transferred money accountId: {} , txId: {}.", accountId, transferTxInfo);

                if (transferTxInfo.getTxId() == null) {
                    throw new AplException.ExecutiveProcessException("Transfer money wasn't finish success. Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
                }

                DexContractAttachment contractAttachment = new DexContractAttachment(contract);
                contractAttachment.setContractStatus(ExchangeContractStatus.STEP_2);
                contractAttachment.setCounterTransferTxId(transferTxInfo.getTxId());
                contractAttachment.setSecretHash(secretHash);
                contractAttachment.setEncryptedSecret(encryptedSecretX);
                contractAttachment.setTimeToReply(DEX_MAX_TIME_OF_ATOMIC_SWAP);

                //TODO move it to some util
                CreateTransactionRequest createTransactionRequest = buildRequest(passphrase, accountId, contractAttachment, Constants.ONE_APL * 2);
                createTransactionRequest.setBroadcast(false);
                Transaction contractTx = dexOrderTransactionCreator.createTransaction(createTransactionRequest);
                if (contractTx == null) {
                    throw new AplException.ExecutiveProcessException("Creating contract wasn't finish. Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
                }
                saveAndBroadcastContractWithTransfer(transferTxInfo.getTransaction(), contractTx);

                processedOrders.add(counterOrder.getId());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }
    }

    @Transactional
    void saveAndBroadcastContractWithTransfer(Transaction transferTx, Transaction contractTx) {
        MandatoryTransaction contractMandatoryTx = new MandatoryTransaction(contractTx, null, null);
        mandatoryTransactionDao.insert(contractMandatoryTx);
        if (transferTx != null) {
            MandatoryTransaction transferMandatoryTx = new MandatoryTransaction(transferTx, contractTx.getFullHash(), null);
            mandatoryTransactionDao.insert(transferMandatoryTx);
            broadcastWhenConfirmed(transferTx, contractTx);
        }
    }

    private void broadcastWhenConfirmed(Transaction txToBroadcast, Transaction unconfirmedTx) {
        CompletableFuture.runAsync(() -> {
            while (!dexService.txExists(unconfirmedTx.getId())) {
                try {
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (InterruptedException ignored) {
                }
            }
            try {
                dexService.broadcast(txToBroadcast);
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        });
    }

    private boolean isContractStep1Valid(ExchangeContract exchangeContract) {

        // everything should be vice-versa here since we return our orders back
        long counterOrderID = exchangeContract.getOrderId();
        long orderID = exchangeContract.getCounterOrderId();

        DexOrder mainOrder = dexService.getOrder(orderID);// getOfferByTransactionId(orderID);

        DexOrder counterOrder = dexService.getOrder(counterOrderID);

        return validateAccountBalance(mainOrder, counterOrder);
    }

    private boolean isContractStep2Valid(ExchangeContract exchangeContract) {

        // everything should be vice-versa here since we return our orders back
        long counterOrderID = exchangeContract.getOrderId();
        long orderID = exchangeContract.getCounterOrderId();

        DexOrder ourOrder = dexService.getOrder(counterOrderID);

        DexOrder hisOrder = dexService.getOrder(orderID);

        return validateAccountBalance(ourOrder, hisOrder) && dexService.hasConfirmations(hisOrder);
    }
    private boolean validateAccountBalance(DexOrder myOrder, DexOrder hisOrder) {
        int rx;

        switch (myOrder.getPairCurrency()) {

            case ETH: {
                // return validateOfferETH(myOffer,hisOffer);
                if (myOrder.getType() == OrderType.SELL) {
                    rx = dexValidator.validateOfferSellAplEth(myOrder, hisOrder);
                } else {
                    rx = dexValidator.validateOfferBuyAplEth(myOrder, hisOrder);
                }
                break;
            }

            case PAX: {
                if (myOrder.getType() == OrderType.SELL) {
                    rx = dexValidator.validateOfferSellAplPax(myOrder, hisOrder);
                } else {
                    rx = dexValidator.validateOfferBuyAplPax(myOrder, hisOrder);
                }
                break;
            }

            default: // return OFFER_VALIDATE_ERROR_IN_PARAMETER;
                rx = OFFER_VALIDATE_ERROR_IN_PARAMETER;
                break;
        }

        log.debug("validation result: {}", rx);

        return rx == OFFER_VALIDATE_OK;
    }



    /**
     * Processing contracts with status step_2.
     *
     * @param accountId
     */
    @Transactional
    private void processContractsForUserStep2(Long accountId) {
        Set<Long> processedOrders = new HashSet<>();
        List<ExchangeContract> contracts = dexService.getDexContracts(DexContractDBRequest.builder()
                .sender(accountId)
                .status(STEP_2.ordinal())
                .build());

        for (ExchangeContract contract : contracts) {
            try {
                DexOrder order = dexService.getOrder(contract.getOrderId());
                DexOrder counterOrder = dexService.getOrder(contract.getCounterOrderId());

                if (contract.getTransferTxId() != null) {
                    log.debug("DexContract has been already created.(Step-2) TransferTxId is not null. ExchangeContractId:{}", contract.getId());
                    continue;
                }
                if (contract.getCounterTransferTxId() == null) {
                    log.debug("Counter order hadn't transferred money yet.(Step-2) TransferTxId is null. ExchangeContractId:{}", contract.getId());
                    continue;
                }

                if (processedOrders.contains(order.getId()) || !isContractStep2Valid(contract)) {
                    //TODO do something
                    continue;
                }
                long timeLeft;
                String contractHexHash = Convert.toHexString(contract.getSecretHash());
                if (order.getType() == OrderType.SELL) {
                    SwapDataInfo swapData = dexSmartContractService.getSwapData(contract.getSecretHash());
                    if (StringUtils.isBlank(swapData.getStatus())) {
                        log.debug("Swap {} does not exist", contractHexHash);
                        continue;
                    }
                    Long swapDeadline = swapData.getTimeDeadLine();
                    long currentTime = timeService.systemTime();
                    timeLeft = swapDeadline - currentTime;
                } else {
                    long id = Long.parseUnsignedLong(contract.getCounterTransferTxId());
                    PhasingPoll poll = phasingPollService.getPoll(id);
                    if (poll == null) {
                        log.debug("Account {} did not send transfer tx {}", contract.getRecipient(), id);
                        continue;
                    }
                    PhasingPollResult result = phasingPollService.getResult(id);
                    if (result != null || poll.getFinishTime() <= timeService.getEpochTime()) {
                        log.debug("Apl phasing transfer {} was already finished", id);
                        continue;
                    }
                    timeLeft = poll.getFinishTime() - timeService.getEpochTime();
                }
                if (timeLeft < 0) {
                    log.debug("Contract expired, unable to proceed with exchange process, order - {}, counterOrder - {}, contract id - {}, hash - {}", order.getId(), counterOrder.getId(), contract.getId(), contractHexHash);
                    continue;
                }
                if (timeLeft < DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS) {
                    log.warn("Will not participate in atomic swap (not enough time), timeLeft {} min, expected at least {} min. Hash - {}", timeLeft / 60, DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS / 60, contractHexHash);
                    continue;
                }
                if (timeLeft > DEX_MAX_TIME_OF_ATOMIC_SWAP_WITH_BIAS) {
                    log.warn("Will not participate in atomic swap (duration is too long), timeLeft {} min, expected not above {} min. Hash - {}", timeLeft / 60, DEX_MAX_TIME_OF_ATOMIC_SWAP_WITH_BIAS / 60, contractHexHash);
                    continue;
                }
                long transferWithApprovalDuration = timeLeft / 2;
                String passphrase = secureStorageService.getUserPassPhrase(accountId);

                CreateTransactionRequest transferMoneyReq = buildRequest(passphrase, accountId, null, null);

                log.debug("DexOfferProcessor Step-2. User transfer money. accountId:{}, offer {}, counterOffer {}.", accountId, order.getId(), counterOrder.getId());

                TransferTransactionInfo transferTransactionInfo = dexService.transferMoneyWithApproval(transferMoneyReq, order, counterOrder.getToAddress(), contract.getId(), contract.getSecretHash(), (int) transferWithApprovalDuration);

                log.debug("DexOfferProcessor Step-2. User transferred money accountId: {} , txId: {}.", accountId, transferTransactionInfo.getTxId());

                if (transferTransactionInfo.getTxId() == null) {
                    throw new AplException.ExecutiveProcessException("Transfer money wasn't finish success.(Step-2) Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
                }


                DexContractAttachment contractAttachment = new DexContractAttachment(contract.getOrderId(), contract.getCounterOrderId(), null, transferTransactionInfo.getTxId(), null, null, STEP_3, (int) transferWithApprovalDuration);

                CreateTransactionRequest createTransactionRequest = buildRequest(passphrase, accountId, contractAttachment, Constants.ONE_APL * 2);
                createTransactionRequest.setBroadcast(false);

                Transaction contractTx = dexOrderTransactionCreator.createTransaction(createTransactionRequest);

                if (contractTx == null) {
                    throw new AplException.ExecutiveProcessException("Creating contract wasn't finish. (Step-2) Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId());
                }
                saveAndBroadcastContractWithTransfer(transferTransactionInfo.getTransaction(), contractTx);
                log.debug("DexOfferProcessor Step-2. User created contract (Step-3). accountId: {} , txId: {}.", accountId, contractTx.getId());

                processedOrders.add(order.getId());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Processing contracts with status step_2.
     *
     * @param accountId
     */
    @Transactional
    private void processIncomeContractsForUserStep3(Long accountId) {

        String passphrase = secureStorageService.getUserPassPhrase(accountId);
        List<ExchangeContract> contracts = dexService.getDexContracts(DexContractDBRequest.builder()
                .recipient(accountId)
                .status(STEP_3.ordinal())
                .build());
        for (ExchangeContract contract : contracts) {
            try {
                DexOrder order = dexService.getOrder(contract.getCounterOrderId());

                if (!isContractStep3Valid(contract, order)) {
                    continue;
                }

                log.debug("DexOfferProcessor Step-2 (part-1). accountId: {}", accountId);

                byte[] secret = Crypto.aesGCMDecrypt(contract.getEncryptedSecret(), Crypto.sha256().digest(Convert.toBytes(passphrase)));

                log.debug("DexOfferProcessor Step-2(part-1). Approving money transfer. accountId: {}", accountId);

                dexService.approveMoneyTransfer(passphrase, accountId, contract.getCounterOrderId(), contract.getTransferTxId(), contract.getId(), secret);

                log.debug("DexOfferProcessor Step-2(part-1). Approved money transfer. accountId: {}", accountId);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }


    @Transactional
    private void processOutcomeContractsForUserStep3(Long accountId) {
        String passphrase = secureStorageService.getUserPassPhrase(accountId);

        DexOrderDBRequest dexOrderDBRequest = new DexOrderDBRequest();
        dexOrderDBRequest.setAccountId(accountId);
        dexOrderDBRequest.setStatus(OrderStatus.WAITING_APPROVAL);
        List<DexOrder> outComeOrders = dexService.getOrders(dexOrderDBRequest);

        for (DexOrder outcomeOrder : outComeOrders) {
            try {
                ExchangeContract contract = dexService.getDexContract(DexContractDBRequest.builder()
                        .sender(accountId)
                        .offerId(outcomeOrder.getId())
                        .status(STEP_3.ordinal())
                        .build());

                if (contract == null) {
                    continue;
                }

                log.debug("DexOfferProcessor Step-2(part-2). accountId: {}", accountId);

                //TODO move it to validation function.
                //Check that contract was not approved, and we can get money.
                if (dexService.isTxApproved(contract.getSecretHash(), contract.getCounterTransferTxId())) {
                    continue;
                }

                //Check if contract was approved, and we can get shared secret.
                if (!dexService.isTxApproved(contract.getSecretHash(), contract.getTransferTxId())) {
                    continue;
                }

                //Check that Phasing tx is not finished.
                if (!DexCurrencyValidator.isEthOrPaxAddress(contract.getCounterTransferTxId())) {
                    Transaction transaction = blockchain.getTransaction(Long.parseUnsignedLong(contract.getCounterTransferTxId()));

                    Integer finishTime = ((PhasingAppendixV2) transaction.getPhasing()).getFinishTime();
                    if (finishTime < blockchain.getLastBlockTimestamp()) {
                        log.error("Phasing transaction is finished. TrId: {}, OrderId:{} ", transaction.getId(), outcomeOrder.getId());
                        continue;
                    }
                }

                log.debug("DexOfferProcessor Step-2(part-2). Approving money transfer. accountId: {}", accountId);

                byte[] secret = dexService.getSecretIfTxApproved(contract.getSecretHash(), contract.getTransferTxId());

                dexService.approveMoneyTransfer(passphrase, accountId, outcomeOrder.getId(), contract.getCounterTransferTxId(), contract.getId(), secret);

                log.debug("DexOfferProcessor Step-2(part-2). Approved money transfer. accountId: {}", accountId);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    private boolean isContractStep3Valid(ExchangeContract exchangeContract, DexOrder dexOrder) {
        //TODO add additional validation.

        return /*isContractStep1Valid(exchangeContract) &&*/ dexOrder.getStatus().isWaitingForApproval() && exchangeContract.getTransferTxId() != null && dexService.hasConfirmations(exchangeContract, dexOrder);
    }


    private CreateTransactionRequest buildRequest(String passphrase, Long accountId, Attachment attachment, Long feeATM) throws ParameterException {
        byte[] keySeed = Crypto.getKeySeed(Helper2FA.findAplSecretBytes(accountId, passphrase));
        CreateTransactionRequest transferMoneyReq = CreateTransactionRequest
                .builder()
                .passphrase(passphrase)
                .deadlineValue("1440")
                .publicKey(Crypto.getPublicKey(keySeed))
                .senderAccount(Account.getAccount(accountId))
                .keySeed(keySeed)
                .broadcast(true)
                .recipientId(0L)
                .ecBlockHeight(0)
                .ecBlockId(0L)
                .build();

        if (attachment != null) {
            transferMoneyReq.setAttachment(attachment);
        }
        if (feeATM != null) {
            transferMoneyReq.setFeeATM(feeATM);
        }

        return transferMoneyReq;
    }

    void processCancelOrders(Long accountId) {
        try {
            String passphrase = secureStorageService.getUserPassPhrase(accountId);
            OrderHeightId orderHeightId;
            synchronized (accountCancelOrderMap) {
                orderHeightId = accountCancelOrderMap.get(accountId);
            }
            long fromDbId = orderHeightId == null ? 0 : orderHeightId.getDbId();
            int orderHeight = 0;
            while (true) {
                List<DexOrder> orders = dexService.getOrders(DexOrderDBRequest.builder()
                        .accountId(accountId)
                        .dbId(fromDbId)
                        .status(OrderStatus.CANCEL)
                        .type(OrderType.BUY.ordinal())
                        .limit(ORDERS_SELECT_SIZE)
                        .build());
                for (DexOrder order : orders) {
                    fromDbId = order.getDbId();
                    orderHeight = order.getHeight();
                    try {
                        dexService.refundEthPaxFrozenMoney(passphrase, order);
                    } catch (AplException.ExecutiveProcessException e) {
                        log.info("Unable to refund cancel order {} for {}, reason: {}", order.getPairCurrency(), order.getFromAddress(), e.getMessage());
                    }
                }
                if (orders.size() < ORDERS_SELECT_SIZE) {
                    break;
                }
            }
            if (orderHeight != 0) {
                synchronized (accountCancelOrderMap) {
                    accountCancelOrderMap.put(accountId, new OrderHeightId(fromDbId, orderHeight));
                }
            }
        } catch (NotValidTransactionException ex) {
            log.warn(ex.getMessage());
        }
    }

    void processExpiredOrders(Long accountId) {
        try {
            String passphrase = secureStorageService.getUserPassPhrase(accountId);
            OrderHeightId orderHeightId;
            synchronized (accountExpiredOrderMap) {
                orderHeightId = accountExpiredOrderMap.get(accountId);
            }
            long fromDbId = orderHeightId == null ? 0 : orderHeightId.getDbId();
            int orderHeight = 0;
            while (true) {
                List<DexOrder> orders = dexService.getOrders(DexOrderDBRequest.builder()
                        .accountId(accountId)
                        .dbId(fromDbId)
                        .status(OrderStatus.EXPIRED)
                        .type(OrderType.BUY.ordinal())
                        .limit(ORDERS_SELECT_SIZE)
                        .build());
                for (DexOrder order : orders) {
                    fromDbId = order.getDbId();
                    orderHeight = order.getHeight();
                    try {
                        dexService.refundEthPaxFrozenMoney(passphrase, order);
                    } catch (AplException.ExecutiveProcessException e) {
                        log.info("Unable to refund expired order {} for {}, reason: {}", order.getPairCurrency(), order.getFromAddress(), e.getMessage());
                    }
                }
                if (orders.size() < ORDERS_SELECT_SIZE) {
                    break;
                }
            }
            if (orderHeight != 0) {
                synchronized (accountExpiredOrderMap) {
                    accountExpiredOrderMap.put(accountId, new OrderHeightId(fromDbId, orderHeight));
                }
            }
        } catch (NotValidTransactionException ex) {
            log.warn(ex.getMessage());
        }
    }

    public void refundDepositsForLostOrders(Long accountId) {
        try {
            String passphrase = secureStorageService.getUserPassPhrase(accountId);

            List<String> addresses = dexSmartContractService.getEthUserAddresses(passphrase, accountId);

            for (String address : addresses) {
                try {
                    List<UserEthDepositInfo> deposits = dexService.getUserFilledDeposits(address);

                    for (UserEthDepositInfo deposit : deposits) {
                        DexOrder order = dexService.getOrder(deposit.getOrderId());
                        if (order == null) {
                            Long timeDiff = ethereumWalletService.getLastBlock().getTimestamp().longValue() - deposit.getCreationTime();

                            if (timeDiff > Constants.DEX_MIN_CONTRACT_TIME_WAITING_TO_REPLY) {
                                try {
                                    dexService.refundEthPaxFrozenMoney(passphrase, accountId, deposit.getOrderId(), address);
                                } catch (AplException.ExecutiveProcessException e) {
                                    log.info("Unable to refund lost order {} for {}, reason: {}", deposit.getOrderId(), address, e.getMessage());
                                }
                            }
                        }
                    }
                } catch (AplException.ExecutiveProcessException e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (NotValidTransactionException ex) {
            log.warn(ex.getMessage());
        }
    }

    public void refundExpiredAtomicSwaps(long accountId) {
        String passphrase = secureStorageService.getUserPassPhrase(accountId);

        List<String> addresses = dexSmartContractService.getEthUserAddresses(passphrase, accountId);

        for (String address : addresses) {
            try {
                List<UserEthDepositInfo> deposits = dexSmartContractService.getUserFilledOrders(address);

                for (UserEthDepositInfo deposit : deposits) {
                    Long orderId = deposit.getOrderId();
                    List<ExchangeContract> contracts = dexService.getContractsByAccountOrderFromStatus(accountId, orderId, (byte) 1); // do not check contracts without atomic swap hash

                    for (ExchangeContract contract : contracts) {
                        byte[] swapHash = contract.getSecretHash();
                        if (swapHash == null) { // swap hash may be not exist for STEP4 contracts (e.i. STEP4 contract was an expired 'STEP1' contract earlier)
                            continue;
                        }
                        SwapDataInfo swapData = dexSmartContractService.getSwapData(swapHash);
                        if (StringUtils.isBlank(swapData.getStatus())) { // eth swap is not exists (all fields are empty or zero)
                            continue;
                        }
                        Long timeDeadLine = swapData.getTimeDeadLine();
                        if (timeDeadLine + SWAP_EXPIRATION_OFFSET < timeService.systemTime()) {
                            String swapHashHex = Convert.toHexString(swapHash);
                            if (!expiredSwaps.contains(swapHashHex)) { // skip swaps under processing
                                expiredSwaps.add(swapHashHex);
                                CompletableFuture.supplyAsync(() -> performFullRefund(swapData.getSecretHash(), passphrase, address, accountId, orderId, contract.getId()), backgroundExecutor)
                                        .handle((r, e) -> {
                                            expiredSwaps.remove(swapHashHex);
                                            if (r != null) {
                                                log.debug("Swap {} have got refunding status {}", Convert.toHexString(swapData.getSecretHash()), r);
                                            }
                                            if (e != null) {
                                                log.error("Unknown error occurred during refund", e);
                                            }
                                            return r;
                                        });
                            } else {
                                log.debug("Swap {} is processing now ", swapHashHex);
                            }
                        }
                    }
                }
            } catch (AplException.ExecutiveProcessException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private boolean performFullRefund(byte[] swapHash, String passphrase, String address, long accountId, long orderId, long contractId) {
        boolean success = true;
        // refund + withdraw or just withdraw
        try {
            boolean depositExist = dexSmartContractService.isDepositForOrderExist(address, orderId);
            boolean refundCompleted = true;
            if (!depositExist) {
                log.debug("Refund initiated for order {}, contract {}", orderId, contractId);
                refundCompleted = dexSmartContractService.refund(swapHash, passphrase, address, accountId, true) != null;
                if (!refundCompleted) {
                    log.warn("Unable to send refund tx for order {}, contract {}", orderId, contractId);
                    success = false;
                }
            }
            if (refundCompleted) {
                String hash = dexService.refundEthPaxFrozenMoney(passphrase, accountId, orderId, address);
                if (StringUtils.isNotBlank(hash)) {
                    log.debug("Finished refund for atomic swap {} , order - {}", Convert.toHexString(swapHash), orderId);
                } else {
                    success = false;
                    log.warn("Refund was not finished, unable to withdraw: order - {} ", orderId);
                }
            }
        } catch (AplException.ExecutiveProcessException e) {
            log.error(e.toString(), e);
        }
        return success;
    }

    public void onRollback(@BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        rollbackCachedOrderDbIds(block.getHeight());
    }

    private void rollbackCachedOrderDbIds(int height) {
        CompletableFuture.runAsync(() -> {
            synchronized (accountCancelOrderMap) {
                rollbackToHeight(height, accountCancelOrderMap);
                rollbackToHeight(height, accountExpiredOrderMap);
            }
        });
    }

    public void onScan(@BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        rollbackCachedOrderDbIds(block.getHeight());
    }

    void processMandatoryTransactions() {
        try {
            long dbId = 0;
            while (true) {
                List<MandatoryTransaction> all = mandatoryTransactionDao.getAll(dbId, ORDERS_SELECT_SIZE);
                for (MandatoryTransaction currentTx : all) {
                    try {
                        dbId = currentTx.getDbEntryId();
                        boolean expired = dexService.isExpired(currentTx);
                        boolean confirmed = dexService.txExists(currentTx.getId());
                        if (!expired) {
                            if (!confirmed) {
                                byte[] requiredTxHash = currentTx.getRequiredTxHash();
                                MandatoryTransaction prevRequiredTx = null;
                                boolean brodcast = true; // brodcast current tx
                                while (requiredTxHash != null) {
                                    long id = Convert.fullHashToId(requiredTxHash);
                                    MandatoryTransaction requiredTx = mandatoryTransactionDao.get(id);
                                    boolean hasConfirmations = dexService.hasAplConfirmations(requiredTx.getId(), 0);
                                    if (hasConfirmations) {
                                        if (prevRequiredTx != null) {
                                            validateAndBroadcast(prevRequiredTx.getTransaction());
                                            brodcast = false;
                                        }
                                        break;
                                    } else if (requiredTx.getRequiredTxHash() == null) {
                                        validateAndBroadcast(requiredTx.getTransaction());
                                        break;
                                    }
                                    prevRequiredTx = requiredTx;
                                    requiredTxHash = requiredTx.getRequiredTxHash();
                                }
                                if (brodcast) {
                                    validateAndBroadcast(currentTx.getTransaction());
                                }
                            }
                        } else {
                            mandatoryTransactionDao.delete(currentTx.getId());
                        }
                    } catch (Throwable e) {
                        log.warn("Unable to brodcast mandatory tx {}, reason - {}", currentTx.getId(), e.getMessage());
                    }
                }
                if (all.size() < ORDERS_SELECT_SIZE) {
                    break;
                }
            }
        } catch (NotValidTransactionException ex) {
            log.warn(ex.getMessage());
        }
    }

    private void validateAndBroadcast(Transaction tx) throws AplException.ValidationException {
        validator.validate(tx);
        dexService.broadcast(tx);
    }

    private void rollbackToHeight(int height, Map<Long, OrderHeightId> cash) {
        Set<Long> rolledBackAccountIdsCancell = accountCancelOrderMap.entrySet()
                .stream()
                .filter(e -> e.getValue().getHeight() >= height)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        rolledBackAccountIdsCancell.forEach(cash::remove);
    }
}
