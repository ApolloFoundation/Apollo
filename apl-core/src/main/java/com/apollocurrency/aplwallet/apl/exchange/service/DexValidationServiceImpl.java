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
import com.apollocurrency.aplwallet.apl.exchange.dao.EthGasStationInfoDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import com.apollocurrency.aplwallet.apl.util.AplException;
import static com.apollocurrency.aplwallet.apl.util.Constants.ONE_APL;
import static com.apollocurrency.aplwallet.apl.util.Constants.APL_COMMISSION;
import static com.apollocurrency.aplwallet.apl.util.Constants.ETH_GAS_MULTIPLIER;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_OK;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_APL_FREEZE;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_APL_COMMISSION;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_ETH_COMMISSION;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_ETH_DEPOSIT;
import java.io.IOException;
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
public class DexValidationServiceImpl implements IDexValidator {
    
    private static final Logger log = LoggerFactory.getLogger(DexValidationServiceImpl.class);   
    private DexSmartContractService dexSmartContractService;
    private EthereumWalletService ethereumWalletService;
    private EthGasStationInfoDao ethGasStationInfoDao;
        
    @Inject
    DexValidationServiceImpl( DexSmartContractService dexSmartContractService, EthereumWalletService ethereumWalletService, EthGasStationInfoDao ethGasStationInfoDao ) {        
        this.dexSmartContractService =  Objects.requireNonNull( dexSmartContractService,"dexSmartContractService is null");           
        this.ethereumWalletService =  Objects.requireNonNull( ethereumWalletService,"ethereumWalletService is null");           
        this.ethGasStationInfoDao = Objects.requireNonNull( ethGasStationInfoDao, "ethGasStationInfoDao is null");

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
    
    Long getAplBalanceAtm(Long hisAccountID) {
        AccountTable at = new AccountTable();
        Account hisAccount = Account.getAccount(hisAccountID);        
        Long hisAplBalance = hisAccount.getBalanceATM();
        return hisAplBalance;
    }
    
    private List<UserEthDepositInfo> getUserEthDeposits(String user) {
        try {
            return dexSmartContractService.getUserFilledDeposits(user);
        } catch (AplException.ExecutiveProcessException ex) {
           log.debug( "Exception caught while getting eth deposits: {} ", ex);
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
        BigInteger amountOnHisWallet = getEthBalanceWei(counterOffer.getFromAddress(), counterOffer.getPairCurrency());
        BigDecimal haveToPay = EthUtil.atmToEth(counterOffer.getOfferAmount()).multiply(counterOffer.getPairRate());
        return amountOnHisWallet.compareTo(EthUtil.etherToWei(haveToPay)) < 0;        
    }
       
    
    boolean checkAplCommisionPayingAbility(Long hisAplBalance) {
        // checking out whether there are commission available
        Long fee = APL_COMMISSION * ONE_APL;
        log.debug("fee: " + fee);              
        return hisAplBalance >= fee;
    }
    
    boolean checkGasPayingAbility(DexOffer hisOffer) {
        EthGasInfo ethGasInfo = null;
        try {
            ethGasInfo = ethGasStationInfoDao.getEthPriceInfo();
        } catch (IOException ex) {
            log.error("Exception got while getting eth gas price: ", ex);
        }        
        
        log.debug("type: {}, hisOffer.getToAddress(): {}, hisOffer.fromToAddress(): {}, currency: {}", hisOffer.getType(),
                hisOffer.getToAddress(), hisOffer.getFromAddress(), hisOffer.getPairCurrency());
        
        String hisAddress = null;
        if (hisOffer.getType() == OfferType.BUY) 
            hisAddress = hisOffer.getFromAddress();
        else hisAddress = hisOffer.getToAddress();
        log.debug("selected: {}",hisAddress);
        // here we have double conversion, gw-eth-wei        
        Long averageGasPriceGw = ethGasInfo.getAverageSpeedPrice();  
        BigInteger hisEthBalanceWei = getEthBalanceWei(/*hisOffer.getToAddress(),*/hisAddress, hisOffer.getPairCurrency());
        BigDecimal averageGasPriceEth = EthUtil.gweiToEth(averageGasPriceGw);
        BigInteger averageGasPriceWei = EthUtil.etherToWei(averageGasPriceEth);
        log.debug("averageGasPriceGw: {}, averageGasPriceWei: {}, hisEthBalanceWei: {} ", averageGasPriceGw, averageGasPriceWei, hisEthBalanceWei );
       
        boolean ethCheckResult = (1 == hisEthBalanceWei.compareTo(averageGasPriceWei.multiply(BigInteger.valueOf(ETH_GAS_MULTIPLIER)))); 
        // for logging
        return ethCheckResult;
    }
    

    @Override
    public int validateOfferBuyAplEth(DexOffer myOffer, DexOffer hisOffer) {                
        log.debug("validateOfferBuyAplEth: ");
        
        // 1) Checking out whether HE has the corresponding amount on his APL balance        
        Long hisAccountID = hisOffer.getAccountId();
        log.debug("hisAccountID(apl): {}, his fromAddr : {}, his toAddr: {}", hisAccountID,hisOffer.getFromAddress(),hisOffer.getToAddress());
        Long hisUnconfirmedAplBalance = getAplUnconfirmedBalance(hisAccountID);
        Long hisAplBalance = getAplBalanceAtm(hisAccountID);  
        Long balanceDelta = hisAplBalance - hisUnconfirmedAplBalance;
        boolean isFrozenEnough = balanceDelta >= hisOffer.getOfferAmount();
        log.debug("isFrozenEnough: {} ", isFrozenEnough);
        if (!isFrozenEnough) return OFFER_VALIDATE_ERROR_APL_FREEZE;      
        boolean ableToPayCommission = checkAplCommisionPayingAbility(hisAplBalance);
        log.debug("ableToPayCommission: {}", ableToPayCommission);                
        if (!ableToPayCommission) return OFFER_VALIDATE_ERROR_APL_COMMISSION;        
        boolean ethCheckResult = checkGasPayingAbility(hisOffer);        
        log.debug("ethCheckResult: {} ", ethCheckResult);
        if (!ethCheckResult) return OFFER_VALIDATE_ERROR_ETH_COMMISSION;        
        return OFFER_VALIDATE_OK;
    }

    @Override
    public int validateOfferSellAplEth(DexOffer myOffer, DexOffer hisOffer) {
        log.debug("validateOfferSellAplEth: ");
        // checking out eth deposits..         
        String hisFromEthAddr = hisOffer.getFromAddress(); 
        log.debug("hisToEthAddr: {},  transactionid: {}", hisFromEthAddr, hisOffer.getTransactionId());        
        List<UserEthDepositInfo> hisEthDeposits =  getUserEthDeposits(hisFromEthAddr);                                      
        BigDecimal hasToPay = EthUtil.atmToEth(hisOffer.getOfferAmount()).multiply(hisOffer.getPairRate());        
        log.debug("hasToPay: {} ", hasToPay);        
        boolean depositDetected = false; 
        for (UserEthDepositInfo current : hisEthDeposits) {            
            if ( (hasToPay.compareTo( current.getAmount())==0) && current.getOrderId().equals(hisOffer.getTransactionId()) ) {
                log.debug("Eth deposit is detected");
                depositDetected = true;
                break;                
            }
        }        
        log.debug("deposit detected: {}", depositDetected);
        if (!depositDetected) return OFFER_VALIDATE_ERROR_ETH_DEPOSIT;
        
        // checking if they are able to pay apl commission
        Long hisAccountID = hisOffer.getAccountId();
        Long hisAplBalance = getAplBalanceAtm(hisAccountID);  
        boolean ableToPayCommission = checkAplCommisionPayingAbility(hisAplBalance);        
        log.debug("ableToPayCommission: {}", ableToPayCommission);                
        if (!ableToPayCommission) return OFFER_VALIDATE_ERROR_APL_COMMISSION;        

        // checking for ETH gas paying ability
        boolean ethCheckResult = checkGasPayingAbility(hisOffer);        
        log.debug("ethCheckResult: {} ", ethCheckResult);
        if (!ethCheckResult) return OFFER_VALIDATE_ERROR_ETH_COMMISSION;
        
        return OFFER_VALIDATE_OK;
    }

    @Override
    public int validateOfferBuyAplPax(DexOffer myOffer, DexOffer hisOffer) {
        log.debug("validateOfferBuyAplPax: ");
        return validateOfferBuyAplEth(myOffer,hisOffer);
        
    }

    @Override
    public int validateOfferSellAplPax(DexOffer myOffer, DexOffer hisOffer) {
        log.debug("validateOfferSellAplPax: ");
        return validateOfferSellAplEth(myOffer, hisOffer);
    }
    
}
