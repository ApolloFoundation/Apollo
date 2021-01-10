package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockchainEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockchainEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.model.dex.TransferTransactionInfo;
import com.apollocurrency.aplwallet.apl.core.service.appdata.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.dex.config.DexConfig;
import com.apollocurrency.aplwallet.apl.dex.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.dex.exchange.exception.NotValidTransactionException;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DBSortOrder;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexOperation;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexOrderSortBy;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.EthDepositInfo;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.EthDepositsWithOffset;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.ExpiredSwap;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderHeightId;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.SwapDataInfo;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.util.DexCurrencyValidator;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.config.Property;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.exception.ParameterException;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.apollocurrency.aplwallet.vault.service.Account2FAService;
import lombok.extern.slf4j.Slf4j;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.apl.dex.exchange.model.ExchangeContractStatus.STEP_1;
import static com.apollocurrency.aplwallet.apl.dex.exchange.model.ExchangeContractStatus.STEP_2;
import static com.apollocurrency.aplwallet.apl.dex.exchange.model.ExchangeContractStatus.STEP_3;
import static com.apollocurrency.aplwallet.apl.dex.exchange.model.ExchangeContractStatus.STEP_4;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_IN_PARAMETER;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_OK;

@Slf4j
@Singleton
public class DexOrderProcessor {
    public static final int DEFAULT_DEX_OFFER_PROCESSOR_DELAY = 3 * 60; // 3 min in seconds
    public static final int MIN_DEX_OFFER_PROCESSOR_DELAY = 15; // 15 sec
    private static final String ETH_SWAP_DESCRIPTION_FORMAT = "Account %s initiate atomic swap '%s' with %s under contract %d";
    private static final String ETH_SWAP_S1_DETAILS_FORMAT = "secretHash:%s;encryptedSecret:%s";
    private static final String ETH_SWAP_S2_DETAILS_FORMAT = "secretHash:%s";
    private static final int ORDERS_SELECT_SIZE = 50;
    private static final int CONTRACT_FETCH_SIZE = 50;
    private static final int AMOUNT_ITERATIONS_FOR_ACCOUNT = 5;
    private static final String SERVICE_NAME = "DexOrderProcessor";
    private static final String BACKGROUND_SERVICE_NAME = SERVICE_NAME + "-background";
    private static final int BACKGROUND_THREADS_NUMBER = 10;

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
    private final Map<Long, OrderHeightId> accountCancelOrderMap = new HashMap<>();
    private final Map<Long, OrderHeightId> accountExpiredOrderMap = new HashMap<>();
    private TaskDispatcher taskDispatcher;
    private TimeService timeService;
    private ExecutorService backgroundExecutor;
    private DexOperationService operationService;
    private AccountService accountService;
    private volatile boolean processorEnabled = true;
    private boolean startProcessor;
    private int processingDelay; // seconds
    private DexConfig dexConfig;
    private Blockchain blockchain;
    private final BlockchainConfig blockchainConfig;
    private final Account2FAService account2FAService;

    @Inject
    public DexOrderProcessor(SecureStorageService secureStorageService, TransactionValidator validator, DexService dexService,
                             DexOrderTransactionCreator dexOrderTransactionCreator, DexValidationServiceImpl dexValidationServiceImpl,
                             DexSmartContractService dexSmartContractService, EthereumWalletService ethereumWalletService,
                             MandatoryTransactionDao mandatoryTransactionDao, TaskDispatchManager taskDispatchManager,
                             AccountService accountService,
                             TimeService timeService,
                             Blockchain blockchain, PhasingPollService phasingPollService, DexOperationService operationService,
                             @Property(name = "apl.dex.orderProcessor.enabled", defaultValue = "true") boolean startProcessor,
                             @Property(name = "apl.dex.orderProcessor.delay", defaultValue = "" + DEFAULT_DEX_OFFER_PROCESSOR_DELAY) int processingDelay,
                             DexConfig dexConfig,
                             BlockchainConfig blockchainConfig,
                             Account2FAService account2FAService
    ) {

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
        this.operationService = Objects.requireNonNull(operationService);
        this.startProcessor = startProcessor;
        this.processingDelay = Math.max(MIN_DEX_OFFER_PROCESSOR_DELAY, processingDelay);
        this.accountService = accountService;
        this.dexConfig = dexConfig;
        this.blockchainConfig = blockchainConfig;
        this.account2FAService = account2FAService;
    }

    @PostConstruct
    public void init() {
        if (startProcessor) {
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
                .delay((int) TimeUnit.SECONDS.toMillis(processingDelay))
                .initialDelay((int) TimeUnit.SECONDS.toMillis(processingDelay))
                .task(task)
                .build();

            taskDispatcher.schedule(dexOrderProcessorTask);

            log.debug("{} initialized. Periodical task configuration: initDelay={} milliseconds, delay={} milliseconds",
                dexOrderProcessorTask.getName(),
                dexOrderProcessorTask.getInitialDelay(),
                dexOrderProcessorTask.getDelay());
            backgroundExecutor = Executors.newFixedThreadPool(BACKGROUND_THREADS_NUMBER, new NamedThreadFactory(BACKGROUND_SERVICE_NAME));
        } else {
            log.warn("Dex Order Processor is disabled. Exchange orders processing is not automatic and require a lot of manual operations.");
        }
    }

    public void onResumeBlockchainEvent(@Observes @BlockchainEvent(BlockchainEventType.RESUME_DOWNLOADING) BlockchainConfig cfg) {
        resumeContractProcessor();
    }

    public void onSuspendBlockchainEvent(@Observes @BlockchainEvent(BlockchainEventType.SUSPEND_DOWNLOADING) BlockchainConfig cfg) {
        suspendContractProcessor();
    }

    public void suspendContractProcessor() {
        if (startProcessor) {
            taskDispatcher.suspend();
        }
    }

    public void resumeContractProcessor() {
        if (startProcessor) {
            taskDispatcher.resume();
        }
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
                String passphrase = secureStorageService.getUserPassPhrase(accountId);
                DexOperation op = operationService.getBy(Convert2.defaultRsAccount(accountId), DexOperation.Stage.ETH_SWAP, contract.getId().toString());
                if (op != null) {
                    String details = op.getDetails();
                    String secretHashValue = extractValue(details, "secretHash", true);
                    SwapDataInfo swapData = dexSmartContractService.getSwapData(Convert.parseHexString(secretHashValue));
                    if (swapData.getTimeDeadLine() != 0) {
                        log.info("Will send new contract step2 transaction for already initiated eth swap {}, contract id {}", secretHashValue, contract.getId());
                        String encryptedSecretValue = extractValue(details, "encryptedSecret", true);
                        String txHashValue = extractValue(details, "ethTxHash", false);
                        boolean notFinishedOp = txHashValue == null;
                        if (notFinishedOp) { // query eth node
                            try {
                                txHashValue = dexSmartContractService.getHashForAtomicSwapTransaction(counterOrder.getId());
                                log.debug("Trying to extract eth swap transaction hash from the eth node event logs, result - {}", txHashValue);
                            } catch (NoSuchElementException e) {
                                log.error("Initiated event was not found for order {} and account {}", counterOrder.getId(), Convert2.defaultRsAccount(accountId));
                                continue;
                            } catch (Throwable e) {
                                log.error("Unable to get atomic swap transaction hash from node event logs. Possible cause: filter rpc api is not supported. Will not proceed with exchange process recovering.", e);
                                continue;
                            }
                        }
                        Transaction transaction = createContractTransactionStep2(contract, passphrase, accountId, txHashValue, Convert.parseHexString(secretHashValue), Convert.parseHexString(encryptedSecretValue));
                        dexService.broadcast(transaction);
                        if (notFinishedOp) {
                            finishEthSwapOperation(counterOrder, op, txHashValue);
                        }
                        processedOrders.add(counterOrder.getId());
                        continue;
                    }
                }
                //Generate secret X
                byte[] secretX = new byte[32];
                Crypto.getSecureRandom().nextBytes(secretX);
                byte[] secretHash = Crypto.sha256().digest(secretX);

                byte[] encryptedSecretX = Crypto.aesGCMEncrypt(secretX, Crypto.sha256().digest(Convert.toBytes(passphrase)));

                String rsAccount = Convert2.defaultRsAccount(accountId);
                String secretHashHex = Convert.toHexString(secretHash);
                DexOperation operation = null;
                if (counterOrder.getType() == OrderType.BUY) { // for now - only for buy orders TODO add for all types
                    operation = new DexOperation(null, rsAccount, DexOperation.Stage.ETH_SWAP, contract.getId().toString(),
                        String.format(ETH_SWAP_DESCRIPTION_FORMAT, rsAccount, secretHashHex, order.getToAddress(), contract.getId()),
                        String.format(ETH_SWAP_S1_DETAILS_FORMAT, secretHashHex, Convert.toHexString(encryptedSecretX)), false,
                        new Timestamp(System.currentTimeMillis()));
                    operationService.save(operation);
                }
                log.debug("DexOfferProcessor Step-1. User transfer money. accountId:{}, offer {}, counterOffer {}.", accountId, order.getId(), counterOrder.getId());
                CreateTransactionRequest transferMoneyReq = buildRequest(passphrase, accountId, null, null);
                TransferTransactionInfo transferTxInfo = dexService.transferMoneyWithApproval(transferMoneyReq, counterOrder, order.getToAddress(), contract.getId(), secretHash, dexConfig.getMaxAtomicSwapDuration());
                if (transferTxInfo.getTxId() == null) {
                    throw new AplException.ExecutiveProcessException("Transfer money wasn't sent. Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
                }
                finishEthSwapOperation(counterOrder, operation, transferTxInfo.getTxId());
                log.debug("DexOfferProcessor Step-1. User transferred money accountId: {} , txId: {}.", accountId, transferTxInfo);

                Transaction contractTx = createContractTransactionStep2(contract, passphrase, accountId, transferTxInfo.getTxId(), secretHash, encryptedSecretX);
                saveAndBroadcastContractWithTransfer(transferTxInfo.getTransaction(), contractTx);

                processedOrders.add(counterOrder.getId());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }
    }

    private void finishEthSwapOperation(DexOrder order, DexOperation operation, String txHash) {
        if (order.getType() == OrderType.BUY) {
            operation.setDetails(operation.getDetails() + ";ethTxHash:" + txHash);
            operationService.finish(operation);
        }
    }

    private String extractValue(String string, String name, boolean mandatory) {
        int valueIndex = string.indexOf(name);
        if (valueIndex == -1) {
            if (mandatory) {
                throw new IllegalStateException("Incorrect format of string " + string + " , unable to find " + name);
            } else {
                return null;
            }
        }
        int endIndex = string.indexOf(";", valueIndex);
        return string.substring(string.indexOf(":", valueIndex), endIndex == -1 ? string.length() : endIndex);
    }

    private Transaction createContractTransactionStep2(ExchangeContract contract, String passphrase, Long accountId, String transferTxId, byte[] secretHash, byte[] encryptedSecret) throws ParameterException, AplException.ValidationException, AplException.ExecutiveProcessException {
        DexContractAttachment contractAttachment = new DexContractAttachment(contract);
        contractAttachment.setContractStatus(ExchangeContractStatus.STEP_2);
        contractAttachment.setCounterTransferTxId(transferTxId);
        contractAttachment.setSecretHash(secretHash);
        contractAttachment.setEncryptedSecret(encryptedSecret);
        contractAttachment.setTimeToReply(dexConfig.getMaxAtomicSwapDuration());

        //TODO move it to some util
        CreateTransactionRequest createTransactionRequest = buildRequest(passphrase, accountId, contractAttachment, Math.multiplyExact(2, blockchainConfig.getOneAPL()));
        createTransactionRequest.setBroadcast(false);
        Transaction contractTx = dexOrderTransactionCreator.createTransactionAndBroadcastIfRequired(createTransactionRequest);
        if (contractTx == null) {
            throw new AplException.ExecutiveProcessException("Creating contract wasn't finish. Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
        }
        return contractTx;
    }

    @Transactional
    void saveAndBroadcastContractWithTransfer(Transaction transferTx, Transaction contractTx) {
        MandatoryTransaction contractMandatoryTx = new MandatoryTransaction(contractTx, null, null);
        mandatoryTransactionDao.insert(contractMandatoryTx);
        if (transferTx != null) {
            MandatoryTransaction transferMandatoryTx = new MandatoryTransaction(transferTx, contractTx.getFullHash(), null);
            mandatoryTransactionDao.insert(transferMandatoryTx);
            dexService.broadcastWhenConfirmed(transferTx, contractTx);
        } else {
            dexService.broadcast(contractTx);
        }
    }

    private boolean isContractStep1Valid(ExchangeContract exchangeContract) {
        long orderID = exchangeContract.getOrderId();
        long counterOrderID = exchangeContract.getCounterOrderId();

        DexOrder myOrder = dexService.getOrder(counterOrderID);
        DexOrder hisOrder = dexService.getOrder(orderID);

        return validateAccountBalance(myOrder, hisOrder, exchangeContract);
    }

    private boolean isContractStep2Valid(ExchangeContract exchangeContract) {
        long orderID = exchangeContract.getOrderId();
        long counterOrderID = exchangeContract.getCounterOrderId();

        DexOrder myOrder = dexService.getOrder(orderID);
        DexOrder hisOrder = dexService.getOrder(counterOrderID);

        return validateAccountBalance(myOrder, hisOrder, exchangeContract) && dexService.hasConfirmations(hisOrder);
    }

    private boolean validateAccountBalance(DexOrder myOrder, DexOrder hisOrder, ExchangeContract contract) {
        int rx;

        switch (myOrder.getPairCurrency()) {

            case ETH: {
                // return validateOfferETH(myOffer,hisOffer);
                if (myOrder.getType() == OrderType.SELL) {
                    if (contract.getContractStatus().isStep1()) {
                        rx = dexValidator.validateOfferSellAplEthActiveDeposit(myOrder, hisOrder);
                    } else {
                        rx = dexValidator.validateOfferSellAplEthAtomicSwap(myOrder, hisOrder, contract.getSecretHash());
                    }
                } else {
                    if (contract.getContractStatus().isStep1()) {
                        rx = dexValidator.validateOfferBuyAplEth(myOrder, hisOrder);
                    } else {
                        rx = dexValidator.validateOfferBuyAplEthPhasing(myOrder, hisOrder, Long.parseUnsignedLong(contract.getCounterTransferTxId()));
                    }
                }
                break;
            }

            case PAX: {
                if (myOrder.getType() == OrderType.SELL) {
                    if (contract.getContractStatus().isStep1()) {
                        rx = dexValidator.validateOfferSellAplPaxActiveDeposit(myOrder, hisOrder);
                    } else {
                        rx = dexValidator.validateOfferSellAplPaxAtomicSwap(myOrder, hisOrder, contract.getSecretHash());
                    }
                } else {
                    if (contract.getContractStatus().isStep1()) {
                        rx = dexValidator.validateOfferBuyAplPax(myOrder, hisOrder);
                    } else {
                        rx = dexValidator.validateOfferBuyAplEthPhasing(myOrder, hisOrder, Long.parseUnsignedLong(contract.getCounterTransferTxId()));
                    }
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
                if (processedOrders.contains(contract.getOrderId())) {
                    continue;
                }
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
                String passphrase = secureStorageService.getUserPassPhrase(accountId);
                DexOperation op = operationService.getBy(Convert2.defaultRsAccount(accountId), DexOperation.Stage.ETH_SWAP, contract.getId().toString());
                if (op != null) {
                    String details = op.getDetails();
                    String secretHashValue = extractValue(details, "secretHash", true);
                    SwapDataInfo swapData = dexSmartContractService.getSwapData(Convert.parseHexString(secretHashValue));
                    if (swapData.getTimeDeadLine() != 0) {
                        long timeLeft = swapData.getTimeDeadLine() - timeService.systemTime();
                        if (timeLeft < dexConfig.getMinAtomicSwapDurationWithDeviation()) {
                            log.info("Will not send dex contract transaction to recover exchange process, not enough time 'timeLeft'={} sec.", timeLeft);
                        }
                        log.info("Will send new contract step3 transaction for already initiated eth swap {}, contract id {}", secretHashValue, contract.getId());
                        String txHashValue = extractValue(details, "ethTxHash", false);
                        boolean notFinishedOp = txHashValue == null;
                        if (notFinishedOp) { // query eth node
                            try {
                                txHashValue = dexSmartContractService.getHashForAtomicSwapTransaction(order.getId());
                                log.debug("Trying to extract eth swap transaction hash from the eth node event logs, result - {}", txHashValue);
                            } catch (NoSuchElementException e) {
                                log.error("Initiated event was not found for order {} and account {}", order.getId(), Convert2.defaultRsAccount(accountId));
                                continue;
                            } catch (Throwable e) {
                                log.error("Unable to get atomic swap transaction hash from node event logs. Possible cause: filter rpc api is not supported. Will not proceed with exchange process recovering.", e);
                                continue;
                            }
                        }

                        Transaction transaction = createContractTransactionStep3(contract, txHashValue, passphrase, accountId, timeLeft);
                        dexService.broadcast(transaction);
                        if (notFinishedOp) {
                            finishEthSwapOperation(order, op, txHashValue);
                        }
                        processedOrders.add(order.getId());
                        continue;
                    }
                }

                if (!isContractStep2Valid(contract)) {
                    //TODO do something
                    continue;
                }

                long timeLeft;
                if (order.getType() == OrderType.SELL) {
                    SwapDataInfo swapData = dexSmartContractService.getSwapData(contract.getSecretHash());

                    Long swapDeadline = swapData.getTimeDeadLine();
                    long currentTime = timeService.systemTime();
                    timeLeft = swapDeadline - currentTime;
                } else {
                    long id = Long.parseUnsignedLong(contract.getCounterTransferTxId());
                    PhasingPoll poll = phasingPollService.getPoll(id);
                    timeLeft = poll.getFinishTime() - timeService.getEpochTime();
                }
                long transferWithApprovalDuration = timeLeft / 2;


                CreateTransactionRequest transferMoneyReq = buildRequest(passphrase, accountId, null, null);

                log.debug("DexOfferProcessor Step-2. User transfer money. accountId:{}, offer {}, counterOffer {}.", accountId, order.getId(), counterOrder.getId());
                DexOperation operation = null;
                if (order.getType() == OrderType.BUY) { // for now - only for buy orders TODO add for all types
                    String rsAccount = Convert2.defaultRsAccount(accountId);
                    String secretHashHex = Convert.toHexString(contract.getSecretHash());
                    operation = new DexOperation(null, rsAccount, DexOperation.Stage.ETH_SWAP, contract.getId().toString(),
                        String.format(ETH_SWAP_DESCRIPTION_FORMAT, rsAccount, secretHashHex, counterOrder.getToAddress(), contract.getId()),
                        String.format(ETH_SWAP_S2_DETAILS_FORMAT, secretHashHex), false,
                        new Timestamp(System.currentTimeMillis()));
                    operationService.save(operation);
                }
                TransferTransactionInfo transferTransactionInfo = dexService.transferMoneyWithApproval(transferMoneyReq, order, counterOrder.getToAddress(), contract.getId(), contract.getSecretHash(), (int) transferWithApprovalDuration);

                log.debug("DexOfferProcessor Step-2. User transferred money accountId: {} , txId: {}.", accountId, transferTransactionInfo.getTxId());

                if (transferTransactionInfo.getTxId() == null) {
                    throw new AplException.ExecutiveProcessException("Transfer money wasn't finish success.(Step-2) Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
                }
                finishEthSwapOperation(order, operation, transferTransactionInfo.getTxId());

                DexContractAttachment contractAttachment = new DexContractAttachment(contract.getOrderId(), contract.getCounterOrderId(), null, transferTransactionInfo.getTxId(), null, null, STEP_3, (int) transferWithApprovalDuration);

                CreateTransactionRequest createTransactionRequest = buildRequest(passphrase, accountId, contractAttachment, Math.multiplyExact(2, blockchainConfig.getOneAPL()));
                createTransactionRequest.setBroadcast(false);

                Transaction contractTx = dexOrderTransactionCreator.createTransactionAndBroadcastIfRequired(createTransactionRequest);

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

    private Transaction createContractTransactionStep3(ExchangeContract contract, String txHash, String passphrase, Long accountId, long transferWithApprovalDuration) throws ParameterException, AplException.ValidationException, AplException.ExecutiveProcessException {
        DexContractAttachment contractAttachment = new DexContractAttachment(contract.getOrderId(), contract.getCounterOrderId(), null, txHash, null, null, STEP_3, (int) transferWithApprovalDuration);

        CreateTransactionRequest createTransactionRequest = buildRequest(passphrase, accountId, contractAttachment, Math.multiplyExact(2, blockchainConfig.getOneAPL()));
        createTransactionRequest.setBroadcast(false);

        Transaction contractTx = dexOrderTransactionCreator.createTransactionAndBroadcastIfRequired(createTransactionRequest);

        if (contractTx == null) {
            throw new AplException.ExecutiveProcessException("Creating contract wasn't finish. (Step-2) Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId());
        }
        return contractTx;
    }

    /**
     * Processing contracts with status step_3.
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

                log.debug("DexOfferProcessor Step-3 (part-1). accountId: {}", accountId);

                byte[] secret = Crypto.aesGCMDecrypt(contract.getEncryptedSecret(), Crypto.sha256().digest(Convert.toBytes(passphrase)));

                log.debug("DexOfferProcessor Step-3(part-1). Approving money transfer. accountId: {}", accountId);

                dexService.approveMoneyTransfer(passphrase, accountId, contract.getCounterOrderId(), contract.getTransferTxId(), contract.getId(), secret);

                log.debug("DexOfferProcessor Step-3(part-1). Approved money transfer. accountId: {}", accountId);
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
                    .build(), Arrays.asList(STEP_3, STEP_4));

                if (contract == null) {
                    continue;
                }

                log.debug("DexOfferProcessor Step-3(part-2). accountId: {}", accountId);

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

                log.debug("DexOfferProcessor Step-3(part-2). Approving money transfer. accountId: {}", accountId);

                byte[] secret = dexService.getSecretIfTxApproved(contract.getSecretHash(), contract.getTransferTxId());

                dexService.approveMoneyTransfer(passphrase, accountId, outcomeOrder.getId(), contract.getCounterTransferTxId(), contract.getId(), secret);

                log.debug("DexOfferProcessor Step-3(part-2). Approved money transfer. accountId: {}", accountId);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    private boolean isContractStep3Valid(ExchangeContract exchangeContract, DexOrder dexOrder) {
        //TODO add additional validation.

        return /*isContractStep1Valid(exchangeContract) &&*/ dexOrder.getStatus().isWaitingForApproval() && exchangeContract.getTransferTxId() != null && dexService.hasConfirmations(exchangeContract, dexOrder);
    }


    private CreateTransactionRequest buildRequest(String passphrase, Long accountId, Attachment attachment, Long feeATM) {
        byte[] keySeed = Crypto.getKeySeed(account2FAService.findAplSecretBytes(accountId, passphrase));
        CreateTransactionRequest transferMoneyReq = CreateTransactionRequest
            .builder()
            .passphrase(passphrase)
            .deadlineValue("1440")
            .publicKey(Crypto.getPublicKey(keySeed))
            .senderAccount(accountService.getAccount(accountId))
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
        refundUserEthPax(accountId, OrderStatus.CANCEL, accountCancelOrderMap);
    }

    void processExpiredOrders(Long accountId) {
        refundUserEthPax(accountId, OrderStatus.EXPIRED, accountExpiredOrderMap);
    }

    private void refundUserEthPax(Long accountId, OrderStatus orderStatus, Map<Long, OrderHeightId> cache) {
        OrderHeightId orderHeightId;
        synchronized (cache) {
            orderHeightId = cache.get(accountId);
        }

        String passphrase = secureStorageService.getUserPassPhrase(accountId);
        long fromDbId = orderHeightId == null ? 0 : orderHeightId.getDbId();
        DexOrder lastSuccessOrder = null;
        boolean wasException = false;

        for (int i = 0; i < AMOUNT_ITERATIONS_FOR_ACCOUNT; i++) {
            List<DexOrder> orders = dexService.getOrders(DexOrderDBRequest.builder()
                .accountId(accountId)
                .dbId(fromDbId)
                .status(orderStatus)
                .type(OrderType.BUY.ordinal())
                .limit(ORDERS_SELECT_SIZE)
                .sortBy(DexOrderSortBy.DB_ID)
                .sortOrder(DBSortOrder.ASC)
                .build());

            for (DexOrder order : orders) {
                try {
                    if (order.getFromAddress() == null && order.getToAddress() == null) {
                        log.debug("Old format order: {}, account {}, skip processing", order.getId(), order.getAccountId());
                    } else {
                        String ethAddress = DexCurrencyValidator.isEthOrPaxAddress(order.getFromAddress()) ? order.getFromAddress() : order.getToAddress();
                        if (dexSmartContractService.isDepositForOrderExist(ethAddress, order.getId())) {
                            dexService.refundEthPaxFrozenMoney(passphrase, order, false);
                        }
                    }
                } catch (AplException.ExecutiveProcessException e) {
                    wasException = true;
                    log.info("Unable to refund cancel order {} for {}, reason: {}", order.getPairCurrency(), order.getFromAddress(), e.getMessage());
                } catch (Exception e) {
                    wasException = true;
                    log.error(e.getMessage(), e);
                }

                fromDbId = order.getDbId();
                if (!wasException) {
                    lastSuccessOrder = order;
                }
            }
            if (orders.size() < ORDERS_SELECT_SIZE) {
                break;
            }
        }
        if (lastSuccessOrder != null && (orderHeightId == null || orderHeightId.getDbId() < lastSuccessOrder.getDbId())) {
            synchronized (cache) {
                cache.put(accountId, new OrderHeightId(lastSuccessOrder.getDbId()));
            }
        }
    }

    public void refundDepositsForLostOrders(Long accountId) {
        try {
            String passphrase = secureStorageService.getUserPassPhrase(accountId);

            List<String> addresses = dexSmartContractService.getEthUserAddresses(passphrase, accountId);

            for (String address : addresses) {
                try {
                    long offset = 0;
                    EthDepositsWithOffset withOffset;
                    do {
                        withOffset = dexService.getUserActiveDeposits(address, offset, CONTRACT_FETCH_SIZE);
                        List<EthDepositInfo> deposits = withOffset.getDeposits();
                        offset = withOffset.getOffset();
                        for (EthDepositInfo deposit : deposits) {
                            DexOrder order = dexService.getOrder(deposit.getOrderId());
                            if (order == null) {
                                long timeDiff = ethereumWalletService.getLastBlock().getTimestamp().longValue() - deposit.getCreationTime();

                                if (timeDiff > dexConfig.getOrphanDepositLifetime()) {
                                    try {
                                        dexService.refundEthPaxFrozenMoney(passphrase, accountId, deposit.getOrderId(), address);
                                    } catch (AplException.ExecutiveProcessException e) {
                                        log.info("Unable to refund lost order {} for {}, reason: {}", deposit.getOrderId(), address, e.getMessage());
                                    }
                                }
                            }
                        }
                    } while (withOffset.getDeposits().size() == CONTRACT_FETCH_SIZE);
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
                List<ExpiredSwap> expiredSwaps = dexSmartContractService.getExpiredSwaps(address);
                for (ExpiredSwap expiredSwap : expiredSwaps) {
                    log.info("Refunding atomic swap {}, id {}", Numeric.toHexString(expiredSwap.getSecretHash()), expiredSwap.getOrderId());
                    dexSmartContractService.refundAndWithdraw(expiredSwap.getSecretHash(), passphrase, address, accountId, false);
                }
            } catch (AplException.ExecutiveProcessException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void validateAndBroadcast(Transaction tx) throws AplException.ValidationException {
        validator.validateFully(tx);
        dexService.broadcast(tx);
    }

}
