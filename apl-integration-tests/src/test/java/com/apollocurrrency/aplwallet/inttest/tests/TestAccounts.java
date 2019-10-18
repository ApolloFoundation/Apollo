package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.*;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;


import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static org.junit.jupiter.api.Assertions.*;

//@RunWith(JUnitPlatform.class)
public class TestAccounts extends TestBaseOld {


    @Test
    @DisplayName("Verify AccountBlockCount endpoint")
    public void testAccountBlockCount() throws IOException {
        GetAccountBlockCountResponse accountBlockCount = getAccountBlockCount(getTestConfiguration().getStandartWallet().getUser());
        log.trace("Account count = {}", accountBlockCount.getNumberOfBlocks());
        assertTrue(accountBlockCount.getNumberOfBlocks() > 0);
    }

    @Test
    @DisplayName("Verify GetAccount endpoint")
    public void testAccount() throws IOException {
        GetAccountResponse account = getAccount(getTestConfiguration().getStandartWallet().getUser());
        log.trace("Get Account = {}", account.getAccountRS());
        assertEquals(account.getAccountRS(), getTestConfiguration().getStandartWallet().getUser());
        assertNotNull(account.getAccount(),"Check account");
        assertNotNull(account.getBalanceATM(),"Check balanceATM");
        assertNotNull(account.getPublicKey(),"Check publicKey");
    }

    @Test
    @DisplayName("Verify AccountBlockIds endpoint")
    public void testAccountBlockIds() throws IOException {
        AccountBlockIdsResponse accountBlockIds = getAccountBlockIds( getTestConfiguration().getStandartWallet().getUser());
        log.trace("BlockIds count = {}", accountBlockIds.getBlockIds().size());
        assertTrue(accountBlockIds.getBlockIds().size() > 0);
    }


    @Test
    @DisplayName("Verify getAccountBlocks endpoint")
    public void testAccountBlocks() throws IOException {
        BlockListInfoResponse accountBlocks = getAccountBlocks( getTestConfiguration().getStandartWallet().getUser());
        log.trace("Blocks count = {}", accountBlocks.getBlocks().size());
        assertTrue(accountBlocks.getBlocks().size() > 0);
    }


    @DisplayName("Verify getAccountId endpoint")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testAccountId(Wallet wallet) throws IOException {
        AccountDTO account = getAccountId(wallet);
        assertEquals(getTestConfiguration().getStandartWallet().getUser(),account.getAccountRS());
        assertEquals(getTestConfiguration().getStandartWallet().getPublicKey(),account.getPublicKey());
        assertNotNull(account.getAccount());
    }

    @DisplayName("Verify getAccountLedger endpoint")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testAccountLedger(Wallet wallet) throws IOException {
        AccountLedgerResponse accountLedger = getAccountLedger(wallet);
        assertTrue(accountLedger.getEntries().size() > 0,"Ledger is NULL");
        assertNotNull(accountLedger.getEntries().get(0).getAccount());
        assertNotNull(accountLedger.getEntries().get(0).getAccountRS());
        assertNotNull(accountLedger.getEntries().get(0).getBalance());
        assertNotNull(accountLedger.getEntries().get(0).getBlock());
        assertNotNull(accountLedger.getEntries().get(0).getChange());
        assertNotNull(accountLedger.getEntries().get(0).getHeight());
        assertNotNull(accountLedger.getEntries().get(0).getLedgerId());
    }


    @Test
    @DisplayName("Get Account Properties")
    public void testAccountProperties() throws IOException {
        AccountPropertiesResponse accountPropertiesResponse = getAccountProperties(getTestConfiguration().getStandartWallet().getUser());
        assertNotNull(accountPropertiesResponse.getProperties(),"Account Properties is NULL");
        assertTrue(accountPropertiesResponse.getProperties().size() > 0,"Account Properties count = 0");
    }

    @Test
    @DisplayName("Verify Search Accounts  endpoint")
    public void testSearchAccounts() throws IOException {
        //Before set Account info Test1
        SearchAccountsResponse searchAccountsResponse = searchAccounts("Test1");
        assertNotNull(searchAccountsResponse, "Response - null");
        assertNotNull(searchAccountsResponse.getAccounts(), "Response accountDTOS - null");
        assertTrue(searchAccountsResponse.getAccounts().size() > 0,"Account not found");
    }

    @DisplayName("Verify Unconfirmed Transactions endpoint")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetUnconfirmedTransactions(Wallet wallet) throws IOException {
        sendMoney(wallet, getTestConfiguration().getStandartWallet().getUser(),2);
        TransactionListResponse transactionInfos = getUnconfirmedTransactions(wallet);
        assertNotNull(transactionInfos.getUnconfirmedTransactions());
        assertTrue(transactionInfos.getUnconfirmedTransactions().size() > 0);
    }

    @Test
    @DisplayName("Verify Unconfirmed Transactions Ids endpoint")
    public void testGetUnconfirmedTransactionsIds() throws IOException {
        RetryPolicy retryPolicy = new RetryPolicy()
                .retryWhen(null)
                .withMaxRetries(3)
                .withDelay(5, TimeUnit.SECONDS);
        sendMoney( getTestConfiguration().getStandartWallet(), getTestConfiguration().getStandartWallet().getUser(),2);
        AccountTransactionIdsResponse accountTransactionIdsResponse = Failsafe.with(retryPolicy).get(() -> getUnconfirmedTransactionIds( getTestConfiguration().getStandartWallet().getUser()));
        assertTrue(accountTransactionIdsResponse.getUnconfirmedTransactionIds().size() > 0);
    }


    @Test
    @DisplayName("Verify Get Guaranteed Balance endpoint")
    public void testGetGuaranteedBalance() throws IOException {
        BalanceDTO balance = getGuaranteedBalance( getTestConfiguration().getStandartWallet().getUser(), 2000);
        assertTrue(balance.getGuaranteedBalanceATM() > 1);
    }

    @DisplayName("Verify Get Balance endpoint")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetBalance(Wallet wallet) throws IOException {
        BalanceDTO balance = getBalance(wallet);
        assertTrue(balance.getBalanceATM() > 1);
        assertTrue(balance.getUnconfirmedBalanceATM() > 1);
    }


    @DisplayName("Get Account Ledger Entry")
    @ParameterizedTest
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
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetAccountPublicKey(Wallet wallet) throws IOException {
        assertEquals(wallet.getPublicKey(), getAccountPublicKey(wallet).getPublicKey());

    }



    @DisplayName("Get Account Blockchain Transactions")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetAccountTransaction(Wallet wallet)throws IOException {
        BlockchainTransactionsResponse blockchainTransactionsResponse =  getAccountTransaction(wallet);
        assertTrue(blockchainTransactionsResponse.getTransactions().size()>0);
    }


    @DisplayName("Send Money")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testSendMoney(Wallet wallet) throws Exception {
        CreateTransactionResponse sendMoneyResponse = sendMoney(wallet,"APL-KL45-8GRF-BKPM-E58NH",100);
        verifyCreatingTransaction(sendMoneyResponse);
    }


    @DisplayName("Send Money Private")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testSendMoneyPrivate(Wallet wallet) throws IOException {
        CreateTransactionResponse sendMoneyResponse = sendMoneyPrivate(wallet,"APL-KL45-8GRF-BKPM-E58NH",100);
        verifyCreatingTransaction(sendMoneyResponse);
    }


    @DisplayName("Set Account info")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void setAccountInfo(Wallet wallet) throws IOException {
        String accountName = "Account "+new Date().getTime();
        String accountDesc= "Decription "+new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountInfo(wallet,accountName,accountDesc);
        verifyCreatingTransaction(setAccountInfo);
        verifyTransactionInBlock(setAccountInfo.getTransactionJSON().getTransaction());
    }


    @DisplayName("Set Account property")
    @Test
    public void setAccountProperty() throws IOException {
        String property = "Property "+new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty( getTestConfiguration().getStandartWallet(),property);
        verifyCreatingTransaction(setAccountInfo);
    }


    @DisplayName("Get Account Property")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void  getAccountPropertyTest(Wallet wallet) throws IOException { ;
        AccountPropertiesResponse propertyResponse = getAccountProperty(wallet);
        assertTrue(propertyResponse.getProperties().size() > 0);
    }

    @DisplayName("Delete Account Property")
    @Disabled
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void  deleteAccountProperty(Wallet wallet) throws IOException {
        String property = "Property "+new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty(wallet,property);
        verifyTransactionInBlock(setAccountInfo.getTransaction());
        CreateTransactionResponse transaction = deleteAccountProperty(wallet, getAccountProperty(wallet).getProperties().get(0).getProperty());
        verifyCreatingTransaction(transaction);
    }

    @DisplayName("Generate Account")
    @Test
    public void  generateAccount() throws IOException { ;
        AccountDTO accountDTO = generateNewAccount();
        assertNotNull(accountDTO.getAccountRS());
        assertNotNull(accountDTO.getPassphrase());
        assertNotNull(accountDTO.getPublicKey());
        assertNotNull(accountDTO.getAccount());
    }



}
