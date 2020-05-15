/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountControlPhasingDTO;
import com.apollocurrency.aplwallet.api.dto.account.WhiteListedAccount;
import com.apollocurrency.aplwallet.api.response.AccountControlPhasingResponse;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountControlPhasingConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.AccountIdParameter;
import com.apollocurrency.aplwallet.apl.core.rest.provider.WhiteListedAccountList;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
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
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OpenAPIDefinition(info = @Info(description = "Provide methods to operate with accounts"))
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
@NoArgsConstructor
@Slf4j
@Path("/accounts/control")
public class AccountControlController {

//    private Blockchain blockchain;
    private BlockchainConfig blockchainConfig;
    private FirstLastIndexParser indexParser;
    private AccountControlPhasingService accountControlPhasingService;
    private AccountControlPhasingConverter accountControlPhasingConverter = new AccountControlPhasingConverter();
    private UnconfirmedTransactionConverter unconfirmedTransactionConverter = new UnconfirmedTransactionConverter();
    private TransactionCreator txCreator;

    @Inject
    public AccountControlController(/*Blockchain blockchain,*/
        FirstLastIndexParser indexParser,
        AccountControlPhasingService accountControlPhasingService,
        BlockchainConfig blockchainConfig,
        TransactionCreator txCreator) {
//        this.blockchain = blockchain;
        this.indexParser = indexParser;
        this.accountControlPhasingService = accountControlPhasingService;
        this.blockchainConfig = blockchainConfig;
        this.txCreator = txCreator;
    }

    @Path("/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get all phasing only entities by using first/last index",
        description = "Get all phasing only entities by using first/last index",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AccountControlPhasingResponse.class)))
        })
    @PermitAll
    public Response getAllPhasingOnlyControls(
        @Parameter(description = "A zero-based index to the first asset ID to retrieve (optional).")
        @QueryParam("firstIndex") @DefaultValue("0") @PositiveOrZero int firstIndex,
        @Parameter(description = "A zero-based index to the last asset ID to retrieve (optional).")
        @QueryParam("lastIndex") @DefaultValue("-1") int lastIndex
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        FirstLastIndexParser.FirstLastIndex flIndex = indexParser.adjustIndexes(firstIndex, lastIndex);
        log.trace("Started getAllPhasingOnlyControls : \t firstIndex={}, lastIndex={}, " +
            "flIndex.firstIndex={}, flIndex.lastIndex={}", firstIndex, lastIndex,
            flIndex.getFirstIndex(), flIndex.getLastIndex());
        AccountControlPhasingResponse dto = new AccountControlPhasingResponse();
        dto.phasingOnlyControls = accountControlPhasingService.getAllStream(flIndex.getFirstIndex(), flIndex.getLastIndex())
            .map(item -> accountControlPhasingConverter.convert(item)).collect(Collectors.toList());
        log.trace("getAllPhasingOnlyControls result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/id")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns the phasing control certain account",
        description = "Returns the phasing control certain account",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AccountControlPhasingDTO.class)))
        })
    @PermitAll
    public Response getPhasingOnlyControl(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("accounts") @NotNull AccountIdParameter accountIdParameter
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getPhasingOnlyControl, accountIdParameter = {}", accountIdParameter);
        long accountId = accountIdParameter.get();
        AccountControlPhasing phasingOnly = accountControlPhasingService.get(accountId);
        AccountControlPhasingDTO dto = accountControlPhasingConverter.apply(phasingOnly);
        log.trace("getPhasingOnlyControl result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/control")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Operation(
        summary = "Sets an account control that blocks transactions unless they are phased with certain parameters",
        description = "Sets an account control that blocks transactions unless they are phased with certain parameters.",
        tags = {"accounts"})
    @ApiResponse(description = "Transaction in json format", content = @Content(schema = @Schema(implementation = TransactionDTO.class)))
    @PermitAll
    @Secured2FA
    public Response setPhasingOnlyControl(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
            @FormParam("account") @NotNull AccountIdParameter accountIdParameter,
        @Parameter(description = "The expected voting model of the phasing. Possible values: NONE(-1), ACCOUNT(0), ATM(1), ASSET(2), CURRENCY(3)",
            required = true, schema = @Schema(implementation = VoteWeighting.VotingModel.class))
            @FormParam("controlVotingModel") VoteWeighting.VotingModel controlVotingModel,
        @Parameter(description = "The expected quorum", schema = @Schema(implementation = Long.class))
            @FormParam("controlQuorum") Long controlQuorum,
        @Parameter(description = "The expected minimum balance", schema = @Schema(implementation = Long.class))
            @FormParam("controlMinBalance") Long controlMinBalance,
        @Parameter(description = "The expected minimum balance model. Possible values: NONE(0), ATM(1), ASSET(2), CURRENCY(3)",
            required = true, schema = @Schema(implementation = VoteWeighting.MinBalanceModel.class))
        @FormParam("controlMinBalanceModel") VoteWeighting.VotingModel controlMinBalanceModel,
        @Parameter(description = "The expected holding ID - asset ID or currency ID", schema = @Schema(implementation = Long.class))
            @FormParam("controlHolding") Long controlHolding,
        @Parameter(description = "multiple values - the expected whitelisted accounts", schema = @Schema(implementation = WhiteListedAccountList.class))
            @FormParam("controlWhitelisted") List<WhiteListedAccount> controlWhitelisted,
        @Parameter(description = "The maximum allowed accumulated total fees for not yet finished phased transactions", schema = @Schema(implementation = Long.class))
            @FormParam("controlMaxFees") Long controlMaxFees,
        @Parameter(description = "The minimum phasing duration (finish height minus current height)", schema = @Schema(implementation = Long.class))
            @FormParam("controlMinDuration") @Min(0) @Max(Constants.MAX_PHASING_DURATION - 1) Long controlMinDuration,
        @Parameter(description = "The maximum allowed phasing duration", schema = @Schema(implementation = Long.class))
            @FormParam("controlMaxDuration") @Min(0) @Max(Constants.MAX_PHASING_DURATION - 1) Long controlMaxDuration,
        @Parameter @Schema(description = "Passphrase to vault account, should be specified if sender account is vault")
            @FormParam("passphrase") String passphrase,
        @Parameter @Schema(description = "Secret phrase of standard account, when specified - passphrase param will be ignored")
            @FormParam("secretPhrase") String secretPhrase,
        @Parameter @Schema(description = "Two-factor auth code, if 2fa enabled")
            @FormParam("code2FA") @DefaultValue("0") Integer code2FA,
        @Context HttpServletRequest servletRequest
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started setPhasingOnlyControl, accountIdParameter = {}", accountIdParameter);
        long accountId = accountIdParameter.get();

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
//            .attachment(attachment)
//            .timestamp(timestamp)
//            .senderAccount(senderAccount)
//            .broadcast(true)
            .feeATM(Constants.ONE_APL)
            .deadlineValue("1440")
//            .keySeed(keySeed)
//            .publicKeyValue(Convert.toHexString(senderAccount.getPublicKey().getPublicKey()))
//            .publicKey(senderAccount.getPublicKey().getPublicKey())
            .secretPhrase(secretPhrase)
            .passphrase(passphrase)
            .build();

        Transaction transaction = txCreator.createTransactionThrowingException(txRequest);
        UnconfirmedTransactionDTO txDto = unconfirmedTransactionConverter.convert(transaction);
        return response.bind(txDto).build();
    }

}
