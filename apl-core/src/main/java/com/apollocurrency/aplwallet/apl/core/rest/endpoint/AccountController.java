/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
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
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.order.entity.AskOrder;
import com.apollocurrency.aplwallet.apl.core.order.service.OrderService;
import com.apollocurrency.aplwallet.apl.core.order.service.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountAssetConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountCurrencyConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.CreateTransactionRespondBuilder;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.form.SendMoneyForm;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.AccountIdParameter;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.LongParameter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountStatisticsService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidBlockchainHeight;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
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
import org.jboss.resteasy.annotations.Form;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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

    private OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderService;

    private FirstLastIndexParser indexParser;

    private AccountStatisticsService accountStatisticsService;
    private TransactionCreator txCreator;
    private UnconfirmedTransactionConverter unconfirmedTransactionConverter = new UnconfirmedTransactionConverter();
    private TransactionConverter transactionConverter;

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
                             @AskOrderService OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderService,
                             FirstLastIndexParser indexParser,
                             AccountStatisticsService accountStatisticsService,
                             TransactionCreator txCreator) {

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
        this.orderService = Objects.requireNonNull(orderService, "orderService is NULL");
        this.indexParser = indexParser;
        this.accountStatisticsService = accountStatisticsService;
        this.transactionConverter =  new TransactionConverter(this.blockchain, unconfirmedTransactionConverter);
        this.txCreator = txCreator;
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
        @Parameter(description = "A zero-based index to the first asset ID to retrieve (optional).")
        @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the last asset ID to retrieve (optional).")
        @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();

        FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);

        if (assetId == null || assetId.get() == 0) {
            List<AccountAsset> accountAssets = accountAssetService.getAssetsByAccount(accountId, height,
                flIndex.getFirstIndex(),
                flIndex.getLastIndex());
            List<AccountAssetDTO> accountAssetDTOList = accountAssetConverter.convert(accountAssets);
            if (includeAssetInfo) {
                accountAssetDTOList.forEach(dto -> accountAssetConverter.addAsset(dto, Asset.getAsset(dto.getAssetId())));
            }

            return response.bind(new AccountAssetsResponse(accountAssetDTOList)).build();
        } else {
            AccountAsset accountAsset = accountAssetService.getAsset(accountId, assetId.get(), height);
            AccountAssetDTO dto = accountAssetConverter.convert(accountAsset);
            if (dto != null) {
                if (includeAssetInfo) {
                    accountAssetConverter.addAsset(dto, Asset.getAsset(assetId.get()));
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
        @QueryParam("timestamp") @PositiveOrZero int timestamp,
        @Parameter(description = "A zero-based index to the first block ID to retrieve (optional).")
        @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the last block ID to retrieve (optional).")
        @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();
        FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);

        List<Block> blocks = accountService.getAccountBlocks(accountId, timestamp, flIndex.getFirstIndex(), flIndex.getLastIndex());
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
        @QueryParam("timestamp") @PositiveOrZero int timestamp,
        @Parameter(description = "A zero-based index to the first block ID to retrieve (optional).")
        @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the last block ID to retrieve (optional).")
        @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex,
        @Parameter(description = "Include transactions detail info")
        @QueryParam("includeTransaction") @DefaultValue("false") boolean includeTransaction
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();
        FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);

        List<Block> blocks = accountService.getAccountBlocks(accountId, timestamp, flIndex.getFirstIndex(), flIndex.getLastIndex());

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
        @Parameter(description = "A zero-based index to the first currency ID to retrieve (optional).")
        @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the last currency ID to retrieve (optional).")
        @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();
        FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);

        if (currencyId == null || currencyId.get() == 0) {
            List<AccountCurrency> accountCurrencies = accountCurrencyService.getCurrenciesByAccount(accountId, height, flIndex.getFirstIndex(), flIndex.getLastIndex());
            List<AccountCurrencyDTO> accountCurrencyDTOList = accountCurrencyConverter.convert(accountCurrencies);
            if (includeCurrencyInfo) {
                accountCurrencyDTOList.forEach(dto -> accountCurrencyConverter
                    .addCurrency(dto,
                        Currency.getCurrency(
                            Convert.parseLong(dto.getCurrency()))));
            }

            return response.bind(new AccountCurrencyResponse(accountCurrencyDTOList)).build();
        } else {
            AccountCurrency accountCurrency = accountCurrencyService.getAccountCurrency(accountId, currencyId.get(), height);
            AccountCurrencyDTO dto = accountCurrencyConverter.convert(accountCurrency);
            if (dto != null) {
                if (includeCurrencyInfo) {
                    accountCurrencyConverter.addCurrency(dto, Currency.getCurrency(currencyId.get()));
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
        @Parameter(description = "A zero-based index to the first order ID to retrieve (optional).")
        @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the last order ID to retrieve (optional).")
        @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex
    ) {

        ResponseBuilder response = ResponseBuilder.startTiming();
        long accountId = accountIdParameter.get();
        FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);

        Stream<AskOrder> ordersByAccount;
        if (assetId == null || assetId.get() == 0) {
            ordersByAccount = orderService.getOrdersByAccount(accountId, flIndex.getFirstIndex(), flIndex.getLastIndex());
        } else {
            ordersByAccount = orderService.getOrdersByAccountAsset(accountId, assetId.get(), flIndex.getFirstIndex(), flIndex.getLastIndex());
        }
        List<String> ordersIdList = ordersByAccount.map(ask -> Long.toUnsignedString(ask.getId())).collect(Collectors.toList());

        return response.bind(new AccountCurrentAskOrderIdsResponse(ordersIdList)).build();
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

    @Path("/sendmoney")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Operation(
        summary = "Send money from sender to recipient account specified by 'amount ATM'",
        description = "Send money from sender to recipient account specified by 'amount ATM'",
        tags = {"accounts"})
    @ApiResponse(description = "Transaction in json format", content = @Content(schema = @Schema(implementation = CreateTransactionResponse.class)))
    @PermitAll
    @Secured2FA
    public Response sendMoney(
        @Form SendMoneyForm sendMoneyForm,
/*
        @Parameter(schema = @Schema(implementation = String.class))
            @Schema(description = "The sender account ID. Should be specified if sender account is Vault, optional for Standard account")
            @FormParam("sender") SenderIdParameter senderIdParameter,
*/

/*
        @Parameter @Schema(description = "Secret phrase of standard account, when specified - passphrase param will be ignored", format = "password")
            @FormParam("secretPhrase") String secretPhrase,
        @Parameter @Schema(description = "account publicKey")
            @FormParam("publicKey") String publicKey,
        @Parameter(required = true, schema = @Schema(implementation = Long.class)) @Schema(description = "fee ATM value, minimum 100000000 APL")
            @FormParam("feeATM") @NotNull @DefaultValue("100000000") @Positive Long feeATM,
        @Parameter(required = true) @Schema(description = "deadline value, minimum 1440")
            @FormParam("deadline") @NotNull @DefaultValue("1440") String deadline,
        @Parameter @Schema(description = "referenced Transaction FullHash")
            @FormParam("referencedTransactionFullHash") String referencedTransactionFullHash,
        @Parameter @Schema(description = "broadcast")
            @FormParam("broadcast") @DefaultValue("true") Boolean broadcast,
        @Parameter @Schema(description = "message")
            @FormParam("message") String message,
        @Parameter @Schema(description = "Is message Text?")
            @FormParam("messageIsText") Boolean messageIsText,
        @Parameter @Schema(description = "Is message Prunable?")
            @FormParam("messageIsPrunable") Boolean messageIsPrunable,
        @Parameter @Schema(description = "message To Encrypt")
            @FormParam("messageToEncrypt") String messageToEncrypt,
        @Parameter @Schema(description = "Is messageToEncrypt Text?")
            @FormParam("messageToEncryptIsText") Boolean messageToEncryptIsText,
        @Parameter @Schema(description = "encrypted Message Data")
            @FormParam("encryptedMessageData") String encryptedMessageData,
        @Parameter @Schema(description = "encrypted Message Nonce")
            @FormParam("encryptedMessageNonce") String encryptedMessageNonce,
        @Parameter @Schema(description = "Is encryptedMessage Prunable?")
            @FormParam("encryptedMessageIsPrunable") Boolean encryptedMessageIsPrunable,
        @Parameter @Schema(description = "compressMessageToEncrypt")
            @FormParam("compressMessageToEncrypt") Boolean compressMessageToEncrypt,
        @Parameter @Schema(description = "messageToEncryptToSelf")
            @FormParam("messageToEncryptToSelf") String messageToEncryptToSelf,
        @Parameter @Schema(description = "messageToEncryptToSelfIsText ?")
            @FormParam("messageToEncryptToSelfIsText") Boolean messageToEncryptToSelfIsText,
        @Parameter @Schema(description = "encryptToSelfMessageData")
            @FormParam("encryptToSelfMessageData") String encryptToSelfMessageData,
        @Parameter @Schema(description = "encryptToSelfMessageNonce")
            @FormParam("encryptToSelfMessageNonce") String encryptToSelfMessageNonce,
        @Parameter @Schema(description = "compressMessageToEncryptToSelf")
            @FormParam("compressMessageToEncryptToSelf") Boolean compressMessageToEncryptToSelf,
        @Parameter @Schema(description = "phased")
            @FormParam("phased") Boolean phased,
        @Parameter @Schema(description = "phasingFinishHeight")
            @FormParam("phasingFinishHeight") @DefaultValue("-1") Integer phasingFinishHeight,
        @Parameter @Schema(description = "The expected voting model of the phasing", implementation = VoteWeighting.VotingModel.class)
            @FormParam("phasingVotingModel") @DefaultValue("NONE") VoteWeighting.VotingModel phasingVotingModel,
        @Parameter(description = "The expected phasing quorum")
            @DefaultValue("0") @FormParam("phasingQuorum") Long phasingQuorum,
        @Parameter(description = "The minimum phasing quorum")
            @DefaultValue("0") @FormParam("phasingMinBalance") Long phasingMinBalance,
        @Parameter(description = "Phasing holding id")
            @DefaultValue("0") @FormParam("phasingHolding") Long phasingHolding,
        @Parameter @Schema(required = true, description = "The expected minimum balance model. Possible values: NONE(0), ATM(1), ASSET(2), CURRENCY(3)",
            implementation = VoteWeighting.MinBalanceModel.class)
            @FormParam("phasingMinBalanceModel") @DefaultValue("NONE") VoteWeighting.VotingModel phasingMinBalanceModel,
        @Parameter @Schema(description = "multiple values - the expected phasing whitelisted account", implementation = String.class)
            @FormParam("phasingWhitelisted") List<AccountIdParameter> phasingWhitelisted,
        @Parameter @Schema(description = "multiple values - the expected 'phasing Full Hash' as string", implementation = String.class)
            @FormParam("phasingLinkedFullHash") List<String> phasingLinkedFullHash,
        @Parameter @Schema(description = "phasing 'Hashed Secret' as HEX string", implementation = String.class)
            @FormParam("phasingHashedSecret") String phasingHashedSecret,
        @Parameter(description = "phasing Hashed 'Secret Algorithm' as byte")
            @DefaultValue("0") @FormParam("phasingHashedSecretAlgorithm") Byte phasingHashedSecretAlgorithm,
        @Parameter @Schema(description = "recipient Public Key as HEX string", implementation = String.class)
            @FormParam("recipientPublicKey") String recipientPublicKey,
        @Parameter(description = "ec Block Id")
            @DefaultValue("0") @FormParam("ecBlockId") Long ecBlockId,
        @Parameter(description = "ec Block Height")
            @DefaultValue("0") @FormParam("ecBlockHeight") Integer ecBlockHeight,

        @Parameter @Schema(description = "Passphrase to vault account, should be specified if sender account is vault", format = "password")
            @FormParam("passphrase") String passphrase,
        @Parameter(description = "Two-factor auth code, if 2fa enabled")
            @FormParam("code2FA") @DefaultValue("0") Integer code2FA,
        @Context HttpServletRequest servletRequest
*/

    @Context HttpServletRequest servletRequest

    ) throws ParameterException {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started sendMoney, recipientIdParameter = {}, sendMoneyForm={}, amountATM={}",
            sendMoneyForm.getRecipient(), sendMoneyForm, sendMoneyForm.getAmountATM());

        long recipientAccountId = sendMoneyForm.getRecipient().get();
        Account recipientAccount = accountService.getAccount(recipientAccountId);
        log.trace("recipientAccountId = {}, recipientAccount={}", recipientAccountId, recipientAccount);
        if (recipientAccount == null || accountService.getPublicKeyByteArray(recipientAccountId) == null) {
            return response.error(ApiErrors.CUSTOM_ERROR_MESSAGE,
                "recipient account not found OR it does not have public key").build();
        }
        Account senderAccount = HttpParameterParserUtil.getSenderAccount(servletRequest);
        log.trace("senderAccount={}", senderAccount);

        CreateTransactionRequest txRequest;
        try {
            txRequest = HttpRequestToCreateTransactionRequestConverter.convert(
                servletRequest, senderAccount, recipientAccount, recipientAccountId, sendMoneyForm.getAmountATM(),
                sendMoneyForm.getFeeATM(), Attachment.ORDINARY_PAYMENT, sendMoneyForm.getBroadcast());
            log.trace("txRequest = {}", txRequest);
        } catch(Exception e) {
            log.warn("Send Money Tx global exception", e);
            return response.error(ApiErrors.CUSTOM_ERROR_MESSAGE, "send money exception: " + e.getMessage()).build();
        }

        Transaction transaction = txCreator.createTransactionThrowingException(txRequest);
        log.trace("sendMoney transaction = {}", transaction);
//        TransactionDTO txDto = transactionConverter.convert(transaction);
        UnconfirmedTransactionDTO txDto = unconfirmedTransactionConverter.convert(transaction);
        log.trace("sendMoney txDto = {}", txDto);
        CreateTransactionResponse createTransactionResponse = new CreateTransactionRespondBuilder(txRequest, transaction, txDto).build();

        log.trace("DONE sendMoney, response = {}", createTransactionResponse);
        return response.bind(createTransactionResponse).build();
    }

}
