package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.EntryDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlockIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountLedgerResponse;
import com.apollocurrency.aplwallet.api.response.AccountPropertiesResponse;
import com.apollocurrency.aplwallet.api.response.AccountTransactionIdsResponse;
import com.apollocurrency.aplwallet.api.response.BlockListInfoResponse;
import com.apollocurrency.aplwallet.api.response.BlockchainTransactionsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountBlockCountResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrency.aplwallet.api.response.SearchAccountsResponse;
import com.apollocurrency.aplwallet.api.response.TransactionListResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.providers.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import net.jodah.failsafe.Failsafe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Accounts")
@Epic(value = "Accounts")
public class TestAccounts extends TestBaseNew {


    @Test
    @DisplayName("Verify AccountBlockCount endpoint")
    public void testAccountBlockCount(){
        GetAccountBlockCountResponse accountBlockCount = getAccountBlockCount(getTestConfiguration().getGenesisWallet().getUser());
        log.info("Account count = {}", accountBlockCount.getNumberOfBlocks());
        assertThat("Genesis account has more than 0 generated blocks",accountBlockCount.getNumberOfBlocks().intValue(), greaterThan( 0 ));
    }

    @Test
    @DisplayName("Verify GetAccount endpoint")
    public void testAccount() {
        GetAccountResponse account = getAccount(getTestConfiguration().getStandartWallet().getUser());
        log.trace("Get Account = {}", account.getAccountRS());
        assertEquals(account.getAccountRS(), getTestConfiguration().getStandartWallet().getUser());
        assertNotNull(account.getAccount(), "Check account");
        assertNotNull(account.getBalanceATM(), "Check balanceATM");
        assertNotNull(account.getPublicKey(), "Check publicKey");
    }

    @Test
    @DisplayName("Verify AccountBlockIds endpoint")
    public void testAccountBlockIds() {
        AccountBlockIdsResponse accountBlockIds = getAccountBlockIds(getTestConfiguration().getGenesisWallet().getUser());
        log.trace("BlockIds count = {}", accountBlockIds.getBlockIds().size());
        assertThat("Genesis account has more than 0 generated blocks",accountBlockIds.getBlockIds().size(), greaterThan( 0 ));
    }


    @Test
    @DisplayName("Verify getAccountBlocks endpoint")
    @Issue("APL-1388")
    public void testAccountBlocks(){
        BlockListInfoResponse accountBlocks = getAccountBlocks(getTestConfiguration().getGenesisWallet().getUser());
        log.info("Blocks count = {}", accountBlocks.getBlocks().size());
        assertThat("Genesis account has more than 0 generated blocks",accountBlocks.getBlocks().size(), greaterThan( 0 ));
    }


    @DisplayName("Verify getAccountId endpoint")
    @Test
    public void testAccountId() {
        Wallet wallet = TestConfiguration.getTestConfiguration().getStandartWallet();
        AccountDTO account = getAccountId(wallet.getPass());
        assertEquals(getTestConfiguration().getStandartWallet().getUser(), account.getAccountRS());
        assertEquals(getTestConfiguration().getStandartWallet().getPublicKey(), account.getPublicKey());
        assertNotNull(account.getAccount());
    }

    @DisplayName("Verify getAccountLedger endpoint")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void testAccountLedger(Wallet wallet) throws IOException {
        AccountLedgerResponse accountLedger = getAccountLedger(wallet);
        assertThat("No any ledger:",accountLedger.getEntries().size(), greaterThan(0));
        assertNotNull(accountLedger.getEntries().stream().findFirst().get().getAccount());
        assertNotNull(accountLedger.getEntries().stream().findFirst().get().getAccountRS());
        assertNotNull(accountLedger.getEntries().stream().findFirst().get().getBalance());
        assertNotNull(accountLedger.getEntries().stream().findFirst().get().getBlock());
        assertNotNull(accountLedger.getEntries().stream().findFirst().get().getChange());
        assertNotNull(accountLedger.getEntries().stream().findFirst().get().getHeight());
        assertNotNull(accountLedger.getEntries().stream().findFirst().get().getLedgerId());
    }


    @Test
    @DisplayName("Get Account Properties")
    public void testAccountProperties() throws IOException {
        String property = "Property " + new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty(getTestConfiguration().getStandartWallet(), property);
        verifyTransactionInBlock(setAccountInfo.getTransaction());
        AccountPropertiesResponse accountPropertiesResponse = getAccountProperties(getTestConfiguration().getStandartWallet().getUser());
        assertNotNull(accountPropertiesResponse.getProperties(), "Account Properties is NULL");
        assertTrue(accountPropertiesResponse.getProperties().size() > 0, "Account Properties count = 0");
    }

    @Test
    @DisplayName("Verify Search Accounts  endpoint")
    public void testSearchAccounts() throws IOException {
        String accountName = "Account " + new Date().getTime();
        String accountDesc = "Decription " + new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountInfo(TestConfiguration.getTestConfiguration().getGenesisWallet(), accountName, accountDesc);
        verifyCreatingTransaction(setAccountInfo);
        verifyTransactionInBlock(setAccountInfo.getTransaction());
        SearchAccountsResponse searchAccountsResponse = searchAccounts(accountName);
        assertNotNull(searchAccountsResponse, "Response - null");
        assertNotNull(searchAccountsResponse.getAccounts(), "Response accountDTOS - null");
        assertThat("Account not found",searchAccountsResponse.getAccounts().size(), greaterThan(0) );
    }

    @DisplayName("Verify Unconfirmed Transactions endpoint")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void testGetUnconfirmedTransactions(Wallet wallet) throws IOException {
        sendMoney(wallet, getTestConfiguration().getStandartWallet().getUser(), 2);
        TransactionListResponse transactionInfos = getUnconfirmedTransactions(wallet);
        assertNotNull(transactionInfos.getUnconfirmedTransactions());
        assertThat(transactionInfos.getUnconfirmedTransactions().size(),greaterThan(0));
    }

    @Test
    @DisplayName("Verify Unconfirmed Transactions Ids endpoint")
    public void testGetUnconfirmedTransactionsIds() throws IOException {
        sendMoney(getTestConfiguration().getStandartWallet(), getTestConfiguration().getStandartWallet().getUser(), 2);
        AccountTransactionIdsResponse accountTransactionIdsResponse = Failsafe.with(retryPolicy).get(() -> getUnconfirmedTransactionIds(getTestConfiguration().getStandartWallet().getUser()));
        assertThat(accountTransactionIdsResponse.getUnconfirmedTransactionIds().size() , greaterThan(0));
    }


    @Test
    @DisplayName("Verify Get Guaranteed Balance endpoint")
    public void testGetGuaranteedBalance() throws IOException {
        BalanceDTO balance = getGuaranteedBalance(getTestConfiguration().getGenesisWallet().getUser(), 1);
        assertTrue(balance.getGuaranteedBalanceATM() > 1);
    }

    @DisplayName("Verify Get Balance endpoint")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void testGetBalance(Wallet wallet) throws IOException {
        BalanceDTO balance = getBalance(wallet);
        assertTrue(balance.getBalanceATM() > 1);
        assertTrue(balance.getUnconfirmedBalanceATM() > 1);
    }


    @DisplayName("Get Account Ledger Entry")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void testGetAccountLedgerEntry(Wallet wallet) throws IOException {
        EntryDTO ledgerEntry = getAccountLedgerEntry(String.valueOf(getAccountLedger(wallet).getEntries().get(0).getLedgerId()));
        assertNotNull(ledgerEntry.getEventType());
        assertNotNull(ledgerEntry.getLedgerId());
        assertNotNull(ledgerEntry.getBalance());
        assertNotNull(ledgerEntry.getHoldingType());
        assertNotNull(ledgerEntry.getBlock());
        assertNotNull(ledgerEntry.getAccount());
    }


    @DisplayName("Get Account Public Key")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void testGetAccountPublicKey(Wallet wallet) throws IOException {
        assertEquals(wallet.getPublicKey(), getAccountPublicKey(wallet).getPublicKey());

    }


    @DisplayName("Get Account Blockchain Transactions")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void testGetAccountTransaction(Wallet wallet) throws IOException {
        BlockchainTransactionsResponse blockchainTransactionsResponse = getAccountTransaction(wallet);
        assertTrue(blockchainTransactionsResponse.getTransactions().size() > 0);
    }


    @DisplayName("Send Money")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void testSendMoney(Wallet wallet) throws Exception {
        Set<String> transactions = new HashSet<>();
        Set<Wallet> wallets = new HashSet<>();

        int countOfTransactions = 50;
        for (int i = 10000; i < countOfTransactions+10000; i++) {
            Wallet new_wallet = getNewWallet(String.valueOf(i));
            wallets.add(new_wallet);
            CreateTransactionResponse sendMoneyResponse = sendMoney(wallet, new_wallet.getUser(), 10);
            verifyCreatingTransaction(sendMoneyResponse);
            transactions.add(sendMoneyResponse.getTransaction());
        }

        for (String trx : transactions) {
            verifyTransactionInBlock(trx);
        }

        for (Wallet wl : wallets) {
            int allBalance = (int)((getBalance(wl).getBalanceATM() - 100000000L) / 100000000) ;
            sendMoney(wl, wallet.getUser(), allBalance);
        }
    }




    @DisplayName("Send Money Private")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void testSendMoneyPrivate(Wallet wallet) throws IOException {
        CreateTransactionResponse sendMoneyResponse = sendMoneyPrivate(wallet, wallet.getUser(), 100);
        verifyCreatingTransaction(sendMoneyResponse);
    }


    @DisplayName("Set Account info")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void setAccountInfo(Wallet wallet) throws IOException {
        String accountName = "Account " + new Date().getTime();
        String accountDesc = "Decription " + new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountInfo(wallet, accountName, accountDesc);
        verifyCreatingTransaction(setAccountInfo);
        verifyTransactionInBlock(setAccountInfo.getTransactionJSON().getTransaction());
    }


    @DisplayName("Set Account property")
    @Test
    public void setAccountProperty() throws IOException {
        String property = "Property " + new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty(getTestConfiguration().getStandartWallet(), property);
        verifyCreatingTransaction(setAccountInfo);
    }


    @DisplayName("Get Account Property")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountPropertyTest(Wallet wallet){
        String property = "Property " + new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty(wallet, property);
        verifyTransactionInBlock(setAccountInfo.getTransaction());
        AccountPropertiesResponse propertyResponse = getAccountProperties(wallet.getUser());
        assertTrue(propertyResponse.getProperties().size() > 0);
    }

    @DisplayName("Delete Account Property")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void deleteAccountProperty(Wallet wallet) {
        String property = "Property " + new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty(wallet, property);
        verifyTransactionInBlock(setAccountInfo.getTransaction());
        CreateTransactionResponse transaction = deleteAccountProperty(wallet, getAccountProperties(wallet.getUser()).getProperties().stream().findFirst().get().getProperty());
        verifyCreatingTransaction(transaction);
    }

    @DisplayName("Generate Account")
    @Test
    public void generateAccount() {
        ;
        Account2FAResponse accountDTO = generateNewAccount();
        assertNotNull(accountDTO.getAccountRS());
        assertNotNull(accountDTO.getPassphrase());
        assertNotNull(accountDTO.getPublicKey());
        assertNotNull(accountDTO.getAccount());
    }

    @DisplayName("Lease Balance")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void leaseBalanceTest(Wallet wallet){
        String firstLeaseWalletPass = "1";
        String secondtLeaseWalletPass = "2";
        CreateTransactionResponse response;

        Wallet firstleaseWallet = new Wallet(getAccountId(firstLeaseWalletPass).getAccountRS(), firstLeaseWalletPass);
        Wallet secondtleaseWallet = new Wallet(getAccountId(secondtLeaseWalletPass).getAccountRS(), secondtLeaseWalletPass);

        GetAccountResponse accountDTO = getAccount(wallet.getUser());
       if (accountDTO.getEffectiveBalanceAPL() > 1000L) {
            startForging(wallet);
            response =  leaseBalance(firstleaseWallet.getUser(),wallet);
            verifyTransactionInBlock(response.getTransaction());
        }


        accountDTO = getAccount(firstleaseWallet.getUser());
        if (accountDTO.getEffectiveBalanceAPL() > 1000L) {
            startForging(firstleaseWallet);
            response =  leaseBalance(secondtleaseWallet.getUser(),firstleaseWallet);
            verifyTransactionInBlock(response.getTransaction());
        }
       startForging(secondtleaseWallet);

        // accountDTO.getEffectiveBalanceAPL() == 0 after 1440 blocks
/*       accountDTO = getAccount(wallet.getUser());
       assertThat("Effective Balance not valid", accountDTO.getEffectiveBalanceAPL(),greaterThan(0L));

       accountDTO = getAccount(firstleaseWallet.getUser());
       assertThat("Effective Balance not valid", accountDTO.getEffectiveBalanceAPL(),greaterThan(0L));*/
    }

}
