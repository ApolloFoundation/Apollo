package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.response.*;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.List;



public interface ITest {

    boolean verifyTransactionInBlock(String transaction);
    TransactionDTO getTransaction(String transaction);
    BlockListInfoResponse getAccountBlocks(String account);
    GetAccountResponse getAccount(String account);
    GetAccountBlockCountResponse getAccountBlockCount(String account);
    AccountBlockIdsResponse getAccountBlockIds(String account);
    AccountDTO getAccountId(Wallet wallet);
    AccountLedgerResponse getAccountLedger(Wallet wallet);
    AccountPropertiesResponse getAccountProperties(String account);
    SearchAccountsResponse  searchAccounts(String searchQuery);
    TransactionListResponse getUnconfirmedTransactions(Wallet wallet);
    AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account);
    BalanceDTO getGuaranteedBalance(String account, int confirmations);
    BalanceDTO getBalance(Wallet wallet);
    EntryDTO getAccountLedgerEntry(String ledgerId);
    CreateTransactionResponse sendMoney(Wallet wallet,String recipient, int moneyAmount);
    AccountDTO getAccountPublicKey (Wallet wallet);
    BlockchainTransactionsResponse getAccountTransaction (Wallet wallet);
    CreateTransactionResponse  setAccountInfo(Wallet wallet,String accountName,String accountDescription);
    CreateTransactionResponse  setAccountProperty(Wallet wallet, String property );
    CreateTransactionResponse  deleteAccountProperty(Wallet wallet,String property);
    AccountPropertiesResponse getAccountProperty(Wallet wallet);
    AccountAliasesResponse getAliases (Wallet wallet);
    AccountCountAliasesResponse getAliasCount(Wallet wallet);
    AccountAliasDTO getAlias(String aliasname);
    CreateTransactionResponse setAlias (Wallet wallet,String aliasURL, String aliasName, Integer feeATM, Integer deadline);
    CreateTransactionResponse deleteAlias(Wallet wallet, String aliasname);
    AccountAliasesResponse getAliasesLike(String aliasename);
    CreateTransactionResponse sellAlias (Wallet wallet,String aliasName);
    CreateTransactionResponse buyAlias (Wallet wallet,String aliasName);
    CreateTransactionResponse sendMoneyPrivate(Wallet wallet,String recipient, int moneyAmount);
    Account2FAResponse generateNewAccount() throws JsonProcessingException;
    Account2FAResponse deleteSecretFile(Wallet wallet) throws JsonProcessingException;
    VaultWalletResponse exportSecretFile(Wallet wallet);
    boolean importSecretFile(String pathToSecretFile, String pass);
    AccountDTO enable2FA(Wallet wallet) throws JsonProcessingException;
    List<String> getPeers();
    PeerDTO getPeer(String peer);
    PeerDTO addPeer(String ip);
    PeerInfo getMyInfo();
    BlockDTO getBlock(String block);
    GetBlockIdResponse getBlockId(String height);
    BlockchainInfoDTO getBlockchainStatus();
    AccountBlocksResponse getBlocks();
    void verifyCreatingTransaction (CreateTransactionResponse transaction);
    CreateTransactionResponse issueAsset (Wallet wallet,String assetName, String description, Integer quantityATU);
    CreateTransactionResponse placeBidOrder (Wallet wallet,String assetID, String priceATM, Integer quantityATU);
    CreateTransactionResponse placeAskOrder (Wallet wallet,String assetID, String priceATM, Integer quantityATU);
    CreateTransactionResponse cancelBidOrder (Wallet wallet,String bidOrder);
    CreateTransactionResponse cancelAskOrder (Wallet wallet,String askOrder);
    CreateTransactionResponse deleteAssetShares (Wallet wallet,String assetID, String quantityATU);
    CreateTransactionResponse dividendPayment (Wallet wallet,String assetID, Integer amountATMPerATU, Integer height);
    AccountAssetsResponse getAccountAssets (Wallet wallet);
    AccountAssetsCountResponse getAccountAssetCount (Wallet wallet);
    AccountAssetDTO getAsset (String asset);
    AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds (Wallet wallet);
    AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds (Wallet wallet);
    AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders (Wallet wallet);
    AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders (Wallet wallet);
    AssetsResponse getAllAssets ();
    AccountOpenAssetOrdersResponse getAllOpenAskOrders ();
    AccountOpenAssetOrdersResponse getAllOpenBidOrders ();
    AssetTradeResponse getAllTrades ();
    AccountAssetOrderDTO getAskOrder (String askOrder);
    AccountCurrentAssetAskOrderIdsResponse getAskOrderIds (String assetID);
    AccountCurrentAssetAskOrdersResponse getAskOrders (String assetID);
    AccountCurrentAssetBidOrdersResponse getBidOrders (String assetID);
    AssetsAccountsCountResponse getAssetAccountCount (String assetID);
    AccountAssetsResponse getAssetAccounts (String assetID);
    ExpectedAssetDeletes getAssetDeletes (Wallet wallet);
    ExpectedAssetDeletes getExpectedAssetDeletes (Wallet wallet);
    AccountAssetsIdsResponse getAssetIds ();
    CreateTransactionResponse transferAsset (Wallet wallet, String asset, Integer quantityATU, String recipient);
    ECBlockDTO getECBlock();
    ForgingResponse getForging();
    ForgingDetails startForging(Wallet wallet);
    ForgingDetails stopForging(Wallet wallet);
    CreateTransactionResponse sendMessage(Wallet wallet,String recipient, String testMessage);
    AccountMessageDTO readMessage(Wallet wallet,String transaction);
    List<ShardDTO> getShards(String ip);
    void createPhasingVote(long phasingFinishHeight, Parameters votingModel, int phasingQuorum,Long phasingMinBalance, Long phasingMinBalanceModel, String phasingHolding);

}
