/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Serhiy Lymar
 */

@Singleton
public class DexValidationServiceImpl implements IDexBasicServiceInterface, IDexValidator {
    
    private static final Logger log = LoggerFactory.getLogger(DexMatchingService.class);
    private DexService dexService;
    
    @Inject
    DexValidationServiceImpl( DexService dexService ) {
        this.dexService =  Objects.requireNonNull( dexService,"dexService is null");        
    }

    @Override
    public void initialize() {
        // placeholder
    }

    @Override
    public void deinitialize() {
        // placeholder
    }
        
    Long getAplUnconfirmedBalance(Long hisAccountID) {
        AccountTable at = new AccountTable();
        Account hisAccount = Account.getAccount(hisAccountID);        
        long hisUnconfirmedAplBalance = hisAccount.getUnconfirmedBalanceATM();                        
        return hisUnconfirmedAplBalance;
    }
    
    Long getAplBalance(Long hisAccountID) {
        AccountTable at = new AccountTable();
        Account hisAccount = Account.getAccount(hisAccountID);        
        long hisUnconfirmedAplBalance = hisAccount.getUnconfirmedBalanceATM();                        
        return hisUnconfirmedAplBalance;
    }
    
    // Long getEthDeposit()

    @Override
    public boolean validateAplSell(DexOffer myOffer, DexOffer counterOffer) {
        
       
        
        // placeholder
        return true; 
    }

    @Override
    public boolean validateAplBuy(DexOffer myOffer, DexOffer counterOffer) {

        // ExchangeContract  exchangeContract = dexService.getDexContract(DexContractDBRequest.builder().counterOfferId(counterOffer.getTransactionId()).build());        
        // placeholder
        return true;
    }

    @Override
    public boolean validatEthSell(DexOffer myOffer, DexOffer counterOffer) {
        // placeholder
        return true;
    }

    @Override
    public boolean validateEthBuy(DexOffer myOffer, DexOffer counterOffer) {
        return true;
    }

    @Override
    public boolean validateStep1(DexOffer myOffer, DexOffer counterOffer) {
        return true;
    }

    @Override
    public boolean validateStep2(DexOffer myOffer, DexOffer counterOffer) {
        return true;
    }
    
}
