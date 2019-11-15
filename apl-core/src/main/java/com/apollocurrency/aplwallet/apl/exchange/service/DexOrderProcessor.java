package com.apollocurrency.aplwallet.apl.exchange.service;

import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_1;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_2;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_3;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_MAX_TIME_OF_ATOMIC_SWAP;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_MAX_TIME_OF_ATOMIC_SWAP_WITH_BIAS;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_IN_PARAMETER;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_OK;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
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
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
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
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.apollocurrency.aplwallet.apl.util.task.TaskOrder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_1;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_2;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_3;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_OFFER_PROCESSOR_DELAY;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_IN_PARAMETER;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_OK;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class DexOrderProcessor {
    private static final int ORDERS_SELECT_SIZE = 30;

    private static final String BACKGROUND_SERVICE_NAME = "DexOrderProcessor";

    private SecureStorageService secureStorageService;
    private DexService dexService;
    private TransactionValidator validator;
    private DexOrderTransactionCreator dexOrderTransactionCreator;
    private MandatoryTransactionDao mandatoryTransactionDao;
    private IDexValidator dexValidator;
    private DexSmartContractService dexSmartContractService;
    private EthereumWalletService ethereumWalletService;
    private TimeService timeService;
    private TaskDispatchManager taskDispatchManager;

    private volatile boolean processorEnabled = true;
    @Getter
    private boolean initialized = false;

    private final Map<Long, OrderHeightId> accountCancelOrderMap = new HashMap<>();
    private final Map<Long, OrderHeightId> accountExpiredOrderMap = new HashMap<>();

    @Inject
    public DexOrderProcessor(SecureStorageService secureStorageService, TransactionValidator validator, DexService dexService, DexOrderTransactionCreator dexOrderTransactionCreator, DexValidationServiceImpl dexValidationServiceImpl, DexSmartContractService dexSmartContractService, EthereumWalletService ethereumWalletService, MandatoryTransactionDao mandatoryTransactionDao, TaskDispatchManager taskDispatchManager) {
        this.secureStorageService = secureStorageService;
        this.dexService = dexService;
        this.dexOrderTransactionCreator = dexOrderTransactionCreator;
        this.dexValidator = dexValidationServiceImpl;
        this.mandatoryTransactionDao = mandatoryTransactionDao;
        this.dexSmartContractService = dexSmartContractService;
        this.ethereumWalletService = ethereumWalletService;
        this.validator = validator;
        this.taskDispatchManager = Objects.requireNonNull(taskDispatchManager, "Task dispatch manager is NULL.");
    }

    @PostConstruct
    public void init(){
        TaskDispatcher dispatcher = taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME);
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
            }else{
                log.debug("Contracts processor is suspended.");
            }
        };

        Task dexOrderProcessorTask = Task.builder()
                .name(BACKGROUND_SERVICE_NAME)
                .delay((int)TimeUnit.MINUTES.toMillis(DEX_OFFER_PROCESSOR_DELAY))
                .initialDelay((int)TimeUnit.MINUTES.toMillis(DEX_OFFER_PROCESSOR_DELAY))
                .task(task)
                .build();

        dispatcher.schedule(dexOrderProcessorTask, TaskOrder.TASK);

        log.debug("{} initialized. Periodical task configuration: initDelay={} milliseconds, delay={} milliseconds",
                dexOrderProcessorTask.getName(),
                dexOrderProcessorTask.getInitialDelay(),
                dexOrderProcessorTask.getDelay());

        initialized = true;
    }

    public void onResumeBlockchainEvent(@Observes @BlockchainEvent(BlockchainEventType.RESUME_DOWNLOADING) BlockchainConfig cfg){
        resumeContractProcessor();
    }

    public void onSuspendBlockchainEvent(@Observes @BlockchainEvent(BlockchainEventType.SUSPEND_DOWNLOADING) BlockchainConfig cfg){
        suspendContractProcessor();
    }

    public void suspendContractProcessor(){
        processorEnabled = false;
    }

    public void resumeContractProcessor(){
        processorEnabled = true;
    }

    private void processContracts() {
        if (secureStorageService.isEnabled()) {

            List<Long> accounts = secureStorageService.getAccounts();

            for (Long account : accounts) {
                //TODO run this 3 functions not every time. (improve performance)
                processCancelOrders(account);
                processExpiredOrders(account);
                refundDepositsForLostOrders(account);


                processContractsForUserStep1(account);
                processContractsForUserStep2(account);

                processIncomeContractsForUserStep3(account);
                processOutcomeContractsForUserStep3(account);
                processMandatoryTransactions();
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
                                
                if (!counterOrder.getStatus().isOpen() ) {
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
            } catch (AplException.ExecutiveProcessException | AplException.ValidationException | ParameterException e) {
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
        //TODO add validation.
        log.debug("isContractStep1Valid entry point");
        
        // everything should be vice-versa here since we return our orders back        
        long counterOrderID = exchangeContract.getOrderId();
        long orderID = exchangeContract.getCounterOrderId();

        log.debug("offerID: {}, counterOfferID: {}", orderID, counterOrderID);

        DexOrder mainOrder = dexService.getOrder(orderID);// getOfferByTransactionId(orderID);

        if (mainOrder == null) {
            log.debug("main offer search error: ");
            return false;
        } else {
            log.debug("mainOffer search: ok, going further");
        }


        DexOrder counterOrder = dexService.getOrder(counterOrderID);

        if (counterOrder == null) {
            log.debug("counterOffer search error: ");
            return false;
        } else {
            log.debug("counterOffer search: ok, going further");
        }


        // DUMPING main offer : pay attention: should be vice-versa in comparison to ZERO step.. 

        log.debug("MainORDER, type:{} accountId: {}, to: {}, from: {}, pairCurrency: {}, pairRate: {} ", mainOrder.getType(), mainOrder.getAccountId(),
                mainOrder.getToAddress(), mainOrder.getFromAddress(), mainOrder.getPairCurrency(), mainOrder.getPairRate());

        log.debug("CounterORDER, type:{} accountId: {}, to: {}, from: {}, pairCurrency: {}, pairRate: {} ", counterOrder.getType(), counterOrder.getAccountId(),
                counterOrder.getToAddress(), counterOrder.getFromAddress(), counterOrder.getPairCurrency(), counterOrder.getPairRate());

        DexCurrencies curr = counterOrder.getPairCurrency();

        int rx;

        switch (curr) {

            case ETH: {
                // return validateOfferETH(myOffer,hisOffer);
                if (mainOrder.getType() == OrderType.SELL) {
                    rx = dexValidator.validateOfferSellAplEth(mainOrder, counterOrder);
                } else {
                    rx = dexValidator.validateOfferBuyAplEth(mainOrder, counterOrder);
                }
                break;
            }

            case PAX: {
                if (mainOrder.getType() == OrderType.SELL) {
                    rx = dexValidator.validateOfferSellAplPax(mainOrder, counterOrder);
                } else {
                    rx = dexValidator.validateOfferBuyAplPax(mainOrder, counterOrder);
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

                SwapDataInfo swapData = dexSmartContractService.getSwapData(contract.getSecretHash());
                Long swapDeadline = swapData.getTimeDeadLine();
                int currentTime = timeService.getEpochTime();
                long timeLeft = swapDeadline - currentTime;
                if (timeLeft < DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS) {
                    log.warn("Will not participate in atomic swap (not enough time), timeLeft {} min, expected at least {} min", timeLeft / 60, DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS / 60);
                    continue;
                }
                if (timeLeft > DEX_MAX_TIME_OF_ATOMIC_SWAP_WITH_BIAS) {
                    log.warn("Will not participate in atomic swap (duration is too long), timeLeft {} min, expected not above {} min", timeLeft / 60, DEX_MAX_TIME_OF_ATOMIC_SWAP_WITH_BIAS / 60);
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
            } catch (AplException.ExecutiveProcessException | AplException.ValidationException | ParameterException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private boolean isContractStep2Valid(ExchangeContract exchangeContract) {
        //TODO add validation.

        log.debug("Validation for step 2 entry point:");
        DexOrder contractOrder1 = dexService.getOrder(exchangeContract.getOrderId());
        DexOrder contractOrder2 = dexService.getOrder(exchangeContract.getCounterOrderId());
        log.debug("Order1 txID: {}", contractOrder1.getId());
        log.debug("Order2 txID: {}", contractOrder2.getId());
        log.debug("Validation step 2: Order1: type: {}, hisOffer.getToAddress(): {}, hisOffer.fromToAddress(): {}, currency: {}", contractOrder1.getType(),
                contractOrder1.getToAddress(), contractOrder1.getFromAddress(), contractOrder1.getPairCurrency());
        log.debug("Validation step 2: Order2: type: {}, hisOffer.getToAddress(): {}, hisOffer.fromToAddress(): {}, currency: {}", contractOrder2.getType(),
                contractOrder2.getToAddress(), contractOrder2.getFromAddress(), contractOrder2.getPairCurrency());

        // this validation seems to be redundant here.. commented it here so that not to get confused
        return /*isContractStep1Valid(exchangeContract) &&*/ dexService.hasConfirmations(contractOrder1) && dexService.hasConfirmations(contractOrder2) /* && (exchangeContract.getTransferTxId() != null)*/;
    }

    /**
     * Processing contracts with status step_2.
     *
     * @param accountId
     */
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
        log.debug("Validation 3 entry point");

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
    }

    void processExpiredOrders(Long accountId) {
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
    }

    public void refundDepositsForLostOrders(Long accountId) {
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
