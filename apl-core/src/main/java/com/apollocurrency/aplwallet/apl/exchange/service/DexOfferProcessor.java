package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.*;
import static com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus.STEP_1;

@Slf4j
@Singleton
public class DexOfferProcessor {

    private SecureStorageService secureStorageService;
    private DexService dexService;
    private DexOfferTransactionCreator dexOfferTransactionCreator;

    @Inject
    public DexOfferProcessor(SecureStorageService secureStorageService, DexService dexService, DexOfferTransactionCreator dexOfferTransactionCreator) {
        this.secureStorageService = secureStorageService;
        this.dexService = dexService;
        this.dexOfferTransactionCreator = dexOfferTransactionCreator;
    }


    public void processContracts(){
        List<Long> accounts = secureStorageService.getAccounts();

        for (Long account : accounts) {
            processContractsForUserStep1(account);
            processContractsForUserStep2(account);
        }

    }

    /**
     * Processing contracts with status step_1.
     * @param accountId
     */
    private void processContractsForUserStep1(Long accountId){
        List<ExchangeContract> incomeContracts =  dexService.getDexContracts(DexContractDBRequest.builder()
                .recipient(accountId)
                .status(STEP_1.ordinal())
                .build());

        for (ExchangeContract incomeContract : incomeContracts) {
            try {
                DexOffer offer = dexService.getOfferByTransactionId(incomeContract.getOrderId());
                DexOffer counterOffer = dexService.getOfferByTransactionId(incomeContract.getCounterOrderId());

                if(!counterOffer.getStatus().isOpen() || !isContractStep1Valid(incomeContract)){
                    continue;
                }

                String passphrase = secureStorageService.getUserPassPhrase(accountId);
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

                String txId = dexService.transferMoneyWithApproval(transferMoneyReq, counterOffer, offer.getToAddress(), incomeContract.getSecretHash(), STEP_2);

                if (txId == null) {
                    throw new AplException.ExecutiveProcessException("Transfer money wasn't finish success. Orderid: " + incomeContract.getOrderId() + ", counterOrder:  " + incomeContract.getCounterOrderId() + ", " + incomeContract.getContractStatus());
                }

                DexContractAttachment contractAttachment = new DexContractAttachment(counterOffer.getTransactionId(), offer.getTransactionId(), null, txId, null, STEP_2);
                //TODO move it to some util
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
                        .attachment(contractAttachment)
                        .build();

                Transaction transaction = dexOfferTransactionCreator.createTransaction(createTransactionRequest);

                if (transaction == null) {
                    throw new AplException.ExecutiveProcessException("Creating contract wasn't finish. Orderid: " + incomeContract.getOrderId() + ", counterOrder:  " + incomeContract.getCounterOrderId() + ", " + incomeContract.getContractStatus());
                }
                log.debug("Atomic swap Step_2 Tx id:" + transaction.getStringId());


            } catch (AplException.ExecutiveProcessException | AplException.ValidationException | ParameterException e) {
                log.error(e.getMessage(), e);
                continue;
            }

        }
    }

    private boolean isContractStep1Valid(ExchangeContract exchangeContract){
        //TODO add validation.
        return true;
    }


    /**
     * Processing contracts with status step_2.
     * @param accountId
     */
    private void processContractsForUserStep2(Long accountId){

        String passphrase = secureStorageService.getUserPassPhrase(accountId);
        List<ExchangeContract> incomeContracts =  dexService.getDexContracts(DexContractDBRequest.builder()
                .recipient(accountId)
                .status(STEP_2.ordinal())
                .build());
        for (ExchangeContract incomeContract : incomeContracts) {
            try {
                DexOffer offer = dexService.getOfferByTransactionId(incomeContract.getOrderId());

                if (!offer.getStatus().isWaitingForApproval() || !isContractStep2Valid(incomeContract)){
                    continue;
                }

                ExchangeContract exchangeContract = dexService.getDexContract(DexContractDBRequest.builder()
                        .sender(accountId)
                        .offerId(incomeContract.getCounterOrderId())
                        .status(STEP_1.ordinal())
                        .build());

                //TODO move to validation
                if(dexService.isTxApproved(exchangeContract.getSecretHash(), offer.getType(), offer.getPairCurrency(), incomeContract.getTransferTxId())){
                    continue;
                }

                byte[] secret = Crypto.aesGCMDecrypt(exchangeContract.getEncryptedSecret(), Crypto.sha256().digest(Convert.toBytes(passphrase)));

                dexService.approveMoneyTransfer(passphrase, accountId, incomeContract.getCounterOrderId(), incomeContract.getTransferTxId(), secret);
            }catch (Exception ex){
                log.error(ex.getMessage(), ex);
                continue;
            }
        }


        DexOfferDBRequest dexOfferDBRequest = new DexOfferDBRequest();
        dexOfferDBRequest.setAccountId(accountId);
        dexOfferDBRequest.setStatus(OfferStatus.WAITING_APPROVAL);
        List<DexOffer> outComeOffers = dexService.getOffers(dexOfferDBRequest);

        for (DexOffer outcomeOffer : outComeOffers) {
            try {
                ExchangeContract outcomeContract = dexService.getDexContract(DexContractDBRequest.builder()
                        .sender(accountId)
                        .offerId(outcomeOffer.getTransactionId())
                        .status(STEP_2.ordinal())
                        .build());

                if(outcomeContract == null){
                    continue;
                }
                ExchangeContract initialContract = dexService.getDexContract(DexContractDBRequest.builder()
                        .sender(outcomeContract.getRecipient())
                        .offerId(outcomeContract.getCounterOrderId())
                        .status(STEP_1.ordinal())
                        .build());

                //TODO move it to validation function.
                //Check on contract was not approved, and we can get money.
                if(!dexService.isTxApproved(initialContract.getSecretHash(), outcomeOffer.getType(), outcomeOffer.getPairCurrency(), outcomeContract.getTransferTxId())){
                    continue;
                }

                //Check if contract was approved, and we can get shared secret.
                if(dexService.isTxApproved(initialContract.getSecretHash(), outcomeOffer.getType().reverse(), outcomeOffer.getPairCurrency(), initialContract.getTransferTxId())){
                    continue;
                }

                byte[] secret = dexService.getSecretIfTxApproved(initialContract.getSecretHash(), outcomeOffer.getType(), outcomeOffer.getPairCurrency(), outcomeContract.getTransferTxId());
                dexService.approveMoneyTransfer(passphrase, accountId, outcomeOffer.getTransactionId(), initialContract.getTransferTxId(), secret);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                continue;
            }

        }

    }

    private boolean isContractStep2Valid(ExchangeContract exchangeContract){
        //TODO add validation.
        return true;
    }

}
