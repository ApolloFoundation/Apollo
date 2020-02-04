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
import com.apollocurrency.aplwallet.apl.core.rest.parameter.IdParameter;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.UnsignedIntegerParameter;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.Validate;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
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
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "include additional lessors information.") @QueryParam("includeLessors") @DefaultValue("false") Boolean includeLessors,
            @Parameter(description = "include additional assets information.") @QueryParam("includeAssets") @DefaultValue("false") Boolean includeAssets,
            @Parameter(description = "include additional currency information.") @QueryParam("includeCurrencies") @DefaultValue("false") Boolean includeCurrencies,
            @Parameter(description = "include effective balance.") @QueryParam("includeEffectiveBalance") @DefaultValue("false") Boolean includeEffectiveBalance,
            @Parameter(description = "require block.") @QueryParam("requireBlock") String requireBlockStr,
            @Parameter(description = "require last block.") @QueryParam("requireLastBlock") String requireLastBlockStr
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        long accountId = RestParameters.parseAccountId(accountStr);
        Account account  = accountService.getAccount(accountId);

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

    @Path("/account/assetCount")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns the assets count.",
            description = "Returns the number of assets by account id and height.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountAssetsCountResponse.class)))
            })
    @PermitAll
    public Response getAccountAssetsCount(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "The blockchain height.") @QueryParam("height") String heightStr) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = RestParameters.parseAccountId(accountStr);
        int height = RestParameters.parseHeight(heightStr, accountService.getBlockchainHeight());

        AccountAssetsCountResponse dto = new AccountAssetsCountResponse();
        dto.setNumberOfAssets(accountAssetService.getAccountAssetCount(accountId, height));

        return response.bind(dto).build();
    }

    @Path("/account/assets")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns the account assets.",
            description = "Returns the account assets by account id and height.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountAssetDTO.class)))
            })
    @PermitAll
    //TODO: need to be adjusted to return one common response.
    // cause this GET returns two different responses (Acjjxtym countAssetDTO or AccountAssetResponse)
    // that depend on the value of the asset parameter.
    public Response getAccountAssets(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "The asset ID.") @QueryParam("asset") Long assetId,
            @Parameter(description = "The blockchain height.") @QueryParam("height") String heightStr,
            @Parameter(description = "Include asset information.") @QueryParam("includeAssetInfo") @DefaultValue("false") Boolean includeAssetInfo
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = RestParameters.parseAccountId(accountStr);
        int height = RestParameters.parseHeight(heightStr, accountService.getBlockchainHeight());
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
            summary = "Returns the blocks count.",
            description = "Returns the number of blocks by account id.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountBlocksCountResponse.class)))
            })
    @PermitAll
    public Response getAccountBlocksCount(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = RestParameters.parseAccountId(accountStr);

        AccountBlocksCountResponse dto = new AccountBlocksCountResponse();
        dto.setNumberOfBlocks(blockchain.getBlockCount(accountId));

        return response.bind(dto).build();
    }

    @Path("/account/blockIds")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns the blocks ID list.",
            description = "Returns the list of blocks ID by account id.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountBlocksCountResponse.class)))
            })
    @RolesAllowed("admin")
    public Response getAccountBlockIds(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "The timestamp." ) @QueryParam("timestamp") Integer timestampParam,
            @Parameter(description = "The first index." ) @QueryParam("firstIndex") Integer firstIndexParam,
            @Parameter(description = "The last index." ) @QueryParam("lastIndex") Integer lastIndexParam,
            @Parameter(description = "The admin password." ) @QueryParam("adminPassword") String adminPassword
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = RestParameters.parseAccountId(accountStr);
        Integer timestamp = RestParameters.parseInt(timestampParam, 0, Integer.MAX_VALUE, "timestamp");
        Integer firstIndex = RestParameters.parseInt(firstIndexParam, 0, Integer.MAX_VALUE, "firstIndex");
        Integer lastIndex = RestParameters.parseInt(lastIndexParam, 0, Integer.MAX_VALUE, "lastIndex");
        /*
        int firstIdx = Math.min(firstIndex, Integer.MAX_VALUE - API.maxRecords + 1);
        lastIndex = Math.min(lastIndex, firstIdx + API.maxRecords - 1);
         */

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
            summary = "Returns the blocks list.",
            description = "Returns the list of blocks by account id.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountBlocksCountResponse.class)))
            })
    @RolesAllowed("admin")
    public Response getAccountBlocks(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "The timestamp." ) @QueryParam("timestamp") Integer timestampParam,
            @Parameter(description = "The first index." ) @QueryParam("firstIndex") Integer firstIndexParam,
            @Parameter(description = "The last index." ) @QueryParam("lastIndex") Integer lastIndexParam,
            @Parameter(description = "The admin password." ) @QueryParam("adminPassword") String adminPassword,
            @Parameter(description = "Include transactions info" ) @QueryParam("includeTransaction") @DefaultValue("false") Boolean includeTransaction
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = RestParameters.parseAccountId(accountStr);
        Integer timestamp = RestParameters.parseInt(timestampParam, 0, Integer.MAX_VALUE, "timestamp");
        Integer firstIndex = RestParameters.parseInt(firstIndexParam, 0, Integer.MAX_VALUE, "firstIndex");
        Integer lastIndex = RestParameters.parseInt(lastIndexParam, 0, Integer.MAX_VALUE, "lastIndex");

        List<Block> blocks = accountService.getAccountBlocks(accountId, timestamp, firstIndex, lastIndex);

        AccountBlocksResponse dto = new AccountBlocksResponse();
        dto.setBlocks(accountBlockConverter.convert(blocks));

        return response.bind(dto).build();
    }

    @Path("/account/currencyCount")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns the currencies count.",
            description = "Returns the number of currencies by account id and height.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountCurrencyCountResponse.class)))
            })
    @PermitAll
    public Response getAccountCurrencyCount(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "The blockchain height." ) @QueryParam("height") String heightStr
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = RestParameters.parseAccountId(accountStr);
        int height = RestParameters.parseHeight(heightStr, accountService.getBlockchainHeight());
        Integer count = accountCurrencyService.getAccountCurrencyCount(accountId, height);

        return response.bind(new AccountCurrencyCountResponse(count)).build();
    }

    @Path("/account/currencies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns the currency list.",
            description = "Returns the list of currencies by account id and height.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountCurrencyResponse.class)))
            })
    @RolesAllowed("admin")
    public Response getAccountCurrencies(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") String accountStr,
            @Parameter(description = "The currency ID." ) @QueryParam("currency") Long currencyId,
            @Parameter(description = "The blockchain height." ) @QueryParam("height") String heightStr,
            @Parameter(description = "Include currency info" ) @QueryParam("includeCurrencyInfo") @DefaultValue("false") Boolean includeCurrencyInfo
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = RestParameters.parseAccountId(accountStr);
        int height = RestParameters.parseHeight(heightStr, accountService.getBlockchainHeight());
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
            summary = "Returns the current asked orders list.",
            description = "Returns the list of current asked orders by account id and asset.",
            tags = {"accounts"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AccountAssetDTO.class)))
            })
    @RolesAllowed("admin")
    public Response getAccountCurrentAskOrderIds(
            @Parameter(description = "The account ID.", required = true) @QueryParam("account") @Validate(required = true) AccountIdParameter accountIdParameter,
            @Parameter(description = "The asset ID.") @QueryParam("asset") @Validate IdParameter assetIdParameter,
            @Parameter(description = "The first index." ) @QueryParam("firstIndex") @Validate UnsignedIntegerParameter firstIndexParam,
            @Parameter(description = "The last index." ) @QueryParam("lastIndex") @Validate UnsignedIntegerParameter lastIndexParam,
            @Parameter(description = "The admin password." ) @QueryParam("adminPassword") String adminPassword
            ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId  = accountIdParameter.get();
        int firstIndex = firstIndexParam.get();
        int lastIndex = lastIndexParam.get();
        List<Order.Ask> ordersByAccount;
        if (assetIdParameter == null) {
            ordersByAccount = accountBalanceService.getAskOrdersByAccount(accountId, firstIndex, lastIndex);
        }else{
            ordersByAccount = accountBalanceService.getAskOrdersByAccountAsset(accountId, assetIdParameter.get(), firstIndex, lastIndex);
        }
        List<String> ordersIdList = ordersByAccount.stream().map(ask -> Long.toUnsignedString(ask.getId())).collect(Collectors.toList());

        return response.bind(new AccountCurrentAskOrderIdsResponse(ordersIdList)).build();
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
