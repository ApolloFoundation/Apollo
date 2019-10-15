package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.http.get.GetAccountBlockCount;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.*;
import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.getInstanse;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestBase {
    public static final Logger log = LoggerFactory.getLogger(TestBase.class);
    public static TestInfo testInfo;
    private static RetryPolicy retryPolicy;

    @BeforeAll
    static void initAll() {
        getTestConfiguration();
        retryPolicy = new RetryPolicy()
                .retryWhen(false)
                .withMaxRetries(20)
                .withDelay(1, TimeUnit.SECONDS);
        try {
            getTestConfiguration()
                    .getVaultWallet()
                    .setPass(importKey(getTestConfiguration()
                    .getVaultWallet()).passphrase);
        }catch (Exception e){}
    }




    @BeforeEach
    void setUP(TestInfo testInfo) {
        TestBase.testInfo = testInfo;
    }



    public boolean verifyTransactionInBlock(String transaction)
    {
        boolean inBlock = false;
        try {
            inBlock = Failsafe.with(retryPolicy).get(() -> getTransaction(transaction).confirmations>=0);
            assertTrue(inBlock);
        }
        catch (Exception e)
        {
            assertTrue(inBlock,"Transaction does't add to block. Transaction "+transaction);
        }
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
        return getInstanse(AccountPropertiesResponse.class);
    }

    public SearchAccountsResponse  searchAccounts(String searchQuery) throws IOException {
        addParameters(RequestType.requestType,RequestType.searchAccounts);
        addParameters(Parameters.query, searchQuery);
        return  getInstanse(SearchAccountsResponse.class);
    }

    public TransactionListInfoResponse getUnconfirmedTransactions(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getUnconfirmedTransactions);
        addParameters(Parameters.wallet,wallet);
        return getInstanse(TransactionListInfoResponse.class);
    }

    public AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account) throws IOException {
        addParameters(RequestType.requestType,RequestType.getUnconfirmedTransactionIds);
        addParameters(Parameters.account,account);
        return getInstanse(AccountTransactionIdsResponse.class);
    }

    public BalanceDTO getGuaranteedBalance(String account, int confirmations) throws IOException {
        addParameters(RequestType.requestType,RequestType.getGuaranteedBalance);
        addParameters(Parameters.account, account);
        addParameters(Parameters.numberOfConfirmations, confirmations);
        return getInstanse(BalanceDTO.class);
    }

    public BalanceDTO getBalance(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getBalance);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(BalanceDTO.class);
    }

    public EntryDTO getAccountLedgerEntry(String ledgerId) throws IOException {
        addParameters(RequestType.requestType, getAccountLedgerEntry);
        addParameters(Parameters.ledgerId,ledgerId);
        return getInstanse(EntryDTO.class);
    }


    public CreateTransactionResponse sendMoney(Wallet wallet,String recipient, int moneyAmount) throws IOException {
        addParameters(RequestType.requestType,RequestType.sendMoney);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount+"00000000");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }


    public AccountDTO getAccountPublicKey (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAccountPublicKey);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountDTO.class);
    }

    public BlockchainTransactionsResponse getAccountTransaction (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getBlockchainTransactions);
        addParameters(Parameters.wallet, wallet);
        return  getInstanse(BlockchainTransactionsResponse.class);
    }

    public CreateTransactionResponse  setAccountInfo(Wallet wallet,String accountName,String accountDescription) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAccountInfo);
        addParameters(Parameters.name, accountName);
        addParameters(Parameters.description, accountDescription);
        addParameters(Parameters.wallet, wallet);
       // addParameters(Parameters.recipient, accountID);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 300000000);
        return   getInstanse(CreateTransactionResponse.class);
    }

    public CreateTransactionResponse  setAccountProperty(Wallet wallet, String property ) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAccountProperty);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.recipient, wallet.getUser());
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 100000000);
        return   getInstanse(CreateTransactionResponse.class);
    }


    public CreateTransactionResponse  deleteAccountProperty(Wallet wallet,String property) throws IOException {
        addParameters(RequestType.requestType,RequestType.deleteAccountProperty);
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        return   getInstanse(CreateTransactionResponse.class);
    }

    public GetPropertyResponse  getAccountProperty(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAccountProperties);
        addParameters(Parameters.recipient, wallet.getUser());
        return  getInstanse(GetPropertyResponse.class);
    }

    //Skrypchenko Serhii
    public GetAliasesResponse getAliases  (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliases);
        addParameters(Parameters.wallet, wallet);
        return  getInstanse(GetAliasesResponse.class);
    }

    //Skrypchenko Serhii
    public GetCountAliasesResponse getAliasCount(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliasCount);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetCountAliasesResponse.class);
    }

    //Skrypchenko Serhii
    public AliasDTO getAlias(String aliasname) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAlias);
        addParameters(Parameters.aliasName, aliasname);
        return  getInstanse(AliasDTO.class);
    }


    //Skrypchenko Serhii
    public  CreateTransactionResponse setAlias (Wallet wallet,String aliasURL, String aliasName, Integer feeATM, Integer deadline) throws IOException {
        addParameters(RequestType.requestType,RequestType.setAlias);
        addParameters(Parameters.aliasURI, aliasURL);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, feeATM);
        addParameters(Parameters.deadline, deadline);
        return getInstanse(CreateTransactionResponse.class);

    }

    //Skrypchenko Serhii
    public CreateTransactionResponse deleteAlias(Wallet wallet, String aliasname) throws IOException {
        addParameters(RequestType.requestType,RequestType.deleteAlias);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.aliasName, aliasname);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    //Serhii Skrypchenko
    public GetAliasesResponse getAliasesLike(String aliasename) throws IOException {
        addParameters(RequestType.requestType,RequestType.getAliasesLike);
        addParameters(Parameters.aliasPrefix, aliasename);
        return getInstanse(GetAliasesResponse.class);
    }


    //Serhii Skrypchenko (sell Alias)
    public CreateTransactionResponse sellAlias (Wallet wallet,String aliasName) throws IOException {
        addParameters(RequestType.requestType, RequestType.sellAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.priceATM, 1500000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    public CreateTransactionResponse buyAlias (Wallet wallet,String aliasName) throws IOException {
        addParameters(RequestType.requestType, RequestType.buyAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.amountATM, 1500000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }
  
    public CreateTransactionResponse sendMoneyPrivate(Wallet wallet,String recipient, int moneyAmount) throws IOException {
        addParameters(RequestType.requestType,RequestType.sendMoneyPrivate);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount+"00000000");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    public AccountDTO generateNewAccount() throws IOException {
        addParameters(RequestType.requestType,RequestType.generateAccount);
        return getInstanse(AccountDTO.class);
    }


     public static Account2FA deleteKey(Wallet wallet) throws IOException {
         addParameters(RequestType.requestType,RequestType.deleteKey);
         addParameters(Parameters.wallet, wallet);
         return mapper.readValue(httpCallPost().body().string(), Account2FA.class);
     }

    public Account2FA exportKey(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.exportKey);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(Account2FA.class);
    }

    public static Account2FA importKey(Wallet wallet) {
        addParameters(RequestType.requestType, importKey);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(Account2FA.class);
    }


    public AccountDTO enable2FA(Wallet wallet) throws IOException {
        addParameters(RequestType.requestType,RequestType.enable2FA);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountDTO.class);
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
        return getInstanse(Peer.class);
    }

    public Peer addPeer(String ip) throws IOException {
        addParameters(RequestType.requestType, RequestType.addPeer);
        addParameters(Parameters.peer, ip);
        addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
        return getInstanse(Peer.class);
    }

    public PeerInfo getMyInfo() throws IOException {
        addParameters(RequestType.requestType, RequestType.getMyInfo);
        return getInstanse(PeerInfo.class);
    }

    public BlockDTO getBlock(String block) throws IOException {
        addParameters(RequestType.requestType, getBlock);
        addParameters(Parameters.block, block);
        addParameters(Parameters.includeTransactions, true);
        return getInstanse(BlockDTO.class);

    }

    public GetBlockIdResponse getBlockId(String height) throws IOException {
        addParameters(RequestType.requestType, getBlockId);
        addParameters(Parameters.height, height);
        return getInstanse(GetBlockIdResponse.class);

    }

    public BlockchainInfoDTO getBlockchainStatus() throws IOException {
        addParameters(RequestType.requestType, getBlockchainStatus);
        return getInstanse(BlockchainInfoDTO.class);
    }

    public GetBloksResponse getBlocks() throws IOException {
        addParameters(RequestType.requestType, getBlocks);
        return getInstanse(GetBloksResponse.class);
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
        return getInstanse(CreateTransactionResponse.class);

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
        return getInstanse(CreateTransactionResponse.class);

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
        return getInstanse(CreateTransactionResponse.class);

    }



    //cancelBidOrder
    public  CreateTransactionResponse cancelBidOrder (Wallet wallet,String bidOrder) throws IOException {
        addParameters(RequestType.requestType, cancelBidOrder);
        addParameters(Parameters.order, bidOrder);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    //cancelAskOrder
    public  CreateTransactionResponse cancelAskOrder (Wallet wallet,String askOrder) throws IOException {
        addParameters(RequestType.requestType, cancelAskOrder);
        addParameters(Parameters.order, askOrder);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    //deleteAssetShares
    public  CreateTransactionResponse deleteAssetShares (Wallet wallet,String assetID, String quantityATU) throws IOException {
        addParameters(RequestType.requestType, deleteAssetShares);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    //dividendPayment
    public  CreateTransactionResponse dividendPayment (Wallet wallet,String assetID, Integer amountATMPerATU, Integer height) throws IOException {
        addParameters(RequestType.requestType, dividendPayment);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.amountATMPerATU, amountATMPerATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.height, height);
        return getInstanse(CreateTransactionResponse.class);
    }


    //getAccountAssets
    public  GetAccountAssetsResponse getAccountAssets (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountAssets);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetAccountAssetsResponse.class);
    }

    //getAccountAssetCount
    public  GetAssetAccountCountResponse getAccountAssetCount (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountAssetCount);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetAssetAccountCountResponse.class);
    }

    //getAsset
    public AssetDTO getAsset (String asset) throws IOException {
        addParameters(RequestType.requestType, getAsset);
        addParameters(Parameters.asset, asset);
        return getInstanse(AssetDTO.class);
    }


    //getAccountCurrentAskOrderIds
    public GetOrderIdsResponse getAccountCurrentAskOrderIds (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountCurrentAskOrderIds);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetOrderIdsResponse.class);
    }

    //getAccountCurrentBidOrderIds
    public GetOrderIdsResponse getAccountCurrentBidOrderIds (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountCurrentBidOrderIds);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetOrderIdsResponse.class);
    }


    //getAccountCurrentAskOrders
    public GetAccountCurrentOrdersResponse getAccountCurrentAskOrders (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountCurrentAskOrders);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetAccountCurrentOrdersResponse.class);
    }

    //getAccountCurrentBidOrders
    public GetAccountCurrentOrdersResponse getAccountCurrentBidOrders (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAccountCurrentBidOrders);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetAccountCurrentOrdersResponse.class);
    }

    //getAllAssets
    public GetAllAssetsResponse getAllAssets () throws IOException {
        addParameters(RequestType.requestType, getAllAssets);
        return getInstanse(GetAllAssetsResponse.class);
    }

    //getAllOpenAskOrders
    public GetOpenOrderResponse getAllOpenAskOrders () throws IOException {
        addParameters(RequestType.requestType, getAllOpenAskOrders);
        return getInstanse(GetOpenOrderResponse.class);
    }


    //getAllOpenBidOrders
    public GetOpenOrderResponse getAllOpenBidOrders () throws IOException {
        addParameters(RequestType.requestType, getAllOpenBidOrders);
        return getInstanse(GetOpenOrderResponse.class);
    }

    //getAllTrades
    public GetAllTradeResponse getAllTrades () throws IOException {
        addParameters(RequestType.requestType, getAllTrades);
        return getInstanse(GetAllTradeResponse.class);
    }

    //getAskOrder
    public OrderDTO getAskOrder (String askOrder) throws IOException {
        addParameters(RequestType.requestType, getAskOrder);
        addParameters(Parameters.order, askOrder);
        return getInstanse(OrderDTO.class);
    }

    //getAskOrderIds
    public GetOrderIdsResponse getAskOrderIds (String assetID) throws IOException {
        addParameters(RequestType.requestType, getAskOrderIds);
        addParameters(Parameters.asset, assetID);
        return getInstanse(GetOrderIdsResponse.class);
    }

    //getAskOrders
    public GetOpenOrderResponse getAskOrders (String assetID) throws IOException {
        addParameters(RequestType.requestType, getAskOrders);
        addParameters(Parameters.asset, assetID);

        return getInstanse(GetOpenOrderResponse.class);
    }

    //getAssetAccountCount
    public  GetAssetAccountCountResponse getAssetAccountCount (String assetID) throws IOException {
        addParameters(RequestType.requestType, getAssetAccountCount);
        addParameters(Parameters.asset, assetID);
        return getInstanse(GetAssetAccountCountResponse.class);
    }

    //getAssetAccounts
    public  GetAccountAssetsResponse getAssetAccounts (String assetID) throws IOException {
        addParameters(RequestType.requestType, getAssetAccounts);
        addParameters(Parameters.asset, assetID);
        return getInstanse(GetAccountAssetsResponse.class);
    }

    //getAssetDeletes
    public  GetExpectedAssetDeletes getAssetDeletes (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAssetDeletes);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetExpectedAssetDeletes.class);
    }

    //getExpectedAssetDeletes
    public  GetExpectedAssetDeletes getExpectedAssetDeletes (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getExpectedAssetDeletes);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetExpectedAssetDeletes.class);
    }

    //getAssetDividends NOT READY YET!!!!!
    /*public  GetAssetDividends getAssetDividends (Wallet wallet) throws IOException {
        addParameters(RequestType.requestType, getAssetDividends);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetAssetDividends.class);
    }*/

    //getAssetIds
    public  GetAssetIdsResponse getAssetIds () throws IOException {
        addParameters(RequestType.requestType, getAssetIds);
        return getInstanse(GetAssetIdsResponse.class);
    }

    //transferAsset
    public  CreateTransactionResponse transferAsset (Wallet wallet, String asset, Integer quantityATU, String recipient) throws IOException {
        addParameters(RequestType.requestType, transferAsset);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.asset, asset);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }




    public ECBlock getECBlock() throws IOException {
        addParameters(RequestType.requestType, getECBlock);
        return getInstanse(ECBlock.class);
    }

    public GetForgingResponse getForging() throws IOException{
        addParameters(RequestType.requestType, getForging);
        addParameters(Parameters.adminPassword,  getTestConfiguration().getAdminPass());
        return getInstanse(GetForgingResponse.class);
    }

    public ForgingDetails startForging(Wallet wallet) throws IOException{
        addParameters(RequestType.requestType, startForging);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.adminPassword,  getTestConfiguration().getAdminPass());
        return getInstanse(ForgingDetails.class);
    }
    public ForgingDetails stopForging(Wallet wallet) throws IOException{
        addParameters(RequestType.requestType, stopForging);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.adminPassword,  getTestConfiguration().getAdminPass());
        return getInstanse(ForgingDetails.class);
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
        return getInstanse(PrunableMessageDTO.class);
    }

    /*
     phasingQuorum is the number of "votes" needed for transaction approval
    (required if phasingVotingModel >= 0, default 0):
     0 for voting model -1
     the number of accounts for model 0
     total ATM for model 1
     total ATU for models 2 and 3
     the number of transactions for model 4  1 for model 5
    */
    public void phasing(long phasingFinishHeight, VoteWeighting.VotingModel votingModel, int phasingQuorum,Long phasingMinBalance, Long phasingMinBalanceModel, String phasingHolding){
        addParameters(Parameters.phased, true);
        addParameters(Parameters.phasingFinishHeight, phasingFinishHeight);
        addParameters(Parameters.votingModel, votingModel);
        addParameters(Parameters.phasingQuorum, phasingQuorum);
        addParameters(Parameters.phasingMinBalance, phasingMinBalance);
        addParameters(Parameters.phasingMinBalanceModel, phasingMinBalanceModel);
        addParameters(Parameters.phasingHolding, phasingHolding);
    }

    @AfterEach
    void testEnd() {
        TestBase.testInfo = null;
    }

    @AfterAll
    static void afterAll() {
        try {
        //    deleteKey(getTestConfiguration().getVaultWallet());
        } catch (Exception e) { }

    }


}
