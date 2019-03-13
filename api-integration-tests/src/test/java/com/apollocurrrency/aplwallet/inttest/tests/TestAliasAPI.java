package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AliasDTO;
import com.apollocurrency.aplwallet.api.request.CreateTransactionRequestDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.GetAliasesResponse;
import com.apollocurrency.aplwallet.api.response.GetCountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.SendMoneyResponse;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAliasAPI extends TestBase {

    //Skrypchenko Serhii
    @DisplayName("Get Aliases")
    @Test
    public void getAliases() throws IOException {
        GetAliasesResponse getAliasesResponse = getAliases(testConfiguration.getTestUser());
        assertTrue(getAliasesResponse.aliases.length >= 1);
    }

    @DisplayName("Get Alias Count")
    @Test
    public void getAliasCount() throws IOException {
        GetCountAliasesResponse getAliasesCount = getAliasCount(testConfiguration.getTestUser());
        assertTrue(getAliasesCount.numberOfAliases >= 1);
    }


    @DisplayName("Get Alias")
    @Test
    public void getAlias() throws IOException {
        AliasDTO aliasDTO = getAlias("testapiautomation");
        assertTrue(aliasDTO.aliasName.length() >= 1);
    }


    @DisplayName("Set Alias")
    @Test
    public void setAlias() throws IOException {
        CreateTransactionResponse setAlias = setAlias("testapi.com", "testapiautomation", 400000000, 1400);
        assertNotNull(setAlias.transactionJSON.senderPublicKey);
        assertNotNull(setAlias.transactionJSON.signature);
        assertNotNull(setAlias.transactionJSON.fullHash);
        assertNotNull(setAlias.transactionJSON.amountATM);
        assertNotNull(setAlias.transactionJSON.ecBlockId);
        assertNotNull(setAlias.transactionJSON.senderRS);
        assertNotNull(setAlias.transactionJSON.transaction);
        assertNotNull(setAlias.transactionJSON.feeATM);
        assertNotNull(setAlias.transactionJSON.type);

    }

    @DisplayName("Delete Alias")
    @Test
    public void deleteAlias() throws IOException {
        CreateTransactionResponse deleteAlias = deleteAlias("testapiautomation");
        assertNotNull(deleteAlias.transactionJSON.senderPublicKey);
        assertNotNull(deleteAlias.transactionJSON.signature);
        assertNotNull(deleteAlias.transactionJSON.fullHash);
        assertNotNull(deleteAlias.transactionJSON.amountATM);
        assertNotNull(deleteAlias.transactionJSON.ecBlockId);
        assertNotNull(deleteAlias.transactionJSON.senderRS);
        assertNotNull(deleteAlias.transactionJSON.transaction);
        assertNotNull(deleteAlias.transactionJSON.feeATM);
        assertNotNull(deleteAlias.transactionJSON.type);
    }


    @DisplayName("Get Aliases Like")
    @Test
    public void getAliasesLike() throws IOException {
        GetAliasesResponse getAliasesLike = getAliasesLike("te");
        assertTrue(getAliasesLike.aliases.length >= 1);

    }

    @DisplayName("Sell Alias")
    @Test
    public void sellAlias() throws IOException {
        CreateTransactionResponse sellAlias = sellAlias("apitest");
        assertTrue(sellAlias.toString().length() >= 1);

    }


    @DisplayName("Buy Alias")
    @Test
    public void buyAlias() throws IOException {
        CreateTransactionResponse buyAlias = buyAlias("apitest");
        assertTrue(buyAlias.toString().length() >= 1);
    }







}