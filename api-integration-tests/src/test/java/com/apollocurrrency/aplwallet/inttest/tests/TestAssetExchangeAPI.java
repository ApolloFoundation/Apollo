package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AliasDTO;
import com.apollocurrency.aplwallet.api.dto.AssetDTO;
import com.apollocurrency.aplwallet.api.response.*;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAssetExchangeAPI extends TestBase {

    //SMOKE API TESTING (STATUS CODE 200)
    @DisplayName("issueAsset")
    @Test
    public void issueAsset() throws IOException {
        CreateTransactionResponse issueAsset = issueAsset("APIORDER11", "issueAssettestAPI", 11);
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
    @Test
    public void getAccountAssets() throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset("setAsset", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        GetAccountAssetsResponse getAccountAssets = getAccountAssets(testConfiguration.getTestUser());
        assertTrue(getAccountAssets.accountAssets.length >= 1);

    }

    @DisplayName("getAccountAssetCount")
    @Test
    public void getAccountAssetCount() throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset("setAsset", "issueAssettestAPI", 11);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        GetAssetAccountCountResponse getAccountAssetCount = getAccountAssetCount(testConfiguration.getTestUser());
        assertTrue(getAccountAssetCount.numberOfAssets.intValue() >= 1);
        System.out.println("number of Assets on " + testConfiguration.getTestUser() + " = " + getAccountAssetCount.numberOfAssets);
    }





    //SMOKE API TESTING using standard TEST CASES
    @DisplayName("issueAsset + placeAskOrder")
    @Test
    public void issueAssetPlaceAskOrder() throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset("APIORDER9", "issueAssettestAPI", 100);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        verifyCreatingTransaction(placeAskOrder(assetID, "99",10));
    }

    @DisplayName("issueAsset + placeAskOrder + cancelAskOrder")
    @Test
    public void issueAssetPlaceCancelAskOrder() throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset("APIASK0", "Creating Asset -> placeAskOrder -> cancelAskOrder", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeAskOrder = placeAskOrder(assetID, "99",10);
        verifyCreatingTransaction(placeAskOrder);
        verifyTransactionInBlock(placeAskOrder.transaction);
        orderID = placeAskOrder.transaction;
        verifyCreatingTransaction(cancelAskOrder(orderID));
    }


    @DisplayName("issueAsset + placeBidOrder")
    @Test
    public void issueAssetPlaceBidOrder() throws IOException {
        String assetID;
        CreateTransactionResponse issueAsset = issueAsset("APIBID", "issueAsset -> placeBidOrder", 60);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        verifyCreatingTransaction(placeBidOrder(assetID, "99",10));
    }

    @DisplayName("issueAsset + placeBidOrder + cancelBidOrder")
    @Test
    public void issueAssetPlaceCancelBidOrder() throws IOException {
        String assetID;
        String orderID;
        CreateTransactionResponse issueAsset = issueAsset("APIBID0", "Creating Asset -> placeBidOrder -> cancelBidOrder", 50);
        verifyCreatingTransaction(issueAsset);
        assetID = issueAsset.transaction;
        verifyTransactionInBlock(assetID);
        CreateTransactionResponse placeBidOrder = placeBidOrder(assetID, "99",10);
        verifyCreatingTransaction(placeBidOrder);
        verifyTransactionInBlock(placeBidOrder.transaction);
        orderID = placeBidOrder.transaction;
        verifyCreatingTransaction(cancelBidOrder(orderID));
    }

    @DisplayName("getAccountAssets + Delete one of them")
    @Test
    public void getAccountAssetsDelete() throws IOException {

        GetAssetAccountCountResponse getAccountAssetCount = getAccountAssetCount(testConfiguration.getTestUser());
        assertTrue(getAccountAssetCount.numberOfAssets.intValue() >= 1);
        System.out.println("number of Assets on " + testConfiguration.getTestUser() + " = " + getAccountAssetCount.numberOfAssets);

        GetAccountAssetsResponse getAccountAssets = getAccountAssets(testConfiguration.getTestUser());
        assertTrue(getAccountAssets.accountAssets.length >= 1);

        CreateTransactionResponse deleteAssetShares = deleteAssetShares(getAccountAssets.accountAssets[0].asset, getAccountAssets.accountAssets[0].quantityATU);
        verifyCreatingTransaction(deleteAssetShares);

        verifyTransactionInBlock(deleteAssetShares.transaction);

        GetAssetAccountCountResponse getAccountAssetCount1 = getAccountAssetCount(testConfiguration.getTestUser());
        assertTrue(getAccountAssetCount1.numberOfAssets.intValue() >= 1);
        System.out.println("number of Assets on " + testConfiguration.getTestUser() + " = " + getAccountAssetCount1.numberOfAssets);

    }








}
