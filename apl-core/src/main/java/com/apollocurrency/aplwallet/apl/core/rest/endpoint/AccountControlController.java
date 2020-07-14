/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountControlPhasingDTO;
import com.apollocurrency.aplwallet.api.response.AccountControlPhasingResponse;
import com.apollocurrency.aplwallet.api.response.LeaseBalanceResponse;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountControlPhasingConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.AccountIdParameter;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.FirstLastIndexBeanParam;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AccountControlEffectiveBalanceLeasing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SetPhasingOnly;
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
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
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
import java.util.stream.Collectors;

@OpenAPIDefinition(info = @Info(description = "Provide methods to operate with accounts"))
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = "admin_api_key", in = SecuritySchemeIn.QUERY, paramName = "adminPassword")
@NoArgsConstructor
@Slf4j
@Path("/accounts/control")
public class AccountControlController {

    private BlockchainConfig blockchainConfig;
    private AccountControlPhasingService accountControlPhasingService;
    private AccountControlPhasingConverter accountControlPhasingConverter = new AccountControlPhasingConverter();
    private UnconfirmedTransactionConverter unconfirmedTransactionConverter = new UnconfirmedTransactionConverter();
    private TransactionCreator txCreator;
    private AccountService accountService;

    public static int maxAPIFetchRecords;

    @Inject
    public AccountControlController(
        AccountControlPhasingService accountControlPhasingService,
        BlockchainConfig blockchainConfig,
        TransactionCreator txCreator,
        AccountService accountService,
        @Property(name = "apl.maxAPIRecords", defaultValue = "100") int maxAPIrecords) {
        this.accountControlPhasingService = accountControlPhasingService;
        this.blockchainConfig = blockchainConfig;
        this.txCreator = txCreator;
        this.accountService = accountService;
        maxAPIFetchRecords = maxAPIrecords;
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
        @Parameter(description = "A zero-based index to the first, last asset ID to retrieve (optional).", schema = @Schema(implementation = FirstLastIndexBeanParam.class))
            @BeanParam FirstLastIndexBeanParam indexBeanParam
        ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        indexBeanParam.adjustIndexes(maxAPIFetchRecords);
        log.trace("Started getAllPhasingOnlyControls : \t indexBeanParam={}", indexBeanParam);
        AccountControlPhasingResponse dto = new AccountControlPhasingResponse();
        dto.phasingOnlyControls = accountControlPhasingService.getAllStream(indexBeanParam.getFirstIndex(), indexBeanParam.getLastIndex())
            .map(item -> accountControlPhasingConverter.convert(item)).collect(Collectors.toList());
        log.trace("getAllPhasingOnlyControls result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/id")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Returns the phasing control on certain account",
        description = "Returns the phasing control on certain account",
        tags = {"accounts"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AccountControlPhasingDTO.class)))
        })
    @PermitAll
    public Response getPhasingOnlyControl(
        @Parameter(description = "The account ID.", required = true, schema = @Schema(implementation = String.class))
        @QueryParam("account") @NotNull AccountIdParameter accountIdParameter
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started getPhasingOnlyControl, accountIdParameter = {}", accountIdParameter);
        long accountId = accountIdParameter.get();
        AccountControlPhasing phasingOnly = accountControlPhasingService.get(accountId);
        AccountControlPhasingDTO dto = accountControlPhasingConverter.apply(phasingOnly);
        log.trace("getPhasingOnlyControl result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/phasing")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Operation(
        summary = "Sets an account control that blocks transactions unless they are phased with certain parameters",
        description = "Sets an account control that blocks transactions unless they are phased with certain parameters."
            + " There is a list of significant fields: 'controlVotingModel', 'controlQuorum', 'controlMinBalance',"
            + "'controlMinBalanceModel', 'controlHolding', 'controlWhitelisted', 'controlMaxFees', 'controlMinDuration', 'controlMaxDuration'",
        tags = {"accounts"})
    @ApiResponse(description = "Transaction in json format", content = @Content(schema = @Schema(implementation = TransactionDTO.class)))
    @PermitAll
    @Secured2FA
    public Response setPhasingOnlyControl(
        @Parameter(required = true, schema = @Schema(implementation = String.class)) @Schema(description = "The sender account ID.")
            @FormParam("sender") @NotNull AccountIdParameter senderAccountIdParameter,
        @Parameter(required = true, schema = @Schema(implementation = VoteWeighting.VotingModel.class)) @Schema(description = "The expected voting model of the phasing. Possible values: NONE(-1), ACCOUNT(0), ATM(1), ASSET(2), CURRENCY(3)")
            @FormParam("controlVotingModel") @DefaultValue("NONE") VoteWeighting.VotingModel controlVotingModel,
        @Parameter(required = true, schema = @Schema(implementation = Long.class)) @Schema(description = "The expected quorum")
            @FormParam("controlQuorum") Long controlQuorum,
        @Parameter(required = true, schema = @Schema(implementation = Long.class)) @Schema(description = "The expected minimum balance")
            @FormParam("controlMinBalance") Long controlMinBalance,
        @Parameter(required = true, schema = @Schema(implementation = VoteWeighting.MinBalanceModel.class)) @Schema(description = "The expected minimum balance model. Possible values: NONE(0), ATM(1), ASSET(2), CURRENCY(3)")
            @FormParam("controlMinBalanceModel") @DefaultValue("NONE") VoteWeighting.MinBalanceModel controlMinBalanceModel,
        @Parameter(required = true, schema = @Schema(implementation = Long.class)) @Schema(description = "The expected holding ID - asset ID or currency ID")
            @FormParam("controlHolding") Long controlHoldingId,

        @Parameter @Schema(description = "multiple values - the expected whitelisted account", implementation = String.class)
            @FormParam("controlWhitelisted") List<AccountIdParameter> controlWhitelisted,
        @Parameter @Schema(description = "The maximum allowed accumulated total fees for not yet finished phased transactions", implementation = Long.class)
            @FormParam("controlMaxFees") Long controlMaxFees,
        @Parameter @Schema(description = "The minimum phasing duration (finish height minus current height)", implementation = Long.class)
            @FormParam("controlMinDuration") @DefaultValue("0") @Min(0) @Max(Constants.MAX_PHASING_DURATION - 1) Long controlMinDuration,
        @Parameter @Schema(description = "The maximum allowed phasing duration", implementation = Long.class)
            @FormParam("controlMaxDuration") @DefaultValue("0") @Min(0) @Max(Constants.MAX_PHASING_DURATION - 1) Long controlMaxDuration,

        @Parameter @Schema(description = "Passphrase to vault account, should be specified if sender account is vault")
            @FormParam("passphrase") String passphrase,
        @Parameter @Schema(description = "account publicKey")
            @FormParam("publicKey") String publicKey,
        @Parameter(required = true, schema = @Schema(implementation = Long.class)) @Schema(description = "fee ATM value")
            @FormParam("feeATM") @NotNull @Positive Long feeATM,
        @Parameter @Schema(description = "deadline value")
            @FormParam("deadline") @DefaultValue("1440") String deadline,
        @Parameter @Schema(description = "referenced Transaction FullHash")
            @FormParam("referencedTransactionFullHash") String referencedTransactionFullHash,
        @Parameter @Schema(description = "broadcast")
            @FormParam("broadcast") Boolean broadcast,
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
            @FormParam("phasingFinishHeight") Integer phasingFinishHeight,
        @Parameter @Schema(description = "The expected voting model of the phasing", implementation = VoteWeighting.VotingModel.class)
            @FormParam("phasingVotingModel") @DefaultValue("NONE") VoteWeighting.VotingModel phasingVotingModel,
        @Parameter @Schema(description = "The expected phasing quorum", implementation = Long.class)
            @FormParam("phasingQuorum") Long phasingQuorum,
        @Parameter @Schema(description = "The minimum phasing quorum", implementation = Long.class)
            @FormParam("phasingMinBalance") Long phasingMinBalance,
        @Parameter @Schema(description = "Phasing holding id", implementation = Long.class)
            @FormParam("phasingHolding") Long phasingHolding,
        @Parameter @Schema(required = true, description = "The expected minimum balance model. Possible values: NONE(0), ATM(1), ASSET(2), CURRENCY(3)",
            implementation = VoteWeighting.MinBalanceModel.class)
            @FormParam("phasingMinBalanceModel") @DefaultValue("NONE") VoteWeighting.VotingModel phasingMinBalanceModel,
        @Parameter @Schema(description = "multiple values - the expected phasing whitelisted account", implementation = String.class)
            @FormParam("phasingWhitelisted") List<AccountIdParameter> phasingWhitelisted,
        @Parameter @Schema(description = "multiple values - the expected 'phasing Full Hash' as string", implementation = String.class)
            @FormParam("phasingLinkedFullHash") List<String> phasingLinkedFullHash,
        @Parameter @Schema(description = "phasing 'Hashed Secret' as HEX string", implementation = String.class)
            @FormParam("phasingHashedSecret") String phasingHashedSecret,
        @Parameter @Schema(description = "phasing Hashed 'Secret Algorithm' as byte", implementation = Byte.class)
            @FormParam("phasingHashedSecretAlgorithm") Byte phasingHashedSecretAlgorithm,
        @Parameter @Schema(description = "recipient Public Key as HEX string", implementation = String.class)
            @FormParam("recipientPublicKey") String recipientPublicKey,
        @Parameter @Schema(description = "ec Block Id", implementation = Long.class)
            @FormParam("ecBlockId") Long ecBlockId,
        @Parameter @Schema(description = "ec Block Height", implementation = Integer.class)
            @FormParam("ecBlockHeight") Integer ecBlockHeight,

        @Parameter @Schema(description = "Secret phrase of standard account, when specified - passphrase param will be ignored")
            @FormParam("secretPhrase") String secretPhrase,
        @Parameter @Schema(description = "Two-factor auth code, if 2fa enabled")
            @FormParam("code2FA") @DefaultValue("0") Integer code2FA,
        @Context HttpServletRequest servletRequest
    ) throws ParameterException {
        ResponseBuilder response = ResponseBuilder.startTiming();
        if (log.isTraceEnabled()) {
            log.trace("Started setPhasingOnlyControl, senderAccountIdParameter = {}, controlVotingModel={}, controlQuorum={}," +
                    "controlMinBalance={}, controlMinBalanceModel={}, controlHoldingId={}, controlWhitelisted={}, controlMaxFees={}," +
                    "controlMinDuration={}, controlMaxDuration={}",
                senderAccountIdParameter, controlVotingModel, controlQuorum, controlMinBalance, controlMinBalanceModel,
                controlHoldingId, controlWhitelisted, controlMaxFees, controlMinDuration, controlMaxDuration);
        }
        long senderAccountId = senderAccountIdParameter.get();
        log.trace("senderAccountId = {}", senderAccountId);

        long[] whitelistedAccountIds = new long[controlWhitelisted.size()];
        for (int i = 0; i < controlWhitelisted.size(); i++) {
            AccountIdParameter idParameter = controlWhitelisted.get(i);
            whitelistedAccountIds[i] = idParameter.parse();
        }
        log.trace("whitelistedAccountIds = {}", whitelistedAccountIds);
        long maxBalanceATM = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        long maxFees = Math.min(controlMaxFees != null ? controlMaxFees : 0, maxBalanceATM);
        log.trace("maxFees = {} because maxBalanceATM={} and controlMaxFees={}", maxFees, maxBalanceATM, controlMaxFees);

        PhasingParams phasingParams = new PhasingParams(
            controlVotingModel.getCode(), controlHoldingId != null ? controlHoldingId : 0, controlQuorum,
            controlMinBalance != null ? controlMinBalance : 0, controlMinBalanceModel.getCode(), whitelistedAccountIds);
        Account senderAccount = accountService.getAccount(senderAccountId);
        log.trace("phasingParams = {}, senderAccountId={}, senderAccount={}", phasingParams, senderAccountId, senderAccount);

        SetPhasingOnly attachment = new SetPhasingOnly(phasingParams, maxFees, controlMinDuration.shortValue(), controlMaxDuration.shortValue());
        CreateTransactionRequest txRequest = HttpRequestToCreateTransactionRequestConverter.convert(
            servletRequest, senderAccount, 0, 0, attachment,
            broadcast != null ? broadcast : false, feeATM != null ? feeATM : Constants.ONE_APL);
        log.trace("txRequest = {}", txRequest);

        Transaction transaction = txCreator.createTransactionThrowingException(txRequest);
        log.trace("setPhasingOnlyControl transaction = {}", transaction);
        UnconfirmedTransactionDTO txDto = unconfirmedTransactionConverter.convert(transaction);
        log.trace("DONE setPhasingOnlyControl txDto = {}", txDto);
        // Returned DTO needs additional check on UI, because PROBABLY JSON is not quite correct
        // see 'LeaseBalanceResponse' as more correct response reference
        return response.bind(txDto).build();
    }

    @Path("/lease")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Operation(
        summary = "Leasing balance from sender to recipient account",
        description = "Leasing balance from sender to recipient account for specified 'period' (minimum 1440 blocks).",
        tags = {"accounts"})
    @ApiResponse(description = "Transaction in json format", content = @Content(schema = @Schema(implementation = LeaseBalanceResponse.class)))
    @PermitAll
    @Secured2FA
    public Response leaseBalance(
        @Parameter(required = true) @Schema(description = "Leasing period, min/default = 1440, max = 65535 blocks", implementation = Integer.class)
            @FormParam("period") @NotNull @Min(1440)  @Max(65535) Integer period,
        @Parameter(required = true, schema = @Schema(implementation = String.class)) @Schema(description = "The sender account ID.")
            @FormParam("sender") @NotNull AccountIdParameter senderIdParameter,
        @Parameter(required = true, schema = @Schema(implementation = String.class)) @Schema(description = "The recipient account ID.")
            @FormParam("recipient") @NotNull AccountIdParameter recipientIdParameter,

        @Parameter @Schema(description = "Secret phrase of standard account, when specified - passphrase param will be ignored")
            @FormParam("secretPhrase") String secretPhrase,
        @Parameter @Schema(description = "account publicKey")
            @FormParam("publicKey") String publicKey,
        @Parameter(required = true, schema = @Schema(implementation = Long.class)) @Schema(description = "fee ATM value")
            @FormParam("feeATM") @NotNull Long feeATM,
        @Parameter @Schema(description = "deadline value")
            @FormParam("deadline") @DefaultValue("1440") String deadline,
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
            @FormParam("phasingFinishHeight") Integer phasingFinishHeight,
        @Parameter @Schema(description = "The expected voting model of the phasing", implementation = VoteWeighting.VotingModel.class)
            @FormParam("phasingVotingModel") @DefaultValue("NONE") VoteWeighting.VotingModel phasingVotingModel,
        @Parameter @Schema(description = "The expected phasing quorum", implementation = Long.class)
            @FormParam("phasingQuorum") Long phasingQuorum,
        @Parameter @Schema(description = "The minimum phasing quorum", implementation = Long.class)
            @FormParam("phasingMinBalance") Long phasingMinBalance,
        @Parameter @Schema(description = "Phasing holding id", implementation = Long.class)
            @FormParam("phasingHolding") Long phasingHolding,
        @Parameter @Schema(required = true, description = "The expected minimum balance model. Possible values: NONE(0), ATM(1), ASSET(2), CURRENCY(3)",
            implementation = VoteWeighting.MinBalanceModel.class)
            @FormParam("phasingMinBalanceModel") @DefaultValue("NONE") VoteWeighting.VotingModel phasingMinBalanceModel,
        @Parameter @Schema(description = "multiple values - the expected phasing whitelisted account", implementation = String.class)
            @FormParam("phasingWhitelisted") List<AccountIdParameter> phasingWhitelisted,
        @Parameter @Schema(description = "multiple values - the expected 'phasing Full Hash' as string", implementation = String.class)
            @FormParam("phasingLinkedFullHash") List<String> phasingLinkedFullHash,
        @Parameter @Schema(description = "phasing 'Hashed Secret' as HEX string", implementation = String.class)
            @FormParam("phasingHashedSecret") String phasingHashedSecret,
        @Parameter @Schema(description = "phasing Hashed 'Secret Algorithm' as byte", implementation = Byte.class)
            @FormParam("phasingHashedSecretAlgorithm") Byte phasingHashedSecretAlgorithm,
        @Parameter @Schema(description = "recipient Public Key as HEX string", implementation = String.class)
            @FormParam("recipientPublicKey") String recipientPublicKey,
        @Parameter @Schema(description = "ec Block Id", implementation = Long.class)
            @FormParam("ecBlockId") Long ecBlockId,
        @Parameter @Schema(description = "ec Block Height", implementation = Integer.class)
            @FormParam("ecBlockHeight") Integer ecBlockHeight,

        @Parameter @Schema(description = "Passphrase to vault account, should be specified if sender account is vault")
            @FormParam("passphrase") String passphrase,
        @Parameter @Schema(description = "Two-factor auth code, if 2fa enabled")
            @FormParam("code2FA") @DefaultValue("0") Integer code2FA,
        @Context HttpServletRequest servletRequest
    ) throws ParameterException {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.trace("Started leaseBalance, recipientIdParameter = {}, senderIdParameter={}, period={}",
            recipientIdParameter, senderIdParameter, period);

        long recipientAccountId = recipientIdParameter.get();
        Account recipientAccount = accountService.getAccount(recipientAccountId);
        log.trace("recipientAccountId = {}, recipientAccount={}", recipientAccountId, recipientAccount);
        if (recipientAccount == null || accountService.getPublicKeyByteArray(recipientAccountId) == null) {
            return response.error(ApiErrors.CUSTOM_ERROR_MESSAGE,
                "recipient account not found OR it does not have public key").build();
        }
        long senderAccountId = senderIdParameter.get();
        Account senderAccount = accountService.getAccount(senderAccountId);
        log.trace("senderAccountId = {}, senderAccount={}", senderAccountId, senderAccount);

        Attachment attachment = new AccountControlEffectiveBalanceLeasing(period);

        CreateTransactionRequest txRequest = HttpRequestToCreateTransactionRequestConverter.convert(
            servletRequest, senderAccount, recipientAccount, recipientAccountId, 0, feeATM, attachment, broadcast);
        log.trace("txRequest = {}", txRequest);

        Transaction transaction = txCreator.createTransactionThrowingException(txRequest);
        log.trace("leaseBalance transaction = {}", transaction);
        UnconfirmedTransactionDTO txDto = unconfirmedTransactionConverter.convert(transaction);
        log.trace("leaseBalance txDto = {}", txDto);
        LeaseBalanceResponse leaseBalanceResponse = new LeaseBalanceResponse();
        leaseBalanceResponse.setTransactionJSON(txDto);
        leaseBalanceResponse.setUnsignedTransactionBytes(Convert.toHexString(transaction.getUnsignedBytes()));
        leaseBalanceResponse.setTransaction(transaction.getStringId());
        leaseBalanceResponse.setFullHash(transaction.getFullHashString());
        leaseBalanceResponse.setTransactionBytes(Convert.toHexString(transaction.getBytes()));
        leaseBalanceResponse.setBroadcasted(txRequest.isBroadcast());

        log.trace("DONE leaseBalance, response = {}", leaseBalanceResponse);
        return response.bind(leaseBalanceResponse).build();
    }

}
