package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetOrderDTO;
import com.apollocurrency.aplwallet.api.response.AccountAssetsCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountOpenAssetOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AssetTradeResponse;
import com.apollocurrency.aplwallet.api.response.AssetsAccountsCountResponse;
import com.apollocurrency.aplwallet.api.response.AssetsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ExpectedAssetDeletes;
import com.apollocurrrency.aplwallet.inttest.helper.providers.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Date;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Asset")
@Epic(value = "Asset")
public class TestAssetExchangeAPI extends TestBaseNew {
      // TODO:  Neeed imp full Exchange
    //SMOKE API TESTING (STATUS CODE 200)
    @DisplayName("issueAsset")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void issueAsset(Wallet wallet) throws IOException {
        CreateTransactionResponse issueAsset = issueAsset(wallet, "APIORDER11", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
    }


    @DisplayName("getAccountAssets")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetsTest(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "setAsset", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        AccountAssetsResponse getAccountAssets = getAccountAssets(wallet);
        assertThat(getAccountAssets.getAccountAssets().size(),greaterThanOrEqualTo(1));

    }

    @DisplayName("getAccountAssetCount")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetCountTest(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "setAsset", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        AccountAssetsCountResponse getAccountAssetCount = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount.getNumberOfAssets() >= 1);
        log.trace("number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount.getNumberOfAssets());
    }

    @DisplayName("getAsset")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAssetTest(Wallet wallet) throws IOException {
        String assetID;
        String assetName = "AS" + String.valueOf(new Date().getTime()).substring(7);
        String description = "description of assetName";
        Integer quantityATU = 50;
        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, description, quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        AccountAssetDTO getAsset = getAsset(assetID);
        assertTrue(getAsset.getName().equals(assetName), String.valueOf(getAsset.getAsset().equals(issueAsset.getTransaction())));
        assertTrue(getAsset.getAccountRS().equals(wallet.getUser()));
        log.trace("asset = " + getAsset.getAsset() + " ; name = " + getAsset.getName() + " ;  AccountRS = " + wallet.getUser());
    }


    @DisplayName("getAccountCurrentAskOrderIds")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentAskOrderIdsTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "AskOrder0", "Creating Asset -> placeAskOrder -> getAccountCurrentAskOrderIds", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds = getAccountCurrentAskOrderIds(wallet);
        assertTrue(getAccountCurrentAskOrderIds.getAskOrderIds().stream().anyMatch(orderID::equals));

    }


    @DisplayName("getAccountCurrentBidOrderIds")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentBidOrderIdsTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "BidOrder1", "Creating Asset -> placeBidOrder -> getAccountCurrentBidOrderIds", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.getTransaction());
        orderID = placeBidOrder.getTransaction();
        AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds = getAccountCurrentBidOrderIds(wallet);
        assertTrue(getAccountCurrentBidOrderIds.getBidOrderIds().stream().anyMatch(orderID::equals));

    }

    //Creating Asset -> placeAskOrder -> getAccountCurrentAskOrders
    @DisplayName("getAccountCurrentAskOrders")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentAskOrdersTest(Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "AskOrder2", "Creating Asset -> placeAskOrder -> getAccountCurrentAskOrders", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders = getAccountCurrentAskOrders(wallet);
        assertTrue(getAccountCurrentAskOrders.getAskOrders().stream().filter(orderDTO -> orderDTO.getOrder().equals(orderID)).count() == 1);

    }

    //Creating Asset -> placeBidOrder -> getAccountCurrentBidOrders
    @DisplayName("getAccountCurrentBidOrders")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentBidOrdersTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "BidOrder2", "Creating Asset -> placeBidOrder -> getAccountCurrentBidOrders", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.getTransaction());
        orderID = placeBidOrder.getTransaction();
        AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders = getAccountCurrentBidOrders(wallet);
        assertTrue(getAccountCurrentBidOrders.getBidOrders().stream().filter(orderDTO -> orderDTO.getOrder().equals(orderID)).count() == 1);

    }

    @DisplayName("getAllAssets")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAllAssetsTest(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "GetAll", "Creating Asset -> getAllAssets", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        AssetsResponse getAllAssets = getAllAssets();
        assertTrue(getAllAssets.getAssets().size() >= 1); //return only first 100 assets

    }


    //getAllOpenAskOrders
    @DisplayName("getAllOpenAskOrders")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAllOpenAskOrdersTest(Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "AssetOpen1", "Creating Asset -> getAllAssets", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        AccountOpenAssetOrdersResponse getAllOpenAskOrders = getAllOpenAskOrders();
        assertTrue(getAllOpenAskOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getOrder().equals(orderID)).count() == 1);
        assertTrue(getAllOpenAskOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getAsset().equals(assetID)).count() == 1);
    }

    //getAllOpenBidOrders
    @DisplayName("getAllOpenBidOrders")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAllOpenBidOrdersTest(Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "AssetBid1", "Creating Asset -> getAllAssets", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.getTransaction());
        orderID = placeBidOrder.getTransaction();
        AccountOpenAssetOrdersResponse getAllOpenBidOrders = getAllOpenBidOrders();
        assertTrue(getAllOpenBidOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getOrder().equals(orderID)).count() == 1);
        assertTrue(getAllOpenBidOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getAsset().equals(assetID)).count() == 1);

    }


    //getAllTrades
    @DisplayName("getAllTrades")
    @Test
    public void getAllTradesTest() throws IOException {
        AssetTradeResponse getAllTrades = getAllTrades();
        assertTrue(getAllTrades.getTrades().size() >= 0);
    }



    @DisplayName("getAskOrder + getAskOrderIds")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAskOrderTest(Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        Integer quantityATU = 50;
        String assetName = "Ask" + RandomStringUtils.randomAlphabetic(7);
        CreateTransactionResponse cancelorderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "issueAsset -> placeAskOrder -> getAskOrdersIds -> getAllOpenAskOrders -> getAskOrder -> cancelAskOrder -> deleteAssetShares", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        AccountCurrentAssetAskOrderIdsResponse getAskOrderIds = getAskOrderIds(assetID);
        assertTrue(getAskOrderIds.getAskOrderIds().size() == 0);


        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();

        AccountCurrentAssetAskOrderIdsResponse getAskOrderIds1 = getAskOrderIds(assetID);
        assertTrue(getAskOrderIds1.getAskOrderIds().stream().anyMatch(orderID::equals));

        AccountCurrentAssetAskOrdersResponse getAskOrders = getAskOrders(assetID);

        assertTrue(getAskOrders.getAskOrders().stream().filter(askOrders -> askOrders.getOrder().equals(orderID)).count() == 1);

        AccountOpenAssetOrdersResponse getAllOpenAskOrders = getAllOpenAskOrders();
        assertTrue(getAllOpenAskOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getOrder().equals(orderID)).count() == 1);
        assertTrue(getAllOpenAskOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getAsset().equals(assetID)).count() == 1);

        AccountAssetOrderDTO getAskOrder = getAskOrder(orderID);
        assertTrue(getAskOrder.getOrder().equals(orderID));

        cancelorderID = cancelAskOrder(wallet, orderID);
        verifyCreatingTransaction(cancelorderID);
        verifyTransactionInBlock(cancelorderID.getTransaction());

        AccountCurrentAssetAskOrderIdsResponse getAskOrderIds2 = getAskOrderIds(assetID);
        assertFalse(getAskOrderIds2.getAskOrderIds().stream().anyMatch(orderID::equals));
        assertTrue(getAskOrderIds.getAskOrderIds().size() == 0);

        AccountOpenAssetOrdersResponse getAskOrder1 = getAllOpenAskOrders();

        assertFalse(getAskOrder1.getOpenOrders().stream().filter(openOrders -> openOrders.getOrder().equals(orderID)).count() == 1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet, assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.getTransaction());
        AccountAssetDTO asset = getAsset(assetID);
        assertEquals(0, asset.getQuantityATU(), String.format("Asset: %s wasn't deleted", assetID));
    }


    //issueAsset + placeAskOrder + getAskOrders
    @DisplayName("issueAsset + placeAskOrder + getAskOrders")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAskOrders(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        Integer quantityATU = 50;
        String assetName = "ASO" + String.valueOf(new Date().getTime()).substring(7);
        CreateTransactionResponse cancelorderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "issueAsset + placeAskOrder + getAskOrders", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        log.trace("Issue Asset API PASS: assetID = " + assetID);

        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        log.trace("Place Ask Order API PASS: orderID = " + orderID);

        AccountCurrentAssetAskOrdersResponse getAskOrders = getAskOrders(assetID);

        log.trace(String.valueOf(getAskOrders.getAskOrders().stream().filter(askOrders -> askOrders.getOrder().equals(orderID)).count()));
        assertTrue(getAskOrders.getAskOrders().stream().filter(askOrders -> askOrders.getOrder().equals(orderID)).count() == 1);

    }

    //getAssetAccountCount
    @DisplayName("getAssetAccountCount")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAssetAccountCount(Wallet wallet) throws IOException {

        String assetID;

        Integer quantityATU = 50;
        String assetName = "AS" + String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "assetAccountCount API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);


        AssetsAccountsCountResponse assetAccountCount = getAssetAccountCount(assetID);

        assertTrue(assetAccountCount.getNumberOfAccounts() == 1);
        log.trace("Number of Accounts using  " + assetID + " = " + assetAccountCount.getNumberOfAccounts());


        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet, assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.getTransaction());
        assetAccountCount = getAssetAccountCount(assetID);

        log.trace("number of Accounts using  " + assetID + " = " + assetAccountCount.getNumberOfAccounts());
        assertTrue(assetAccountCount.getNumberOfAccounts() == 0);

    }


    //getAssetAccounts
    @DisplayName("getAssetAccounts")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAssetAccounts(Wallet wallet) throws IOException {
        String assetID;

        Integer quantityATU = 50;
        String assetName = "AS" + String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "getAssetAccounts API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        AccountAssetsResponse assetAccounts = getAssetAccounts(assetID);
        assertTrue(assetAccounts.getAccountAssets().stream().filter(accountAssets -> accountAssets.getAsset().contains(assetID)).count() == 1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet, assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.getTransaction());

        assetAccounts = getAssetAccounts(assetID);
        //assertTrue(Arrays.stream(getAssetAccount.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);
        assertTrue(assetAccounts.getAccountAssets().size() == 0);
    }


    //getAssetDeletes
    @DisplayName("getAssetDeletes + getExpectedAssetDeletes")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAssetDeletesTest(Wallet wallet) throws IOException {
        String assetID;

        Integer quantityATU = 50;
        String assetName = "AS" + String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "getAssetDelete API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        ExpectedAssetDeletes assetDeletes = getAssetDeletes(wallet);
        assertTrue(assetDeletes.getDeletes().stream().filter(deletes -> deletes.getAsset().contains(assetID)).count() == 0);

        AccountAssetsResponse assetAccounts = getAssetAccounts(assetID);
        assertTrue(assetAccounts.getAccountAssets().stream().filter(accountAssets -> accountAssets.getAsset().contains(assetID)).count() == 1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet, assetID, quantityATU.toString());

        ExpectedAssetDeletes getExpectedAssetDeletes = getExpectedAssetDeletes(wallet);
        assertTrue(getExpectedAssetDeletes.getDeletes().stream().filter(deletes -> deletes.getAsset().contains(assetID)).count() == 1);

        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.getTransaction());

        assetAccounts = getAssetAccounts(assetID);
        assertTrue(assetAccounts.getAccountAssets().size() == 0);

        assetDeletes = getAssetDeletes(wallet);
        assertTrue(assetDeletes.getDeletes().stream().filter(deletes -> deletes.getAsset().contains(assetID)).count() == 1);

    }


    //getAssetIds
    @DisplayName("getAssetAccounts")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    @Order(2)
    public void getAssetIdsTest() {
        AccountAssetsIdsResponse getAssetIds = getAssetIds();
        assertThat(getAssetIds.getAssetIds().size(),greaterThanOrEqualTo(0));
        AccountAssetsResponse accountAssets = getAccountAssets(getTestConfiguration().getVaultWallet());
        assertThat(accountAssets.getAccountAssets().size(),greaterThan(0));

    }

    //transferAsset
    @DisplayName("transferAsset")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void transferAsset(Wallet wallet) throws IOException {

        String assetID;
        Integer quantityATU = 50;
        String assetName = "TR" + String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "TransferAsset API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        if (!wallet.isVault()) {
            CreateTransactionResponse transferAsset = transferAsset(wallet, assetID, quantityATU, getTestConfiguration().getVaultWallet().getUser());
            verifyCreatingTransaction(transferAsset);
            verifyTransactionInBlock(transferAsset.getTransaction());

            AccountAssetDTO getAccountAssets = getAccountAssets(getTestConfiguration().getVaultWallet(),assetID);
            assertEquals(assetID,getAccountAssets.getAsset());
        } else {
            CreateTransactionResponse transferAsset = transferAsset(wallet, assetID, quantityATU, getTestConfiguration().getStandartWallet().getUser());
            verifyCreatingTransaction(transferAsset);
            verifyTransactionInBlock(transferAsset.getTransaction());

            AccountAssetDTO getAccountAssets = getAccountAssets(getTestConfiguration().getStandartWallet(),assetID);
            assertEquals(assetID,getAccountAssets.getAsset());

        }

    }


    //SMOKE API TESTING using standard TEST CASES
    @DisplayName("IssueAsset -> Place Ask Order")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceAskOrder(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "APIORDER9", "Integration Test Asset", 100);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        verifyCreatingTransaction(placeAskOrder(wallet, assetID, "99", 10));
    }

    @DisplayName("Issue Asset -> Place Ask Order -> Cancel Ask Order")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceCancelAskOrder(Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "APIASK0", "Integration Test Asset", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        verifyCreatingTransaction(cancelAskOrder(wallet, orderID));
    }


    @DisplayName("Issue Asset -> Place Bid Order")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceBidOrder(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "APIBID", "Integration Test Asset", 60);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        verifyCreatingTransaction(placeBidOrder(wallet, assetID, "99", 10));
    }

    @DisplayName("Issue Asset -> Place Bid Order -> Cancel Bid Order")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceCancelBidOrder(Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, "APIBID0", "Integration Test Asset", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet, assetID, "99", 10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.getTransaction());
        orderID = placeBidOrder.getTransaction();
        verifyCreatingTransaction(cancelBidOrder(wallet, orderID));
    }

    @DisplayName("issueAsset + getAccountAssets + deleteAssetShares")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetsDeleteTest(Wallet wallet) throws IOException {

        String assetID;
        String assetName = "AS" + String.valueOf(new Date().getTime()).substring(7);
        // String assetName = "assetName0";
        Integer quantityATU = 50;
        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "Integration Test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        AccountAssetsCountResponse getAccountAssetCount = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount.getNumberOfAssets().intValue() >= 1);
        log.trace("Number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount.getNumberOfAssets());

        AccountAssetsResponse getAccountAssets = getAccountAssets(wallet);
        assertTrue(getAccountAssets.getAccountAssets().size() >= 1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet, assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);

        verifyTransactionInBlock(deleteAssetShares.getTransaction());

        AccountAssetsCountResponse getAccountAssetCount1 = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount1.getNumberOfAssets().intValue() >= 1);
        log.trace("Number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount1.getNumberOfAssets());

    }
}
