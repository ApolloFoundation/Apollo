package com.apollocurrrency.aplwallet.inttest.API;

import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;
import org.junit.Ignore;
import org.junit.jupiter.api.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;


import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.*;
import static com.apollocurrency.aplwallet.api.dto.RequestType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

//@RunWith(JUnitPlatform.class)
public class TestAccounts {
    private static final Logger log = LoggerFactory.getLogger(TestAccounts.class);
    private static TestConfiguration testConfiguration;
    private static ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
     static void setupConfiguration() {
        testConfiguration = TestConfiguration.getTestConfiguration();
    }


    @Test
    @DisplayName("Verify AccountBlockCount endpoint")
    public void testAccountBlockCount() throws IOException {
        GetAccountBlockCount accountBlockCount = getAccountBlockCount(testConfiguration.getTestUser());
        log.trace("Acoount count = {}", accountBlockCount.numberOfBlocks);
        assertTrue(accountBlockCount.numberOfBlocks > 0);
    }

    @Test
  //  @Disabled("Check fields on GetAccount API and fbc.core.model.Account")
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
    @Ignore("Need Set Account Properties")
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
        assertNotNull(searchAccountsResponse.accountDTOS, "Response accountDTOS - null");
        assertTrue(searchAccountsResponse.accountDTOS.length >0);
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

    public BlockListInfoResponse getAccountBlocks(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountBlocks);
        addParameters(Parameters.account, account);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string().toString(), BlockListInfoResponse.class);
    }


    public GetAccountResponse getAccount(String account) throws IOException {
        addParameters(RequestType.requestType, getAccount);
        addParameters(Parameters.account, account);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAccountResponse.class);
    }


    public GetAccountBlockCount getAccountBlockCount(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountBlockCount);
        addParameters(Parameters.account, account);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), GetAccountBlockCount.class);
    }

    public AccountBlockIdsResponse getAccountBlockIds(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountBlockIds);
        addParameters(Parameters.account, account);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), AccountBlockIdsResponse.class);
    }

    public AccountDTO getAccountId(String account, String secretPhrase) throws IOException {
        addParameters(RequestType.requestType, getAccountId);
        addParameters(Parameters.account, account);
        addParameters(Parameters.secretPhrase, secretPhrase);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), AccountDTO.class);
    }

    public AccountLedgerResponse getAccountLedger(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountLedger);
        addParameters(Parameters.account, account);
        Response response = httpCallPost();
        assertEquals(200, response.code());
      //  System.out.println(response.body().string());
        return   mapper.readValue(response.body().string().toString(), AccountLedgerResponse.class);
    }

    public AccountPropertiesResponse getAccountProperties(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountProperties);
        addParameters(Parameters.recipient, account);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), AccountPropertiesResponse.class);
    }

    public SearchAccountsResponse  searchAccounts(String searchQuery) throws IOException {
        addParameters(RequestType.requestType,RequestType.searchAccounts);
        addParameters(Parameters.query, searchQuery);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), SearchAccountsResponse.class);
    }

    public List<TransactionInfo> getUnconfirmedTransactions(String account) throws IOException {
        addParameters(RequestType.requestType,RequestType.getUnconfirmedTransactions);
        addParameters(Parameters.account,account);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string().toString(), TransactionListInfoResponse.class).unconfirmedTransactions;
    }

    public AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account) throws IOException {
        addParameters(RequestType.requestType,RequestType.getUnconfirmedTransactionIds);
        addParameters(Parameters.account,account);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string().toString(), AccountTransactionIdsResponse.class);
    }

    public BalanceDTO getGuaranteedBalance(String account, int confirmations) throws IOException {
        addParameters(RequestType.requestType,RequestType.getGuaranteedBalance);
        addParameters(Parameters.account, account);
        addParameters(Parameters.numberOfConfirmations, confirmations);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string().toString(), BalanceDTO.class);
    }

    public BalanceDTO getBalance(String account) throws IOException {
        addParameters(RequestType.requestType, getBalance);
        addParameters(Parameters.account, account);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), BalanceDTO.class);
    }


    private CreateTransactionResponse sendMoney(String recipient, int moneyAmount) throws IOException {
      addParameters(RequestType.requestType,RequestType.sendMoney);
      addParameters(Parameters.recipient, recipient);
      addParameters(Parameters.amountATM, moneyAmount+"00000000");
      addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
      addParameters(Parameters.feeATM, "500000000");
      addParameters(Parameters.deadline, 1440);
      Response response = httpCallPost();
      assertEquals(200, response.code());
      return   mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);
    }


}
