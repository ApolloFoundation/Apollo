/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.RestParameters;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FAConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FADetailsConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Apollo accounts endpoint
 */
@Slf4j
@Path("/accounts")
public class AccountController {

    private static final String PARAMS2FA_NOT_FOUND_ERROR_MSG=String.format("Request attribute '%s' not found.",
                                                                             RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE);

    @Inject @Setter
    private Account2FAHelper account2FAHelper;

    @Inject @Setter
    private AccountService accountService;

    @Inject @Setter
    private AccountAssetService accountAssetService;

    @Inject @Setter
    private AccountCurrencyService accountCurrencyService;

    @Inject @Setter
    private AccountBalanceService accountBalanceService;

    @Inject @Setter
    private AccountConverter converter;

    @Inject @Setter
    private WalletKeysConverter walletKeysConverter;

    @Inject @Setter
    private Account2FADetailsConverter faDetailsConverter;

    @Inject @Setter
    private Account2FAConverter faConverter;

    @Path("/account")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns account information",
            description = "Returns account information by account id.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountDTO.class)))
            })
    @PermitAll
    public Response getAccount(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "include additional lessors information.") @QueryParam("includeLessors") @DefaultValue("false") Boolean includeLessors,
            @Parameter(description = "include additional assets information.") @QueryParam("includeAssets") @DefaultValue("false") Boolean includeAssets,
            @Parameter(description = "include additional currency information.") @QueryParam("includeCurrencies") @DefaultValue("false") Boolean includeCurrencies,
            @Parameter(description = "include effective balance.") @QueryParam("includeEffectiveBalance") @DefaultValue("false") Boolean includeEffectiveBalance,
            @Parameter(description = "require block.") @QueryParam("requireBlock") String requireBlockStr,
            @Parameter(description = "require last block.") @QueryParam("requireLastBlock") String requireLastBlockStr
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        if (accountStr == null) {
            return response.error( ApiErrors.MISSING_PARAM, "account").build();
        }

        Account account  = accountBalanceService.retrieveAccountByAccountId(accountStr);

        if (account == null) {
            return response.error( ApiErrors.UNKNOWN_VALUE, "account", accountStr).build();
        }

        AccountDTO dto = converter.convert(account);
        if (includeEffectiveBalance){
            converter.addEffectiveBalances(dto, account);
        }
        if (includeLessors){
            List<Account> lessors = accountService.getLessors(account);
            converter.addAccountLessors(dto, lessors, includeEffectiveBalance);
        }
        if(includeAssets){
            List<AccountAsset> assets = accountAssetService.getAssetAccounts(account, 0, -1);
            converter.addAccountAssets(dto, assets);
        }
        if(includeCurrencies){
            List<AccountCurrency> currencies = accountCurrencyService.getCurrencies(account);
            converter.addAccountCurrencies(dto, currencies);
        }

        return response.bind(dto).build();
    }

    @Path("/account")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Generate account Returns account information",
            description = "Returns new account, publicKey, accountRS.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = WalletKeysInfoDTO.class)))
            })
    @PermitAll
    public Response generateAccount( @Parameter(description = "The passphrase", required = true) @FormParam("passphrase") String passphrase ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        if (StringUtils.isEmpty(passphrase)){
            return response.error( ApiErrors.MISSING_PARAM, "passphrase").build();
        }
        WalletKeysInfo walletKeysInfo = account2FAHelper.generateUserWallet(passphrase);

        if (walletKeysInfo == null){
            return response.error( ApiErrors.ACCOUNT_GENERATION_ERROR).build();
        }

        WalletKeysInfoDTO dto = walletKeysConverter.convert(walletKeysInfo);

        return response.bind(dto).build();
    }

    @Path("/exportKey")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Delete account",
            description = "Delete account and Remove secret bytes from keystore.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = AccountKeyDTO.class)))
            })
    @PermitAll
    public Response exportKey( @Parameter(description = "The passphrase", required = true) @FormParam("passphrase") String passphrase,
                               @Parameter(description = "The account ID.") @FormParam("account") String accountStr) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = account2FAHelper.parse2FARequestParams(accountStr, passphrase, null);

        byte [] secretBytes = account2FAHelper.findAplSecretBytes(params2FA);

        AccountKeyDTO dto = new AccountKeyDTO(
                Long.toUnsignedString(params2FA.getAccountId()),
                Convert2.rsAccount(params2FA.getAccountId()),
                null, Convert.toHexString(secretBytes) );

        return response.bind(dto).build();
    }

    @Path("/deleteKey")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Delete account",
            description = "Delete account and Remove secret bytes from keystore.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = AccountKeyDTO.class)))
            })
    @PermitAll
    @Secured2FA
    public Response deleteKey( @Parameter(description = "The passphrase", required = true) @FormParam("passphrase") String passphrase,
                              @Parameter(description = "The account ID.") @FormParam("account") String accountStr,
                              @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") Integer code2FA,
                              @Context org.jboss.resteasy.spi.HttpRequest request ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        TwoFactorAuthParameters  params2FA = (TwoFactorAuthParameters) request.getAttribute(RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE);
        checkNotNull(params2FA, request.getUri().toString());
        account2FAHelper.validate2FAParameters(params2FA);

        KeyStoreService.Status status = account2FAHelper.deleteAccount(params2FA);

        AccountKeyDTO dto = new AccountKeyDTO(
                Long.toUnsignedString(params2FA.getAccountId()),
                Convert2.rsAccount(params2FA.getAccountId()),
                status.message, null );

        return response.bind(dto).build();
    }

    @Path("/confirm2FA")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Confirm two factor authentication",
            description = "Confirm two factor authentication.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = Account2FADTO.class)))
            })
    @Secured2FA
    @PermitAll
    public Response confirm2FA(
            @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
            @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
            @Parameter(description = "The account ID.") @FormParam("account") String accountStr,
            @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") Integer code2FA,
            @Context org.jboss.resteasy.spi.HttpRequest request
            ) {
        ResponseBuilder response = ResponseBuilder.startTiming();

        //TwoFactorAuthParameters  params2FA = account2FAHelper.verify2FA(accountStr, passphraseParam, secretPhraseParam, code2FA);
        TwoFactorAuthParameters  params2FA = (TwoFactorAuthParameters) request.getAttribute(RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE);
        checkNotNull(params2FA, request.getUri().toString());
        account2FAHelper.validate2FAParameters(params2FA);

        account2FAHelper.confirm2FA(params2FA, params2FA.getCode2FA());
        Account2FADTO dto = faConverter.convert(params2FA);

        return response.bind(dto).build();
    }


    @Path("/disable2FA")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Disable two factor authentication",
            description = "Disable two factor authentication.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = Account2FADTO.class)))
            })
    @Secured2FA
    @PermitAll
    public Response disable2FA(
            @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
            @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
            @Parameter(description = "The account ID.") @FormParam("account") String accountStr,
            @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") Integer code2FA,
            @Context org.jboss.resteasy.spi.HttpRequest request
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();

        //TwoFactorAuthParameters params2FA = account2FAHelper.verify2FA(accountStr, passphraseParam, secretPhraseParam, code2FA);
        TwoFactorAuthParameters  params2FA = (TwoFactorAuthParameters) request.getAttribute(RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE);
        checkNotNull(params2FA, request.getUri().toString());
        account2FAHelper.validate2FAParameters(params2FA);

        account2FAHelper.disable2FA(params2FA, code2FA);

        Account2FADTO dto = faConverter.convert(params2FA);

        return response.bind(dto).build();
    }

    @Path("/enable2FA")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Enable two factor authentication",
            description = "Enable two factor authentication.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = Account2FADetailsDTO.class)))
            })
    @PermitAll
    public Response enable2FA(
            @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
            @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
            @Parameter(description = "The account ID.") @FormParam("account") String accountStr
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = account2FAHelper.parse2FARequestParams(accountStr, passphraseParam, secretPhraseParam);

        TwoFactorAuthDetails twoFactorAuthDetails = account2FAHelper.enable2FA(params2FA);

        Account2FADetailsDTO dto = faDetailsConverter.convert(twoFactorAuthDetails);
        faDetailsConverter.addAccount(dto, params2FA.getAccountId());

        return response.bind(dto).build();
    }

    private void checkNotNull(TwoFactorAuthParameters params2FA, String uri){
        if (params2FA == null) {
            log.error("{} request={}", PARAMS2FA_NOT_FOUND_ERROR_MSG, uri);
            throw new RestParameterException(ApiErrors.INTERNAL_SERVER_EXCEPTION, PARAMS2FA_NOT_FOUND_ERROR_MSG);
        }
    }

}
