package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.request.CreateTransactionRequestDTO;
import com.apollocurrency.aplwallet.api.response.*;
import com.apollocurrrency.aplwallet.inttest.tests.TestAccounts;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.api.dto.RequestType.*;
import static com.apollocurrency.aplwallet.api.dto.RequestType.getBalance;
import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.*;
import static org.junit.Assert.*;

public class TestBase {
    public static final Logger log = LoggerFactory.getLogger(TestAccounts.class);
    public TestConfiguration testConfiguration = TestConfiguration.getTestConfiguration();;
    public static ObjectMapper mapper = new ObjectMapper();

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

    public EntryDTO getAccountLedgerEntry(String ledgerId) throws IOException {
        addParameters(RequestType.requestType, getAccountLedgerEntry);
        addParameters(Parameters.ledgerId,ledgerId);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), EntryDTO.class);
    }


    public CreateTransactionResponse sendMoney(String recipient, int moneyAmount) throws IOException {
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


    public String getAccountPublicKey (String accountID) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAccountPublicKey);
        addParameters(Parameters.account, accountID);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), AccountDTO.class).publicKey;
    }

    public BlockchainTransactionsResponse getAccountTransaction (String accountID) throws IOException {
        addParameters(RequestType.requestType,RequestType.getBlockchainTransactions);
        addParameters(Parameters.account, accountID);
        Response response = httpCallGet();
       // System.out.println(response.body().string());
        assertEquals(200, response.code());
        return  mapper.readValue(response.body().string().toString(), BlockchainTransactionsResponse.class);
    }

    public CreateTransactionResponse  setAccountInfo(String accountID,String accountPass,String accountName,String accountDescription) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAccountInfo);
        addParameters(Parameters.name, accountName);
        addParameters(Parameters.description, accountDescription);
        addParameters(Parameters.secretPhrase, accountPass);
        addParameters(Parameters.recipient, accountID);
        addParameters(Parameters.deadline, 60);
        addParameters(Parameters.feeATM, 500000000);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);

    }

    public CreateTransactionResponse  setAccountProperty(String accountID, String pass, String property ) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAccountProperty);
        addParameters(Parameters.secretPhrase, pass);
        addParameters(Parameters.recipient, accountID);
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 100000000);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);
    }


    public GetPropertyResponse  getAccountProperty(String accountID) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAccountProperties);
        addParameters(Parameters.recipient, testConfiguration.getTestUser());
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), GetPropertyResponse.class);
    }


    //Skrypchenko Serhii
    public GetAliasesResponse getAliases  (String accountID) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliases);
        addParameters(Parameters.account, accountID);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), GetAliasesResponse.class);
    }

    //Skrypchenko Serhii
    public GetCountAliasesResponse getAliasCount(String accountID) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliasCount);
        addParameters(Parameters.account, accountID);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), GetCountAliasesResponse.class);
    }

    //Skrypchenko Serhii
    public AliasDTO getAlias(String aliasname) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAlias);
        //addParameters(Parameters.account, accountID);
        addParameters(Parameters.aliasName, aliasname);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), AliasDTO.class);
    }


    //Skrypchenko Serhii
    public  CreateTransactionResponse setAlias (String aliasURL, String aliasName, Integer feeATM, Integer deadline) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAlias);
        addParameters(Parameters.aliasURI, aliasURL);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.feeATM, feeATM);
        addParameters(Parameters.deadline, deadline);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }

    //Skrypchenko Serhii
    public CreateTransactionResponse deleteAlias(String aliasname) throws IOException {
        addParameters(RequestType.requestType,RequestType.deleteAlias);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.aliasName, aliasname);
        addParameters(Parameters.feeATM, 400000000);
        addParameters(Parameters.deadline, 60);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }

    //Serhii Skrypchenko
    public GetAliasesResponse getAliasesLike(String aliasename) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliasesLike);
        //addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.aliasPrefix, aliasename);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAliasesResponse.class);
        //GetAliasesResponse getAliasesResponse = gson.fromJson(response.body().string().toString(), GetAliasesResponse.class);
        //assertEquals(testConfiguration.getTestUser(), getAliasesResponse.getAliases()[0].getAccountRS());
    }


    //Serhii Skrypchenko (sell Alias)
    public CreateTransactionResponse sellAlias (String aliasName) throws IOException {
        addParameters(RequestType.requestType, RequestType.sellAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.feeATM, 500000000);
        addParameters(Parameters.priceATM, 1500000000);
        addParameters(Parameters.deadline, 60);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());


        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }

    public CreateTransactionResponse buyAlias (String aliasName) throws IOException {
        addParameters(RequestType.requestType, RequestType.buyAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.feeATM, 500000000);
        addParameters(Parameters.amountATM, 1500000000);
        addParameters(Parameters.deadline, 60);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());


        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }

}
