/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.EthGasStationInfoDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.util.Constants.APL_COMMISSION;
import static com.apollocurrency.aplwallet.apl.util.Constants.ETH_GAS_MULTIPLIER;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_APL_COMMISSION;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_APL_FREEZE;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_ETH_COMMISSION;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_ETH_DEPOSIT;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_OK;
import static com.apollocurrency.aplwallet.apl.util.Constants.ONE_APL;

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
    DexValidationServiceImpl(DexSmartContractService dexSmartContractService, EthereumWalletService ethereumWalletService, EthGasStationInfoDao ethGasStationInfoDao) {
        this.dexSmartContractService = Objects.requireNonNull(dexSmartContractService, "dexSmartContractService is null");
        this.ethereumWalletService = Objects.requireNonNull(ethereumWalletService, "ethereumWalletService is null");
        this.ethGasStationInfoDao = Objects.requireNonNull( ethGasStationInfoDao, "ethGasStationInfoDao is null");

    }

    Long getAplUnconfirmedBalance(Long hisAccountID) {
        Account hisAccount = Account.getAccount(hisAccountID);
        long hisUnconfirmedAplBalance = hisAccount.getUnconfirmedBalanceATM();
        return hisUnconfirmedAplBalance;
    }

    Long getAplBalanceAtm(Long hisAccountID) {
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

    BigInteger getUserEthDeposit(String user, DexCurrency currencyType) {
        return  ethereumWalletService.getEthOrPaxBalanceWei(user, currencyType);
    }

    BigInteger getEthOrPaxBalanceWei(String user, DexCurrency currencyType) {
        return ethereumWalletService.getEthOrPaxBalanceWei(user, currencyType );
    }


    BigInteger getOnlyEthBalanceWei(String user) {
        return ethereumWalletService.getOnlyEthBalanceWei(user);
    }


    boolean isEthOrPaxDepositValid(DexOrder myOffer, DexOrder counterOffer) {
        BigInteger amountOnHisWallet = getEthOrPaxBalanceWei(counterOffer.getFromAddress(), counterOffer.getPairCurrency());
        BigDecimal haveToPay = EthUtil.atmToEth(counterOffer.getOrderAmount()).multiply(counterOffer.getPairRate());
        return amountOnHisWallet.compareTo(EthUtil.etherToWei(haveToPay)) < 0;
    }


    boolean checkAplCommisionPayingAbility(Long hisAplBalance) {
        // checking out whether there are commission available
        Long fee = APL_COMMISSION * ONE_APL;
        log.debug("fee: " + fee);
        return hisAplBalance >= fee;
    }

    boolean checkGasPayingAbility(DexOrder hisOrder) {
        EthGasInfo ethGasInfo = null;
        try {
            ethGasInfo = ethGasStationInfoDao.getEthPriceInfo();

            if (ethGasInfo == null) {
                log.error("Exception got while getting eth gas price: ");
                return false;
            }
        } catch (IOException ex) {
            log.error("Exception got while getting eth gas price: ", ex);
        }

        log.debug("type: {}, hisOffer.getToAddress(): {}, hisOffer.fromToAddress(): {}, currency: {}", hisOrder.getType(),
                hisOrder.getToAddress(), hisOrder.getFromAddress(), hisOrder.getPairCurrency());

        String hisAddress;
        if (hisOrder.getType() == OrderType.BUY)
            hisAddress = hisOrder.getFromAddress();
        else hisAddress = hisOrder.getToAddress();
        log.debug("selected: {}",hisAddress);
        // here we have double conversion, gw-eth-wei        
        Long averageGasPriceGw = ethGasInfo.getAverageSpeedPrice();

        if (averageGasPriceGw == null) {
            log.error("Error getting gas info");
            return false;
        }

        // whether it is ETH or PAX = we don't care. ETH balance matters
        BigInteger hisEthBalanceWei = getOnlyEthBalanceWei(hisAddress);

        BigInteger averageGasPriceWei = EthUtil.gweiToWei(averageGasPriceGw);
        log.debug("averageGasPriceGw: {}, averageGasPriceWei: {}, hisEthBalanceWei: {} ", averageGasPriceGw, averageGasPriceWei, hisEthBalanceWei );

        boolean ethCheckResult = (1 == hisEthBalanceWei.compareTo(averageGasPriceWei.multiply(BigInteger.valueOf(ETH_GAS_MULTIPLIER))));
        // for logging
        return ethCheckResult;
    }


    @Override
    public int validateOfferBuyAplEth(DexOrder myOrder, DexOrder hisOrder) {
        log.debug("validateOfferBuyAplEth: ");

        // 1) Checking out whether HE has the corresponding amount on his APL balance        
        Long hisAccountID = hisOrder.getAccountId();
        log.debug("hisAccountID(apl): {}, his fromAddr : {}, his toAddr: {}", hisAccountID,hisOrder.getFromAddress(),hisOrder.getToAddress());

        Long hisUnconfirmedAplBalance = getAplUnconfirmedBalance(hisAccountID);
        Long hisAplBalance = getAplBalanceAtm(hisAccountID);
        Long balanceDelta = hisAplBalance - hisUnconfirmedAplBalance;
        boolean isFrozenEnough = balanceDelta >= hisOrder.getOrderAmount();

        log.debug("isFrozenEnough: {} ", isFrozenEnough);

        if (!isFrozenEnough) {
            return OFFER_VALIDATE_ERROR_APL_FREEZE;
        }
        boolean ableToPayCommission = checkAplCommisionPayingAbility(hisAplBalance);

        log.debug("ableToPayCommission: {}", ableToPayCommission);

        if (!ableToPayCommission) {
            return OFFER_VALIDATE_ERROR_APL_COMMISSION;
        }
        boolean ethCheckResult = checkGasPayingAbility(hisOrder);

        log.debug("ethCheckResult: {} ", ethCheckResult);
        if (!ethCheckResult) {
            return OFFER_VALIDATE_ERROR_ETH_COMMISSION;
        }
        return OFFER_VALIDATE_OK;
    }

    @Override
    public int validateOfferSellAplEth(DexOrder myOffer, DexOrder hisOrder) {
        log.debug("validateOfferSellAplEth: ");
        String hisFromEthAddr = hisOrder.getFromAddress();
        log.debug("hisToEthAddr: {},  transactionid: {}", hisFromEthAddr, hisOrder.getId());
        List<UserEthDepositInfo> hisEthDeposits = getUserEthDeposits(hisFromEthAddr);
        BigDecimal hasToPay = EthUtil.atmToEth(hisOrder.getOrderAmount()).multiply(hisOrder.getPairRate());
        log.debug("hasToPay: {} ", hasToPay);
        boolean depositDetected = false;
        for (UserEthDepositInfo current : hisEthDeposits) {
            if ( (hasToPay.compareTo( current.getAmount())==0) && current.getOrderId().equals(hisOrder.getId()) ) {
                log.debug("Eth deposit is detected");
                depositDetected = true;
                break;
            }
        }
        log.debug("deposit detected: {}", depositDetected);
        if (!depositDetected) return OFFER_VALIDATE_ERROR_ETH_DEPOSIT;

        // checking if they are able to pay apl commission
        Long hisAccountID = hisOrder.getAccountId();
        Long hisAplBalance = getAplBalanceAtm(hisAccountID);
        boolean ableToPayCommission = checkAplCommisionPayingAbility(hisAplBalance);
        log.debug("ableToPayCommission: {}", ableToPayCommission);
        if (!ableToPayCommission) return OFFER_VALIDATE_ERROR_APL_COMMISSION;

        // checking for ETH gas paying ability
        boolean ethCheckResult = checkGasPayingAbility(hisOrder);
        log.debug("ethCheckResult: {} ", ethCheckResult);
        if (!ethCheckResult) return OFFER_VALIDATE_ERROR_ETH_COMMISSION;

        return OFFER_VALIDATE_OK;
    }

    @Override
    public int validateOfferBuyAplPax(DexOrder myOrder, DexOrder hisOrder) {
        log.debug("validateOfferBuyAplPax: ");
        return validateOfferBuyAplEth(myOrder,hisOrder);

    }

    @Override
    public int validateOfferSellAplPax(DexOrder myOrder, DexOrder hisOrder) {
        log.debug("validateOfferSellAplPax: ");
        return validateOfferSellAplEth(myOrder, hisOrder);
    }

}
