package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.response.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public class TestBaseNew extends TestBase {

    @Override
    public boolean verifyTransactionInBlock(String transaction) {
        throw new NotImplementedException();
    }

    @Override
    public TransactionDTO getTransaction(String transaction) {
        throw new NotImplementedException();
    }

    @Override
    public BlockListInfoResponse getAccountBlocks(String account) {
        throw new NotImplementedException();
    }

    @Override
    public GetAccountResponse getAccount(String account) {
        throw new NotImplementedException();
    }

    @Override
    public GetAccountBlockCountResponse getAccountBlockCount(String account) {
        throw new NotImplementedException();
    }

    @Override
    public AccountBlockIdsResponse getAccountBlockIds(String account) {
        throw new NotImplementedException();
    }

    @Override
    public AccountDTO getAccountId(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountLedgerResponse getAccountLedger(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountPropertiesResponse getAccountProperties(String account) {
        throw new NotImplementedException();
    }

    @Override
    public SearchAccountsResponse searchAccounts(String searchQuery) {
        throw new NotImplementedException();
    }

    @Override
    public TransactionListResponse getUnconfirmedTransactions(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account) {
        throw new NotImplementedException();
    }

    @Override
    public BalanceDTO getGuaranteedBalance(String account, int confirmations) {
        throw new NotImplementedException();
    }

    @Override
    public BalanceDTO getBalance(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public EntryDTO getAccountLedgerEntry(String ledgerId) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse sendMoney(Wallet wallet, String recipient, int moneyAmount) {
        throw new NotImplementedException();
    }

    @Override
    public AccountDTO getAccountPublicKey(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public BlockchainTransactionsResponse getAccountTransaction(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse setAccountInfo(Wallet wallet, String accountName, String accountDescription) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse setAccountProperty(Wallet wallet, String property) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse deleteAccountProperty(Wallet wallet, String property) {
        throw new NotImplementedException();
    }

    @Override
    public AccountPropertiesResponse getAccountProperty(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAliasesResponse getAliases(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountCountAliasesResponse getAliasCount(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAliasDTO getAlias(String aliasname) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse setAlias(Wallet wallet, String aliasURL, String aliasName, Integer feeATM, Integer deadline) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse deleteAlias(Wallet wallet, String aliasname) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAliasesResponse getAliasesLike(String aliasename) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse sellAlias(Wallet wallet, String aliasName) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse buyAlias(Wallet wallet, String aliasName) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse sendMoneyPrivate(Wallet wallet, String recipient, int moneyAmount) {
        throw new NotImplementedException();
    }

    @Override
    public AccountDTO generateNewAccount() {
        throw new NotImplementedException();
    }

    @Override
    public Account2FA deleteSecretFile(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public Account2FA exportSecretFile(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public Account2FA importSecretFile(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountDTO enable2FA(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getPeers() {
        throw new NotImplementedException();
    }

    @Override
    public PeerDTO getPeer(String peer) {
        throw new NotImplementedException();
    }

    @Override
    public PeerDTO addPeer(String ip) {
        throw new NotImplementedException();
    }

    @Override
    public PeerInfo getMyInfo() {
        throw new NotImplementedException();
    }

    @Override
    public BlockDTO getBlock(String block) {
        throw new NotImplementedException();
    }

    @Override
    public GetBlockIdResponse getBlockId(String height) {
        throw new NotImplementedException();
    }

    @Override
    public BlockchainInfoDTO getBlockchainStatus() {
        throw new NotImplementedException();
    }

    @Override
    public AccountBlocksResponse getBlocks() {
        throw new NotImplementedException();
    }

    @Override
    public void verifyCreatingTransaction(CreateTransactionResponse transaction) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse issueAsset(Wallet wallet, String assetName, String description, Integer quantityATU) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse placeBidOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse placeAskOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse cancelBidOrder(Wallet wallet, String bidOrder) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse cancelAskOrder(Wallet wallet, String askOrder) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse deleteAssetShares(Wallet wallet, String assetID, String quantityATU) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse dividendPayment(Wallet wallet, String assetID, Integer amountATMPerATU, Integer height) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAssetsResponse getAccountAssets(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAssetsCountResponse getAccountAssetCount(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAssetDTO getAsset(String asset) {
        throw new NotImplementedException();
    }

    @Override
    public AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AssetsResponse getAllAssets() {
        throw new NotImplementedException();
    }

    @Override
    public AccountOpenAssetOrdersResponse getAllOpenAskOrders() {
        throw new NotImplementedException();
    }

    @Override
    public AccountOpenAssetOrdersResponse getAllOpenBidOrders() {
        throw new NotImplementedException();
    }

    @Override
    public AssetTradeResponse getAllTrades() {
        throw new NotImplementedException();
    }

    @Override
    public AccountAssetOrderDTO getAskOrder(String askOrder) {
        throw new NotImplementedException();
    }

    @Override
    public AccountCurrentAssetAskOrderIdsResponse getAskOrderIds(String assetID) {
        throw new NotImplementedException();
    }

    @Override
    public AccountCurrentAssetAskOrdersResponse getAskOrders(String assetID) {
        throw new NotImplementedException();
    }

    @Override
    public AccountCurrentAssetBidOrdersResponse getBidOrders(String assetID) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAssetsCountResponse getAssetAccountCount(String assetID) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAssetsResponse getAssetAccounts(String assetID) {
        throw new NotImplementedException();
    }

    @Override
    public ExpectedAssetDeletes getAssetDeletes(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public ExpectedAssetDeletes getExpectedAssetDeletes(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public AccountAssetsIdsResponse getAssetIds() {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse transferAsset(Wallet wallet, String asset, Integer quantityATU, String recipient) {
        throw new NotImplementedException();
    }

    @Override
    public ECBlockDTO getECBlock() {
        throw new NotImplementedException();
    }

    @Override
    public ForgingResponse getForging() {
        throw new NotImplementedException();
    }

    @Override
    public ForgingDetails startForging(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public ForgingDetails stopForging(Wallet wallet) {
        throw new NotImplementedException();
    }

    @Override
    public CreateTransactionResponse sendMessage(Wallet wallet, String recipient, String testMessage) {
        throw new NotImplementedException();
    }

    @Override
    public AccountMessageDTO readMessage(Wallet wallet, String transaction) {
        throw new NotImplementedException();
    }

    @Override
    public void createPhasingVote(long phasingFinishHeight, Parameters votingModel, int phasingQuorum, Long phasingMinBalance, Long phasingMinBalanceModel, String phasingHolding) {
        throw new NotImplementedException();
    }
}
