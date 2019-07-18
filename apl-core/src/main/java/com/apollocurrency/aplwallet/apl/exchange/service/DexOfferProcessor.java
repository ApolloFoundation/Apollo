package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

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
            processContractsForUser(account);
        }

    }

    /**
     * Processing contracts with status step_1.
     * @param accountId
     */
    private void processContractsForUser(Long accountId){

        List<ExchangeContract> incomeContracts =  dexService.getDexContractsForAccount(accountId);

        for (ExchangeContract incomeContract : incomeContracts) {

            DexOffer offer = dexService.getOfferByTransactionId(incomeContract.getOrderId());
            DexOffer counterOffer = dexService.getOfferByTransactionId(incomeContract.getCounterOrderId());

            if(counterOffer.getStatus().isOpen() && isContractValid(incomeContract)){
                String txId;
                CreateTransactionRequest transferMoneyReq = CreateTransactionRequest
                        .builder()
                        .passphrase(secureStorageService.getUserPassPhrase(accountId))
                        .senderAccount(Account.getAccount(accountId))
                        .build();

                try {
                    txId = dexService.transferMoneyWithApproval(transferMoneyReq, counterOffer, offer.getToAddress(), incomeContract.getSecretHash(), ExchangeContractStatus.STEP_2);

                    if(txId == null){
                          throw new AplException.ExecutiveProcessException("Transfer money wasn't finish success. Orderid: " + incomeContract.getOrderId() + ", counterOrder:  " + incomeContract.getCounterOrderId()+", " + incomeContract.getContractStatus());
                    }

                    DexContractAttachment contractAttachment = new DexContractAttachment(counterOffer.getTransactionId(), offer.getTransactionId(), null, txId, null, ExchangeContractStatus.STEP_2);
                    //TODO move it to some util
                    String passphrase = secureStorageService.getUserPassPhrase(accountId);
                    CreateTransactionRequest createTransactionRequest = CreateTransactionRequest
                            .builder()
                            .passphrase(passphrase)
                            .keySeed(Crypto.getKeySeed(passphrase))
                            .publicKey(Crypto.getPublicKey(Crypto.getKeySeed(passphrase)))
                            .senderAccount(Account.getAccount(accountId))
                            .deadlineValue("1440")
                            .feeATM(Constants.ONE_APL * 2)
                            .broadcast(true)
                            .recipientId(0L)
                            .ecBlockHeight(0)
                            .ecBlockId(0L)
                            .attachment(contractAttachment)
                            .build();

                    Transaction transaction = dexOfferTransactionCreator.createTransaction(createTransactionRequest);

                    if(transaction == null){
                        throw new AplException.ExecutiveProcessException("Creating contract wasn't finish. Orderid: " + incomeContract.getOrderId() + ", counterOrder:  " + incomeContract.getCounterOrderId()+", " + incomeContract.getContractStatus());
                    }

                    log.debug("Atomic swap Step_2 Tx id:" + transaction.getStringId());
                } catch (AplException.ExecutiveProcessException e) {
                    log.error(e.getMessage(), e);
                    continue;
                } catch (AplException.ValidationException e) {
                    log.error(e.getMessage(), e);
                    continue;
                } catch (ParameterException e) {
                    log.error(e.getMessage(), e);
                    continue;
                }

            }

        }



    }

    private boolean isContractValid(ExchangeContract exchangeContract){
        //TODO add validation.
        return true;
    }



}
