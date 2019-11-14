package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ShufflingDTO;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Shuffling")
@Epic(value = "Shuffling")
public class TestShuffling extends TestBaseOld {
    final int NON_SHUFFLEABLE = 32;

    private final ArrayList<Wallet> wallets = new ArrayList<>();
    @BeforeEach
    @Override
    public void setUP(TestInfo testInfo) {
                super.setUP(testInfo);
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
        for (Wallet wallet: wallets) {
       // setUpCurrency(wallet);
        }
    }


    @DisplayName("Create shuffling and cancel")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {0,1,2})
    public void shufflingCreateTest(int type){
        int  registrationPeriod  = RandomUtils.nextInt(500,10080);
        CreateTransactionResponse shuffling = null;
        Wallet randomStandart = getRandomStandartWallet();
        Wallet randomVault =  getRandomVaultWallet();

        for (Wallet wallet: wallets) {
            switch (type){
                case 0:
                    shuffling = shufflingCreate(wallet, registrationPeriod,3,1000,null,type);
                    break;
                case 1:
                     AccountAssetsResponse assets  =  getAccountAssets(wallet);
                     String assetID =  assets.getAccountAssets().stream()
                             .filter(asset -> asset.getQuantityATU() > 10).findFirst().get().getAsset();
                     assertNotNull(assets.getAccountAssets());
                     verifyTransactionInBlock(transferAsset(wallet,assetID,3,randomStandart.getUser()).getTransaction());
                     verifyTransactionInBlock(transferAsset(wallet,assetID,3,randomVault.getUser()).getTransaction());
                     shuffling = shufflingCreate(wallet, registrationPeriod,3,1,assetID,type);
                    break;
                case 2:
                    AccountCurrencyResponse currencies  = getAccountCurrencies(wallet);
                    assertNotNull(currencies.getAccountCurrencies());
                    AccountCurrencyDTO currencyDTO = currencies.getAccountCurrencies().stream()
                            .filter(currencie -> (currencie.getType()&NON_SHUFFLEABLE) != NON_SHUFFLEABLE)
                            .findFirst().get();
                    verifyTransactionInBlock(transferCurrency(randomStandart.getUser(),currencyDTO.getCurrency(),wallet,3).getTransaction());
                    verifyTransactionInBlock(transferCurrency(randomVault.getUser(),currencyDTO.getCurrency(),wallet,3).getTransaction());
                     shuffling =  shufflingCreate(wallet, registrationPeriod,3,1,currencyDTO.getCurrency(),type);;
                     break;
            }
            System.out.println(shuffling.getTransaction());
            verifyCreatingTransaction(shuffling);
            verifyTransactionInBlock(shuffling.getTransaction());

            shufflingRegister(randomStandart,shuffling.getFullHash());
            shufflingRegister(randomVault,shuffling.getFullHash());


            /*
            ShufflingDTO shufflingDTO = getShuffling(shuffling.getTransaction());
            CreateTransactionResponse cancelTrx = shufflingCancel(wallet,shufflingDTO.getShuffling(),wallet.getUser(),shufflingDTO.getShufflingStateHash());
            verifyCreatingTransaction(cancelTrx);
             */
        }
    }


     private Wallet getRandomStandartWallet(){
        String randomPass = String.valueOf(RandomUtils.nextInt(1,199));
        return new Wallet(getAccountId(randomPass).getAccountRS(),randomPass);
    }

    private Wallet getRandomVaultWallet(){
        Account2FAResponse account = generateNewAccount();
        Wallet vaultWallet = new Wallet(account.getAccountRS(),account.getPassphrase(),true);
        log.debug(String.format("Vault Wallet: %s pass: %s",vaultWallet.getUser(),vaultWallet.getPass()));
        System.out.println(vaultWallet.getUser());
        System.out.println(vaultWallet.getPass());
        verifyTransactionInBlock(
                sendMoney(TestConfiguration.getTestConfiguration().getGenesisWallet(),vaultWallet.getUser(),5000).getTransaction()
        );
        verifyTransactionInBlock(
                sendMoney(vaultWallet,vaultWallet.getUser(),10).getTransaction()
        );
        return vaultWallet;
    }

    private void setUpCurrency(Wallet wallet){
       issueCurrency(wallet,1,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                150,
                150,
                0);
    }


}
