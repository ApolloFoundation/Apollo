package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AliasDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.GetAliasesResponse;
import com.apollocurrency.aplwallet.api.response.GetCountAliasesResponse;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestAliasAPI extends TestBase {

    //Skrypchenko Serhii
    @DisplayName("setAlias + getAliasesCount + Get Aliases")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    @Disabled
    public void getAliasesTest(Wallet wallet) throws IOException {
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet,"testapi.com", "setAliasAPI"+new Date().getTime(), 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        alias = setAlias.transaction;
        verifyTransactionInBlock(alias);
        GetCountAliasesResponse getAliasesCount = getAliasCount(wallet);
        assertTrue(getAliasesCount.numberOfAliases >= 1);
        System.out.println(getAliasesCount.numberOfAliases);
        GetAliasesResponse getAliasesResponse = getAliases(wallet);
        assertTrue(Arrays.stream(getAliasesResponse.aliases).filter(aliasDTO -> aliasDTO.alias.equals(alias)).count()==1);

    }

    @DisplayName("Get Alias Count")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAliasCountTest(Wallet wallet) throws IOException {
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet,"testapi.com", "setAliasAPI"+new Date().getTime(), 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        alias = setAlias.transaction;
        verifyTransactionInBlock(alias);
        GetCountAliasesResponse getAliasesCount = getAliasCount(wallet);
        assertTrue(getAliasesCount.numberOfAliases >= 1);
        System.out.println(getAliasesCount.numberOfAliases);
    }


    @DisplayName("setAlias + Get Alias")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAlias(Wallet wallet) throws IOException {
        String aliasname = "setAliasAPI"+new Date().getTime();
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet,"testapi.com", aliasname, 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        alias = setAlias.transaction;
        verifyTransactionInBlock(alias);
        AliasDTO aliasDTO = getAlias(aliasname);
        assertTrue(Arrays.stream(new String[]{aliasDTO.aliasName}).anyMatch(aliasname::equals));
    }


    @DisplayName("Set Alias")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void setAliasTest(Wallet wallet) throws IOException {
        CreateTransactionResponse setAlias = setAlias(wallet,"testapi.com", "setAlias"+new Date().getTime(), 400000000, 1400);
        verifyCreatingTransaction(setAlias);


    }

   // @Disabled
    @DisplayName("Delete Alias")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void deleteAliasTest(Wallet wallet) throws IOException {
        String aliasname = "setAliasAPI"+String.valueOf(new Date().getTime()).substring(7);
        String aliasset;
        String aliasdelete;
        CreateTransactionResponse setAlias = setAlias(wallet,"testapi.com", aliasname, 1000000000, 1400);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.transaction;
        verifyTransactionInBlock(aliasset);
        AliasDTO getAlias = getAlias(aliasname);
        assertTrue(Arrays.stream(new String[]{getAlias.aliasName}).anyMatch(aliasname::equals));
        CreateTransactionResponse deleteAlias = deleteAlias(wallet, aliasname);
        verifyCreatingTransaction(deleteAlias);
        aliasdelete = deleteAlias.transaction;
        verifyTransactionInBlock(aliasdelete);

        GetAliasesResponse getAliasesResponse = getAliases(wallet);
        assertFalse(Arrays.stream(getAliasesResponse.aliases).filter(aliasDTO -> aliasDTO.alias.equals(aliasname)).count()==1);
    }


    @DisplayName("Get Aliases Like")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void getAliasesLikeTest(Wallet wallet) throws IOException {
        String aliasname = "Alias"+new Date().getTime();
        String aliasset;
        String aliassearch = "Alias";

        CreateTransactionResponse setAlias = setAlias(wallet,"testapi.com", aliasname, 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.transaction;
        verifyTransactionInBlock(aliasset);
        GetAliasesResponse getAliasesLike = getAliasesLike(aliasname);
        //assertTrue(Arrays.stream(getAliasesLike.aliases)).anyMatch(aliasname::equals));
        assertTrue(Arrays.stream(getAliasesLike.aliases).filter(aliasDTO -> aliasDTO.aliasName.contains(aliassearch)).count()>=1);


    }

    @DisplayName("setAlias + sellAlias + buyAlias")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void sellAlias(Wallet wallet) throws IOException {
        String aliasname = "Alias"+new Date().getTime();
        String aliasset;
        String aliassell;
        CreateTransactionResponse setAlias = setAlias(wallet,"testapi.com", aliasname, 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.transaction;
        verifyTransactionInBlock(aliasset);

        CreateTransactionResponse sellAlias = sellAlias(wallet,aliasname);
        assertTrue(sellAlias.toString().length() >= 1);
        verifyCreatingTransaction(sellAlias);
        aliassell = sellAlias.transaction;
        verifyTransactionInBlock(aliassell);


        CreateTransactionResponse buyAlias = buyAlias(wallet, aliasname);
        assertTrue(buyAlias.toString().length() >= 1);

    }


    @DisplayName("NEGATIVE: buyAlias if ALIAS is not for sale at the moment")
    @Tag("NEGATIVE")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void buyAlias(Wallet wallet) throws IOException {
        String aliasname = "AlS"+String.valueOf(new Date().getTime()).substring(7);
        Date date = new Date();
        System.out.println(date.getTime());
        System.out.println(String.valueOf(date.getTime()).substring(7));
        String aliasset;


        CreateTransactionResponse setAlias = setAlias(wallet,"testapi.com", aliasname, 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.transaction;
        verifyTransactionInBlock(aliasset);
        System.out.println(aliasname);


        CreateTransactionResponse buyAlias = buyAlias(wallet,aliasname);
        assertTrue(buyAlias.errorDescription.contains("alias is not for sale at the moment"),buyAlias.errorDescription);
        assertTrue(buyAlias.errorCode.compareTo(new Long(4)) == 0);

        CreateTransactionResponse deleteAlias = deleteAlias(wallet, aliasname);
        verifyCreatingTransaction(deleteAlias);

        verifyTransactionInBlock(deleteAlias.transaction);

    }







}