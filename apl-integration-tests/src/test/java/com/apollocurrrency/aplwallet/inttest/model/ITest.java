package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.AccountAliasDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetOrderDTO;
import com.apollocurrency.aplwallet.api.dto.AccountMessageDTO;
import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.Currency;
import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.dto.EntryDTO;
import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.dto.PollDTO;
import com.apollocurrency.aplwallet.api.dto.TaggedDataDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlockIdsResponse;
import com.apollocurrency.aplwallet.api.response.BlocksResponse;
import com.apollocurrency.aplwallet.api.response.AccountCountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountLedgerResponse;
import com.apollocurrency.aplwallet.api.response.AccountOpenAssetOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountPropertiesResponse;
import com.apollocurrency.aplwallet.api.response.AccountTransactionIdsResponse;
import com.apollocurrency.aplwallet.api.response.AllTaggedDataResponse;
import com.apollocurrency.aplwallet.api.response.AssetTradeResponse;
import com.apollocurrency.aplwallet.api.response.AssetsAccountsCountResponse;
import com.apollocurrency.aplwallet.api.response.AssetsResponse;
import com.apollocurrency.aplwallet.api.response.BlockListInfoResponse;
import com.apollocurrency.aplwallet.api.response.BlockchainTransactionsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.CurrenciesResponse;
import com.apollocurrency.aplwallet.api.response.CurrencyAccountsResponse;
import com.apollocurrency.aplwallet.api.response.DataTagCountResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.ExpectedAssetDeletes;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountBlockCountResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrency.aplwallet.api.response.GetBlockIdResponse;
import com.apollocurrency.aplwallet.api.response.PollResultResponse;
import com.apollocurrency.aplwallet.api.response.PollVotesResponse;
import com.apollocurrency.aplwallet.api.response.SearchAccountsResponse;
import com.apollocurrency.aplwallet.api.response.TransactionListResponse;
import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.util.List;


public interface ITest {

    boolean verifyTransactionInBlock(String transaction);

    TransactionDTO getTransaction(String transaction);

    BlockListInfoResponse getAccountBlocks(String account);

    GetAccountResponse getAccount(String account);

    GetAccountBlockCountResponse getAccountBlockCount(String account);

    AccountBlockIdsResponse getAccountBlockIds(String account);

    AccountDTO getAccountId(String secretPhrase);

    AccountLedgerResponse getAccountLedger(Wallet wallet);

    AccountPropertiesResponse getAccountProperties(String account);

    SearchAccountsResponse searchAccounts(String searchQuery);

    TransactionListResponse getUnconfirmedTransactions(Wallet wallet);

    AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account);

    BalanceDTO getGuaranteedBalance(String account, int confirmations);

    BalanceDTO getBalance(Wallet wallet);

    EntryDTO getAccountLedgerEntry(String ledgerId);

    CreateTransactionResponse sendMoney(Wallet wallet, String recipient, int moneyAmount);

    AccountDTO getAccountPublicKey(Wallet wallet);

    BlockchainTransactionsResponse getAccountTransaction(Wallet wallet);

    CreateTransactionResponse setAccountInfo(Wallet wallet, String accountName, String accountDescription);

    CreateTransactionResponse setAccountProperty(Wallet wallet, String property);

    CreateTransactionResponse deleteAccountProperty(Wallet wallet, String property);

    AccountPropertiesResponse getAccountProperty(Wallet wallet);

    AccountAliasesResponse getAliases(Wallet wallet);

    AccountCountAliasesResponse getAliasCount(Wallet wallet);

    AccountAliasDTO getAlias(String aliasname);

    CreateTransactionResponse setAlias(Wallet wallet, String aliasURL, String aliasName, Integer feeATM, Integer deadline);

    CreateTransactionResponse deleteAlias(Wallet wallet, String aliasname);

    AccountAliasesResponse getAliasesLike(String aliasename);

    CreateTransactionResponse sellAlias(Wallet wallet, String aliasName);

    CreateTransactionResponse buyAlias(Wallet wallet, String aliasName);

    CreateTransactionResponse sendMoneyPrivate(Wallet wallet, String recipient, int moneyAmount);

    Account2FAResponse generateNewAccount() throws JsonProcessingException;

    Account2FAResponse deleteSecretFile(Wallet wallet) throws JsonProcessingException;

    VaultWalletResponse exportSecretFile(Wallet wallet);

    boolean importSecretFile(String pathToSecretFile, String pass);

    AccountDTO enable2FA(Wallet wallet) throws JsonProcessingException;

    List<String> getPeers();

    PeerDTO getPeer(String peer);

    PeerDTO addPeer(String ip);

    PeerInfo getMyInfo();

    BlockDTO getBlock(String block) throws JsonProcessingException;

    GetBlockIdResponse getBlockId(String height);

    BlockchainInfoDTO getBlockchainStatus();

    BlocksResponse getBlocks();

    void verifyCreatingTransaction(CreateTransactionResponse transaction);

    CreateTransactionResponse issueAsset(Wallet wallet, String assetName, String description, Integer quantityATU);

    CreateTransactionResponse placeBidOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU);

    CreateTransactionResponse placeAskOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU);

    CreateTransactionResponse cancelBidOrder(Wallet wallet, String bidOrder);

    CreateTransactionResponse cancelAskOrder(Wallet wallet, String askOrder);

    CreateTransactionResponse deleteAssetShares(Wallet wallet, String assetID, String quantityATU);

    CreateTransactionResponse dividendPayment(Wallet wallet, String assetID, Integer amountATMPerATU, Integer height);

    AccountAssetsResponse getAccountAssets(Wallet wallet);

    AccountAssetsCountResponse getAccountAssetCount(Wallet wallet);

    AccountAssetDTO getAsset(String asset);

    AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds(Wallet wallet);

    AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds(Wallet wallet);

    AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders(Wallet wallet);

    AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders(Wallet wallet);

    AssetsResponse getAllAssets();

    AccountOpenAssetOrdersResponse getAllOpenAskOrders();

    AccountOpenAssetOrdersResponse getAllOpenBidOrders();

    AssetTradeResponse getAllTrades();

    AccountAssetOrderDTO getAskOrder(String askOrder);

    AccountCurrentAssetAskOrderIdsResponse getAskOrderIds(String assetID);

    AccountCurrentAssetAskOrdersResponse getAskOrders(String assetID);

    AccountCurrentAssetBidOrdersResponse getBidOrders(String assetID);

    AssetsAccountsCountResponse getAssetAccountCount(String assetID);

    AccountAssetsResponse getAssetAccounts(String assetID);

    ExpectedAssetDeletes getAssetDeletes(Wallet wallet);

    ExpectedAssetDeletes getExpectedAssetDeletes(Wallet wallet);

    AccountAssetsIdsResponse getAssetIds();

    CreateTransactionResponse transferAsset(Wallet wallet, String asset, Integer quantityATU, String recipient);

    ECBlockDTO getECBlock();

    ForgingResponse getForging();

    ForgingDetails startForging(Wallet wallet);

    ForgingDetails stopForging(Wallet wallet);

    CreateTransactionResponse sendMessage(Wallet wallet, String recipient, String testMessage);

    AccountMessageDTO readMessage(Wallet wallet, String transaction);

    List getShards(String ip);

    void createPhasingVote(long phasingFinishHeight, Parameters votingModel, int phasingQuorum, Long phasingMinBalance, Long phasingMinBalanceModel, String phasingHolding);

    List<DexOrderDto> getDexOrders(String orderType, String pairCurrency, String status, String accountId);

    List<DexOrderDto> getDexOrders();

    List<DexOrderDto> getDexHistory(String account, String pair, String type);

    List<DexOrderDto> getDexHistory(String account);

    EthGasInfoResponse getEthGasInfo();

    List<DexTradeInfoDto> getDexTradeInfo(String pairCurrency, Integer startTime, Integer finishTime);

    CreateTransactionResponse dexCancelOrder(String orderId, Wallet wallet);

    String createDexOrder(String pairRate, String offerAmount, Wallet wallet, boolean isBuyOrder, boolean isEth);

    List<DexOrderDto> getDexOrders(String accountId);

    Account2FAResponse getDexBalances(String ethAddress);

    WithdrawResponse dexWidthraw(String fromAddress, Wallet wallet, String toAddress, String amount, String transferFee, boolean isEth);

    CreateTransactionResponse issueCurrency(Wallet wallet, int type, String name, String description, String code, int initialSupply, int maxSupply, int decimals);

    CurrenciesResponse getAllCurrencies();

    Currency getCurrency(String CurrencyId);

    CurrencyAccountsResponse getCurrencyAccounts(String CurrencyId);

    CreateTransactionResponse deleteCurrency(Wallet wallet, String CurrencyId);

    CreateTransactionResponse transferCurrency(String recipient, String currency, Wallet wallet, int units);

    CreateTransactionResponse currencyReserveClaim(String currency, Wallet wallet, int units);

    CreateTransactionResponse currencyReserveIncrease(String currency, Wallet wallet, int amountPerUnitATM);

    CreateTransactionResponse publishExchangeOffer(String currency, Wallet wallet, int buyRateATM, int sellRateATM, int initialBuySupply, int initialSellSupply);

    CreateTransactionResponse currencySell(String currency, Wallet wallet, int units, int rate);

    CreateTransactionResponse currencyBuy(String currency, Wallet wallet, int units, int rate);

    CreateTransactionResponse scheduleCurrencyBuy(String currency, Wallet wallet, int units, int rate, String offerIssuer);

    PollDTO getPoll(String poll);

    CreateTransactionResponse createPoll(Wallet wallet, int votingModel, String name, int plusFinishHeight, String holding, int minBalance, int maxRangeValue);

    CreateTransactionResponse castVote(Wallet wallet, String poll, int vote);

    AccountCurrencyResponse getAccountCurrencies(Wallet wallet);

    CreateTransactionResponse shufflingCreate(Wallet wallet, int registrationPeriod, int participantCount, int amount, String holding, int holdingType);

    PollVotesResponse getPollVotes(String poll);

    PollResultResponse getPollResult(String poll);

    CreateTransactionResponse uploadTaggedData(Wallet wallet, String name, String description, String tags, String channel, File file);

    AllTaggedDataResponse getAllTaggedData();

    TaggedDataDTO getTaggedData(String transaction);

    DataTagCountResponse getDataTagCount();

    AllTaggedDataResponse searchTaggedDataByName(String query);

    AllTaggedDataResponse searchTaggedDataByTag(String tag);

    CreateTransactionResponse extendTaggedData(Wallet wallet, String transaction);

}
