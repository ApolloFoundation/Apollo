package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AccountAliasDTO;
import com.apollocurrency.aplwallet.api.response.AccountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountCountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.providers.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Alias")
@Epic(value = "Alias")
public class TestAliasAPI extends TestBaseNew {

    @DisplayName("SetAlias -> GetAliasesCount -> Get Aliases")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAliasesTest(Wallet wallet) throws IOException {
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", "ITest" + new Date().getTime());
        verifyCreatingTransaction(setAlias);
        alias = setAlias.getTransaction();
        verifyTransactionInBlock(alias);
        AccountCountAliasesResponse getAliasesCount = getAliasCount(wallet);
        assertThat(getAliasesCount.getNumberOfAliases(),greaterThan(1L));
        AccountAliasesResponse accountAliasesResponse = getAliases(wallet);
        assertThat(accountAliasesResponse.getAliases().size() ,greaterThanOrEqualTo(1));

    }

    @DisplayName("Get Alias Count")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAliasCountTest(Wallet wallet) throws IOException {
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", "setAliasAPI" + new Date().getTime());
        verifyCreatingTransaction(setAlias);
        alias = setAlias.getTransaction();
        verifyTransactionInBlock(alias);
        AccountCountAliasesResponse getAliasesCount = getAliasCount(wallet);
        assertTrue(getAliasesCount.getNumberOfAliases() >= 1);
    }


    @DisplayName("Set Alias -> Get Alias")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAlias(Wallet wallet) {
        String aliasname = "setAliasAPI" + new Date().getTime();
        String alias;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname);
        verifyCreatingTransaction(setAlias);
        alias = setAlias.getTransaction();
        verifyTransactionInBlock(alias);
        AccountAliasDTO aliasDTO = getAlias(aliasname);
        assertTrue(Arrays.asList(new String[]{aliasDTO.getAliasName()}).contains(aliasname));
    }


    @DisplayName("Set Alias")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void setAliasTest(Wallet wallet) {
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", "setAlias" + new Date().getTime());
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
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname);
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

        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.getTransaction();
        verifyTransactionInBlock(aliasset);
        AccountAliasesResponse getAliasesLike = getAliasesLike(aliasname);
        assertTrue(getAliasesLike.getAliases().stream().filter(aliasDTO -> aliasDTO.getAliasName().contains(aliassearch)).count() >= 1);


    }

    @DisplayName("SetAlias -> SellAlias -> BuyAlias")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void sellAlias(Wallet wallet) throws IOException {
        String aliasname = "Alias" + new Date().getTime();
        String aliasset;
        String aliassell;
        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname);
        verifyCreatingTransaction(setAlias);
        aliasset = setAlias.getTransaction();
        verifyTransactionInBlock(aliasset);

        CreateTransactionResponse sellAlias = sellAlias(wallet, aliasname,1500000000);
        assertTrue(sellAlias.toString().length() >= 1);
        verifyCreatingTransaction(sellAlias);
        aliassell = sellAlias.getTransaction();
        verifyTransactionInBlock(aliassell);


        CreateTransactionResponse buyAlias = buyAlias(wallet, aliasname,1500000000);
        assertTrue(buyAlias.toString().length() >= 1);

    }


    @DisplayName("NEGATIVE: buyAlias if ALIAS is not for sale at the moment")
    @Tag("NEGATIVE")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void buyAlias(Wallet wallet) throws IOException {
        String aliasname = "AlS" + String.valueOf(new Date().getTime()).substring(7);
        String aliasset;

        CreateTransactionResponse setAlias = setAlias(wallet, "testapi.com", aliasname);
        verifyCreatingTransaction(setAlias);

        aliasset = setAlias.getTransaction();
        verifyTransactionInBlock(aliasset);

        CreateTransactionResponse buyAlias = buyAlias(wallet, aliasname,1);
        assertTrue(buyAlias.errorDescription.contains("alias is not for sale at the moment"), buyAlias.errorDescription);

        assertTrue(buyAlias.errorCode.compareTo(new Long(4)) == 0);

        CreateTransactionResponse deleteAlias = deleteAlias(wallet, aliasname);
        verifyCreatingTransaction(deleteAlias);

        verifyTransactionInBlock(deleteAlias.getTransaction());

    }


}