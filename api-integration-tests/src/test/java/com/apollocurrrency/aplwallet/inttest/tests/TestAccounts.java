package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.*;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;
import org.junit.jupiter.api.*;


import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


import static org.junit.jupiter.api.Assertions.*;

//@RunWith(JUnitPlatform.class)
public class TestAccounts extends TestBase {


    @Test
    @DisplayName("Verify AccountBlockCount endpoint")
    public void testAccountBlockCount() throws IOException {
        GetAccountBlockCount accountBlockCount = getAccountBlockCount(testConfiguration.getTestUser());
        log.trace("Acoount count = {}", accountBlockCount.numberOfBlocks);
        assertTrue(accountBlockCount.numberOfBlocks > 0);
    }

    @Test
    @DisplayName("Verify GetAccount endpoint")
    public void testAccount() throws IOException {
        GetAccountResponse account = getAccount(testConfiguration.getTestUser());
        log.trace("Get Account = {}", account.accountRS);
        assertEquals(account.accountRS,testConfiguration.getTestUser());
        assertNotNull(account.account,"Check account");
        assertNotNull(account.balanceATM,"Check balanceATM");
        assertNotNull(account.publicKey,"Check publicKey");
    }

    @Test
    @DisplayName("Verify AccountBlockIds endpoint")
    public void testAccountBlockIds() throws IOException {
        AccountBlockIdsResponse accountBlockIds = getAccountBlockIds(testConfiguration.getTestUser());
        log.trace("BlockIds count = {}", accountBlockIds.blockIds.size());
        assertTrue(accountBlockIds.blockIds.size() > 0);
    }


    @Test
    @DisplayName("Verify getAccountBlocks endpoint")
    public void testAccountBlocks() throws IOException {
        BlockListInfoResponse accountBlocks = getAccountBlocks(testConfiguration.getTestUser());
        log.trace("Blocks count = {}", accountBlocks.blocks.size());
        assertTrue(accountBlocks.blocks.size() > 0);
    }

    @Test
    @DisplayName("Verify getAccountId endpoint")
    public void testAccountId() throws IOException {
        AccountDTO account = getAccountId(testConfiguration.getTestUser(),testConfiguration.getSecretPhrase());
        assertEquals(testConfiguration.getTestUser(),account.accountRS);
        assertEquals(testConfiguration.getPublicKey(),account.publicKey);
        assertNotNull(account.account);
    }

    @Test
    @DisplayName("Verify getAccountLedger endpoint")
    public void testAccountLedger() throws IOException {
        AccountLedgerResponse accountLedger = getAccountLedger(testConfiguration.getTestUser());
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
        AccountPropertiesResponse  accountPropertiesResponse = getAccountProperties(testConfiguration.getTestUser());
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

    @Test
    @DisplayName("Verify Unconfirmed Transactions endpoint")
    public void testGetUnconfirmedTransactions() throws IOException {
        RetryPolicy retryPolicy = new RetryPolicy()
                .retryWhen(null)
                .withMaxRetries(3)
                .withDelay(10, TimeUnit.SECONDS);
        sendMoney(testConfiguration.getTestUser(),2);
        List<TransactionInfo> unconfirmedTransactionse = Failsafe.with(retryPolicy).get(() -> getUnconfirmedTransactions(testConfiguration.getTestUser()));
        assertNotNull( unconfirmedTransactionse);
        assertTrue(unconfirmedTransactionse.size() > 0);
    }

    @Test
    @DisplayName("Verify Unconfirmed Transactions Ids endpoint")
    public void testGetUnconfirmedTransactionsIds() throws IOException {
        RetryPolicy retryPolicy = new RetryPolicy()
                .retryWhen(null)
                .withMaxRetries(3)
                .withDelay(5, TimeUnit.SECONDS);
        sendMoney(testConfiguration.getTestUser(),2);
        AccountTransactionIdsResponse accountTransactionIdsResponse = Failsafe.with(retryPolicy).get(() -> getUnconfirmedTransactionIds(testConfiguration.getTestUser()));
        assertTrue(accountTransactionIdsResponse.unconfirmedTransactionIds.size() > 0);
    }


    @Test
    @DisplayName("Verify Get Guaranteed Balance endpoint")
    public void testGetGuaranteedBalance() throws IOException {
        BalanceDTO balance = getGuaranteedBalance(testConfiguration.getTestUser(), 2000);
        assertTrue(balance.guaranteedBalanceATM > 1);
    }

    @Test
    @DisplayName("Verify Get Balance endpoint")
    public void testGetBalance() throws IOException {
        BalanceDTO balance = getBalance(testConfiguration.getTestUser());
        assertTrue(balance.balanceATM > 1);
        assertTrue(balance.unconfirmedBalanceATM > 1);
    }


    @Test
    @DisplayName("Get Account Ledger Entry")
    public void testGetAccountLedgerEntry() throws IOException {
        EntryDTO ledgerEntry = getAccountLedgerEntry(String.valueOf(getAccountLedger(testConfiguration.getTestUser()).entries[0].ledgerId));
        assertNotNull(ledgerEntry.eventType);
        assertNotNull(ledgerEntry.ledgerId);
        assertNotNull(ledgerEntry.balance);
        assertNotNull(ledgerEntry.holdingType);
        assertNotNull(ledgerEntry.block);
        assertNotNull(ledgerEntry.account);
    }


    @Test
    @DisplayName("Get Account Public Key")
    public void testGetAccountPublicKey() throws IOException {
        assertEquals(testConfiguration.getPublicKey(), getAccountPublicKey(testConfiguration.getTestUser()));

    }


    @Test
    @DisplayName("Get Account Blockchain Transactions")
    public void testGetAccountTransaction()throws IOException {
        BlockchainTransactionsResponse blockchainTransactionsResponse =  getAccountTransaction(testConfiguration.getTestUser());
        assertTrue(blockchainTransactionsResponse.transactions.size()>0);
    }


    @DisplayName("Send Money")
    @Test
    public void testSendMoney() throws IOException {
        CreateTransactionResponse sendMoneyResponse = sendMoney("APL-KL45-8GRF-BKPM-E58NH",100);
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
    @Test
    public void testSendMoneyPrivate() throws IOException {
        CreateTransactionResponse sendMoneyResponse = sendMoneyPrivate("APL-KL45-8GRF-BKPM-E58NH",100);
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
    @Test
    public void setAccountInfo() throws IOException {
        String accountName = "Account "+new Date().getTime();
        String accountDesc= "Decription "+new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountInfo(testConfiguration.getTestUser(),testConfiguration.getSecretPhrase(),accountName,accountDesc);
        assertNotNull(setAccountInfo.transactionJSON.senderPublicKey);
        assertNotNull(setAccountInfo.transactionJSON.signature);
        assertNotNull(setAccountInfo.transactionJSON.fullHash);
        assertNotNull(setAccountInfo.transactionJSON.amountATM);
        assertNotNull(setAccountInfo.transactionJSON.ecBlockId);
        assertNotNull(setAccountInfo.transactionJSON.senderRS);
        assertNotNull(setAccountInfo.transactionJSON.transaction);
        assertNotNull(setAccountInfo.transactionJSON.feeATM);
    }


    @DisplayName("Set Account property")
    @Test
    public void setAccountProperty() throws IOException {
        String property = "Property "+new Date().getTime();
        CreateTransactionResponse setAccountInfo = setAccountProperty(testConfiguration.getTestUser(),testConfiguration.getSecretPhrase(),property);
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
    @Test
    public void  getAccountProperty() throws IOException { ;
        GetPropertyResponse propertyResponse = getAccountProperty(testConfiguration.getTestUser());
        assertTrue(propertyResponse.properties.length >0);
    }

    @DisplayName("Delete Account Property")
    @Test
    public void  deleteAccountProperty() throws IOException { ;
        CreateTransactionResponse transaction = deleteAccountProperty(testConfiguration.getSecretPhrase(),getAccountProperty(testConfiguration.getTestUser()).properties[0].property);
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

    @DisplayName("Delete Secret Key")
    @Test
    public void  deleteKey() throws IOException { ;
        AccountDTO accountDTO = generateNewAccount();
        Account2FA deletedAccount = deleteKey(accountDTO.accountRS,accountDTO.passphrase);
        assertEquals(Status2FA.OK,deletedAccount.getStatus());
    }

    @DisplayName("Enable 2FA")
    @Test
    public void  enable2FA() throws IOException { ;
        AccountDTO accountDTO = generateNewAccount();
        accountDTO = enable2FA(accountDTO.accountRS,accountDTO.passphrase);
        assertNotNull(accountDTO.secret);
    }






}
