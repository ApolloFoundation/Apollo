package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.*;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import java.io.IOException;
import java.util.Date;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static org.junit.jupiter.api.Assertions.*;


public class TestAssetExchangeAPI extends TestBaseOld {

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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        AccountAssetsResponse getAccountAssets = getAccountAssets(wallet);
        assertTrue(getAccountAssets.getAccountAssets().size() >= 1);

    }

    @DisplayName("getAccountAssetCount")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetCountTest(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"setAsset", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        AccountAssetsCountResponse getAccountAssetCount = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount.getNumberOfAssets().intValue() >= 1);
        System.out.println("number of Assets on " + wallet.getUser()+ " = " + getAccountAssetCount.getNumberOfAssets());
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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        AccountAssetDTO getAsset = getAsset(assetID);
        assertTrue(getAsset.getName().equals(assetName),String.valueOf(getAsset.getAsset().equals(issueAsset.getTransaction())));
        assertTrue(getAsset.getAccountRS().equals(wallet.getUser()));
        System.out.println("asset = " + getAsset.getAsset() + " ; name = " + getAsset.getName() + " ;  AccountRS = " + wallet.getUser());
    }


    @DisplayName("getAccountCurrentAskOrderIds")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentAskOrderIdsTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"AskOrder0", "Creating Asset -> placeAskOrder -> getAccountCurrentAskOrderIds", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds = getAccountCurrentAskOrderIds(wallet);
        assertTrue(getAccountCurrentAskOrderIds.getAskOrderIds().stream().anyMatch(orderID::equals));

    }


    @DisplayName("getAccountCurrentBidOrderIds")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountCurrentBidOrderIdsTest(Wallet wallet) throws IOException {

        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"BidOrder1", "Creating Asset -> placeBidOrder -> getAccountCurrentBidOrderIds", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.getTransaction());
        orderID = placeBidOrder.getTransaction();
        AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds = getAccountCurrentBidOrderIds(wallet);
        assertTrue(getAccountCurrentBidOrderIds.getBidOrderIds().stream().anyMatch(orderID::equals));

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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders = getAccountCurrentAskOrders(wallet);
        assertTrue(getAccountCurrentAskOrders.getAskOrders().stream().filter(orderDTO -> orderDTO.getOrder().equals(orderID)).count()==1);

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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.getTransaction());
        orderID = placeBidOrder.getTransaction();
        AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders = getAccountCurrentBidOrders(wallet);
        assertTrue(getAccountCurrentBidOrders.getBidOrders().stream().filter(orderDTO -> orderDTO.getOrder().equals(orderID)).count()== 1);

    }

    @DisplayName("getAllAssets")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAllAssetsTest(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"GetAll", "Creating Asset -> getAllAssets", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        AssetsResponse getAllAssets = getAllAssets();
        //System.out.println(Arrays.stream(getAllAssets.assets).filter(assetDTO -> assetDTO.asset.equals(assetID)).count() >= 1);
        //System.out.println(assetID);
        assertTrue(getAllAssets.getAssets().size() >= 1); //return only first 100 assets

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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        AccountOpenAssetOrdersResponse getAllOpenAskOrders = getAllOpenAskOrders();
        assertTrue(getAllOpenAskOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getOrder().equals(orderID)).count() == 1);
        assertTrue(getAllOpenAskOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getAsset().equals(assetID)).count() == 1);
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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        System.out.println(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.getTransaction());
        orderID = placeBidOrder.getTransaction();
        System.out.println(orderID);
        AccountOpenAssetOrdersResponse getAllOpenBidOrders = getAllOpenBidOrders();
        assertTrue(getAllOpenBidOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getOrder().equals(orderID)).count() == 1);
        assertTrue(getAllOpenBidOrders.getOpenOrders().stream().filter(openOrders -> openOrders.getAsset().equals(assetID)).count() == 1);

    }


    //getAllTrades
    @DisplayName("getAllTrades")
    @Test
    public  void getAllTradesTest () throws IOException {
        AssetTradeResponse getAllTrades = getAllTrades();
        assertTrue(getAllTrades.getTrades().size() >= 0);
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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        AccountCurrentAssetAskOrderIdsResponse getAskOrderIds = getAskOrderIds(assetID);
        assertTrue(getAskOrderIds.getAskOrderIds().size() == 0);


        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        System.out.println(orderID);

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

        assertFalse(getAskOrder1.getOpenOrders().stream().filter(openOrders -> openOrders.getOrder().equals(orderID)).count()==1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.getTransaction());

        AssetsResponse getAllAssets = getAllAssets();
        assertTrue(getAllAssets.getAssets().stream().filter(assetDTO -> assetDTO.getAsset().equals(assetID)).count()== 0);

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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        System.out.println("issueAsset API PASS: assetID = " + assetID);

        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        System.out.println("placeAskOrder API PASS: orderID = " + orderID);


        AccountCurrentAssetAskOrdersResponse getAskOrders = getAskOrders(assetID);

        System.out.println(getAskOrders.getAskOrders().stream().filter(askOrders -> askOrders.getOrder().equals(orderID)).count());
        assertTrue(getAskOrders.getAskOrders().stream().filter(askOrders -> askOrders.getOrder().equals(orderID)).count() == 1);

    }

    //getAssetAccountCount
    @DisplayName("getAssetAccountCount")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAssetAccountCount (Wallet wallet) throws IOException {

        String assetID;

        Integer quantityATU = 50;
        String assetName = "AS"+String.valueOf(new Date().getTime()).substring(7);

        CreateTransactionResponse issueAsset = issueAsset(wallet, assetName, "assetAccountCount API test", quantityATU);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);


        AssetsAccountsCountResponse assetAccountCount = getAssetAccountCount(assetID);

        assertTrue(assetAccountCount.getNumberOfAccounts() == 1);
        System.out.println("number of Accounts using  " + assetID + " = " + assetAccountCount.getNumberOfAccounts());


        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.getTransaction());
        assetAccountCount = getAssetAccountCount(assetID);

        log.trace("number of Accounts using  " + assetID + " = " + assetAccountCount.getNumberOfAccounts());
        assertTrue(assetAccountCount.getNumberOfAccounts() == 0);

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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        System.out.println("assetID = " + assetID);

        AccountAssetsResponse assetAccounts = getAssetAccounts(assetID);
        assertTrue(assetAccounts.getAccountAssets().stream().filter(accountAssets -> accountAssets.getAsset().contains(assetID)).count()==1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.getTransaction());

        assetAccounts = getAssetAccounts(assetID);
        //assertTrue(Arrays.stream(getAssetAccount.accountAssets).filter(accountAssets -> accountAssets.asset.contains(assetID)).count()==1);
        assertTrue(assetAccounts.getAccountAssets().size() == 0);
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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        System.out.println("assetID = " + assetID);


        ExpectedAssetDeletes assetDeletes = getAssetDeletes(wallet);
        assertTrue(assetDeletes.getDeletes().stream().filter(deletes -> deletes.getAsset().contains(assetID)).count() == 0);

        AccountAssetsResponse assetAccounts = getAssetAccounts(assetID);
        assertTrue(assetAccounts.getAccountAssets().stream().filter(accountAssets -> accountAssets.getAsset().contains(assetID)).count()==1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());

        ExpectedAssetDeletes getExpectedAssetDeletes = getExpectedAssetDeletes(wallet);
        assertTrue(getExpectedAssetDeletes.getDeletes().stream().filter(deletes -> deletes.getAsset().contains(assetID)).count()==1);

        verifyCreatingTransaction(deleteAssetShares);
        verifyTransactionInBlock(deleteAssetShares.getTransaction());

        assetAccounts = getAssetAccounts(assetID);
        assertTrue(assetAccounts.getAccountAssets().size() == 0);

        assetDeletes = getAssetDeletes(wallet);
        assertTrue(assetDeletes.getDeletes().stream().filter(deletes -> deletes.getAsset().contains(assetID)).count()==1);

    }


    //getAssetIds
    @DisplayName("getAssetAccounts")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public  void getAssetIdsTest () throws IOException {

        AccountAssetsIdsResponse getAssetIds = getAssetIds();
        assertTrue(getAssetIds.getAssetIds().size() >= 0);

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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        System.out.println("assetID = " + assetID);

        if (!wallet.isVault()) {
            CreateTransactionResponse transferAsset = transferAsset(wallet, assetID, quantityATU, getTestConfiguration().getVaultWallet().getUser());
            verifyCreatingTransaction(transferAsset);
            verifyTransactionInBlock(transferAsset.getTransaction());

            AccountAssetsResponse getAccountAssets = getAccountAssets(getTestConfiguration().getVaultWallet());
            assertTrue(getAccountAssets.getAccountAssets().stream().filter(accountAssets -> accountAssets.getAsset().contains(assetID)).count()==1);
        }
        else {
            CreateTransactionResponse transferAsset = transferAsset(wallet, assetID, quantityATU, getTestConfiguration().getStandartWallet().getUser());
            verifyCreatingTransaction(transferAsset);
            verifyTransactionInBlock(transferAsset.getTransaction());

            AccountAssetsResponse getAccountAssets = getAccountAssets(getTestConfiguration().getStandartWallet());
            assertTrue(getAccountAssets.getAccountAssets().stream().filter(accountAssets -> accountAssets.getAsset().contains(assetID)).count()==1);

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
        assetID = issueAsset.getTransaction();
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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.getTransaction());
        orderID = placeAskOrder.getTransaction();
        verifyCreatingTransaction(cancelAskOrder(wallet,orderID));
    }


    @DisplayName("issueAsset + placeBidOrder")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void issueAssetPlaceBidOrder(Wallet wallet) throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset(wallet,"APIBID", "issueAsset -> placeBidOrder", 60);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.getTransaction();
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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(wallet,assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.getTransaction());
        orderID = placeBidOrder.getTransaction();
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
        assetID = issueAsset.getTransaction();
        verifyTransactionInBlock(assetID);

        AccountAssetsCountResponse getAccountAssetCount = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount.getNumberOfAssets().intValue() >= 1);
        System.out.println("number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount.getNumberOfAssets());

        AccountAssetsResponse getAccountAssets = getAccountAssets(wallet);
        assertTrue(getAccountAssets.getAccountAssets().size() >= 1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,assetID, quantityATU.toString());
        verifyCreatingTransaction(deleteAssetShares);

        verifyTransactionInBlock(deleteAssetShares.getTransaction());

        AccountAssetsCountResponse getAccountAssetCount1 = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount1.getNumberOfAssets().intValue() >= 1);
        System.out.println("number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount1.getNumberOfAssets());

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
