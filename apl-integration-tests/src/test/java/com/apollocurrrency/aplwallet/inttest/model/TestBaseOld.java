package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.AccountAliasDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetOrderDTO;
import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.AccountMessageDTO;
import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.dto.EntryDTO;
import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlockIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlocksResponse;
import com.apollocurrency.aplwallet.api.response.AccountCountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountLedgerResponse;
import com.apollocurrency.aplwallet.api.response.AccountOpenAssetOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountPropertiesResponse;
import com.apollocurrency.aplwallet.api.response.AccountTransactionIdsResponse;
import com.apollocurrency.aplwallet.api.response.AssetTradeResponse;
import com.apollocurrency.aplwallet.api.response.AssetsAccountsCountResponse;
import com.apollocurrency.aplwallet.api.response.AssetsResponse;
import com.apollocurrency.aplwallet.api.response.BlockListInfoResponse;
import com.apollocurrency.aplwallet.api.response.BlockchainTransactionsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.ExpectedAssetDeletes;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountBlockCountResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrency.aplwallet.api.response.GetBlockIdResponse;
import com.apollocurrency.aplwallet.api.response.GetPeersIpResponse;
import com.apollocurrency.aplwallet.api.response.SearchAccountsResponse;
import com.apollocurrency.aplwallet.api.response.TransactionListResponse;
import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import io.qameta.allure.Step;
import net.jodah.failsafe.Failsafe;
import okhttp3.Response;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.*;
import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.getInstanse;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestBaseOld extends TestBase {
    public static final Logger log = LoggerFactory.getLogger(TestBaseOld.class);

    @BeforeEach
    void setUP(TestInfo testInfo) {
        this.testInfo = testInfo;
    }


    public boolean verifyTransactionInBlock(String transaction)
    {
        boolean inBlock = false;
        try {
            inBlock = Failsafe.with(retryPolicy).get(() -> getTransaction(transaction).getConfirmations() >= 0);
            Assertions.assertTrue(inBlock);
        }
        catch (Exception e)
        {
            Assertions.assertTrue(inBlock,"Transaction does't add to block. Transaction "+transaction);
        }
        return inBlock;
    }

    public TransactionDTO getTransaction(String transaction) {
        addParameters(RequestType.requestType, RequestType.getTransaction);
        addParameters(Parameters.transaction, transaction);
        return getInstanse(TransactionDTO.class);
    }

    public BlockListInfoResponse getAccountBlocks(String account) {
        addParameters(RequestType.requestType, getAccountBlocks);
        addParameters(Parameters.account, account);
        return getInstanse(BlockListInfoResponse.class);
    }


    public GetAccountResponse getAccount(String account) {
        addParameters(RequestType.requestType, getAccount);
        addParameters(Parameters.account, account);
        return getInstanse(GetAccountResponse.class);
    }


    public GetAccountBlockCountResponse getAccountBlockCount(String account) {
        addParameters(RequestType.requestType, getAccountBlockCount);
        addParameters(Parameters.account, account);
        return getInstanse(GetAccountBlockCountResponse.class);
    }

    public AccountBlockIdsResponse getAccountBlockIds(String account) {
        addParameters(RequestType.requestType, getAccountBlockIds);
        addParameters(Parameters.account, account);
        return getInstanse(AccountBlockIdsResponse.class);
    }

    public AccountDTO getAccountId(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountId);
        addParameters(Parameters.wallet,wallet);
        return getInstanse(AccountDTO.class);
    }

    public AccountLedgerResponse getAccountLedger(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountLedger);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountLedgerResponse.class);
    }

    public AccountPropertiesResponse getAccountProperties(String account) {
        addParameters(RequestType.requestType, getAccountProperties);
        addParameters(Parameters.recipient, account);
        return getInstanse(AccountPropertiesResponse.class);
    }

    public SearchAccountsResponse  searchAccounts(String searchQuery) {
        addParameters(RequestType.requestType,RequestType.searchAccounts);
        addParameters(Parameters.query, searchQuery);
        return  getInstanse(SearchAccountsResponse.class);
    }

    public TransactionListResponse getUnconfirmedTransactions(Wallet wallet) {
        addParameters(RequestType.requestType,RequestType.getUnconfirmedTransactions);
        addParameters(Parameters.wallet,wallet);
        return getInstanse(TransactionListResponse.class);
    }

    public AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account) {
        addParameters(RequestType.requestType,RequestType.getUnconfirmedTransactionIds);
        addParameters(Parameters.account,account);
        return getInstanse(AccountTransactionIdsResponse.class);
    }

    public BalanceDTO getGuaranteedBalance(String account, int confirmations) {
        addParameters(RequestType.requestType,RequestType.getGuaranteedBalance);
        addParameters(Parameters.account, account);
        addParameters(Parameters.numberOfConfirmations, confirmations);
        return getInstanse(BalanceDTO.class);
    }

    public BalanceDTO getBalance(Wallet wallet) {
        addParameters(RequestType.requestType, getBalance);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(BalanceDTO.class);
    }

    public EntryDTO getAccountLedgerEntry(String ledgerId) {
        addParameters(RequestType.requestType, getAccountLedgerEntry);
        addParameters(Parameters.ledgerId,ledgerId);
        return getInstanse(EntryDTO.class);
    }


    public CreateTransactionResponse sendMoney(Wallet wallet,String recipient, int moneyAmount) {
        addParameters(RequestType.requestType,RequestType.sendMoney);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount+"00000000");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }


    public AccountDTO getAccountPublicKey (Wallet wallet) {
        addParameters(RequestType.requestType,RequestType.getAccountPublicKey);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountDTO.class);
    }

    public BlockchainTransactionsResponse getAccountTransaction (Wallet wallet) {
        addParameters(RequestType.requestType,RequestType.getBlockchainTransactions);
        addParameters(Parameters.wallet, wallet);
        return  getInstanse(BlockchainTransactionsResponse.class);
    }

    public CreateTransactionResponse  setAccountInfo(Wallet wallet,String accountName,String accountDescription) {
        addParameters(RequestType.requestType,RequestType.setAccountInfo);
        addParameters(Parameters.name, accountName);
        addParameters(Parameters.description, accountDescription);
        addParameters(Parameters.wallet, wallet);
       // addParameters(Parameters.recipient, accountID);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 300000000);
        return   getInstanse(CreateTransactionResponse.class);
    }

    public CreateTransactionResponse  setAccountProperty(Wallet wallet, String property ) {
        addParameters(RequestType.requestType,RequestType.setAccountProperty);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.recipient, wallet.getUser());
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 100000000);
        return   getInstanse(CreateTransactionResponse.class);
    }


    public CreateTransactionResponse  deleteAccountProperty(Wallet wallet,String property) {
        addParameters(RequestType.requestType,RequestType.deleteAccountProperty);
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        return   getInstanse(CreateTransactionResponse.class);
    }

    public AccountPropertiesResponse getAccountProperty(Wallet wallet) {
        addParameters(RequestType.requestType,RequestType.getAccountProperties);
        addParameters(Parameters.recipient, wallet.getUser());
        return  getInstanse(AccountPropertiesResponse.class);
    }

    //Skrypchenko Serhii
    public AccountAliasesResponse getAliases  (Wallet wallet) {
        addParameters(RequestType.requestType,RequestType.getAliases);
        addParameters(Parameters.wallet, wallet);
        return  getInstanse(AccountAliasesResponse.class);
    }

    //Skrypchenko Serhii
    @Step
    public AccountCountAliasesResponse getAliasCount(Wallet wallet) {
        addParameters(RequestType.requestType,RequestType.getAliasCount);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCountAliasesResponse.class);
    }

    //Skrypchenko Serhii
    public AccountAliasDTO getAlias(String aliasname) {
        addParameters(RequestType.requestType,RequestType.getAlias);
        addParameters(Parameters.aliasName, aliasname);
        return getInstanse(AccountAliasDTO.class);
    }


    //Skrypchenko Serhii
    @Step
    public  CreateTransactionResponse setAlias (Wallet wallet,String aliasURL, String aliasName, Integer feeATM, Integer deadline) {
        addParameters(RequestType.requestType,RequestType.setAlias);
        addParameters(Parameters.aliasURI, aliasURL);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, feeATM);
        addParameters(Parameters.deadline, deadline);
        return getInstanse(CreateTransactionResponse.class);

    }

    //Skrypchenko Serhii
    public CreateTransactionResponse deleteAlias(Wallet wallet, String aliasname) {
        addParameters(RequestType.requestType,RequestType.deleteAlias);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.aliasName, aliasname);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    //Serhii Skrypchenko
    public AccountAliasesResponse getAliasesLike(String aliasename) {
        addParameters(RequestType.requestType,RequestType.getAliasesLike);
        addParameters(Parameters.aliasPrefix, aliasename);
        return getInstanse(AccountAliasesResponse.class);
    }


    //Serhii Skrypchenko (sell Alias)
    public CreateTransactionResponse sellAlias (Wallet wallet,String aliasName) {
        addParameters(RequestType.requestType, RequestType.sellAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.priceATM, 1500000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    public CreateTransactionResponse buyAlias (Wallet wallet,String aliasName) {
        addParameters(RequestType.requestType, RequestType.buyAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.amountATM, 1500000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }
  
    public CreateTransactionResponse sendMoneyPrivate(Wallet wallet,String recipient, int moneyAmount) {
        addParameters(RequestType.requestType,RequestType.sendMoneyPrivate);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount+"00000000");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    public Account2FAResponse generateNewAccount() {
        addParameters(RequestType.requestType,RequestType.generateAccount);
        return getInstanse(Account2FAResponse.class);
    }


     public Account2FAResponse deleteSecretFile(Wallet wallet) {
         try {
         addParameters(RequestType.requestType,RequestType.deleteKey);
         addParameters(Parameters.wallet, wallet);
          return mapper.readValue(httpCallPost().body().string(), Account2FAResponse.class);
         } catch (IOException e) {
             e.printStackTrace();
         }
         return null;
     }


    public VaultWalletResponse exportSecretFile(Wallet wallet) {
        addParameters(RequestType.requestType,RequestType.exportKey);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(VaultWalletResponse.class);
    }

    public boolean importSecretFile(String pathToSecretFile, String pass) {
       // addParameters(RequestType.requestType, importKey);
      //  addParameters(Parameters.wallet, wallet);
      //  return getInstanse(Account2FAResponse.class);
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    public  List<ShardDTO> getShards(String ip) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    public AccountDTO enable2FA(Wallet wallet) {
       // addParameters(RequestType.requestType,RequestType.enable2FA);
      //  addParameters(Parameters.wallet, wallet);
       // return getInstanse(AccountDTO.class);
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    public List<DexOrderDto> getDexOrders(String orderType, String pairCurrency, String status, String accountId) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    public List<DexOrderDto> getDexOrders() {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    public List<DexOrderDto> getDexHistory(String account, String pair, String type) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    public List<DexOrderDto> getDexHistory(String account) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    public EthGasInfoResponse getEthGas() {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    public List<String> getPeers() {
        try {
        addParameters(RequestType.requestType, RequestType.getPeers);
        addParameters(Parameters.active, true);
        Response response = httpCallGet();
        Assertions.assertEquals(200, response.code());
        GetPeersIpResponse peers = mapper.readValue(response.body().string(), GetPeersIpResponse.class);
        return peers.getPeers();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PeerDTO getPeer(String peer) {
        addParameters(RequestType.requestType, RequestType.getPeer);
        addParameters(Parameters.peer, peer);
        return getInstanse(PeerDTO.class);
    }

    public PeerDTO addPeer(String ip) {
        addParameters(RequestType.requestType, RequestType.addPeer);
        addParameters(Parameters.peer, ip);
        addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
        return getInstanse(PeerDTO.class);
    }

    public PeerInfo getMyInfo() {
        addParameters(RequestType.requestType, RequestType.getMyInfo);
        return getInstanse(PeerInfo.class);
    }

    public BlockDTO getBlock(String block) {
        addParameters(RequestType.requestType, getBlock);
        addParameters(Parameters.block, block);
        addParameters(Parameters.includeTransactions, true);
        return getInstanse(BlockDTO.class);

    }

    public GetBlockIdResponse getBlockId(String height) {
        addParameters(RequestType.requestType, getBlockId);
        addParameters(Parameters.height, height);
        return getInstanse(GetBlockIdResponse.class);

    }

    public BlockchainInfoDTO getBlockchainStatus() {
        addParameters(RequestType.requestType, getBlockchainStatus);
        return getInstanse(BlockchainInfoDTO.class);
    }

    public AccountBlocksResponse getBlocks() {
        addParameters(RequestType.requestType, getBlocks);
        return getInstanse(AccountBlocksResponse.class);
    }


    public void verifyCreatingTransaction (CreateTransactionResponse transaction) {
        assertNotNull(transaction);
        assertNotNull(transaction.getTransaction(), transaction.errorDescription);
        assertNotNull(transaction.getTransactionJSON(),transaction.errorDescription);
        assertNotNull(transaction.getTransactionJSON().getSenderPublicKey());
        assertNotNull(transaction.getTransactionJSON().getSignature());
        assertNotNull(transaction.getTransactionJSON().getFullHash());
        assertNotNull(transaction.getTransactionJSON().getAmountATM());
        assertNotNull(transaction.getTransactionJSON().getEcBlockId());
        assertNotNull(transaction.getTransactionJSON().getSenderRS());
        assertNotNull(transaction.getTransactionJSON().getTransaction());
        assertNotNull(transaction.getTransactionJSON().getFeeATM());
        assertNotNull(transaction.getTransactionJSON().getType());

    }

    //AssetExchange
    //issueAsset
    public  CreateTransactionResponse issueAsset (Wallet wallet,String assetName, String description, Integer quantityATU) {
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
    public  CreateTransactionResponse placeBidOrder (Wallet wallet,String assetID, String priceATM, Integer quantityATU) {
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
    public  CreateTransactionResponse placeAskOrder (Wallet wallet,String assetID, String priceATM, Integer quantityATU) {
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
    public  CreateTransactionResponse cancelBidOrder (Wallet wallet,String bidOrder) {
        addParameters(RequestType.requestType, cancelBidOrder);
        addParameters(Parameters.order, bidOrder);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    //cancelAskOrder
    public  CreateTransactionResponse cancelAskOrder (Wallet wallet,String askOrder) {
        addParameters(RequestType.requestType, cancelAskOrder);
        addParameters(Parameters.order, askOrder);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    //deleteAssetShares
    public  CreateTransactionResponse deleteAssetShares (Wallet wallet,String assetID, String quantityATU) {
        addParameters(RequestType.requestType, deleteAssetShares);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    //dividendPayment
    public  CreateTransactionResponse dividendPayment (Wallet wallet,String assetID, Integer amountATMPerATU, Integer height) {
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
    public AccountAssetsResponse getAccountAssets (Wallet wallet) {
        addParameters(RequestType.requestType, getAccountAssets);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountAssetsResponse.class);
    }

    //getAccountAssetCount
    public AccountAssetsCountResponse getAccountAssetCount (Wallet wallet) {
        addParameters(RequestType.requestType, getAccountAssetCount);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountAssetsCountResponse.class);
    }

    //getAsset
    public AccountAssetDTO getAsset (String asset) {
        addParameters(RequestType.requestType, getAsset);
        addParameters(Parameters.asset, asset);
        return getInstanse(AccountAssetDTO.class);
    }


    //getAccountCurrentAskOrderIds
    public AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds (Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrentAskOrderIds);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCurrentAssetAskOrderIdsResponse.class);
    }

    //getAccountCurrentBidOrderIds
    public AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds (Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrentBidOrderIds);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCurrentAssetBidOrderIdsResponse.class);
    }


    //getAccountCurrentAskOrders
    public AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders (Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrentAskOrders);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCurrentAssetAskOrdersResponse.class);
    }

    //getAccountCurrentBidOrders
    public AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders (Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrentBidOrders);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCurrentAssetBidOrdersResponse.class);
    }

    //getAllAssets
    public AssetsResponse getAllAssets () {
        addParameters(RequestType.requestType, getAllAssets);
        return getInstanse(AssetsResponse.class);
    }

    //getAllOpenAskOrders
    public AccountOpenAssetOrdersResponse getAllOpenAskOrders () {
        addParameters(RequestType.requestType, getAllOpenAskOrders);
        return getInstanse(AccountOpenAssetOrdersResponse.class);
    }


    //getAllOpenBidOrders
    public AccountOpenAssetOrdersResponse getAllOpenBidOrders () {
        addParameters(RequestType.requestType, getAllOpenBidOrders);
        return getInstanse(AccountOpenAssetOrdersResponse.class);
    }

    //getAllTrades
    public AssetTradeResponse getAllTrades () {
        addParameters(RequestType.requestType, getAllTrades);
        return getInstanse(AssetTradeResponse.class);
    }

    //getAskOrder
    public AccountAssetOrderDTO getAskOrder (String askOrder) {
        addParameters(RequestType.requestType, getAskOrder);
        addParameters(Parameters.order, askOrder);
        return getInstanse(AccountAssetOrderDTO.class);
    }

    //getAskOrderIds
    public AccountCurrentAssetAskOrderIdsResponse getAskOrderIds (String assetID) {
        addParameters(RequestType.requestType, getAskOrderIds);
        addParameters(Parameters.asset, assetID);
        return getInstanse(AccountCurrentAssetAskOrderIdsResponse.class);
    }

    //getAskOrders
    public AccountCurrentAssetAskOrdersResponse getAskOrders (String assetID) {
        addParameters(RequestType.requestType, getAskOrders);
        addParameters(Parameters.asset, assetID);

        return getInstanse(AccountCurrentAssetAskOrdersResponse.class);
    }

    //getBidOrders
    public AccountCurrentAssetBidOrdersResponse getBidOrders (String assetID) {
        addParameters(RequestType.requestType, getBidOrders);
        addParameters(Parameters.asset, assetID);

        return getInstanse(AccountCurrentAssetBidOrdersResponse.class);
    }


    //getAssetAccountCount
    public AssetsAccountsCountResponse getAssetAccountCount (String assetID) {
        addParameters(RequestType.requestType, getAssetAccountCount);
        addParameters(Parameters.asset, assetID);
        return getInstanse(AssetsAccountsCountResponse.class);
    }

    //getAssetAccounts
    public  AccountAssetsResponse getAssetAccounts (String assetID) {
        addParameters(RequestType.requestType, getAssetAccounts);
        addParameters(Parameters.asset, assetID);
        return getInstanse(AccountAssetsResponse.class);
    }

    //getAssetDeletes
    public ExpectedAssetDeletes getAssetDeletes (Wallet wallet) {
        addParameters(RequestType.requestType, getAssetDeletes);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(ExpectedAssetDeletes.class);
    }

    //getExpectedAssetDeletes
    public  ExpectedAssetDeletes getExpectedAssetDeletes (Wallet wallet) {
        addParameters(RequestType.requestType, getExpectedAssetDeletes);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(ExpectedAssetDeletes.class);
    }

    //getAssetDividends NOT READY YET!!!!!
    /*public  GetAssetDividends getAssetDividends (Wallet wallet) {
        addParameters(RequestType.requestType, getAssetDividends);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetAssetDividends.class);
    }*/

    //getAssetIds
    public AccountAssetsIdsResponse getAssetIds () {
        addParameters(RequestType.requestType, getAssetIds);
        return getInstanse(AccountAssetsIdsResponse.class);
    }

    //transferAsset
    public  CreateTransactionResponse transferAsset (Wallet wallet, String asset, Integer quantityATU, String recipient) {
        addParameters(RequestType.requestType, transferAsset);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.asset, asset);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }




    public ECBlockDTO getECBlock() {
        addParameters(RequestType.requestType, getECBlock);
        return getInstanse(ECBlockDTO.class);
    }

    public ForgingResponse getForging(){
        addParameters(RequestType.requestType, getForging);
        addParameters(Parameters.adminPassword,  getTestConfiguration().getAdminPass());
        return getInstanse(ForgingResponse.class);
    }

    public ForgingDetails startForging(Wallet wallet){
        addParameters(RequestType.requestType, startForging);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.adminPassword,  getTestConfiguration().getAdminPass());
        return getInstanse(ForgingDetails.class);
    }

    public ForgingDetails stopForging(Wallet wallet){
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

    public AccountMessageDTO readMessage(Wallet wallet,String transaction) {
        addParameters(RequestType.requestType,RequestType.readMessage);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.transaction,transaction);
        return getInstanse(AccountMessageDTO.class);
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
    public void createPhasingVote(long phasingFinishHeight, Parameters votingModel, int phasingQuorum, Long phasingMinBalance, Long phasingMinBalanceModel, String phasingHolding){
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
        this.testInfo = null;
    }


    @AfterAll
    static void afterAll() {
        try {
        //    deleteKey(getTestConfiguration().getVaultWallet());
        } catch (Exception e) { }

    }


}
