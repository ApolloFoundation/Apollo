package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AccountAliasDTO;
import com.apollocurrency.aplwallet.api.response.AccountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountCountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Alias")
@Epic(value = "Alias")
public class TestAliasAPI extends TestBaseOld {


    @DisplayName("SetAlias -> GetAliasesCount -> Get Aliases")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAliasesTest(Wallet wallet) throws IOException {
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", "ITest" + new Date().getTime(), 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        alias = setAlias.getTransaction();
        verifyTransactionInBlock(alias);
        AccountCountAliasesResponse getAliasesCount = getAliasCount(wallet);
        assertTrue(getAliasesCount.getNumberOfAliases() >= 1);
        AccountAliasesResponse accountAliasesResponse = getAliases(wallet);
        assertTrue(accountAliasesResponse.getAliases().stream().filter(aliasDTO -> aliasDTO.getAlias().equals(alias)).count() == 1);

    }

    @DisplayName("Get Alias Count")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAliasCountTest(Wallet wallet) throws IOException {
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", "setAliasAPI" + new Date().getTime(), 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        alias = setAlias.getTransaction();
        verifyTransactionInBlock(alias);
        AccountCountAliasesResponse getAliasesCount = getAliasCount(wallet);
        assertTrue(getAliasesCount.getNumberOfAliases() >= 1);
    }


    @DisplayName("Set Alias -> Get Alias")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAlias(Wallet wallet) throws IOException {
        String aliasname = "setAliasAPI" + new Date().getTime();
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname, 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        alias = setAlias.getTransaction();
        verifyTransactionInBlock(alias);
        AccountAliasDTO aliasDTO = getAlias(aliasname);
        assertTrue(Arrays.asList(new String[]{aliasDTO.getAliasName()}).contains(aliasname));
    }


    @DisplayName("Set Alias")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void setAliasTest(Wallet wallet) throws IOException {
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", "setAlias" + new Date().getTime(), 400000000, 1400);
        verifyCreatingTransaction(setAlias);


    }

    // @Disabled
    @DisplayName("Delete Alias")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void deleteAliasTest(Wallet wallet) throws IOException {
        String aliasname = "setAliasAPI" + String.valueOf(new Date().getTime()).substring(7);
        String aliasset;
        String aliasdelete;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname, 1000000000, 1400);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.getTransaction();
        verifyTransactionInBlock(aliasset);
        AccountAliasDTO getAlias = getAlias(aliasname);
        assertTrue(Arrays.asList(new String[]{getAlias.getAliasName()}).contains(aliasname));
        CreateTransactionResponse deleteAlias = deleteAlias(wallet, aliasname);
        verifyCreatingTransaction(deleteAlias);
        aliasdelete = deleteAlias.getTransaction();
        verifyTransactionInBlock(aliasdelete);

        AccountAliasesResponse accountAliasesResponse = getAliases(wallet);
        assertFalse(accountAliasesResponse.getAliases().stream().filter(aliasDTO -> aliasDTO.getAlias().equals(aliasname)).count() == 1);
    }


    @DisplayName("Get Aliases Like")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAliasesLikeTest(Wallet wallet) throws IOException {
        String aliasname = "Alias" + new Date().getTime();
        String aliasset;
        String aliassearch = "Alias";

        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname, 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.getTransaction();
        verifyTransactionInBlock(aliasset);
        AccountAliasesResponse getAliasesLike = getAliasesLike(aliasname);
        //assertTrue(Arrays.stream(getAliasesLike.aliases)).anyMatch(aliasname::equals));
        assertTrue(getAliasesLike.getAliases().stream().filter(aliasDTO -> aliasDTO.getAliasName().contains(aliassearch)).count() >= 1);


    }

    @DisplayName("setAlias + sellAlias + buyAlias")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void sellAlias(Wallet wallet) throws IOException {
        String aliasname = "Alias" + new Date().getTime();
        String aliasset;
        String aliassell;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname, 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.getTransaction();
        verifyTransactionInBlock(aliasset);

        CreateTransactionResponse sellAlias = sellAlias(wallet, aliasname);
        assertTrue(sellAlias.toString().length() >= 1);
        verifyCreatingTransaction(sellAlias);
        aliassell = sellAlias.getTransaction();
        verifyTransactionInBlock(aliassell);


        CreateTransactionResponse buyAlias = buyAlias(wallet, aliasname);
        assertTrue(buyAlias.toString().length() >= 1);

    }


    @DisplayName("NEGATIVE: buyAlias if ALIAS is not for sale at the moment")
    @Tag("NEGATIVE")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void buyAlias(Wallet wallet) throws IOException {
        String aliasname = "AlS" + String.valueOf(new Date().getTime()).substring(7);
        Date date = new Date();
        String aliasset;


        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname, 400000000, 1400);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.getTransaction();
        verifyTransactionInBlock(aliasset);


        CreateTransactionResponse buyAlias = buyAlias(wallet, aliasname);
        assertTrue(buyAlias.errorDescription.contains("alias is not for sale at the moment"), buyAlias.errorDescription);
        assertTrue(buyAlias.errorCode.compareTo(new Long(4)) == 0);

        CreateTransactionResponse deleteAlias = deleteAlias(wallet, aliasname);
        verifyCreatingTransaction(deleteAlias);

        verifyTransactionInBlock(deleteAlias.getTransaction());

    }


}