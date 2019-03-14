package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AliasDTO;
import com.apollocurrency.aplwallet.api.dto.AssetDTO;
import com.apollocurrency.aplwallet.api.response.*;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAssetExchangeAPI extends TestBase {

    //SMOKE API TESTING (STATUS CODE 200)
    @DisplayName("issueAsset")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void issueAsset(Wallet wallet) throws IOException {
        CreateTransactionResponse issueAsset = issueAsset(wallet,"APIORDER11", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
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

    @DisplayName("getAccountAssets")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetstTest(Wallet wallet) throws IOException {
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

    @DisplayName("getAccountAssets + Delete one of them")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAccountAssetsDeleteTest(Wallet wallet) throws IOException {

        GetAssetAccountCountResponse getAccountAssetCount = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount.numberOfAssets.intValue() >= 1);
        System.out.println("number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount.numberOfAssets);

        GetAccountAssetsResponse getAccountAssets = getAccountAssets(wallet);
        assertTrue(getAccountAssets.accountAssets.length >= 1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(wallet,getAccountAssets.accountAssets[0].asset, getAccountAssets.accountAssets[0].quantityATU);
        verifyCreatingTransaction(deleteAssetShares);

        verifyTransactionInBlock(deleteAssetShares.transaction);

        GetAssetAccountCountResponse getAccountAssetCount1 = getAccountAssetCount(wallet);
        assertTrue(getAccountAssetCount1.numberOfAssets.intValue() >= 1);
        System.out.println("number of Assets on " + wallet.getUser() + " = " + getAccountAssetCount1.numberOfAssets);

    }








}
