package com.apollocurrrency.aplwallet.inttest.helper;

import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import com.apollocurrrency.aplwallet.inttest.model.steps.AccountSteps;

public class WalletFactory {
    private static final AccountSteps ACCOUNT_STEPS = new AccountSteps();

    public static Wallet getNewVaultWallet(){
      Account2FAResponse account = ACCOUNT_STEPS.generateNewAccount();
      return new Wallet(account.getAccountRS(),account.getPassphrase(),true,account.getEth().iterator().next().getAddress(), String.valueOf(account.getId()));
    }
}
