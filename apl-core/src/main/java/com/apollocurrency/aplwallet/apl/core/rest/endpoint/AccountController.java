/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.account.Account2FADTO;
import com.apollocurrency.aplwallet.api.dto.account.Account2FADetailsDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountKeyDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountsCountDto;
import com.apollocurrency.aplwallet.api.dto.account.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.api.response.AccountAssetsCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlockIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlocksCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAskOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountNotFoundResponse;
import com.apollocurrency.aplwallet.api.response.BlocksResponse;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.model.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.model.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FAConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FADetailsConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountAssetConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountCurrencyConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.AccountIdParameter;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.FirstLastIndexBeanParam;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.LongParameter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountStatisticsService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidBlockchainHeight;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidTimestamp;
import com.apollocurrency.aplwallet.apl.core.service.appdata.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Apollo accounts endpoint
 */
@OpenAPIDefinition(info = @Info(description = "Provide methods to operate with accounts"))
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
@NoArgsConstructor
@Slf4j
@Path("/accounts")
@Setter
public class AccountController {

    public static int maxAPIFetchRecords;
    private Blockchain blockchain;
    private Account2FAHelper account2FAHelper;
    private AccountService accountService;
    private AccountPublicKeyService accountPublicKeyService;
    private AccountAssetService accountAssetService;
    private AccountCurrencyService accountCurrencyService;
    private AccountAssetConverter accountAssetConverter;
    private AccountCurrencyConverter accountCurrencyConverter;
    private AccountConverter converter;
    private BlockConverter blockConverter;
    private WalletKeysConverter walletKeysConverter;
    private Account2FADetailsConverter faDetailsConverter;
    private Account2FAConverter faConverter;
    private OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderService;
    private AccountStatisticsService accountStatisticsService;
    private AssetService assetService;
    private CurrencyService currencyService;

    @Inject
    public AccountController(Blockchain blockchain,
                             Account2FAHelper account2FAHelper,
                             AccountService accountService,
                             AccountPublicKeyService accountPublicKeyService,
                             AccountAssetService accountAssetService,
                             AccountCurrencyService accountCurrencyService,
                             AccountAssetConverter accountAssetConverter,
                             AccountCurrencyConverter accountCurrencyConverter,
                             AccountConverter converter,
                             BlockConverter blockConverter,
                             WalletKeysConverter walletKeysConverter,
                             Account2FADetailsConverter faDetailsConverter,
                             Account2FAConverter faConverter,
                             @AskOrderService OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderService,
                             @Property(name = "apl.maxAPIRecords", defaultValue = "100") int maxAPIrecords,
                             AccountStatisticsService accountStatisticsService,
                             AssetService assetService,
                             CurrencyService currencyService) {

        this.blockchain = blockchain;
        this.account2FAHelper = account2FAHelper;
        this.accountService = accountService;
        this.accountPublicKeyService = accountPublicKeyService;
        this.accountAssetService = accountAssetService;
        this.accountCurrencyService = accountCurrencyService;
        this.accountAssetConverter = accountAssetConverter;
        this.accountCurrencyConverter = accountCurrencyConverter;
        this.converter = converter;
        this.blockConverter = blockConverter;
        this.walletKeysConverter = walletKeysConverter;
        this.faDetailsConverter = faDetailsConverter;
        this.faConverter = faConverter;
        this.orderService = Objects.requireNonNull(orderService, "orderService is NULL");
        maxAPIFetchRecords = maxAPIrecords;
        this.accountStatisticsService = accountStatisticsService;
        this.assetService = assetService;
        this. currencyService =  currencyService;
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
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "include additional lessors, lessorsRS and lessorsInfo (optional)")
        @QueryParam("includeLessors") @DefaultValue("false") boolean includeLessors,
        @Parameter(description = "include additional assetBalances and unconfirmedAssetBalances (optional)")
        @QueryParam("includeAssets") @DefaultValue("false") boolean includeAssets,
        @Parameter(description = "include accountCurrencies (optional)") @QueryParam("includeCurrencies")
        @DefaultValue("false") boolean includeCurrencies,
        @Parameter(description = "include effectiveBalanceAPL and guaranteedBalanceATM (optional)")
        @QueryParam("includeEffectiveBalance") @DefaultValue("false") boolean includeEffectiveBalance
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        long accountId = accountIdParameter.get();
        Account account = accountService.getAccount(accountId);

        if (account == null) {
            AccountNotFoundResponse accountErrorResponse = new AccountNotFoundResponse(
                ResponseBuilder.createErrorResponse(
                    ApiErrors.UNKNOWN_VALUE,
                    null,
                    "account", accountId));
            accountErrorResponse.setAccount(Long.toUnsignedString(accountId));
            accountErrorResponse.setAccountRS(Convert2.rsAccount(accountId));
            accountErrorResponse.set2FA(account2FAHelper.isEnabled2FA(accountId));
            return response.error(accountErrorResponse).build();
        }

        if (account.getPublicKey() == null) {
            PublicKey pKey = accountPublicKeyService.getPublicKey(account.getId());
            account.setPublicKey(pKey);
        }

        AccountDTO dto = converter.convert(account);
        if (includeEffectiveBalance) {
            converter.addEffectiveBalances(dto, account);
        }
        if (includeLessors) {
            List<Account> lessors = accountService.getLessors(account);
            converter.addAccountLessors(dto, lessors, includeEffectiveBalance);
        }
        if (includeAssets) {
            List<AccountAsset> assets = accountAssetService.getAssetsByAccount(account, 0, -1);
            converter.addAccountAssets(dto, assets);
        }
        if (includeCurrencies) {
            List<AccountCurrency> currencies = accountCurrencyService.getCurrenciesByAccount(account);
            converter.addAccountCurrencies(dto, currencies);
        }

        return response.bind(dto).build();
    }

    @Path("/account")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "Generate new vault account and return the detail information",
        description = "Generate new vault account on current node and return new account, publicKey, accountRS.",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "text/html",
                    schema = @Schema(implementation = WalletKeysInfoDTO.class)))
        })
    @PermitAll
    public Response generateAccount(@Parameter(description = "The passphrase") @FormParam("passphrase") String passphrase) {

        ResponseBuilder response = ResponseBuilder.startTiming();

        WalletKeysInfo walletKeysInfo = account2FAHelper.generateUserWallet(passphrase);

        if (walletKeysInfo == null) {
            return response.error(ApiErrors.ACCOUNT_GENERATION_ERROR).build();
        }

        WalletKeysInfoDTO dto = walletKeysConverter.convert(walletKeysInfo);

        return response.bind(dto).build();
    }

    @Path("/asset-count")
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
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The height of the blockchain to determine the asset count (optional, default is last block).")
        @QueryParam("height") @DefaultValue("-1") @ValidBlockchainHeight int height) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();

        AccountAssetsCountResponse dto = new AccountAssetsCountResponse();
        dto.setNumberOfAssets(accountAssetService.getCountByAccount(accountId, height));

        return response.bind(dto).build();
    }

    @Path("/assets")
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
    //TODO: need to be refactored to return one common response.
    // This GET returns two different responses (countAssetDTO or AccountAssetResponse)
    // it depends on the value of the asset parameter.
    public Response getAccountAssets(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The asset ID (optional).")
        @QueryParam("asset") LongParameter assetId,
        @Parameter(description = "The height of the blockchain to determine the asset count (optional, default is last block).")
        @QueryParam("height") @DefaultValue("-1") @ValidBlockchainHeight int height,
        @Parameter(description = "Include asset information (optional).")
        @QueryParam("includeAssetInfo") @DefaultValue("false") boolean includeAssetInfo,
        @Parameter(description = "A zero-based index to the first, last asset ID to retrieve (optional).", schema = @Schema(implementation = FirstLastIndexBeanParam.class))
        @BeanParam FirstLastIndexBeanParam indexBeanParam
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();

        indexBeanParam.adjustIndexes(maxAPIFetchRecords);

        if (assetId == null || assetId.get() == 0) {
            List<AccountAsset> accountAssets = accountAssetService.getAssetsByAccount(accountId, height,
                indexBeanParam.getFirstIndex(),
                indexBeanParam.getLastIndex());
            List<AccountAssetDTO> accountAssetDTOList = accountAssetConverter.convert(accountAssets);
            if (includeAssetInfo) {
                accountAssetDTOList.forEach(dto -> accountAssetConverter.addAsset(dto, assetService.getAsset(dto.getAssetId())));
            }

            return response.bind(new AccountAssetsResponse(accountAssetDTOList)).build();
        } else {
            AccountAsset accountAsset = accountAssetService.getAsset(accountId, assetId.get(), height);
            AccountAssetDTO dto = accountAssetConverter.convert(accountAsset);
            if (dto != null) {
                if (includeAssetInfo) {
                    accountAssetConverter.addAsset(dto, assetService.getAsset(assetId.get()));
                }

                return response.bind(dto).build();
            } else {
                return response.emptyResponse();
            }
        }
    }

    @Path("/block-count")
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
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();

        AccountBlocksCountResponse dto = new AccountBlocksCountResponse();
        dto.setNumberOfBlocks(blockchain.getBlockCount(accountId));

        return response.bind(dto).build();
    }

    @Path("/block-ids")
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
    @PermitAll
    public Response getAccountBlockIds(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional).")
        @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp,
        @Parameter(description = "A zero-based index to the first, last asset ID to retrieve (optional).", schema = @Schema(implementation = FirstLastIndexBeanParam.class))
        @BeanParam FirstLastIndexBeanParam indexBeanParam
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();
        indexBeanParam.adjustIndexes(maxAPIFetchRecords);

        List<Block> blocks = accountService.getAccountBlocks(accountId, indexBeanParam.getFirstIndex(), indexBeanParam.getLastIndex(), timestamp);
        List<String> blockIds = blocks.stream().map(Block::getStringId).collect(Collectors.toList());

        AccountBlockIdsResponse dto = new AccountBlockIdsResponse();
        dto.setBlockIds(blockIds);

        return response.bind(dto).build();
    }

    @Path("/blocks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get all blocks forged (generated) by an account.",
        description = "Return all blocks forged (generated) by an account in reverse block height order.",
        security = @SecurityRequirement(name = "admin_api_key"),
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BlocksResponse.class)))
        })
    @PermitAll
    public Response getAccountBlocks(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The earliest block (in seconds since the genesis block) to retrieve (optional).")
        @QueryParam("timestamp") @DefaultValue("-1") @ValidTimestamp int timestamp,
        @Parameter(description = "A zero-based index to the first, last asset ID to retrieve (optional).", schema = @Schema(implementation = FirstLastIndexBeanParam.class))
        @BeanParam FirstLastIndexBeanParam indexBeanParam,
        @Parameter(description = "Include transactions detail info")
        @QueryParam("includeTransaction") @DefaultValue("false") boolean includeTransaction
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();
        indexBeanParam.adjustIndexes(maxAPIFetchRecords);

        List<Block> blocks = accountService.getAccountBlocks(accountId, indexBeanParam.getFirstIndex(), indexBeanParam.getLastIndex(), timestamp);

        BlocksResponse dto = new BlocksResponse();
        dto.setBlocks(blockConverter.convert(blocks));

        return response.bind(dto).build();
    }

    @Path("/currency-count")
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
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The height of the blockchain to determine the currency count (optional, default is last block).")
        @QueryParam("height") @DefaultValue("-1") @ValidBlockchainHeight int height
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();

        Integer count = accountCurrencyService.getCountByAccount(accountId, height);

        return response.bind(new AccountCurrencyCountResponse(count)).build();
    }

    @Path("/currencies")
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
    @PermitAll
    public Response getAccountCurrencies(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The currency ID (optional).")
        @QueryParam("currency") LongParameter currencyId,
        @Parameter(description = "The height of the blockchain to determine the currencies (optional, default is last block).")
        @QueryParam("height") @DefaultValue("-1") @ValidBlockchainHeight int height,
        @Parameter(description = "Include additional currency info (optional)")
        @QueryParam("includeCurrencyInfo") @DefaultValue("false") boolean includeCurrencyInfo,
        @Parameter(description = "A zero-based index to the first, last asset ID to retrieve (optional).", schema = @Schema(implementation = FirstLastIndexBeanParam.class))
        @BeanParam FirstLastIndexBeanParam indexBeanParam
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();
        indexBeanParam.adjustIndexes(maxAPIFetchRecords);

        if (currencyId == null || currencyId.get() == 0) {
            List<AccountCurrency> accountCurrencies = accountCurrencyService.getCurrenciesByAccount(
                accountId, height, indexBeanParam.getFirstIndex(), indexBeanParam.getLastIndex());
            List<AccountCurrencyDTO> accountCurrencyDTOList = accountCurrencyConverter.convert(accountCurrencies);
            if (includeCurrencyInfo) {
                accountCurrencyDTOList.forEach(dto -> accountCurrencyConverter
                    .addCurrency(dto,
                        currencyService.getCurrency(
                            Convert.parseLong(dto.getCurrency()))));
            }

            return response.bind(new AccountCurrencyResponse(accountCurrencyDTOList)).build();
        } else {
            AccountCurrency accountCurrency = accountCurrencyService.getAccountCurrency(accountId, currencyId.get(), height);
            AccountCurrencyDTO dto = accountCurrencyConverter.convert(accountCurrency);
            if (dto != null) {
                if (includeCurrencyInfo) {
                    accountCurrencyConverter.addCurrency(dto, currencyService.getCurrency(currencyId.get()));
                }
                return response.bind(dto).build();
            } else {
                return response.emptyResponse();
            }
        }
    }

    @Path("/current-ask-order-ids")
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
    @PermitAll
    public Response getAccountCurrentAskOrderIds(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class)) @QueryParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The asset ID.") @QueryParam("asset") LongParameter assetId,
        @Parameter(description = "A zero-based index to the first, last asset ID to retrieve (optional).", schema = @Schema(implementation = FirstLastIndexBeanParam.class))
        @BeanParam FirstLastIndexBeanParam indexBeanParam
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();
        indexBeanParam.adjustIndexes(maxAPIFetchRecords);

        Stream<AskOrder> ordersByAccount;
        if (assetId == null || assetId.get() == 0) {
            ordersByAccount = orderService.getOrdersByAccount(accountId, indexBeanParam.getFirstIndex(), indexBeanParam.getLastIndex());
        } else {
            ordersByAccount = orderService.getOrdersByAccountAsset(
                accountId, assetId.get(), indexBeanParam.getFirstIndex(), indexBeanParam.getLastIndex());
        }
        List<String> ordersIdList = ordersByAccount.map(ask -> Long.toUnsignedString(ask.getId())).collect(Collectors.toList());

        return response.bind(new AccountCurrentAskOrderIdsResponse(ordersIdList)).build();
    }

    @Path("/export-key")
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
    //TODO: It's a good idea to protect the exportkey method by @Secured2FA annotation
    public Response exportKey(@Parameter(description = "The secret passphrase of the account.", required = true)
                              @FormParam("passphrase") @NotNull String passphrase,
                              @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
                              @FormParam("account") @NotNull AccountIdParameter accountIdParameter
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        accountIdParameter.get();
        TwoFactorAuthParameters params2FA = account2FAHelper.create2FAParameters(accountIdParameter.getRawData(), passphrase, null, null);

        byte[] secretBytes = account2FAHelper.findAplSecretBytes(params2FA);

        AccountKeyDTO dto = new AccountKeyDTO(
            Long.toUnsignedString(params2FA.getAccountId()),
            Convert2.rsAccount(params2FA.getAccountId()),
            null, Convert.toHexString(secretBytes));

        return response.bind(dto).build();
    }

    @Path("/delete-key")
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
    public Response deleteKey(@Parameter(description = "The passphrase", required = true)
                              @FormParam("passphrase") @NotNull String passphrase,
                              @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
                              @FormParam("account") @NotNull AccountIdParameter accountIdParameter,
                              @Parameter(description = "The 2FA code.", required = true)
                              @FormParam("code2FA") Integer code2FA,
                              @Context org.jboss.resteasy.spi.HttpRequest request
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = RestParametersParser.get2FARequestAttribute(request);

        KeyStoreService.Status status = account2FAHelper.deleteAccount(params2FA);

        AccountKeyDTO dto = new AccountKeyDTO(Long.toUnsignedString(params2FA.getAccountId()),
            Convert2.rsAccount(params2FA.getAccountId()),
            status.message, null);

        return response.bind(dto).build();
    }

    @Path("/confirm2fa")
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

    @PermitAll
    @Secured2FA
    public Response confirm2FA(
        @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
        @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
        @Parameter(description = "The account ID.") @FormParam("account") String accountStr,
        @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") @NotNull Integer code2FA,
        @Context org.jboss.resteasy.spi.HttpRequest request
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = RestParametersParser.get2FARequestAttribute(request);

        account2FAHelper.confirm2FA(params2FA);
        Account2FADTO dto = faConverter.convert(params2FA);

        return response.bind(dto).build();
    }


    @Path("/disable2fa")
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

    @PermitAll
    @Secured2FA
    public Response disable2FA(
        @Parameter(description = "The passphrase") @FormParam("passphrase") String passphraseParam,
        @Parameter(description = "The secret phrase") @FormParam("secretPhrase") String secretPhraseParam,
        @Parameter(description = "The account ID.") @FormParam("account") String accountStr,
        @Parameter(description = "The 2FA code.", required = true) @FormParam("code2FA") @NotNull Integer code2FA,
        @Context org.jboss.resteasy.spi.HttpRequest request
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        TwoFactorAuthParameters params2FA = RestParametersParser.get2FARequestAttribute(request);

        account2FAHelper.disable2FA(params2FA);

        Account2FADTO dto = faConverter.convert(params2FA);

        return response.bind(dto).build();
    }

    @Path("/enable2fa")
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
        TwoFactorAuthParameters params2FA = account2FAHelper.create2FAParameters(accountStr, passphraseParam, secretPhraseParam, null);

        TwoFactorAuthDetails twoFactorAuthDetails = account2FAHelper.enable2FA(params2FA);

        Account2FADetailsDTO dto = faDetailsConverter.convert(twoFactorAuthDetails);
        faDetailsConverter.addAccount(dto, params2FA.getAccountId());

        return response.bind(dto).build();
    }

    @Path("/statistic")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns statistics Account information",
        description = "Returns statistics information about specified count of account",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AccountsCountDto.class)))
        }
    )
    @PermitAll
    public Response counts(
        @Parameter(name = "numberOfAccounts", description = "number Of returned Accounts, optional, minimal value = 50, maximum = 500", allowEmptyValue = true)
        @QueryParam("numberOfAccounts") String numberOfAccountsStr
    ) {
        log.trace("Started counts : \t'numberOfAccounts' = {}", numberOfAccountsStr);
        ResponseBuilder response = ResponseBuilder.startTiming();
        int numberOfAccounts = RestParametersParser.parseInt(numberOfAccountsStr, "numberOfAccounts",
            Constants.MIN_TOP_ACCOUNTS_NUMBER, Constants.MAX_TOP_ACCOUNTS_NUMBER, false);
        int numberOfAccountsMax = Math.max(numberOfAccounts, Constants.MIN_TOP_ACCOUNTS_NUMBER);

        AccountsCountDto dto = accountStatisticsService.getAccountsStatistic(numberOfAccountsMax);
        log.trace("counts result : {}", dto);
        return response.bind(dto).build();
    }


}
