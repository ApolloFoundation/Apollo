package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
@Singleton
public class DexApiValidator {
    private EthereumWalletService walletService;
    private KeyStoreService keyStoreService;

    @Inject
    public DexApiValidator(EthereumWalletService walletService, KeyStoreService keyStoreService) {
        this.walletService = walletService;
        this.keyStoreService = keyStoreService;
    }

    private void validateAplAccountBalance(long orderAmountGwei, long accountId) throws ParameterException {
        Account account = Account.getAccount(accountId);
        if (account.getUnconfirmedBalanceATM() <= EthUtil.gweiToAtm(orderAmountGwei)) {
            throw new ParameterException(JSONResponses.NOT_ENOUGH_APL);
        }
    }

    private void validateEthPaxAccountBalance(long orderAmountGwei, long pairRateGwei, String walletAddress, DexCurrency currency) throws ParameterException {
        BigInteger haveToPayWei = BigInteger.valueOf(orderAmountGwei).multiply(BigInteger.valueOf(pairRateGwei));
        validateEthPaxAccountBalance(haveToPayWei, walletAddress, currency);
    }
    public void validateEthAccountForDeposit(String passphrase, DexOrder order) throws ParameterException {
        validateVaultAccount(order.getAccountId(), passphrase);
        validateEthAccount(order.getAccountId(), passphrase, order.getFromAddress());
        BigInteger haveToPayWei = EthUtil.etherToWei(EthUtil.atmToEth(order.getOrderAmount()).multiply(order.getPairRate()));
        validateEthPaxAccountBalance(haveToPayWei, order.getFromAddress(), order.getPairCurrency());
    }



    private void validateEthPaxAccountBalance(BigInteger haveToPay, String walletAddress, DexCurrency currency) throws ParameterException {
        BigInteger balanceWei = walletService.getEthOrPaxBalanceWei(walletAddress, currency);
        if (haveToPay.compareTo(balanceWei) >= 0) {
            throw new ParameterException(JSONResponses.notEnoughCurrency(currency));
        }
    }
    public void validateVaultAccount(long sender, String passphrase) throws ParameterException {
        WalletKeysInfo walletKeysInfo = keyStoreService.getWalletKeysInfo(passphrase, sender);
        if (walletKeysInfo == null) {
            throw new ParameterException(JSONResponses.incorrect("account or passphrase", "Bad credentials"));
        }
    }

    public void validateEthAccount(long sender, String passphrase, String ethWalletAddress) throws ParameterException {
        WalletKeysInfo walletKeysInfo = keyStoreService.getWalletKeysInfo(passphrase, sender);
        EthWalletKey ethWallet = walletKeysInfo.getEthWalletForAddress(ethWalletAddress);
        if (ethWallet == null) {
            throw new ParameterException(JSONResponses.incorrect(DexApiConstants.WALLET_ADDRESS, "Account  does not own the specified eth address"));
        }
    }

    public void validateParametersForOrderTransaction(long sender, String passphrase, OrderType orderType, long orderAmountGwei, long pairRateGwei, String walletAddress, DexCurrency currency) throws ParameterException {
        validatePairedCurrency(currency);
        validateVaultAccount(sender, passphrase);

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
