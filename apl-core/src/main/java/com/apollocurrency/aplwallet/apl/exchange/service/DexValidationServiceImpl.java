/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
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
    
    private static final Logger log = LoggerFactory.getLogger(DexValidationServiceImpl.class);
    // private DexService dexService;
    private DexSmartContractService dexSmartContractService;
    private EthereumWalletService ethereumWalletService;
    
    @Inject
    DexValidationServiceImpl( /*DexService dexService,*/ DexSmartContractService dexSmartContractService, EthereumWalletService ethereumWalletService ) {
        //this.dexService =  Objects.requireNonNull( dexService,"dexService is null");   
        this.dexSmartContractService =  Objects.requireNonNull( dexSmartContractService,"dexSmartContractService is null");           
        this.ethereumWalletService =  Objects.requireNonNull( ethereumWalletService,"dexSmartContractService is null");           

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
    
    private List<UserEthDepositInfo> getUserEthDeposits(String user) {
        try {
            return dexSmartContractService.getUserFilledDeposits(user);
        } catch (AplException.ExecutiveProcessException ex) {
            java.util.logging.Logger.getLogger(DexValidationServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    BigInteger getUserEthDeposit(String user, DexCurrencies currencyType) {
        return  ethereumWalletService.getBalanceWei(user, currencyType);
    }
    
    BigInteger getEthBalanceWei(String user, DexCurrencies currencyType) {
        return ethereumWalletService.getBalanceWei(user, currencyType );
    }
    
    boolean validateEthDeposit(DexOffer myOffer, DexOffer counterOffer) {
        // right now it is checking the balance. TODO: switch to checking out deposits
        BigInteger amountOnHisWallet = getEthBalanceWei(counterOffer.getFromAddress(), counterOffer.getPairCurrency());
        BigDecimal haveToPay = EthUtil.atmToEth(counterOffer.getOfferAmount()).multiply(counterOffer.getPairRate());
        return amountOnHisWallet.compareTo(EthUtil.etherToWei(haveToPay)) < 0;        
    }
       
    @Override
    public boolean validateAplSell(DexOffer myOffer, DexOffer counterOffer) {
       
        
        // placeholder
        return true; 
    }

    @Override
    public boolean validateAplBuy(DexOffer myOffer, DexOffer counterOffer) {
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
