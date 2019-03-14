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
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestBase {
    public static final Logger log = LoggerFactory.getLogger(TestAccounts.class);
    public TestConfiguration testConfiguration = TestConfiguration.getTestConfiguration();;
    public static ObjectMapper mapper = new ObjectMapper();
    RetryPolicy retryPolicy;

    public TestBase() {
        retryPolicy = new RetryPolicy()
                .retryWhen(false)
                .withMaxRetries(10)
                .withDelay(5, TimeUnit.SECONDS);
    }

    public boolean verifyTransactionInBlock(String transaction)
    {
     return Failsafe.with(retryPolicy).get(() -> getTransaction(transaction).confirmations.compareTo(new Long(0))==1);
    }

    public TransactionDTO getTransaction(String transaction) throws IOException {
        addParameters(RequestType.requestType, RequestType.getTransaction);
        addParameters(Parameters.transaction, transaction);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string().toString(), TransactionDTO.class);
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
        //System.out.println(response.body().string());
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
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 300000000);
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


    public CreateTransactionResponse  deleteAccountProperty(String pass,String property) throws IOException {
        addParameters(RequestType.requestType,RequestType.deleteAccountProperty);
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.secretPhrase, pass);
        addParameters(Parameters.feeATM, 100000000);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);
    }

    public GetPropertyResponse  getAccountProperty(String accountID) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAccountProperties);
        addParameters(Parameters.recipient, accountID);
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
  
    public CreateTransactionResponse sendMoneyPrivate(String recipient, int moneyAmount) throws IOException {
        addParameters(RequestType.requestType,RequestType.sendMoneyPrivate);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount+"00000000");
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }

    public AccountDTO generateNewAccount() throws IOException {
        addParameters(RequestType.requestType,RequestType.generateAccount);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        System.out.println(response.body());
        return   mapper.readValue(response.body().string(), AccountDTO.class);
    }


     public Account2FA deleteKey(String accountID,String pass) throws IOException {
         addParameters(RequestType.requestType,RequestType.deleteKey);
         addParameters(Parameters.account, accountID);
         addParameters(Parameters.passphrase,pass);
         Response response = httpCallPost();
         assertEquals(200, response.code());
         return   mapper.readValue(response.body().string(), Account2FA.class);

     }

    public AccountDTO enable2FA(String accountID,String pass) throws IOException {
        addParameters(RequestType.requestType,RequestType.enable2FA);
        addParameters(Parameters.account, accountID);
        addParameters(Parameters.passphrase,pass);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string(), AccountDTO.class);
    }


    public String[] getPeers() throws IOException {
        addParameters(RequestType.requestType, RequestType.getPeers);
        addParameters(Parameters.active, true);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        Peers peers = mapper.readValue(response.body().string(), Peers.class);
        return peers.peers;
    }

    public Peer getPeer(String peer) throws IOException {
        addParameters(RequestType.requestType, RequestType.getPeer);
        addParameters(Parameters.peer, peer);
        Response response = httpCallGet();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), Peer.class);
    }

    public Peer addPeer(String ip) throws IOException {
        addParameters(RequestType.requestType, RequestType.addPeer);
        addParameters(Parameters.peer, ip);
        addParameters(Parameters.adminPassword, testConfiguration.getAdminPass());
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), Peer.class);
    }

    public PeerInfo getMyInfo() throws IOException {
        addParameters(RequestType.requestType, RequestType.getMyInfo);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), PeerInfo.class);
    }

    public BlockDTO getBlock(String block) throws IOException {
        addParameters(RequestType.requestType, getBlock);
        addParameters(Parameters.block, block);
        addParameters(Parameters.includeTransactions, true);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), BlockDTO.class);

    }

    public GetBlockIdResponse getBlockId(String height) throws IOException {
        addParameters(RequestType.requestType, getBlockId);
        addParameters(Parameters.height, height);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetBlockIdResponse.class);

    }

    public BlockchainInfoDTO getBlockchainStatus() throws IOException {
        addParameters(RequestType.requestType, getBlockchainStatus);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), BlockchainInfoDTO.class);
    }

    public GetBloksResponse getBlocks() throws IOException {
        addParameters(RequestType.requestType, getBlocks);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetBloksResponse.class);
    }



    public void verifyCreatingTransaction (CreateTransactionResponse transaction) {
        assertNotNull(transaction.transactionJSON.senderPublicKey);
        assertNotNull(transaction.transactionJSON.signature);
        assertNotNull(transaction.transactionJSON.fullHash);
        assertNotNull(transaction.transactionJSON.amountATM);
        assertNotNull(transaction.transactionJSON.ecBlockId);
        assertNotNull(transaction.transactionJSON.senderRS);
        assertNotNull(transaction.transactionJSON.transaction);
        assertNotNull(transaction.transactionJSON.feeATM);
        assertNotNull(transaction.transactionJSON.type);

    }

    //AssetExchange
    //issueAsset
    public  CreateTransactionResponse issueAsset (String assetName, String description, Integer quantityATU) throws IOException {
        addParameters(RequestType.requestType, issueAsset);
        addParameters(Parameters.name, assetName);
        addParameters(Parameters.description, description);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 60);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }

    //placeBidOrder
    public  CreateTransactionResponse placeBidOrder (String assetID, String priceATM, Integer quantityATU) throws IOException {
        addParameters(RequestType.requestType, placeBidOrder);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.priceATM, priceATM);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000");
        addParameters(Parameters.deadline, 60);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }
    //placeAskOrder
    public  CreateTransactionResponse placeAskOrder (String assetID, String priceATM, Integer quantityATU) throws IOException {
        addParameters(RequestType.requestType, placeAskOrder);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.priceATM, priceATM);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000");
        addParameters(Parameters.deadline, 60);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }



    //cancelBidOrder
    public  CreateTransactionResponse cancelBidOrder (String bidOrder) throws IOException {
        addParameters(RequestType.requestType, cancelBidOrder);
        addParameters(Parameters.order, bidOrder);
        //addParameters(Parameters.description, description);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        //addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 60);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }

    //cancelAskOrder
    public  CreateTransactionResponse cancelAskOrder (String askOrder) throws IOException {
        addParameters(RequestType.requestType, cancelAskOrder);
        addParameters(Parameters.order, askOrder);
        //addParameters(Parameters.description, description);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        //addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }

    //deleteAssetShares
    public  CreateTransactionResponse deleteAssetShares (String assetID, String quantityATU) throws IOException {
        addParameters(RequestType.requestType, deleteAssetShares);
        addParameters(Parameters.asset, assetID);
        //addParameters(Parameters.description, description);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }

    //dividendPayment
    public  CreateTransactionResponse dividendPayment (String assetID, Integer amountATMPerATU, Integer height) throws IOException {
        addParameters(RequestType.requestType, dividendPayment);
        addParameters(Parameters.asset, assetID);
        //addParameters(Parameters.description, description);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        //addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.amountATMPerATU, amountATMPerATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.height, height);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }


    //getAccountAssets
    public  GetAccountAssetsResponse getAccountAssets (String accountID) throws IOException {
        addParameters(RequestType.requestType, getAccountAssets);
        addParameters(Parameters.account, accountID);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAccountAssetsResponse.class);
    }

    //getAccountAssetCount
    public  GetAssetAccountCountResponse getAccountAssetCount (String accountID) throws IOException {
        addParameters(RequestType.requestType, getAccountAssetCount);
        addParameters(Parameters.account, accountID);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAssetAccountCountResponse.class);
    }


    public ECBlock getECBlock() throws IOException {
        addParameters(RequestType.requestType, getECBlock);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), ECBlock.class);
    }


}
