package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import com.apollocurrency.aplwallet.apl.dex.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.dex.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.vault.service.KMSService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.math.BigInteger;

@Singleton
public class DexApiValidator {
    private final EthereumWalletService walletService;
    private final KMSService KMSService;
    private final AccountService accountService;

    @Inject
    public DexApiValidator(EthereumWalletService walletService, KMSService KMSService, AccountService accountService) {
        this.walletService = walletService;
        this.KMSService = KMSService;
        this.accountService = accountService;
    }

    private void validateAplAccountBalance(long orderAmountGwei, long accountId) throws ParameterException {
        Account account = accountService.getAccount(accountId);
        if (account.getUnconfirmedBalanceATM() <= EthUtil.gweiToAtm(orderAmountGwei)) {
            throw new ParameterException(JSONResponses.NOT_ENOUGH_APL);
        }
    }

    private void validateEthPaxAccountBalance(long orderAmountGwei, long pairRateGwei, String walletAddress, DexCurrency currency) throws ParameterException {
        BigInteger haveToPayWei = BigInteger.valueOf(orderAmountGwei).multiply(BigInteger.valueOf(pairRateGwei));
        validateEthPaxAccountBalance(haveToPayWei, walletAddress, currency);
    }

    public void validateEthAccountForDeposit(String passphrase, DexOrder order) throws ParameterException {
        BigInteger haveToPayWei = EthUtil.etherToWei(EthUtil.atmToEth(order.getOrderAmount()).multiply(order.getPairRate()));
        validateEthAccountForDeposit(order.getAccountId(), passphrase, order.getFromAddress(), haveToPayWei, order.getPairCurrency());
    }

    public void validateEthAccountForDeposit(long account, String passphrase, String walletAddress, BigInteger amountWei, DexCurrency currency) throws ParameterException {
        validateEthAccount(account, passphrase, walletAddress);
        validateEthPaxAccountBalance(amountWei, walletAddress, currency);
    }


    private void validateEthPaxAccountBalance(BigInteger haveToPay, String walletAddress, DexCurrency currency) throws ParameterException {
        BigInteger balanceWei = walletService.getEthOrPaxBalanceWei(walletAddress, currency);
        if (haveToPay.compareTo(balanceWei) >= 0) {
            throw new ParameterException(JSONResponses.notEnoughCurrency(currency));
        }
    }

    public void validateVaultAccount(long sender) throws ParameterException {
        if (!KMSService.isWalletExist(sender)) {
            throw new ParameterException(JSONResponses.incorrect("account or passphrase", "Bad credentials"));
        }
    }

    public void validateEthAccount(long sender, String passphrase, String ethWalletAddress) throws ParameterException {
        validateVaultAccount(sender);
        if (KMSService.isEthKeyExist(sender, passphrase, ethWalletAddress)) {
            throw new ParameterException(JSONResponses.incorrect(DexApiConstants.WALLET_ADDRESS, "Account  does not own the specified eth address"));
        }
    }

    public void validateParametersForOrderTransaction(long sender, String passphrase, OrderType orderType, long orderAmountGwei, long pairRateGwei, String walletAddress, DexCurrency currency) throws ParameterException {
        validatePairedCurrency(currency);
        validateVaultAccount(sender);

        validateOrderAccountBalance(sender, passphrase, orderType, orderAmountGwei, pairRateGwei, walletAddress, currency);

    }

    private void validatePairedCurrency(DexCurrency currency) throws ParameterException {
        if (currency == DexCurrency.APL) {
            throw new ParameterException(JSONResponses.incorrect(DexApiConstants.PAIR_CURRENCY, DexApiConstants.PAIR_CURRENCY + " cannot be equal to " + DexCurrency.APL));
        }
    }

    public void validateOrderAccountBalance(long sender, String passphrase, OrderType orderType, long orderAmountGwei, long pairRateGwei, String walletAddress, DexCurrency currency) throws ParameterException {
        if (!currency.isEthOrPax()) {
            throw new ParameterException(JSONResponses.incorrect(DexApiConstants.PAIR_CURRENCY, "Only PAX and ETH supported as pairCurrency"));
        }
        validateEthAccount(sender, passphrase, walletAddress);
        if (orderType == OrderType.BUY) {
            if (!EthUtil.isAddressValid(walletAddress)) {
                throw new ParameterException(JSONResponses.incorrect(DexApiConstants.WALLET_ADDRESS, "Eth account is incorrect"));
            }
            validateEthPaxAccountBalance(orderAmountGwei, pairRateGwei, walletAddress, currency);
        } else {
            validateAplAccountBalance(orderAmountGwei, sender);
        }
    }
}
