package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AssetDTO;
import com.apollocurrency.aplwallet.api.dto.OrderDTO;
import com.apollocurrency.aplwallet.api.response.*;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.function.BooleanSupplier;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static org.junit.jupiter.api.Assertions.*;


public class TestAssetExchangeAPI extends TestBase {

    //SMOKE API TESTING (STATUS CODE 200)
    @DisplayName("issueAsset")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void issueAsset(Wallet wallet) throws IOException {
        CreateTransactionResponse issueAsset = issueAsset(wallet,"APIORDER11", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
    }


    @DisplayName("getAccountAssets")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetsTest(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"setAsset", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        GetAccountAssetsResponse getAccountAssets = getAccountAssets(wallet);
        assertTrue(getAccountAssets.accountAssets.length >= 1);

    }

    @DisplayName("getAccountAssetCount")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetCountTest(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"setAsset", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        GetAssetAccountCountResponse getAccountAssetCount = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount.numberOfAssets.intValue() >= 1);
        System.out.println("number of Assets on " + wallet.getUser()+ " = " + getAccountAssetCount.numberOfAssets);
    }

    @DisplayName("getAsset")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAssetTest(Wallet wallet) throws IOException {
        String assetID;
        String assetName = "AS"+String.valueOf(new Date().getTime()).substring(7);
        String description = "description of assetName";
        Integer quantityATU = 10;
        CreateTransactionResponse issueAsset = issueAsset(wallet,assetName, description, quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
 
        AssetDTO getAsset = getAsset(assetID);   
        assertTrue(getAsset.name.equals(assetName),String.valueOf(getAsset.asset.equals(issueAsset.transaction)));
        assertTrue(getAsset.accountRS.equals(wallet.getUser()));
        System.out.println("asset = " + getAsset.asset + " ; name = " + getAsset.name + " ;  AccountRS = " + wallet.getUser());
    }


    @DisplayName("getAccountCurrentAskOrderIds")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentAskOrderIdsTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"AskOrder0", "Creating Asset -> placeAskOrder -> getAccountCurrentAskOrderIds", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.transaction);
        orderID = placeAskOrder.transaction;
        GetOrderIdsResponse getAccountCurrentAskOrderIds = getAccountCurrentAskOrderIds(wallet);
        assertTrue(Arrays.stream(getAccountCurrentAskOrderIds.askOrderIds).anyMatch(orderID::equals));

    }


    @DisplayName("getAccountCurrentBidOrderIds")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentBidOrderIdsTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"BidOrder1", "Creating Asset -> placeBidOrder -> getAccountCurrentBidOrderIds", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.transaction);
        orderID = placeBidOrder.transaction;
        GetOrderIdsResponse getAccountCurrentBidOrderIds = getAccountCurrentBidOrderIds(wallet);
        assertTrue(Arrays.stream(getAccountCurrentBidOrderIds.bidOrderIds).anyMatch(orderID::equals));

    }

    //Creating Asset -> placeAskOrder -> getAccountCurrentAskOrders
    @DisplayName("getAccountCurrentAskOrders")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentAskOrdersTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"AskOrder2", "Creating Asset -> placeAskOrder -> getAccountCurrentAskOrders", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.transaction);
        orderID = placeAskOrder.transaction;
        GetAccountCurrentOrdersResponse getAccountCurrentAskOrders = getAccountCurrentAskOrders(wallet);
        assertTrue(Arrays.stream(getAccountCurrentAskOrders.askOrders).filter(orderDTO -> orderDTO.order.equals(orderID)).count()==1);

    }

    //Creating Asset -> placeBidOrder -> getAccountCurrentBidOrders
    @DisplayName("getAccountCurrentBidOrders")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentBidOrdersTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"BidOrder2", "Creating Asset -> placeBidOrder -> getAccountCurrentBidOrders", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.transaction);
        orderID = placeBidOrder.transaction;
        GetAccountCurrentOrdersResponse getAccountCurrentBidOrders = getAccountCurrentBidOrders(wallet);
        assertTrue(Arrays.stream(getAccountCurrentBidOrders.bidOrders).filter(orderDTO -> orderDTO.order.equals(orderID)).count()==1);

    }

    @DisplayName("getAllAssets")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAllAssetsTest(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"GetAll", "Creating Asset -> getAllAssets", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        GetAllAssetsResponse getAllAssets = getAllAssets();
        //System.out.println(Arrays.stream(getAllAssets.assets).filter(assetDTO -> assetDTO.asset.equals(assetID)).count() >= 1);
        //System.out.println(assetID);
        assertTrue(getAllAssets.assets.length >= 1); //return only first 100 assets

    }


    //getAllOpenAskOrders
    @DisplayName("getAllOpenAskOrders")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAllOpenAskOrdersTest (Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"AssetOpen1", "Creating Asset -> getAllAssets", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.transaction);
        orderID = placeAskOrder.transaction;
        GetOpenOrderResponse getAllOpenAskOrders = getAllOpenAskOrders();
        assertTrue(Arrays.stream(getAllOpenAskOrders.openOrders).filter(openOrders -> openOrders.order.equals(orderID)).count()==1);
        assertTrue(Arrays.stream(getAllOpenAskOrders.openOrders).filter(openOrders -> openOrders.asset.equals(assetID)).count()==1);
    }

    //getAllOpenBidOrders
    @DisplayName("getAllOpenBidOrders")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAllOpenBidOrdersTest (Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"AssetBid1", "Creating Asset -> getAllAssets", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        System.out.println(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.transaction);
        orderID = placeBidOrder.transaction;
        System.out.println(orderID);
        GetOpenOrderResponse getAllOpenBidOrders = getAllOpenBidOrders();
        assertTrue(Arrays.stream(getAllOpenBidOrders.openOrders).filter(openOrders -> openOrders.order.equals(orderID)).count()==1);
        assertTrue(Arrays.stream(getAllOpenBidOrders.openOrders).filter(openOrders -> openOrders.asset.equals(assetID)).count()==1);

    }


    //getAllTrades
    @DisplayName("getAllTrades")
    @Test
    public  void getAllTradesTest () throws IOException {
        GetAllTradeResponse getAllTrades = getAllTrades();
        assertTrue(getAllTrades.trades.length >= 1);
    }


    //Creating Asset -> place AskOrder -> getAllOpenAskOrders -> getAskOrder -> cancelAskOrder -> deleteAssetShares
    @DisplayName("getAskOrder + getAskOrderIds")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAskOrderTest (Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        Integer quantityATU = 50;
        String assetName = "Ask"+String.valueOf(new Date().getTime()).substring(7);
        CreateTransactionResponse cancelorderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "issueAsset -> placeAskOrder -> getAskOrdersIds -> getAllOpenAskOrders -> getAskOrder -> cancelAskOrder -> deleteAssetShares", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);

        GetOrderIdsResponse getAskOrderIds = getAskOrderIds(assetID);
        assertTrue(getAskOrderIds.askOrderIds.length == 0);


        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.transaction);
        orderID = placeAskOrder.transaction;
        System.out.println(orderID);

        GetOrderIdsResponse getAskOrderIds1 = getAskOrderIds(assetID);
        assertTrue(Arrays.stream(getAskOrderIds1.askOrderIds).anyMatch(orderID::equals));

        GetOpenOrderResponse getAskOrders = getAskOrders(assetID);

        System.out.println(Arrays.stream(getAskOrders.askOrders).filter(askOrders -> askOrders.order.equals(orderID)).count());
        assertTrue(Arrays.stream(getAskOrders.askOrders).filter(askOrders -> askOrders.order.equals(orderID)).count()==1);

        GetOpenOrderResponse getAllOpenAskOrders = getAllOpenAskOrders();
        assertTrue(Arrays.stream(getAllOpenAskOrders.openOrders).filter(openOrders -> openOrders.order.equals(orderID)).count()==1);
        assertTrue(Arrays.stream(getAllOpenAskOrders.openOrders).filter(openOrders -> openOrders.asset.equals(assetID)).count()==1);

        OrderDTO getAskOrder = getAskOrder(orderID);
        assertTrue(getAskOrder.order.equals(orderID));

        cancelorderID = cancelAskOrder(wallet, orderID);
        verifyCreatingTransaction(cancelorderID);
        verifyTransactionInBlock(cancelorderID.transaction);

        GetOrderIdsResponse getAskOrderIds2 = getAskOrderIds(assetID);
        assertFalse(Arrays.stream(getAskOrderIds2.askOrderIds).anyMatch(orderID::equals));
        assertTrue(getAskOrderIds.askOrderIds.length == 0);

        GetOpenOrderResponse getAskOrder1 = getAllOpenAskOrders();
        System.out.println(Arrays.stream(getAskOrder1.openOrders).filter(openOrders -> openOrders.order.equals(orderID)).count());

        assertFalse(Arrays.stream(getAskOrder1.openOrders).filter(openOrders -> openOrders.order.equals(orderID)).count()==1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.transaction);

        GetAllAssetsResponse getAllAssets = getAllAssets();
        assertTrue(Arrays.stream(getAllAssets.assets).filter(assetDTO -> assetDTO.asset.equals(assetID)).count()== 0);

    }


    //issueAsset + placeAskOrder + getAskOrders
    @DisplayName("issueAsset + placeAskOrder + getAskOrders")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAskOrders (Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        Integer quantityATU = 50;
        String assetName = "ASO"+String.valueOf(new Date().getTime()).substring(7);
        CreateTransactionResponse cancelorderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "issueAsset + placeAskOrder + getAskOrders", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        System.out.println("issueAsset API PASS: assetID = " + assetID);

        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.transaction);
        orderID = placeAskOrder.transaction;
        System.out.println("placeAskOrder API PASS: orderID = " + orderID);


        GetOpenOrderResponse getAskOrders = getAskOrders(assetID);

        System.out.println(Arrays.stream(getAskOrders.askOrders).filter(askOrders -> askOrders.order.equals(orderID)).count());
        assertTrue(Arrays.stream(getAskOrders.askOrders).filter(askOrders -> askOrders.order.equals(orderID)).count()==1);

    }

    //getAssetAccountCount
    @DisplayName("getAssetAccountCount")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAssetAccountCount (Wallet wallet) throws IOException {

        String assetID;

        Integer quantityATU = 50;
        String assetName = "AS"+String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "getAssetAccountCount API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);


        GetAssetAccountCountResponse getAssetAccountCount = getAssetAccountCount(assetID);

        assertTrue(getAssetAccountCount.getNumberOfAccounts() == 1);
        System.out.println("number of Accounts using  " + assetID + " = " + getAssetAccountCount.getNumberOfAccounts());


        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.transaction);
        getAssetAccountCount = getAssetAccountCount(assetID);

        System.out.println("number of Accounts using  " + assetID + " = " + getAssetAccountCount.getNumberOfAccounts());
        //System.out.println("number of Accounts using  " + assetID + " = " + getAssetAccountCount.getNumberOfAccounts());
        assertTrue(getAssetAccountCount.getNumberOfAccounts() == 0);

    }


    //getAssetAccounts
    @DisplayName("getAssetAccounts")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAssetAccounts (Wallet wallet) throws IOException {
        String assetID;

        Integer quantityATU = 50;
        String assetName = "AS"+String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "getAssetAccounts API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        System.out.println("assetID = " + assetID);

        GetAccountAssetsResponse getAssetAccount = getAssetAccounts(assetID);
        System.out.println(Arrays.stream(getAssetAccount.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);
        assertTrue(Arrays.stream(getAssetAccount.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.transaction);

        getAssetAccount = getAssetAccounts(assetID);
        System.out.println(getAssetAccount.accountAssets.length);
        //assertTrue(Arrays.stream(getAssetAccount.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);
        assertTrue(getAssetAccount.accountAssets.length == 0);
    }


    //getAssetDeletes
    @DisplayName("getAssetDeletes + getExpectedAssetDeletes")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAssetDeletesTest (Wallet wallet) throws IOException {
        String assetID;

        Integer quantityATU = 50;
        String assetName = "AS"+String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "getAssetDelete API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        System.out.println("assetID = " + assetID);


        GetExpectedAssetDeletes getAssetDeletes = getAssetDeletes(wallet);
        assertTrue(Arrays.stream(getAssetDeletes.deletes).filter(deletes -> deletes.asset.contains(assetID)).count()==0);

        GetAccountAssetsResponse getAssetAccount = getAssetAccounts(assetID);
        System.out.println(Arrays.stream(getAssetAccount.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);
        assertTrue(Arrays.stream(getAssetAccount.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());

        GetExpectedAssetDeletes getExpectedAssetDeletes = getExpectedAssetDeletes(wallet);
        assertTrue(Arrays.stream(getExpectedAssetDeletes.deletes).filter(deletes -> deletes.asset.contains(assetID)).count()==1);

        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.transaction);

        getAssetAccount = getAssetAccounts(assetID);
        System.out.println(getAssetAccount.accountAssets.length);
        assertTrue(getAssetAccount.accountAssets.length == 0);

        getAssetDeletes = getAssetDeletes(wallet);
        assertTrue(Arrays.stream(getAssetDeletes.deletes).filter(deletes -> deletes.asset.contains(assetID)).count()==1);

    }


    //getAssetIds
    @DisplayName("getAssetAccounts")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAssetIdsTest () throws IOException {

        GetAssetIdsResponse getAssetIds = getAssetIds();
        assertTrue(getAssetIds.assetIds.length >= 0);

    }

    //transferAsset
    @DisplayName("transferAsset")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void transferAsset (Wallet wallet) throws IOException {

        String assetID;
        Integer quantityATU = 50;
        String assetName = "TR"+String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "transferAsset API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        System.out.println("assetID = " + assetID);

        if (wallet.getSecretKey() == null) {
            CreateTransactionResponse transferAsset = transferAsset(wallet, assetID, quantityATU, getTestConfiguration().getVaultWallet().getUser());
            verifyCreatingTransaction(transferAsset);
            verifyTransactionInBlock(transferAsset.transaction);

            GetAccountAssetsResponse getAccountAssets = getAccountAssets(getTestConfiguration().getVaultWallet());
            assertTrue(Arrays.stream(getAccountAssets.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);
        }
        else {
            CreateTransactionResponse transferAsset = transferAsset(wallet, assetID, quantityATU, getTestConfiguration().getStandartWallet().getUser());
            verifyCreatingTransaction(transferAsset);
            verifyTransactionInBlock(transferAsset.transaction);

            GetAccountAssetsResponse getAccountAssets = getAccountAssets(getTestConfiguration().getStandartWallet());
            assertTrue(Arrays.stream(getAccountAssets.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);

        }

    }




    //SMOKE API TESTING using standard TEST CASES
    @DisplayName("issueAsset + placeAskOrder")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceAskOrder(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"APIORDER9", "issueAssettestAPI", 100);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        verifyCreatingTransaction(placeAskOrder(wallet,assetID, "99",10));
    }

    @DisplayName("issueAsset + placeAskOrder + cancelAskOrder")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceCancelAskOrder(Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"APIASK0", "Creating Asset -> placeAskOrder -> cancelAskOrder", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.transaction);
        orderID = placeAskOrder.transaction;
        verifyCreatingTransaction(cancelAskOrder(wallet,orderID));
    }


    @DisplayName("issueAsset + placeBidOrder")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceBidOrder(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"APIBID", "issueAsset -> placeBidOrder", 60);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        verifyCreatingTransaction(placeBidOrder(wallet,assetID, "99",10));
    }

    @DisplayName("issueAsset + placeBidOrder + cancelBidOrder")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceCancelBidOrder(Wallet wallet) throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"APIBID0", "Creating Asset -> placeBidOrder -> cancelBidOrder", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.transaction);
        orderID = placeBidOrder.transaction;
        verifyCreatingTransaction(cancelBidOrder(wallet,orderID));
    }

    @DisplayName("issueAsset + getAccountAssets + deleteAssetShares")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetsDeleteTest(Wallet wallet) throws IOException {

        String assetID;
         String assetName = "AS"+String.valueOf(new Date().getTime()).substring(7);
       // String assetName = "assetName0";
        Integer quantityATU = 50;
        CreateTransactionResponse issueAsset = issueAsset(wallet,assetName, "Creating Asset -> getAccountAssetCount + getAccountAssets -> Delete created asset", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);

        GetAssetAccountCountResponse getAccountAssetCount = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount.numberOfAssets.intValue() >= 1);
        System.out.println("number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount.numberOfAssets);

        GetAccountAssetsResponse getAccountAssets = getAccountAssets(wallet);
        assertTrue(getAccountAssets.accountAssets.length >= 1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);

        verifyTransactionInBlock(deleteAssetShares.transaction);

        GetAssetAccountCountResponse getAccountAssetCount1 = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount1.numberOfAssets.intValue() >= 1);
        System.out.println("number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount1.numberOfAssets);

    }

    // NEED to be reviewed and refactored
    /*@DisplayName("placeBidOrder")
    @Test
    public void placeBidOrder() throws IOException {
        CreateTransactionResponse placeBidOrder = placeBidOrder("16277556351674600694", "99", 100);
        verifyCreatingTransaction(placeBidOrder);
    }

    @DisplayName("placeAskOrder")
    @Test
    public void placeAskOrder() throws IOException {
        CreateTransactionResponse placeAskOrder = placeAskOrder("16277556351674600694", "99",100);
        verifyCreatingTransaction(placeAskOrder);
    }


    @DisplayName("cancelBidOrder")
    @Test
    public void cancelBidOrder() throws IOException {
        CreateTransactionResponse cancelBidOrder = cancelBidOrder("10274755394091494068");
        verifyCreatingTransaction(cancelBidOrder);
    }

    @DisplayName("cancelAskOrder")
    @Test
    public void cancelAskOrder() throws IOException {
        CreateTransactionResponse cancelAskOrder = cancelAskOrder("10274755394091494068");
        verifyCreatingTransaction(cancelAskOrder);
    }*/

    /*@DisplayName("deleteAssetShares")
    @Test
    public void deleteAssetShares() throws IOException {
        CreateTransactionResponse deleteAssetShares = deleteAssetShares("13850145991084991260", "10");
        verifyCreatingTransaction(deleteAssetShares);
    }*/

    /*@DisplayName("dividendPayment")
    @Test
    public void dividendPayment() throws IOException {
        CreateTransactionResponse dividendPayment = dividendPayment("9065918785929852826", 100, 61449);
        verifyCreatingTransaction(dividendPayment);
    }*/






}
