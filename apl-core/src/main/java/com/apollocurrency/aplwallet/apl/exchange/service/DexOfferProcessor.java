package com.apollocurrency.aplwallet.apl.exchange.service;

import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_1;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_2;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_3;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_IN_PARAMETER;
import lombok.extern.slf4j.Slf4j;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;


import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;


@Slf4j
@Singleton
public class DexOfferProcessor {

    private SecureStorageService secureStorageService;
    private DexService dexService;
    private DexOfferTransactionCreator dexOfferTransactionCreator;
    
    private IDexValidator dexValidator;

    @Inject
    public DexOfferProcessor(SecureStorageService secureStorageService, DexService dexService, 
            DexValidationServiceImpl dexValidator, DexOfferTransactionCreator dexOfferTransactionCreator) {
        this.secureStorageService = secureStorageService;
        this.dexService = dexService;
        this.dexOfferTransactionCreator = dexOfferTransactionCreator;
        this.dexValidator = dexValidator;
    }


    public void processContracts() {
        List<Long> accounts = secureStorageService.getAccounts();

        for (Long account : accounts) {
            processContractsForUserStep1(account);
            processContractsForUserStep2(account);

            processIncomeContractsForUserStep3(account);
            processOutcomeContractsForUserStep3(account);
        }

    }

    /**
     * Processing contracts with status step_1.
     *
     * @param accountId
     */
    private void processContractsForUserStep1(Long accountId) {
        List<ExchangeContract> contracts = dexService.getDexContracts(DexContractDBRequest.builder()
                .recipient(accountId)
                .status(STEP_1.ordinal())
                .build());

        for (ExchangeContract contract : contracts) {
            try {
                DexOffer offer = dexService.getOfferByTransactionId(contract.getOrderId());
                DexOffer counterOffer = dexService.getOfferByTransactionId(contract.getCounterOrderId());

                if (contract.getCounterTransferTxId() != null) {
                    log.debug("DexContract has been already created.(Step-1) ExchangeContractId:{}", contract.getId());
                    continue;
                }
                
                
                if (!counterOffer.getStatus().isOpen() )  {
                    log.debug("order: {} is not in open status (status: {}), skipping", counterOffer.getAccountId(), counterOffer.getStatus() );
                    continue;
                } else {
                    log.debug("order: {} is open, doing further checkups", counterOffer.getAccountId() );
                }
                
                if (!isContractStep1Valid(contract)) {
                    log.debug("Order is in the status: {}, not valid now.", counterOffer.getStatus());//TODO do something.
                    continue;                    
                } else {
                    log.debug("Order {} is validated, going deeper", counterOffer.getAccountId());
                }
                
                
//                if (!counterOffer.getStatus().isOpen() || !isContractStep1Valid(contract)) {
//                    log.debug("Order is in the status: {}, not valid now.", counterOffer.getStatus());//TODO do something.
//                    continue;
//                }
            



                
                
                //Generate secret X
                byte[] secretX = new byte[32];
                Crypto.getSecureRandom().nextBytes(secretX);
                byte[] secretHash = Crypto.sha256().digest(secretX);
                String passphrase = secureStorageService.getUserPassPhrase(accountId);
                byte[] encryptedSecretX = Crypto.aesGCMEncrypt(secretX, Crypto.sha256().digest(Convert.toBytes(passphrase)));

                CreateTransactionRequest transferMoneyReq = buildRequest(passphrase, accountId, null, null);

                log.debug("DexOfferProcessor Step-1. User transfer money. accountId:{}, offer {}, counterOffer {}.", accountId, offer.getTransactionId(), counterOffer.getTransactionId());

                String txId = dexService.transferMoneyWithApproval(transferMoneyReq, counterOffer, offer.getToAddress(), secretHash, STEP_2);

                log.debug("DexOfferProcessor Step-1. User transferred money accountId: {} , txId: {}.", accountId, txId);

                if (txId == null) {
                    throw new AplException.ExecutiveProcessException("Transfer money wasn't finish success. Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
                }

                DexContractAttachment contractAttachment = new DexContractAttachment(contract);
                contractAttachment.setContractStatus(ExchangeContractStatus.STEP_2);
                contractAttachment.setCounterTransferTxId(txId);
                contractAttachment.setSecretHash(secretHash);
                contractAttachment.setEncryptedSecret(encryptedSecretX);


                //TODO move it to some util
                CreateTransactionRequest createTransactionRequest = buildRequest(passphrase, accountId, contractAttachment, Constants.ONE_APL * 2);

                Transaction transaction = dexOfferTransactionCreator.createTransaction(createTransactionRequest);

                if (transaction == null) {
                    throw new AplException.ExecutiveProcessException("Creating contract wasn't finish. Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
                }
                log.debug("DexOfferProcessor Step-1. User created contract (Step-2). accountId: {} , txId: {}.", accountId, transaction.getId());

            } catch (AplException.ExecutiveProcessException | AplException.ValidationException | ParameterException e) {
                log.error(e.getMessage(), e);
                continue;
            }

        }
    }

    private boolean isContractStep1Valid(ExchangeContract exchangeContract) {
        //TODO add validation.
        
        
        // dexService.getDexContract(dexContractDBRequest)        
        // DexContractDBRequest dBRequest = new DeMaskProcessor
        log.debug("isContractStep1Valid entry point");
        long counterOrderID = exchangeContract.getCounterOrderId();                
        long orderID = exchangeContract.getOrderId();
        
        log.debug("offerID: {}, counterOfferID: {}", orderID, counterOrderID);
        
        DexOffer mainOffer = dexService.getOfferByTransactionId(orderID);
        
        if (mainOffer == null) {
            log.debug("main offer search error: ");
            return false;
        } else {
            log.debug("mainOffer search: ok, going further");
        }
        
                      
        DexOffer counterOffer = dexService.getOfferByTransactionId(counterOrderID);

        if (counterOffer == null) {
            log.debug("counterOffer search error: ");
            return false;
        } else {
            log.debug("counterOffer search: ok, going further");
        }

        
        // DUMPING main offer 
        
        log.debug("MainORDER, type:{} accountId: {}, to: {}, from: {}, pairCurrency: {}, pairRate: {} ", mainOffer.getType(), mainOffer.getAccountId(), 
                mainOffer.getToAddress(), mainOffer.getFromAddress(), mainOffer.getPairCurrency(), mainOffer.getPairRate());

        log.debug("CounterORDER, type:{} accountId: {}, to: {}, from: {}, pairCurrency: {}, pairRate: {} ", counterOffer.getType(), counterOffer.getAccountId(), 
                counterOffer.getToAddress(), counterOffer.getFromAddress(), counterOffer.getPairCurrency(), counterOffer.getPairRate());
        
        DexCurrencies curr = counterOffer.getPairCurrency();
        
        int rx=0;
        
        switch (curr) {

            case ETH: { 
                // return validateOfferETH(myOffer,hisOffer);
                if (mainOffer.getType() == OfferType.SELL) 
                    rx =  dexValidator.validateOfferSellAplEth(mainOffer, counterOffer); 
                else rx=  dexValidator.validateOfferBuyAplEth(mainOffer, counterOffer);                
                break;
            }
            
            case PAX: {
                if (mainOffer.getType() == OfferType.SELL) 
                    rx = dexValidator.validateOfferSellAplPax(mainOffer, counterOffer); 
                else rx = dexValidator.validateOfferBuyAplPax(mainOffer, counterOffer);                                
            }
            
            default: // return OFFER_VALIDATE_ERROR_IN_PARAMETER;
                rx = OFFER_VALIDATE_ERROR_IN_PARAMETER;
                break;
        }        
        
        log.debug("validation result: {}",rx);
        
        
        
        return true;
    }


    /**
     * Processing contracts with status step_2.
     *
     * @param accountId
     */
    private void processContractsForUserStep2(Long accountId) {
        List<ExchangeContract> contracts = dexService.getDexContracts(DexContractDBRequest.builder()
                .sender(accountId)
                .status(STEP_2.ordinal())
                .build());

        for (ExchangeContract contract : contracts) {
            try {
                DexOffer offer = dexService.getOfferByTransactionId(contract.getOrderId());
                DexOffer counterOffer = dexService.getOfferByTransactionId(contract.getCounterOrderId());

                if (contract.getTransferTxId() != null) {
                    log.debug("DexContract has been already created.(Step-2) TransferTxId is not null. ExchangeContractId:{}", contract.getId());
                    continue;
                }
                if (contract.getCounterTransferTxId() == null) {
                    log.debug("Counter order hadn't transferred money yet.(Step-2) TransferTxId is null. ExchangeContractId:{}", contract.getId());
                    continue;
                }

                if (!isContractStep2Valid(contract)) {
                    //TODO do something
                    continue;
                }

                String passphrase = secureStorageService.getUserPassPhrase(accountId);

                CreateTransactionRequest transferMoneyReq = buildRequest(passphrase, accountId, null, null);

                log.debug("DexOfferProcessor Step-2. User transfer money. accountId:{}, offer {}, counterOffer {}.", accountId, offer.getTransactionId(), counterOffer.getTransactionId());

                String txId = dexService.transferMoneyWithApproval(transferMoneyReq, offer, counterOffer.getToAddress(), contract.getSecretHash(), STEP_2);

                log.debug("DexOfferProcessor Step-2. User transferred money accountId: {} , txId: {}.", accountId, txId);

                if (txId == null) {
                    throw new AplException.ExecutiveProcessException("Transfer money wasn't finish success.(Step-2) Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId() + ", " + contract.getContractStatus());
                }


                DexContractAttachment contractAttachment = new DexContractAttachment(contract);
                contractAttachment.setContractStatus(ExchangeContractStatus.STEP_3);
                contractAttachment.setTransferTxId(txId);

                CreateTransactionRequest createTransactionRequest = buildRequest(passphrase, accountId, contractAttachment, Constants.ONE_APL * 2);

                Transaction transaction = dexOfferTransactionCreator.createTransaction(createTransactionRequest);

                if (transaction == null) {
                    throw new AplException.ExecutiveProcessException("Creating contract wasn't finish. (Step-2) Orderid: " + contract.getOrderId() + ", counterOrder:  " + contract.getCounterOrderId());
                }
                log.debug("DexOfferProcessor Step-2. User created contract (Step-3). accountId: {} , txId: {}.", accountId, transaction.getId());

            } catch (AplException.ExecutiveProcessException | AplException.ValidationException | ParameterException e) {
                log.error(e.getMessage(), e);
                continue;
            }
        }
    }

    private boolean isContractStep2Valid(ExchangeContract exchangeContract) {
        //TODO add validation.
        return true;
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
                DexOffer offer = dexService.getOfferByTransactionId(contract.getCounterOrderId());

                if (!isContractStep3Valid(contract, offer)) {
                    continue;
                }

                log.debug("DexOfferProcessor Step-2 (part-1). accountId: {}", accountId);

                byte[] secret = Crypto.aesGCMDecrypt(contract.getEncryptedSecret(), Crypto.sha256().digest(Convert.toBytes(passphrase)));

                log.debug("DexOfferProcessor Step-2(part-1). Approving money transfer. accountId: {}", accountId);

                dexService.approveMoneyTransfer(passphrase, accountId, contract.getCounterOrderId(), contract.getTransferTxId(), secret);

                log.debug("DexOfferProcessor Step-2(part-1). Approved money transfer. accountId: {}", accountId);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }

    }


    private void processOutcomeContractsForUserStep3(Long accountId) {
        String passphrase = secureStorageService.getUserPassPhrase(accountId);

        DexOfferDBRequest dexOfferDBRequest = new DexOfferDBRequest();
        dexOfferDBRequest.setAccountId(accountId);
        dexOfferDBRequest.setStatus(OfferStatus.WAITING_APPROVAL);
        List<DexOffer> outComeOffers = dexService.getOffers(dexOfferDBRequest);

        for (DexOffer outcomeOffer : outComeOffers) {
            try {
                ExchangeContract contract = dexService.getDexContract(DexContractDBRequest.builder()
                        .sender(accountId)
                        .offerId(outcomeOffer.getTransactionId())
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

                dexService.approveMoneyTransfer(passphrase, accountId, outcomeOffer.getTransactionId(), contract.getCounterTransferTxId(), secret);

                log.debug("DexOfferProcessor Step-2(part-2). Approved money transfer. accountId: {}", accountId);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    private boolean isContractStep3Valid(ExchangeContract exchangeContract, DexOffer dexOffer) {
        //TODO add additional validation.
        return dexOffer.getStatus().isWaitingForApproval() && exchangeContract.getTransferTxId() != null && dexService.hasConfirmations(exchangeContract, dexOffer);
    }


    private CreateTransactionRequest buildRequest(String passphrase, Long accountId, Attachment attachment, Long feeATM) throws ParameterException {
        CreateTransactionRequest transferMoneyReq = CreateTransactionRequest
                .builder()
                .passphrase(passphrase)
                .deadlineValue("1440")
                .publicKey(Account.getPublicKey(accountId))
                .senderAccount(Account.getAccount(accountId))
                .keySeed(Crypto.getKeySeed(Helper2FA.findAplSecretBytes(accountId, passphrase)))
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

}
