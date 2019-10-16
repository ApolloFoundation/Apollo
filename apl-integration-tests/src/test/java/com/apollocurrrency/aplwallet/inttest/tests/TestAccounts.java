package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
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
public class TestAccounts extends TestBase {


    @Test
    @DisplayName("Verify AccountBlockCount endpoint")
    public void testAccountBlockCount() throws IOException {
        GetAccountBlockCountResponse accountBlockCount = getAccountBlockCount(getTestConfiguration().getStandartWallet().getUser());
        log.trace("Acoount count = {}", accountBlockCount.getNumberOfBlocks());
        assertTrue(accountBlockCount.getNumberOfBlocks() > 0);
    }

    @Test
    @DisplayName("Verify GetAccount endpoint")
    public void testAccount() throws IOException {
        GetAccountResponse account = getAccount(getTestConfiguration().getStandartWallet().getUser());
        log.trace("Get Account = {}", account.accountRS);
        assertEquals(account.accountRS, getTestConfiguration().getStandartWallet().getUser());
        assertNotNull(account.account,"Check account");
        assertNotNull(account.balanceATM,"Check balanceATM");
        assertNotNull(account.publicKey,"Check publicKey");
    }

    @Test
    @DisplayName("Verify AccountBlockIds endpoint")
    public void testAccountBlockIds() throws IOException {
        AccountBlockIdsResponse accountBlockIds = getAccountBlockIds( getTestConfiguration().getStandartWallet().getUser());
        log.trace("BlockIds count = {}", accountBlockIds.blockIds.size());
        assertTrue(accountBlockIds.blockIds.size() > 0);
    }


    @Test
    @DisplayName("Verify getAccountBlocks endpoint")
    public void testAccountBlocks() throws IOException {
        BlockListInfoResponse accountBlocks = getAccountBlocks( getTestConfiguration().getStandartWallet().getUser());
        log.trace("Blocks count = {}", accountBlocks.blocks.size());
        assertTrue(accountBlocks.blocks.size() > 0);
    }


    @DisplayName("Verify getAccountId endpoint")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testAccountId(Wallet wallet) throws IOException {
        AccountDTO account = getAccountId(wallet);
        assertEquals(getTestConfiguration().getStandartWallet().getUser(),account.accountRS);
        assertEquals(getTestConfiguration().getStandartWallet().getPublicKey(),account.publicKey);
        assertNotNull(account.account);
    }

    @DisplayName("Verify getAccountLedger endpoint")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testAccountLedger(Wallet wallet) throws IOException {
        AccountLedgerResponse accountLedger = getAccountLedger(wallet);
        assertTrue(accountLedger.entries.length > 0,"Ledger is NULL");
        assertNotNull(accountLedger.entries[0].account);
        assertNotNull(accountLedger.entries[0].accountRS);
        assertNotNull(accountLedger.entries[0].balance);
        assertNotNull(accountLedger.entries[0].block);
        assertNotNull(accountLedger.entries[0].change);
        assertNotNull(accountLedger.entries[0].height);
        assertNotNull(accountLedger.entries[0].ledgerId);
    }


    @Test
    @DisplayName("Get Account Properties")
    public void testAccountProperties() throws IOException {
        AccountPropertiesResponse accountPropertiesResponse = getAccountProperties(getTestConfiguration().getStandartWallet().getUser());
        assertNotNull(accountPropertiesResponse.properties,"Account Properties is NULL");
        assertTrue(accountPropertiesResponse.properties.size() > 0,"Account Properties count = 0");
    }

    @Test
    @DisplayName("Verify Search Accounts  endpoint")
    public void testSearchAccounts() throws IOException {
        //Before set Account info Test1
        SearchAccountsResponse searchAccountsResponse = searchAccounts("Test1");
        assertNotNull(searchAccountsResponse, "Response - null");
        assertNotNull(searchAccountsResponse.accounts, "Response accountDTOS - null");
        assertTrue(searchAccountsResponse.accounts.length >0,"Account not found");
    }

    @DisplayName("Verify Unconfirmed Transactions endpoint")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetUnconfirmedTransactions(Wallet wallet) throws IOException {
        sendMoney(wallet, getTestConfiguration().getStandartWallet().getUser(),2);
        TransactionListResponse transactionInfos = getUnconfirmedTransactions(wallet);
        assertNotNull(transactionInfos.unconfirmedTransactions);
        assertTrue(transactionInfos.unconfirmedTransactions.size() > 0);
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
        assertTrue(accountTransactionIdsResponse.unconfirmedTransactionIds.size() > 0);
    }


    @Test
    @DisplayName("Verify Get Guaranteed Balance endpoint")
    public void testGetGuaranteedBalance() throws IOException {
        BalanceDTO balance = getGuaranteedBalance( getTestConfiguration().getStandartWallet().getUser(), 2000);
        assertTrue(balance.guaranteedBalanceATM > 1);
    }

    @DisplayName("Verify Get Balance endpoint")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetBalance(Wallet wallet) throws IOException {
        BalanceDTO balance = getBalance(wallet);
        assertTrue(balance.balanceATM > 1);
        assertTrue(balance.unconfirmedBalanceATM > 1);
    }


    @DisplayName("Get Account Ledger Entry")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetAccountLedgerEntry(Wallet wallet) throws IOException {
        EntryDTO ledgerEntry = getAccountLedgerEntry(String.valueOf(getAccountLedger(wallet).entries[0].ledgerId));
        assertNotNull(ledgerEntry.eventType);
        assertNotNull(ledgerEntry.ledgerId);
        assertNotNull(ledgerEntry.balance);
        assertNotNull(ledgerEntry.holdingType);
        assertNotNull(ledgerEntry.block);
        assertNotNull(ledgerEntry.account);
    }



    @DisplayName("Get Account Public Key")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetAccountPublicKey(Wallet wallet) throws IOException {
        assertEquals(wallet.getPublicKey(), getAccountPublicKey(wallet).publicKey);

    }



    @DisplayName("Get Account Blockchain Transactions")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testGetAccountTransaction(Wallet wallet)throws IOException {
        BlockchainTransactionsResponse blockchainTransactionsResponse =  getAccountTransaction(wallet);
        assertTrue(blockchainTransactionsResponse.transactions.size()>0);
    }


    @DisplayName("Send Money")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testSendMoney(Wallet wallet) throws Exception {
        CreateTransactionResponse sendMoneyResponse = sendMoney(wallet,"APL-KL45-8GRF-BKPM-E58NH",100);
        assertNotNull(sendMoneyResponse.transactionJSON.senderPublicKey);
        assertNotNull(sendMoneyResponse.transactionJSON.signature);
        assertNotNull(sendMoneyResponse.transactionJSON.fullHash);
        assertNotNull(sendMoneyResponse.transactionJSON.amountATM);
        assertNotNull(sendMoneyResponse.transactionJSON.ecBlockId);
        assertNotNull(sendMoneyResponse.transactionJSON.senderRS);
        assertNotNull(sendMoneyResponse.transactionJSON.transaction);
        assertNotNull(sendMoneyResponse.transactionJSON.feeATM);
    }


    @DisplayName("Send Money Private")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void testSendMoneyPrivate(Wallet wallet) throws IOException {
        CreateTransactionResponse sendMoneyResponse = sendMoneyPrivate(wallet,"APL-KL45-8GRF-BKPM-E58NH",100);
        assertNotNull(sendMoneyResponse.transactionJSON.senderPublicKey);
        assertNotNull(sendMoneyResponse.transactionJSON.signature);
        assertNotNull(sendMoneyResponse.transactionJSON.fullHash);
        assertNotNull(sendMoneyResponse.transactionJSON.amountATM);
        assertNotNull(sendMoneyResponse.transactionJSON.ecBlockId);
        assertNotNull(sendMoneyResponse.transactionJSON.senderRS);
        assertNotNull(sendMoneyResponse.transactionJSON.transaction);
        assertNotNull(sendMoneyResponse.transactionJSON.feeATM);
    }


    @DisplayName("Set Account info")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void setAccountInfo(Wallet wallet) throws IOException {
        String accountName = "Account "+new Date().getTime();
        String accountDesc= "Decription "+new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountInfo(wallet,accountName,accountDesc);
        assertNotNull(setAccountInfo.transactionJSON.senderPublicKey);
        assertNotNull(setAccountInfo.transactionJSON.signature);
        assertNotNull(setAccountInfo.transactionJSON.fullHash);
        assertNotNull(setAccountInfo.transactionJSON.amountATM);
        assertNotNull(setAccountInfo.transactionJSON.ecBlockId);
        assertNotNull(setAccountInfo.transactionJSON.senderRS);
        assertNotNull(setAccountInfo.transactionJSON.transaction);
        assertNotNull(setAccountInfo.transactionJSON.feeATM);
        verifyTransactionInBlock(setAccountInfo.transactionJSON.transaction);
    }


    @DisplayName("Set Account property")
    @Test
    public void setAccountProperty() throws IOException {
        String property = "Property "+new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty( getTestConfiguration().getStandartWallet(),property);
        assertNotNull(setAccountInfo.transactionJSON.senderPublicKey);
        assertNotNull(setAccountInfo.transactionJSON.signature);
        assertNotNull(setAccountInfo.transactionJSON.fullHash);
        assertNotNull(setAccountInfo.transactionJSON.amountATM);
        assertNotNull(setAccountInfo.transactionJSON.ecBlockId);
        assertNotNull(setAccountInfo.transactionJSON.senderRS);
        assertNotNull(setAccountInfo.transactionJSON.transaction);
        assertNotNull(setAccountInfo.transactionJSON.feeATM);
    }


    @DisplayName("Get Account Property")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void  getAccountPropertyTest(Wallet wallet) throws IOException { ;
        GetPropertyResponse propertyResponse = getAccountProperty(wallet);
        assertTrue(propertyResponse.properties.length >0);
    }

    @DisplayName("Delete Account Property")
    @Disabled
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void  deleteAccountProperty(Wallet wallet) throws IOException {
        String property = "Property "+new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty(wallet,property);
        verifyTransactionInBlock(setAccountInfo.transaction);
        CreateTransactionResponse transaction = deleteAccountProperty(wallet,getAccountProperty(wallet).properties[0].property);
        assertNotNull(transaction.transactionJSON.senderPublicKey);
        assertNotNull(transaction.transactionJSON.signature);
        assertNotNull(transaction.transactionJSON.fullHash);
        assertNotNull(transaction.transactionJSON.amountATM);
        assertNotNull(transaction.transactionJSON.ecBlockId);
        assertNotNull(transaction.transactionJSON.senderRS);
        assertNotNull(transaction.transactionJSON.transaction);
        assertNotNull(transaction.transactionJSON.feeATM);

    }

    @DisplayName("Generate Account")
    @Test
    public void  generateAccount() throws IOException { ;
        AccountDTO accountDTO = generateNewAccount();
        assertNotNull(accountDTO.accountRS);
        assertNotNull(accountDTO.passphrase);
        assertNotNull(accountDTO.publicKey);
        assertNotNull(accountDTO.account);
    }



}
