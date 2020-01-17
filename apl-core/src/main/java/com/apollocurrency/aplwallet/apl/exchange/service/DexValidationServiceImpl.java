/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.EthGasStationInfoDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
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
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_MAX_TIME_OF_ATOMIC_SWAP_WITH_BIAS;
import static com.apollocurrency.aplwallet.apl.util.Constants.DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS;
import static com.apollocurrency.aplwallet.apl.util.Constants.ETH_GAS_MULTIPLIER;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_APL_COMMISSION;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_APL_DEPOSIT;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_APL_FREEZE;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_ATOMIC_SWAP_IS_NOT_EXIST;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_ETH_COMMISSION;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_ETH_DEPOSIT;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_ETH_SYSTEM;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_PHASING_IS_NOT_EXIST;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_PHASING_WAS_FINISHED;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_TIME_IS_NOT_CORRECT;
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
    private TimeService timeService;
    private PhasingPollService phasingPollService;
    private Blockchain blockchain;

    @Inject
    DexValidationServiceImpl(DexSmartContractService dexSmartContractService, EthereumWalletService ethereumWalletService, EthGasStationInfoDao ethGasStationInfoDao, TimeService timeService,
                             PhasingPollService phasingPollService, Blockchain blockchain) {
        this.dexSmartContractService = Objects.requireNonNull(dexSmartContractService, "dexSmartContractService is null");
        this.ethereumWalletService = Objects.requireNonNull(ethereumWalletService, "ethereumWalletService is null");
        this.ethGasStationInfoDao = Objects.requireNonNull(ethGasStationInfoDao, "ethGasStationInfoDao is null");
        this.timeService = Objects.requireNonNull(timeService, "timeService is null");
        this.phasingPollService = Objects.requireNonNull(phasingPollService, "phasingPollService is null");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is null");
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
    public int validateOfferBuyAplEthPhasing(DexOrder myOffer, DexOrder hisOrder, Long txId) {
        PhasingPoll poll = phasingPollService.getPoll(txId);
        if (poll == null) {
            return OFFER_VALIDATE_ERROR_PHASING_IS_NOT_EXIST;
        }

        PhasingPollResult result = phasingPollService.getResult(txId);
        if (result != null || poll.getFinishTime() <= timeService.getEpochTime()) {
            return OFFER_VALIDATE_ERROR_PHASING_WAS_FINISHED;
        }

        int timeLeft = poll.getFinishTime() - timeService.getEpochTime();

        if (timeLeft < 0 || timeLeft < DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS || timeLeft > DEX_MAX_TIME_OF_ATOMIC_SWAP_WITH_BIAS) {
            return OFFER_VALIDATE_ERROR_TIME_IS_NOT_CORRECT;
        }

        try {
            Transaction transaction = blockchain.getTransaction(txId);
            DexControlOfFrozenMoneyAttachment attachment = (DexControlOfFrozenMoneyAttachment) transaction.getAttachment();
            if (hisOrder.getOrderAmount() == null || hisOrder.getOrderAmount().compareTo(attachment.getOfferAmount()) != 0) {
                log.debug("Apl deposit is not right.");
                return OFFER_VALIDATE_ERROR_APL_DEPOSIT;
            }
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            return OFFER_VALIDATE_ERROR_APL_DEPOSIT;
        }

        return OFFER_VALIDATE_OK;
    }

    @Override
    public int validateOfferSellAplEthActiveDeposit(DexOrder myOffer, DexOrder hisOrder) {
        log.debug("validateOfferSellAplEthActiveDeposit: ");
        String hisFromEthAddr = hisOrder.getFromAddress();
        log.debug("hisToEthAddr: {},  transactionid: {}", hisFromEthAddr, hisOrder.getId());
        List<UserEthDepositInfo> hisEthDeposits;

        try {
            hisEthDeposits = dexSmartContractService.getUserActiveDeposits(hisFromEthAddr);
        } catch (AplException.ExecutiveProcessException e) {
            log.error(e.getMessage(), e);
            return OFFER_VALIDATE_ERROR_ETH_DEPOSIT;
        }

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


    public int validateOfferSellAplEthAtomicSwap(DexOrder myOffer, DexOrder hisOrder, byte[] secretHash) {
        log.debug("validateOfferSellAplEthActiveDeposit: ");
        String hisFromEthAddr = hisOrder.getFromAddress();
        log.debug("hisToEthAddr: {},  transactionid: {}", hisFromEthAddr, hisOrder.getId());

        SwapDataInfo swapData;
        try {
            swapData = dexSmartContractService.getSwapData(secretHash);
        } catch (AplException.ExecutiveProcessException e) {
            return OFFER_VALIDATE_ERROR_ETH_SYSTEM;
        }

        if (swapData == null || swapData.getTimeDeadLine() == 0) {
            return OFFER_VALIDATE_ERROR_ATOMIC_SWAP_IS_NOT_EXIST;
        }

        long timeLeft = swapData.getTimeDeadLine() - timeService.systemTime();
        if (timeLeft < DEX_MIN_TIME_OF_ATOMIC_SWAP_WITH_BIAS) {
            log.info("Will not send dex contract transaction to recover exchange process, not enough time 'timeLeft'={} sec.", timeLeft);
        }

        BigDecimal hasToPay = EthUtil.atmToEth(hisOrder.getOrderAmount()).multiply(hisOrder.getPairRate());
        log.debug("hasToPay: {} ", hasToPay);

        if ((hasToPay.compareTo(swapData.getAmount()) != 0)) {
            log.debug("Eth deposit is not right. ");
            return OFFER_VALIDATE_ERROR_ETH_DEPOSIT;
        }

        return OFFER_VALIDATE_OK;
    }

    @Override
    public int validateOfferBuyAplPax(DexOrder myOrder, DexOrder hisOrder) {
        log.debug("validateOfferBuyAplPax: ");
        return validateOfferBuyAplEth(myOrder,hisOrder);

    }

    @Override
    public int validateOfferSellAplPaxActiveDeposit(DexOrder myOrder, DexOrder hisOrder) {
        log.debug("validateOfferSellAplPaxActiveDeposit: ");
        return validateOfferSellAplEthActiveDeposit(myOrder, hisOrder);
    }

    @Override
    public int validateOfferSellAplPaxAtomicSwap(DexOrder myOffer, DexOrder hisOffer, byte[] secretHash) {
        log.debug("validateOfferSellAplPaxAtomicSwap: ");
        return validateOfferSellAplEthAtomicSwap(myOffer, hisOffer, secretHash);
    }
}
