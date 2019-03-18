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
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.api.dto.RequestType.*;
import static com.apollocurrency.aplwallet.api.dto.RequestType.getBalance;
import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.*;
import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.getInstanse;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestBase {
    public static final Logger log = LoggerFactory.getLogger(TestAccounts.class);
    public TestConfiguration testConfiguration = TestConfiguration.getTestConfiguration();;
    public static ObjectMapper mapper = new ObjectMapper();
    RetryPolicy retryPolicy;

    public TestBase()  {
        retryPolicy = new RetryPolicy()
                .retryWhen(false)
                .withMaxRetries(10)
                .withDelay(5, TimeUnit.SECONDS);
        try {
            deleteKey(testConfiguration.getVaultWallet());
        } catch (IOException e) {
        }
        try {
            testConfiguration.getVaultWallet().setPass(importKey(testConfiguration.getVaultWallet()).passphrase);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


    public boolean verifyTransactionInBlock(String transaction)
    {
        boolean inBlock = Failsafe.with(retryPolicy)
                .get(() -> getTransaction(transaction)
                        .confirmations
                        .compareTo(new Long("0"))==1);
        assertTrue(inBlock);
        return inBlock;
    }

    public TransactionDTO getTransaction(String transaction) throws IOException {
        addParameters(RequestType.requestType, RequestType.getTransaction);
        addParameters(Parameters.transaction, transaction);
        return getInstanse(TransactionDTO.class);
    }

    public BlockListInfoResponse getAccountBlocks(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountBlocks);
        addParameters(Parameters.account, account);
        return getInstanse(BlockListInfoResponse.class);
    }


    public GetAccountResponse getAccount(String account) throws IOException {
        addParameters(RequestType.requestType, getAccount);
        addParameters(Parameters.account, account);
        return getInstanse(GetAccountResponse.class);
    }


    public GetAccountBlockCount getAccountBlockCount(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountBlockCount);
        addParameters(Parameters.account, account);
        return getInstanse(GetAccountBlockCount.class);
    }

    public AccountBlockIdsResponse getAccountBlockIds(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountBlockIds);
        addParameters(Parameters.account, account);
        return getInstanse(AccountBlockIdsResponse.class);
    }

    public AccountDTO getAccountId(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountId);
        addParameters(Parameters.wallet,wallet);
        return getInstanse(AccountDTO.class);
    }

    public AccountLedgerResponse getAccountLedger(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountLedger);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountLedgerResponse.class);
    }

    public AccountPropertiesResponse getAccountProperties(String account) throws IOException {
        addParameters(RequestType.requestType, getAccountProperties);
        addParameters(Parameters.recipient, account);
        return  getInstanse(AccountPropertiesResponse.class);
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


    public CreateTransactionResponse sendMoney(Wallet wallet,String recipient, int moneyAmount) throws IOException {
        addParameters(RequestType.requestType,RequestType.sendMoney);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount+"00000000");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string(), CreateTransactionResponse.class);
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
        assertEquals(200, response.code());
        return  mapper.readValue(response.body().string().toString(), BlockchainTransactionsResponse.class);
    }

    public CreateTransactionResponse  setAccountInfo(Wallet wallet,String accountName,String accountDescription) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAccountInfo);
        addParameters(Parameters.name, accountName);
        addParameters(Parameters.description, accountDescription);
        addParameters(Parameters.wallet, wallet);
       // addParameters(Parameters.recipient, accountID);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 300000000);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);
    }

    public CreateTransactionResponse  setAccountProperty(Wallet wallet, String property ) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAccountProperty);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.recipient, wallet.getUser());
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 100000000);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);
    }


    public CreateTransactionResponse  deleteAccountProperty(Wallet wallet,String property) throws IOException {
        addParameters(RequestType.requestType,RequestType.deleteAccountProperty);
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);
    }

    public GetPropertyResponse  getAccountProperty(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAccountProperties);
        addParameters(Parameters.recipient, wallet.getUser());
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), GetPropertyResponse.class);
    }

    //Skrypchenko Serhii
    public GetAliasesResponse getAliases  (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliases);
        addParameters(Parameters.wallet, wallet);

        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string(), GetAliasesResponse.class);
    }

    //Skrypchenko Serhii
    public GetCountAliasesResponse getAliasCount(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliasCount);
        addParameters(Parameters.wallet, wallet);

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
    public  CreateTransactionResponse setAlias (Wallet wallet,String aliasURL, String aliasName, Integer feeATM, Integer deadline) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAlias);
        addParameters(Parameters.aliasURI, aliasURL);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, feeATM);
        addParameters(Parameters.deadline, deadline);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }

    //Skrypchenko Serhii
    public CreateTransactionResponse deleteAlias(Wallet wallet, String aliasname) throws IOException {
        addParameters(RequestType.requestType,RequestType.deleteAlias);
        addParameters(Parameters.wallet, wallet);

        addParameters(Parameters.aliasName, aliasname);
        addParameters(Parameters.feeATM, 400000000);
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }

    //Serhii Skrypchenko
    public GetAliasesResponse getAliasesLike(String aliasename) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliasesLike);
        //addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.aliasPrefix, aliasename);
        //addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAliasesResponse.class);
        //GetAliasesResponse getAliasesResponse = gson.fromJson(response.body().string().toString(), GetAliasesResponse.class);
        //assertEquals(testConfiguration.getTestUser(), getAliasesResponse.getAliases()[0].getAccountRS());
    }


    //Serhii Skrypchenko (sell Alias)
    public CreateTransactionResponse sellAlias (Wallet wallet,String aliasName) throws IOException {
        addParameters(RequestType.requestType, RequestType.sellAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);

        addParameters(Parameters.feeATM, 500000000);
        addParameters(Parameters.priceATM, 1500000000);
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());


        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }

    public CreateTransactionResponse buyAlias (Wallet wallet,String aliasName) throws IOException {
        addParameters(RequestType.requestType, RequestType.buyAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);

        addParameters(Parameters.feeATM, 500000000);
        addParameters(Parameters.amountATM, 1500000000);
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());


        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }
  
    public CreateTransactionResponse sendMoneyPrivate(Wallet wallet,String recipient, int moneyAmount) throws IOException {
        addParameters(RequestType.requestType,RequestType.sendMoneyPrivate);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount+"00000000");
        addParameters(Parameters.wallet, wallet);
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


     public Account2FA deleteKey(Wallet wallet) throws IOException {
         addParameters(RequestType.requestType,RequestType.deleteKey);
         addParameters(Parameters.wallet, wallet);
         Response response = httpCallPost();
         assertEquals(200, response.code());
         return   mapper.readValue(response.body().string(), Account2FA.class);
     }

    public Account2FA exportKey(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.exportKey);
        addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string(), Account2FA.class);
    }

    public Account2FA importKey(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, importKey);
        addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string(), Account2FA.class);
    }


    public AccountDTO enable2FA(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.enable2FA);
        addParameters(Parameters.wallet, wallet);
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
        assertNotNull(transaction);
        assertNotNull(transaction.transaction,transaction.errorDescription);
        assertNotNull(transaction.transactionJSON,transaction.errorDescription);
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
    public  CreateTransactionResponse issueAsset (Wallet wallet,String assetName, String description, Integer quantityATU) throws IOException {
        addParameters(RequestType.requestType, issueAsset);
        addParameters(Parameters.name, assetName);
        addParameters(Parameters.description, description);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }

    //placeBidOrder
    public  CreateTransactionResponse placeBidOrder (Wallet wallet,String assetID, String priceATM, Integer quantityATU) throws IOException {
        addParameters(RequestType.requestType, placeBidOrder);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.priceATM, priceATM);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000");
        addParameters(Parameters.deadline, 1400);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }
    //placeAskOrder
    public  CreateTransactionResponse placeAskOrder (Wallet wallet,String assetID, String priceATM, Integer quantityATU) throws IOException {
        addParameters(RequestType.requestType, placeAskOrder);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.priceATM, priceATM);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000");
        addParameters(Parameters.deadline, 1400);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }



    //cancelBidOrder
    public  CreateTransactionResponse cancelBidOrder (Wallet wallet,String bidOrder) throws IOException {
        addParameters(RequestType.requestType, cancelBidOrder);
        addParameters(Parameters.order, bidOrder);
        //addParameters(Parameters.description, description);
        addParameters(Parameters.wallet, wallet);
        //addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);

    }

    //cancelAskOrder
    public  CreateTransactionResponse cancelAskOrder (Wallet wallet,String askOrder) throws IOException {
        addParameters(RequestType.requestType, cancelAskOrder);
        addParameters(Parameters.order, askOrder);
        //addParameters(Parameters.description, description);
        addParameters(Parameters.wallet, wallet);
        //addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }

    //deleteAssetShares
    public  CreateTransactionResponse deleteAssetShares (Wallet wallet,String assetID, String quantityATU) throws IOException {
        addParameters(RequestType.requestType, deleteAssetShares);
        addParameters(Parameters.asset, assetID);
        //addParameters(Parameters.description, description);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), CreateTransactionResponse.class);
    }

    //dividendPayment
    public  CreateTransactionResponse dividendPayment (Wallet wallet,String assetID, Integer amountATMPerATU, Integer height) throws IOException {
        addParameters(RequestType.requestType, dividendPayment);
        addParameters(Parameters.asset, assetID);
        //addParameters(Parameters.description, description);
        addParameters(Parameters.wallet, wallet);
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
    public  GetAccountAssetsResponse getAccountAssets (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountAssets);
        addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAccountAssetsResponse.class);
    }

    //getAccountAssetCount
    public  GetAssetAccountCountResponse getAccountAssetCount (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountAssetCount);
        addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
       // System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAssetAccountCountResponse.class);
    }

    //getAsset
    public AssetDTO getAsset (Wallet wallet, String asset) throws IOException {
        addParameters(RequestType.requestType, getAsset);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.asset, asset);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), AssetDTO.class);
    }


    //getAccountCurrentAskOrderIds
    public GetOrderIdsResponse getAccountCurrentAskOrderIds (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountCurrentAskOrderIds);
        addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetOrderIdsResponse.class);
    }

    //getAccountCurrentBidOrderIds
    public GetOrderIdsResponse getAccountCurrentBidOrderIds (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountCurrentBidOrderIds);
        addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetOrderIdsResponse.class);
    }


    //getAccountCurrentAskOrders
    public GetAccountCurrentOrdersResponse getAccountCurrentAskOrders (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountCurrentAskOrders);
        addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAccountCurrentOrdersResponse.class);
    }

    //getAccountCurrentBidOrders
    public GetAccountCurrentOrdersResponse getAccountCurrentBidOrders (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountCurrentBidOrders);
        addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAccountCurrentOrdersResponse.class);
    }

    //getAllAssets
    public GetAllAssetsResponse getAllAssets (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAllAssets);
        //addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAllAssetsResponse.class);
    }

    //getAllOpenAskOrders
    public GetOpenOrderResponse getAllOpenAskOrders (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAllOpenAskOrders);
        //addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetOpenOrderResponse.class);
    }


    //getAllOpenBidOrders
    public GetOpenOrderResponse getAllOpenBidOrders (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAllOpenBidOrders);
        //addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetOpenOrderResponse.class);
    }

    //getAllTrades
    public GetAllTradeResponse getAllTrades (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAllTrades);
        //addParameters(Parameters.wallet, wallet);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetAllTradeResponse.class);
    }

    //getAskOrder
    public OrderDTO getAskOrder (Wallet wallet, String askOrder) throws IOException {
        addParameters(RequestType.requestType, getAskOrder);

        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.order, askOrder);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), OrderDTO.class);
    }

    //getAskOrderIds
    public GetOrderIdsResponse getAskOrderIds (Wallet wallet, String assetID) throws IOException {
        addParameters(RequestType.requestType, getAskOrderIds);

        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.asset, assetID);
        Response response = httpCallPost();
        //System.out.println(response.body().string());
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), GetOrderIdsResponse.class);
    }








    public ECBlock getECBlock() throws IOException {
        addParameters(RequestType.requestType, getECBlock);
        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), ECBlock.class);
    }

    public GetForgingResponse getForging(Wallet wallet) throws IOException{
        addParameters(RequestType.requestType, getForging);
        addParameters(Parameters.adminPassword, testConfiguration.getAdminPass());
        Response response = httpCallPost();
        return mapper.readValue(response.body().string(), GetForgingResponse.class);
    }

    public ForgingDetails startForging(Wallet wallet) throws IOException{
        addParameters(RequestType.requestType, startForging);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.adminPassword, testConfiguration.getAdminPass());
        Response response = httpCallPost();
        return mapper.readValue(response.body().string(), ForgingDetails.class);
    }
    public ForgingDetails stopForging(Wallet wallet) throws IOException{
        addParameters(RequestType.requestType, stopForging);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.adminPassword, testConfiguration.getAdminPass());
        Response response = httpCallPost();
        return mapper.readValue(response.body().string(), ForgingDetails.class);
    }

    public CreateTransactionResponse sendMessage(Wallet wallet,String recipient, String testMessage) { ;
        addParameters(RequestType.requestType,RequestType.sendMessage);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.message, testMessage);
        addParameters(Parameters.feeATM, 500000000);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.messageIsPrunable, true);
        return getInstanse(CreateTransactionResponse.class);
    }

    public PrunableMessageDTO readMessage(Wallet wallet,String transaction) throws IOException {
        addParameters(RequestType.requestType,RequestType.readMessage);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.transaction,transaction);
        Response response = httpCallPost();
        Assert.assertEquals(200, response.code());
        return mapper.readValue(response.body().string(), PrunableMessageDTO.class);
    }


}
