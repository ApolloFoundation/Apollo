/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.Account2FADTO;
import com.apollocurrency.aplwallet.api.dto.Account2FADetailsDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.AccountKeyDTO;
import com.apollocurrency.aplwallet.api.dto.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.api.response.AccountAssetsCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlockIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlocksCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlocksResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAskOrderIdsResponse;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.RestParameters;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FAConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FADetailsConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountAssetConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountBlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountCurrencyConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.AccountIdParameter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidBlockchainHeight;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Apollo accounts endpoint
 */
@NoArgsConstructor
@Slf4j
@Path("/accounts")
public class AccountController {

    private static final String PARAMS2FA_NOT_FOUND_ERROR_MSG=String.format("Request attribute '%s' not found.",
                                                                             RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE);
    @Inject @Setter
    private Blockchain blockchain;
    @Inject @Setter
    private Account2FAHelper account2FAHelper;
    @Inject @Setter
    private AccountService accountService;
    @Inject @Setter
    private AccountAssetService accountAssetService;
    @Inject @Setter
    private AccountCurrencyService accountCurrencyService;
    @Inject @Setter
    private AccountAssetConverter accountAssetConverter;
    @Inject @Setter
    private AccountCurrencyConverter accountCurrencyConverter;
    @Inject @Setter
    private AccountConverter converter;
    @Inject @Setter
    private AccountBlockConverter accountBlockConverter;
    @Inject @Setter
    private WalletKeysConverter walletKeysConverter;
    @Inject @Setter
    private Account2FADetailsConverter faDetailsConverter;
    @Inject @Setter
    private Account2FAConverter faConverter;
    @Inject @Setter
    private AccountBalanceService accountBalanceService;

    public AccountController(Blockchain blockchain,
                             Account2FAHelper account2FAHelper,
                             AccountService accountService,
                             AccountAssetService accountAssetService,
                             AccountCurrencyService accountCurrencyService,
                             AccountAssetConverter accountAssetConverter,
                             AccountCurrencyConverter accountCurrencyConverter,
                             AccountConverter converter,
                             AccountBlockConverter accountBlockConverter,
                             WalletKeysConverter walletKeysConverter,
                             Account2FADetailsConverter faDetailsConverter,
                             Account2FAConverter faConverter,
                             AccountBalanceService accountBalanceService) {

        this.blockchain = blockchain;
        this.account2FAHelper = account2FAHelper;
        this.accountService = accountService;
        this.accountAssetService = accountAssetService;
        this.accountCurrencyService = accountCurrencyService;
        this.accountAssetConverter = accountAssetConverter;
        this.accountCurrencyConverter = accountCurrencyConverter;
        this.converter = converter;
        this.accountBlockConverter = accountBlockConverter;
        this.walletKeysConverter = walletKeysConverter;
        this.faDetailsConverter = faDetailsConverter;
        this.faConverter = faConverter;
        this.accountBalanceService = accountBalanceService;
    }

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
            @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
            @Parameter(description = "include additional lessors, lessorsRS and lessorsInfo (optional)") @QueryParam("includeLessors") @DefaultValue("false") boolean includeLessors,
            @Parameter(description = "include additional assetBalances and unconfirmedAssetBalances (optional)") @QueryParam("includeAssets") @DefaultValue("false") boolean includeAssets,
            @Parameter(description = "include accountCurrencies (optional)") @QueryParam("includeCurrencies") @DefaultValue("false") boolean includeCurrencies,
            @Parameter(description = "include effectiveBalanceAPL and guaranteedBalanceATM (optional)") @QueryParam("includeEffectiveBalance") @DefaultValue("false") boolean includeEffectiveBalance
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        long accountId = accountIdParameter.get();
        Account account  = accountService.getAccount(accountId);

        if (account == null) {
            return response.error( ApiErrors.UNKNOWN_VALUE, "account", accountId).build();
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
            summary = "Generate new account and return the detail information",
            description = "Generate new account and return new account, publicKey, accountRS.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = WalletKeysInfoDTO.class)))
            })
    @PermitAll
    public Response generateAccount( @Parameter(description = "The passphrase", required = true) @FormParam("passphrase") @NotNull String passphrase ) {

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

    @Path("/account/assetCount")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get the number of assets owned by an account given the account ID.",
            description = "Return the number of assets by account id or accountRS and height.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountAssetsCountResponse.class)))
            })
    @PermitAll
    public Response getAccountAssetsCount(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The height of the blockchain to determine the asset count (optional, default is last block).") @QueryParam("height") @ValidBlockchainHeight Integer heightParam) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();
        int height = null == heightParam? -1: heightParam;

        AccountAssetsCountResponse dto = new AccountAssetsCountResponse();
        dto.setNumberOfAssets(accountAssetService.getAccountAssetCount(accountId, height));

        return response.bind(dto).build();
    }

    @Path("/account/assets")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get the account assets.",
            description = "Return the account assets by account id and height.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountAssetDTO.class)))
            })
    @PermitAll
    //TODO: need to be adjusted to return one common response.
    // This GET returns two different responses (countAssetDTO or AccountAssetResponse)
    // it depends on the value of the asset parameter.
    public Response getAccountAssets(
            @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
            @Parameter(description = "The asset ID (optional).") @QueryParam("asset") @PositiveOrZero Long assetId,
            @Parameter(description = "The height of the blockchain to determine the asset count (optional, default is last block).") @QueryParam("height") @ValidBlockchainHeight Integer heightParam,
            @Parameter(description = "Include asset information (optional).") @QueryParam("includeAssetInfo") @DefaultValue("false") boolean includeAssetInfo
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();
        int height = null == heightParam? -1: heightParam;
        if (assetId == null || assetId == 0) {
            List<AccountAsset> accountAssets = accountAssetService.getAssetAccounts(accountId, height, 0, -1);
            List<AccountAssetDTO> accountAssetDTOList = accountAssetConverter.convert(accountAssets);
            if(includeAssetInfo){
                accountAssetDTOList.forEach(dto -> accountAssetConverter.addAsset(dto, Asset.getAsset(dto.getAssetId())));
            }

            return response.bind(new AccountAssetsResponse(accountAssetDTOList)).build();
        }else{
            AccountAsset accountAsset = accountAssetService.getAsset(accountId, assetId, height);
            AccountAssetDTO dto = accountAssetConverter.convert(accountAsset);
            if(includeAssetInfo){
                accountAssetConverter.addAsset(dto, Asset.getAsset(assetId));
            }

            return response.bind(dto).build();
        }
    }

    @Path("/account/blockCount")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get the number of blocks forged by an account.",
            description = "Return the number of blocks forged by an account.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountBlocksCountResponse.class)))
            })
    @PermitAll
    public Response getAccountBlocksCount(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter
        ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();

        AccountBlocksCountResponse dto = new AccountBlocksCountResponse();
        dto.setNumberOfBlocks(blockchain.getBlockCount(accountId));

        return response.bind(dto).build();
    }

    @Path("/account/blockIds")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get the block IDs of all blocks forged by an account.",
            description = "Get the block IDs of all blocks forged (generated) by an account in reverse block height order.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountBlocksCountResponse.class)))
            })
    @RolesAllowed("admin")
    public Response getAccountBlockIds(
            @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
            @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." ) @QueryParam("timestamp") @PositiveOrZero int timestamp,
            @Parameter(description = "A zero-based index to the first block ID to retrieve (optional)." ) @QueryParam("firstIndex") @PositiveOrZero int firstIndex,
            @Parameter(description = "A zero-based index to the last block ID to retrieve (optional)." ) @QueryParam("lastIndex") @PositiveOrZero int lastIndex,
            @Parameter(description = "The admin password." ) @QueryParam("adminPassword") String adminPassword
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();

        List<Block> blocks = accountService.getAccountBlocks(accountId, timestamp, firstIndex, lastIndex);
        List<String> blockIds = blocks.stream().map(Block::getStringId).collect(Collectors.toList());

        AccountBlockIdsResponse dto = new AccountBlockIdsResponse();
        dto.setBlockIds(blockIds);

        return response.bind(dto).build();
    }

    @Path("/account/blocks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get all blocks forged (generated) by an account.",
            description = "Return all blocks forged (generated) by an account in reverse block height order.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountBlocksCountResponse.class)))
            })
    @RolesAllowed("admin")
    public Response getAccountBlocks(
            @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
            @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional)." ) @QueryParam("timestamp") @PositiveOrZero int timestamp,
            @Parameter(description = "A zero-based index to the first block ID to retrieve (optional)." ) @QueryParam("firstIndex") @PositiveOrZero int firstIndex,
            @Parameter(description = "A zero-based index to the last block ID to retrieve (optional)." ) @QueryParam("lastIndex") @PositiveOrZero int lastIndex,
            @Parameter(description = "Include transactions detail info" ) @QueryParam("includeTransaction") @DefaultValue("false") boolean includeTransaction,
            @Parameter(description = "The admin password." ) @QueryParam("adminPassword") String adminPassword
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();

        List<Block> blocks = accountService.getAccountBlocks(accountId, timestamp, firstIndex, lastIndex);

        AccountBlocksResponse dto = new AccountBlocksResponse();
        dto.setBlocks(accountBlockConverter.convert(blocks));

        return response.bind(dto).build();
    }

    @Path("/account/currencyCount")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get the number of currencies issued by a given account.",
            description = "Return the number of currencies issued by a given account and height.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountCurrencyCountResponse.class)))
            })
    @PermitAll
    public Response getAccountCurrencyCount(
            @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
            @Parameter(description = "The height of the blockchain to determine the currency count (optional, default is last block).") @QueryParam("height") @ValidBlockchainHeight Integer heightParam
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();
        int height = null == heightParam? -1: heightParam;

        Integer count = accountCurrencyService.getAccountCurrencyCount(accountId, height);

        return response.bind(new AccountCurrencyCountResponse(count)).build();
    }

    @Path("/account/currencies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get the currencies issued by a given account.",
            description = "Return the currencies issued by a given account and height.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountCurrencyResponse.class)))
            })
    @RolesAllowed("admin")
    public Response getAccountCurrencies(
            @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
            @Parameter(description = "The currency ID (optional)." ) @QueryParam("currency") @PositiveOrZero Long currencyId,
            @Parameter(description = "The height of the blockchain to determine the currencies (optional, default is last block).") @QueryParam("height") @ValidBlockchainHeight Integer heightParam,
            @Parameter(description = "Include additional currency info (optional)" ) @QueryParam("includeCurrencyInfo") @DefaultValue("false") boolean includeCurrencyInfo
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();
        int height = null == heightParam? -1: heightParam;
        if (currencyId == null || currencyId == 0) {
            List<AccountCurrency> accountCurrencies = accountCurrencyService.getCurrencies(accountId, height, 0, -1);
            List<AccountCurrencyDTO> accountCurrencyDTOList = accountCurrencyConverter.convert(accountCurrencies);
            if(includeCurrencyInfo){
                accountCurrencyDTOList.forEach(dto -> accountCurrencyConverter
                        .addCurrency(dto,
                                Currency.getCurrency(
                                        Long.parseLong(dto.getCurrency()))));
            }

            return response.bind(new AccountCurrencyResponse(accountCurrencyDTOList)).build();
        }else{
            AccountCurrency accountCurrency = accountCurrencyService.getAccountCurrency(accountId, currencyId, height);
            AccountCurrencyDTO dto = accountCurrencyConverter.convert(accountCurrency);
            if(includeCurrencyInfo){
                accountCurrencyConverter.addCurrency(dto,Currency.getCurrency(currencyId));
            }

            return response.bind(dto).build();
        }
    }

    @Path("/account/currentAskOrderIds")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get current asset order IDs given an account ID.",
            description = "Get current asset order IDs given an account ID in reverse block height order. The admin password is required.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountCurrentAskOrderIdsResponse.class)))
            })
    @RolesAllowed("admin")
    public Response getAccountCurrentAskOrderIds(
            @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
            @Parameter(description = "The asset ID.") @QueryParam("asset") @PositiveOrZero Long assetIdParam,
            @Parameter(description = "A zero-based index to the first block ID to retrieve (optional)." ) @QueryParam("firstIndex") @PositiveOrZero int firstIndex,
            @Parameter(description = "A zero-based index to the last block ID to retrieve (optional)." ) @QueryParam("lastIndex") @PositiveOrZero int lastIndex,
            @Parameter(description = "The admin password." ) @QueryParam("adminPassword") String adminPassword //need for Swagger documentation
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();
        List<Order.Ask> ordersByAccount;
        if ( assetIdParam == null || assetIdParam == 0 ) {
            ordersByAccount = accountBalanceService.getAskOrdersByAccount(accountId, firstIndex, lastIndex);
        }else{
            ordersByAccount = accountBalanceService.getAskOrdersByAccountAsset(accountId, assetIdParam, firstIndex, lastIndex);
        }
        List<String> ordersIdList = ordersByAccount.stream().map(ask -> Long.toUnsignedString(ask.getId())).collect(Collectors.toList());

        return response.bind(new AccountCurrentAskOrderIdsResponse(ordersIdList)).build();
    }
/* check below ============================== */
    @Path("/exportKey")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Export the public key associated with an account ID.",
            description = "Export the public key associated with an account ID.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "text/html",
                                    schema = @Schema(implementation = AccountKeyDTO.class)))
            })
    @PermitAll
    public Response exportKey( @Parameter(description = "The secret passphrase of the account.", required = true) @FormParam("passphrase") @NotNull String passphrase,
                               @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        accountIdParameter.get();
        TwoFactorAuthParameters params2FA = account2FAHelper.parse2FARequestParams(accountIdParameter.getRawData(), passphrase, null);

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
