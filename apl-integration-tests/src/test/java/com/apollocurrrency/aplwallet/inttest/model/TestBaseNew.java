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
import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
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
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.DisplayName;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import static io.restassured.RestAssured.given;

public class TestBaseNew extends TestBase {
    @Override
    public boolean verifyTransactionInBlock(String transaction) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public TransactionDTO getTransaction(String transaction) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public BlockListInfoResponse getAccountBlocks(String account) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public GetAccountResponse getAccount(String account) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public GetAccountBlockCountResponse getAccountBlockCount(String account) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountBlockIdsResponse getAccountBlockIds(String account) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountDTO getAccountId(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountLedgerResponse getAccountLedger(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountPropertiesResponse getAccountProperties(String account) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public SearchAccountsResponse searchAccounts(String searchQuery) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public TransactionListResponse getUnconfirmedTransactions(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public BalanceDTO getGuaranteedBalance(String account, int confirmations) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public BalanceDTO getBalance(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public EntryDTO getAccountLedgerEntry(String ledgerId) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse sendMoney(Wallet wallet, String recipient, int moneyAmount) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountDTO getAccountPublicKey(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public BlockchainTransactionsResponse getAccountTransaction(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse setAccountInfo(Wallet wallet, String accountName, String accountDescription) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse setAccountProperty(Wallet wallet, String property) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse deleteAccountProperty(Wallet wallet, String property) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountPropertiesResponse getAccountProperty(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAliasesResponse getAliases(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCountAliasesResponse getAliasCount(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAliasDTO getAlias(String aliasname) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse setAlias(Wallet wallet, String aliasURL, String aliasName, Integer feeATM, Integer deadline) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse deleteAlias(Wallet wallet, String aliasname) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAliasesResponse getAliasesLike(String aliasename) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse sellAlias(Wallet wallet, String aliasName) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse buyAlias(Wallet wallet, String aliasName) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse sendMoneyPrivate(Wallet wallet, String recipient, int moneyAmount) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    @Step("Generate New Account")
    public Account2FAResponse generateNewAccount() throws JsonProcessingException {
        //TODO: Change on REST Easy
        HashMap<String, String> param = new HashMap();
        param.put(RequestType.requestType.toString(), RequestType.generateAccount.toString());
        String path = "/apl";
        Response response =  given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path);
       return mapper.readValue(response.body().prettyPrint(), Account2FAResponse.class);
    }

    @Override
    @Step("Delete Secret File")
    public Account2FAResponse deleteSecretFile(Wallet wallet) throws JsonProcessingException {
        //TODO: Change on REST Easy
        HashMap<String, String> param = new HashMap();
        param.put(RequestType.requestType.toString(), RequestType.deleteKey.toString());
        param.put(Parameters.account.toString(), wallet.getUser());
        param.put(Parameters.passphrase.toString(), wallet.getPass());
        String path = "/apl";
        Response response =  given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path);
        return mapper.readValue(response.body().prettyPrint(), Account2FAResponse.class);
    }

    @Override
    @Step("Export Secret File")
    public VaultWalletResponse exportSecretFile(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param.put("account", wallet.getUser());
        param.put("passPhrase", wallet.getPass());

        String path = "/rest/keyStore/download";
        return given().log().all()
                .spec(restHelper.getSpec())
                 .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path).as(VaultWalletResponse.class);
    }

    @Override
    @Step("Import Secret File")
    public boolean importSecretFile(String pathToSecretFile, String pass) {
        String path = "/rest/keyStore/upload";
        Response response = given().log().all()
                .spec(restHelper.getSpec())
                .header("Content-Type", "multipart/form-data")
                .multiPart("keyStore", new File(pathToSecretFile))
                .formParam("passPhrase", pass)
                .when()
                .post(path);
        return !response.body().asString().contains("error");
    }

    @Override
    @Step("Enable 2FA")
    public AccountDTO enable2FA(Wallet wallet) throws JsonProcessingException {
        //TODO: Change on REST Easy
        HashMap<String, String> param = new HashMap();
        param.put(RequestType.requestType.toString(), RequestType.enable2FA.toString());
        param = restHelper.addWalletParameters(param,wallet);

        String path = "/apl";
        Response response =  given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path);
        return mapper.readValue(response.body().prettyPrint(), AccountDTO.class);
    }

    @Override
    @Step
    @DisplayName("Get All Peers")
    public List<String> getPeers() {
        String path = "/rest/networking/peer/all";
            return given().log().uri()
                    .spec(restHelper.getSpec())
                    .when()
                    .get(path).as(GetPeersIpResponse.class).getPeers();

    }

    @Override
    @Step
    @DisplayName("Get Peer")
    public PeerDTO getPeer(String peer) {
        String path = String.format("/rest/networking/peer?peer=%s",peer);
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(PeerDTO.class);
    }

    @Override
    public PeerDTO addPeer(String ip) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    @Step("Get My Peer Info")
    public PeerInfo getMyInfo() {
        String path = "/rest/networking/peer/mypeerinfo";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(PeerInfo.class);
    }

    @Override
    @Step("Get Block")
    public BlockDTO getBlock(String block) throws JsonProcessingException {
        //TODO: Change on REST Easy
        HashMap<String, String> param = new HashMap();
        param.put(RequestType.requestType.toString(), RequestType.getBlock.toString());
        String path = "/apl";
        Response response = given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path);
        return mapper.readValue(response.body().prettyPrint(), BlockDTO.class);
    }
    @Step("Get Last Block")
    public BlockDTO getLastBlock(String peer) throws JsonProcessingException {
        //TODO: Change on REST Easy
        HashMap<String, String> param = new HashMap();
        param.put(RequestType.requestType.toString(), RequestType.getBlock.toString());
        String path = "/apl";
        Response response = given()
                .baseUri(String.format("http://%s:%s", peer, TestConfiguration.getTestConfiguration().getPort()))
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path);
        return mapper.readValue(response.body().prettyPrint(), BlockDTO.class);
    }

    //TODO add: boolean isAvailableForNow, int minAskPrice, int maxBidPrice
    @Override
    @Step("Get Dex Orders with param: Type {orderType}, Pair Currency {pairCurrency}")
    public List<DexOrderDto> getDexOrders(String orderType, String pairCurrency, String status, String accountId) {
        HashMap<String, String> param = new HashMap();
        param.put("orderType", orderType);
        param.put("pairCurrency", pairCurrency);
        param.put("status", status);
        param.put("accountId", accountId);

        String path = "/rest/dex/offers";
        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                //.get(path).as(DexOrderResponse.class);
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    //TODO add: boolean isAvailableForNow, int minAskPrice, int maxBidPrice
    @Override
    @Step("Get Dex Orders")
    public List<DexOrderDto> getDexOrders() {
        String path = "/rest/dex/offers";
        return   given().log().all()
                .spec(restHelper.getSpec())
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }


    @Override
    @Step("Get Dex History with param: Account: {0}, Pair: {1} , Type: {2}")
    public List<DexOrderDto> getDexHistory(String account, String pair, String type) {
        HashMap<String, String> param = new HashMap();
        param.put("pair", pair);
        param.put("type", type);
        param.put("accountId", account);

        String path = "/rest/dex/history";
        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Override
    @Step("Get Dex History")
    public List<DexOrderDto> getDexHistory(String account) {
        HashMap<String, String> param = new HashMap();
        param.put("accountId", account);

        String path = "/rest/dex/history";
        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Override
    @Step("Get Eth Gas Info")
    public EthGasInfoResponse getEthGasInfo() {
        String path = "/rest/dex/ethInfo";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(EthGasInfoResponse.class);
    }

    @Override
    @Step("Get Dex Trade Info")
    public List<DexTradeInfoDto> getDexTradeInfo(String pairCurrency, Integer startTime, Integer finishTime) {
        HashMap<String, String> param = new HashMap();
        param.put("pairCurrency", pairCurrency);
        param.put("start", String.valueOf(startTime));
        param.put("finish", String.valueOf(finishTime));

        String path = "/rest/dex/tradeInfo";
        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexTradeInfoDto.class);
    }


    @Override
    public GetBlockIdResponse getBlockId(String height) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public BlockchainInfoDTO getBlockchainStatus() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountBlocksResponse getBlocks() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void verifyCreatingTransaction(CreateTransactionResponse transaction) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse issueAsset(Wallet wallet, String assetName, String description, Integer quantityATU) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse placeBidOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse placeAskOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse cancelBidOrder(Wallet wallet, String bidOrder) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse cancelAskOrder(Wallet wallet, String askOrder) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse deleteAssetShares(Wallet wallet, String assetID, String quantityATU) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse dividendPayment(Wallet wallet, String assetID, Integer amountATMPerATU, Integer height) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetsResponse getAccountAssets(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetsCountResponse getAccountAssetCount(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetDTO getAsset(String asset) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AssetsResponse getAllAssets() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountOpenAssetOrdersResponse getAllOpenAskOrders() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountOpenAssetOrdersResponse getAllOpenBidOrders() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AssetTradeResponse getAllTrades() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetOrderDTO getAskOrder(String askOrder) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetAskOrderIdsResponse getAskOrderIds(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetAskOrdersResponse getAskOrders(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetBidOrdersResponse getBidOrders(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AssetsAccountsCountResponse getAssetAccountCount(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetsResponse getAssetAccounts(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ExpectedAssetDeletes getAssetDeletes(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ExpectedAssetDeletes getExpectedAssetDeletes(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetsIdsResponse getAssetIds() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse transferAsset(Wallet wallet, String asset, Integer quantityATU, String recipient) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ECBlockDTO getECBlock() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    @Step("Get Forging")
    public ForgingResponse getForging() {
        String path = "/rest/nodeinfo/forgers";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(ForgingResponse.class);
    }

    @Override
    @Step("Get Shards from peer")
    public List<ShardDTO> getShards(String ip) {
        String path = "/rest/shards";
        return given().log().uri()
                .contentType(ContentType.JSON)
                .baseUri(String.format("http://%s:%s",ip,7876))
                .when()
                .get(path).getBody().jsonPath().getList("",ShardDTO.class);
    }

    @Override
    public ForgingDetails startForging(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ForgingDetails stopForging(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse sendMessage(Wallet wallet, String recipient, String testMessage) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountMessageDTO readMessage(Wallet wallet, String transaction) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void createPhasingVote(long phasingFinishHeight, Parameters votingModel, int phasingQuorum, Long phasingMinBalance, Long phasingMinBalanceModel, String phasingHolding) {
        throw new NotImplementedException("Not implemented");
    }
}
