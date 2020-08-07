package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionHash;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidAtomicSwapTime;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;

import static com.apollocurrency.aplwallet.apl.core.rest.endpoint.DexApiConstants.COUNTER_ORDER_ID;
import static com.apollocurrency.aplwallet.apl.core.rest.endpoint.DexApiConstants.ORDER_ID;
import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ORDER_DURATION_SEC;

@Path("/dex")
@OpenAPIDefinition(info = @Info(description = "Provide set of api endpoints to send dex-related transactions manually"))
@Singleton
public class DexTransactionSendingController {
    private DexApiValidator validator;
    private DexService dexService;

    @Inject
    public DexTransactionSendingController(DexApiValidator validator, DexService dexService) {
        this.validator = validator;
        this.dexService = dexService;
    }

    //Not delete, required for RESTEASY
    public DexTransactionSendingController() {
    }

    @POST
    @Path("/orders")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Send apl dex order transaction", description = "Create, validate and conditionally broadcast Apollo DexOrderTransaction, depending on specified parameters ",
        responses = @ApiResponse(description = "Transaction in json format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionDTO.class))))
    public Response sendOrder(
        @Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
        @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
        @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
        @Parameter(description = "Type of the APL offer. (BUY APL / SELL APL) ", required = true) @FormParam("offerType") OrderType orderType,
        @Parameter(description = "Address from which money will be transferred for BUY orders. For SELL orders must specify ETH 'to' address") @FormParam(DexApiConstants.WALLET_ADDRESS) String walletAddress,
        @Parameter(description = "APL amount (buy or sell) for order in Gwei (1 Gwei = 0.000000001), " +
            "in other words  - amount of apollo atoms multiplied by 10. 1 Gwei = 10^-9, 1 ATM = 10^-8", required = true) @FormParam("offerAmount") @Min(1) Long orderAmount,
        @Parameter(description = "Paired currency. (ETH or PAX)", required = true) @FormParam(DexApiConstants.PAIR_CURRENCY) DexCurrency pairCurrency,
        @Parameter(description = "Pair rate in Gwei. (1 Gwei = 0.000000001). Represent real pair rate, multiplied by 10^9", required = true) @FormParam("pairRate") @Min(1) Long pairRate,
        @Parameter(description = "Amount of time for this offer. (seconds)", required = true) @FormParam("amountOfTime") @Min(1) @Max(MAX_ORDER_DURATION_SEC) Integer amountOfTime,
        @Parameter(description = "Desirable order status. Accepted OPEN or PENDING. Default is OPEN.") @FormParam("orderStatus") @DefaultValue("OPEN") OrderStatus status,
        @Context HttpServletRequest req

    ) throws ParameterException, AplException.ValidationException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();

        if (status != OrderStatus.OPEN && status != OrderStatus.PENDING) {
            return ResponseBuilder.apiError(ApiErrors.INCORRECT_PARAM, "orderStatus", "Expected OPEN or PENDING, got " + status).build();
        }
        validator.validateParametersForOrderTransaction(account.getId(), accountDetails.getPassphrase(), orderType, orderAmount, pairRate, walletAddress, pairCurrency);
        String fromAddress;
        String toAddress;
        if (orderType == OrderType.BUY) {
            fromAddress = walletAddress;
            toAddress = Convert2.defaultRsAccount(account.getId());
        } else {
            fromAddress = Convert2.defaultRsAccount(account.getId());
            toAddress = walletAddress;
        }
        Transaction tx =
            dexService.createDexOrderTransaction(req, account,
                fromAddress, toAddress,
                amountOfTime, orderType, EthUtil.gweiToEth(pairRate), status, EthUtil.gweiToAtm(orderAmount), pairCurrency);

        return Response.ok(JSONData.unconfirmedTransaction(tx).toJSONString()).build();
    }

    @POST
    @Path("/eth-deposits")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Make an ETH/PAX deposit for order", description = "Will send ETH transaction to create ETH or PAX deposit. For ERC20 will increase allowance firstly. " +
        "There are two ways to make a deposit, first - extract required amount,senderAddress,currency from already sent order and perform validations, second - create a deposit directly for specified amount," +
        "orderId, currency and walletAddress, such option can be used for node, which is downloading blockchain or order is not broadcasted/confirmed yet on this node." +
        "For the first way, walletAddress, amountGwei,currency are not required, for the second these parameters are mandatory. By default first way followed, " +
        "when walletAddress, amountGwei,currency are specified - second way activates",
        responses = @ApiResponse(description = "Deposit transaction hash", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionHash.class))))
    public Response freezeEthPax(
        @Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
        @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
        @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
        @Parameter(description = "Id of order for which money has to be deposited. Signed or unsigned int64/long", required = true) @FormParam(ORDER_ID) String orderIdString,
        @Parameter(description = "Amount of eth/pax to transfer in gwei") @FormParam("amount") long amountGwei,
        @Parameter(description = "Eth address, which will send eth deposit transaction.") @FormParam(DexApiConstants.WALLET_ADDRESS) String walletAddress,
        @Parameter(description = "Currency to deposit, ETH or PAX") @FormParam(DexApiConstants.PAIR_CURRENCY) DexCurrency cur
    ) throws ParameterException, AplException.ExecutiveProcessException {
        AccountDetails details = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = details.getAccount();
        String hash;
        if (amountGwei > 0 && StringUtils.isNotBlank(walletAddress) && cur != null) {
            long orderId = Convert.parseLong(orderIdString);
            BigInteger amountWei = EthUtil.gweiToWei(amountGwei);
            validator.validateEthAccountForDeposit(account.getId(), passphrase, walletAddress, amountWei, cur);
            hash = dexService.freezeEthPax(passphrase, account.getId(), orderId, cur, amountWei, walletAddress);
        } else {
            DexOrder order = getAndVerifyAccountOrder(orderIdString, account.getId());
            validator.validateEthAccountForDeposit(details.getPassphrase(), order);
            hash = dexService.freezeEthPax(details.getPassphrase(), order);
        }
        return Response.ok(new TransactionHash(hash)).build();
    }

    @POST
    @Path("/contracts-step1")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Create STEP1 contract ", description = "Will send APL transaction to create new offering contract for PENDING (sender) order and another OPEN order.",
        responses = @ApiResponse(description = "Apl transaction in JSON format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionDTO.class))))
    public Response sendContractStep1(@Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
                                      @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
                                      @Parameter(description = "Two factor authentication code, if 2fa is enabled") @DefaultValue("0") @FormParam("code2FA") @Min(0) @Max(999999) int code2FA,
                                      @Parameter(description = "Amount of time to be active for contract") @FormParam("timeToReply") @ValidAtomicSwapTime @DefaultValue("1000") int timeToReply,
                                      @Parameter(description = "Id of order, owned by sender account in PENDING status. Signed or unsigned int64/long", required = true) @FormParam("orderId") @NotBlank String orderId,
                                      @Parameter(description = "Id of order to match sender's order. Required OPEN status and opposite type. Signed or unsigned int64/long", required = true) @FormParam("counterOrderId") @NotBlank String counterOrderId,
                                      @Context HttpServletRequest req
    ) throws ParameterException, AplException.ValidationException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();
        DexOrder dexOrder = getAndVerifyAccountOrder(orderId, account.getId());
        DexOrder counterOrder = getAndVerifyCounterOrder(counterOrderId, dexOrder);
        if (dexOrder.getStatus() != OrderStatus.PENDING) {
            throw new ParameterException(JSONResponses.incorrect(ORDER_ID, "Expected order status 'PENDING', got  " + dexOrder.getStatus()));
        }
        if (counterOrder.getStatus() != OrderStatus.OPEN) {
            throw new ParameterException(JSONResponses.incorrect(COUNTER_ORDER_ID, "Expected counter order status 'OPEN', got " + counterOrder.getStatus()));
        }
        Transaction transaction = dexService.sendContractStep1Transaction(req, account, dexOrder, counterOrder, timeToReply);
        return Response.ok(JSONData.unconfirmedTransaction(transaction).toJSONString()).build();
    }

    @POST
    @Path("/contracts-step2")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Create STEP2 contract ", description = "Will send APL contract transaction to update existing STEP1 contract specified by id and fill it with secretHash, encryptedSecret, timeToReply and counterTransferTx data." +
        "Can be sent only by account, owning counterOrder specified in the contract",
        responses = @ApiResponse(description = "Apl transaction in JSON format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionDTO.class))))
    public Response sendContractStep2(@Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
                                      @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
                                      @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
                                      @Parameter(description = "Amount of time to be active for contract in seconds", required = true) @FormParam("timeToReply") @ValidAtomicSwapTime int timeToReply,
                                      @Parameter(description = "Id of the contract to update with a new contract transaction.  Signed or unsigned int64/long", required = true) @FormParam("contractId") @NotBlank String contractId,
                                      @Parameter(description = "SHA-256 hash of the secret in the hexadecimal format", required = true) @FormParam("secretHash") @NotBlank String secretHash,
                                      @Parameter(description = "64-bytes encrypted secret in the hexadecimal format", required = true) @FormParam("encryptedSecret") @NotBlank String encryptedSecret,
                                      @Parameter(description = "Unsigned Id of apl TransferMoneyWithApproval transaction for sell counter order or hexadecimal hash of 'initiate' ETH transaction, sent to smart contract", required = true) @FormParam("counterTransferTx") @NotBlank String counterTransferTx,
                                      @Context HttpServletRequest req
    ) throws ParameterException, AplException.ValidationException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();
        ExchangeContract contract = dexService.getDexContractById(Convert.parseLong(contractId));
        if (contract == null) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "Contract does not exist"));
        }
        if (contract.getContractStatus() != ExchangeContractStatus.STEP_1) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "Expected status 'STEP1', got " + contract.getContractStatus()));
        }
        if (contract.getRecipient() != account.getId()) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "Account is not a recipient in the contract"));
        }
        byte[] secretHashBytes = getAndValidateSecretHash(secretHash);
        byte[] encryptedSecretBytes = Convert.parseHexString(encryptedSecret);
        if (encryptedSecretBytes.length != 64) {
            throw new ParameterException(JSONResponses.incorrect("encryptedSecret", "Expected size of encrypted secret is 64, got " + encryptedSecretBytes.length));
        }

        Transaction transaction = dexService.sendContractStep2Transaction(req, account, secretHashBytes, encryptedSecretBytes, counterTransferTx, timeToReply, contract);
        return Response.ok(JSONData.unconfirmedTransaction(transaction).toJSONString()).build();
    }

    private byte[] getAndValidateSecretHash(String secretHash) throws ParameterException {
        byte[] secretHashBytes = Convert.parseHexString(secretHash);
        if (secretHashBytes.length != 32) {
            throw new ParameterException(JSONResponses.incorrect("secretHash", "Expected size of secret hash is 32, got " + secretHashBytes.length));
        }
        return secretHashBytes;
    }

    @POST
    @Path("/contracts-step3")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Create STEP3 contract ", description = "Will send APL contract transaction to update existing STEP2 contract specified by id and fill it with timeToReply and transferTx data." +
        "Can be sent only by account, owning order specified in the contract (account, who originally created contract with STEP1)",
        responses = @ApiResponse(description = "Apl transaction in JSON format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionDTO.class))))
    public Response sendContractStep3(@Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
                                      @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
                                      @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
                                      @Parameter(description = "Amount of time to be active for contract in seconds", required = true) @FormParam("timeToReply") @ValidAtomicSwapTime int timeToReply,
                                      @Parameter(description = "Id of the contract to update with a new contract transaction.  Signed or unsigned int64/long", required = true) @FormParam("contractId") @NotBlank String contractId,
                                      @Parameter(description = "Unsigned Id of apl TransferMoneyWithApproval transaction for sell order or hexadecimal hash of 'initiate' ETH transaction, sent to smart contract", required = true) @FormParam("transferTx") @NotBlank String transferTx,
                                      @Context HttpServletRequest req
    ) throws ParameterException, AplException.ValidationException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();
        ExchangeContract contract = dexService.getDexContractById(Convert.parseLong(contractId));
        if (contract == null) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "Contract does not exist"));
        }
        if (contract.getContractStatus() != ExchangeContractStatus.STEP_2) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "Expected status 'STEP2', got " + contract.getContractStatus()));
        }
        DexOrder order = dexService.getOrder(contract.getOrderId());
        if (order.getAccountId() != account.getId()) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "Order, specified in contract is not owned by account"));
        }

        Transaction transaction = dexService.sendContractStep3Transaction(req, account, transferTx, timeToReply, contract);
        return Response.ok(JSONData.unconfirmedTransaction(transaction).toJSONString()).build();
    }

    @POST
    @Path("/apl-swap")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Send apl using phased transaction linked to the contract STEP2/STEP3 and dex order", description = "Will send APL DexTransferMoney transaction with Phasing appendix, amount of transaction will be transferred when and only when another account reveals secret. " +
        "Also BUY dex order will be closed after successful approval. If phasing is expiring without approval, contract will be closed (exchange is unsuccessful ) and order, eventually, can be reopened if its finish time is not reached " +
        "Can be sent only by account, owning SELL order specified in the contract. IMPORTANT: created transaction will not be broadcasted because it require existence of the contract in STEP2/STEP3 but to make such contract" +
        " by sending appropriate transaction, you require hash of transfer transaction, which you can obtain only by creating, but not sending TransferMoney transaction. When contract transaction will be confirmed, you should broadcast your TransferMoney transaction. ",
        responses = @ApiResponse(description = "Apl transaction in JSON format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionDTO.class))))
    public Response transferMoneyWithApproval(
        @Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
        @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
        @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
        @Parameter(description = "Amount of time for transaction to expire. Use twice times less duration than left in already initiated atomic swap", required = true) @FormParam("atomicSwapDuration") @ValidAtomicSwapTime int atomicSwapDuration,
        @Parameter(description = "Id of the contract to which TransferMoney transaction will be linked.  Signed or unsigned int64/long", required = true) @FormParam("contractId") @NotBlank String contractId,
        @Parameter(description = "SHA-256 hash of the secret in the hexadecimal format", required = true) @FormParam("secretHash") @NotBlank String secretHash,
        @Parameter(description = "Amount of atms to transfer", required = true) @FormParam("amount") @Min(0) long amountAtm,
        @Context HttpServletRequest request
    ) throws ParameterException, AplException.ValidationException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();
        long contractParsedId = Convert.parseLong(contractId);
        ExchangeContract contract = dexService.getDexContractById(contractParsedId);
        if (contract == null) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "Contract does not exist"));
        }
        DexOrder sellOrder = getSellOrder(contract);
        if (sellOrder.getAccountId() != account.getId()) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "SELL order, specified in the contract is not owned by account"));
        }
        byte[] secretHashBytes = getAndValidateSecretHash(secretHash);
        long recipientId = account.getId() == contract.getRecipient() ? contract.getSender() : contract.getRecipient();
        Transaction transaction = dexService.transferApl(request, account, contractParsedId, recipientId, amountAtm, atomicSwapDuration, secretHashBytes);
        return Response.ok(JSONData.unconfirmedTransaction(transaction).toJSONString()).build();
    }

    @POST
    @Path("/eth-swap")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Create ETH swap", description = "Call eth 'initiate' function on smart contract, which will freeze eth/pax for specified time [10m..180d] and will create atomic swap, which" +
        " can be redeemed by another user by revealing secret. After expiration, atomic swap become not-redeemable, but refundable. User has to call 'refundAndWithdraw' function to get back eth/pax, or call 'refund' and after confirmation - 'withdraw' ",
        responses = @ApiResponse(description = "Eth transaction hash in hexadecimal format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionHash.class))))
    public Response initiateAtomicSwap(
        @Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
        @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
        @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
        @Parameter(description = "Amount of time for atomic swap to expire. Use twice times less duration than left in already initiated atomic swap.", required = true) @FormParam("atomicSwapDuration") @ValidAtomicSwapTime int atomicSwapDuration,
        @Parameter(description = "Id of the contract to which ETH 'atomic swap' will be linked.  Signed or unsigned int64/long", required = true) @FormParam("contractId") @NotBlank String contractId,
        @Parameter(description = "SHA-256 hash of the secret in the hexadecimal format", required = true) @FormParam("secretHash") @NotBlank String secretHash,
        @Context HttpServletRequest request
    ) throws ParameterException, AplException.ExecutiveProcessException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();
        long contractParsedId = Convert.parseLong(contractId);
        ExchangeContract contract = dexService.getDexContractById(contractParsedId);
        if (contract == null) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "Contract does not exist"));
        }
        DexOrder buyOrder = getBuyOrder(contract);
        if (buyOrder.getAccountId() != account.getId()) {
            throw new ParameterException(JSONResponses.incorrect("contractId", "BUY order, specified in the contract is not owned by account"));
        }
        byte[] secretHashBytes = getAndValidateSecretHash(secretHash);
        if (!dexService.depositExists(buyOrder.getId(), buyOrder.getFromAddress())) {
            throw new ParameterException(JSONResponses.error("ETH deposit does not exist for buy order: " + buyOrder.getId()));
        }
        String hash = dexService.initiate(accountDetails.getPassphrase(), buyOrder, getSellOrder(contract).getToAddress(), atomicSwapDuration / 60, secretHashBytes);
        return Response.ok(new TransactionHash(hash)).build();
    }

    @POST
    @Path("/eth-swap-approvals")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Redeem ETH swap", description = "Call eth 'redeem' function on smart contract, which will transfer frozen eth/pax to the caller address, specified by 'walletAddress' parameter." +
        " Account has to be whitelisted in redeemable, non-expired atomic swap ",
        responses = @ApiResponse(description = "Eth transaction hash in hexadecimal format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionHash.class))))
    public Response redeemAtomicSwap(
        @Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
        @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
        @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
        @Parameter(description = "Secret sequence of bytes represented in the hexadecimal format to redeem the swap by revealing it", required = true) @FormParam("secret") @NotBlank String secret,
        @Parameter(description = "Eth address to which eth/pax will be transferred after successful redeeming", required = true) @FormParam(DexApiConstants.WALLET_ADDRESS) String walletAddress,
        @Context HttpServletRequest request
    ) throws ParameterException, AplException.ExecutiveProcessException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();
        validator.validateEthAccount(account.getId(), accountDetails.getPassphrase(), walletAddress);
        byte[] secretBytes = getAndValidateSecretHash(secret);
        byte[] secretHash = Crypto.sha256().digest(secretBytes);
        if (!dexService.swapIsRedeemable(secretHash, walletAddress)) {
            throw new ParameterException(JSONResponses.error("Swap is not redeemable for address: " + walletAddress));
        }
        String hash = dexService.redeem(accountDetails.getPassphrase(), walletAddress, account.getId(), secretBytes);
        return Response.ok(new TransactionHash(hash)).build();
    }


    @POST
    @Path("/eth-swap-refunds")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Refund ETH swap", description = "Call eth 'refund' or 'refundAndWithdraw' function on smart contract."
        + "When 'fullRefund=true' specified 'refundAndWithdraw' function will be called, which will refund atomic swap and send eth/pax back to account, " +
        "otherwise 'refund' will be called, which will withdraw money from atomic swap and transfer it to the deposit from which money were withdrawn, after that deposit is ready for another atomic swap",
        responses = @ApiResponse(description = "Eth transaction hash in hexadecimal format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionHash.class))))
    public Response refundAtomicSwap(
        @Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
        @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
        @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
        @Parameter(description = "SHA-256 hash of the secret in the hexadecimal format", required = true) @FormParam("secretHash") @NotBlank String secretHash,
        @Parameter(description = "Eth address to which eth/pax will be transferred after successful refunding") @FormParam(DexApiConstants.WALLET_ADDRESS) String walletAddress,
        @Parameter(description = "Perform full refund of expired atomic swap or just renew deposit to prepare it for another possible atomic swap ") @FormParam("fullRefund") @DefaultValue("true") boolean fullRefund,
        @Context HttpServletRequest request
    ) throws ParameterException, AplException.ExecutiveProcessException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();
        validator.validateEthAccount(account.getId(), accountDetails.getPassphrase(), walletAddress);
        byte[] secretHashBytes = getAndValidateSecretHash(secretHash);
        if (!dexService.swapIsRefundable(secretHashBytes, walletAddress)) {
            throw new ParameterException(JSONResponses.error("Swap is not refundable for address: " + walletAddress));
        }
        String hash;
        if (fullRefund) {
            hash = dexService.refundAndWithdraw(accountDetails.getPassphrase(), walletAddress, account.getId(), secretHashBytes);
        } else {
            hash = dexService.refund(accountDetails.getPassphrase(), walletAddress, account.getId(), secretHashBytes);
        }
        return Response.ok(new TransactionHash(hash)).build();
    }

    @POST
    @Path("/eth-deposit-withdrawals")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"dex"}, summary = "Withdraw eth/pax deposits", description = "Call 'withdraw' function on smart contract, which will transfer deposit amount of eth/pax back to the account for specified order",
        responses = @ApiResponse(description = "Eth transaction hash in hexadecimal format", responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TransactionHash.class))))
    public Response withdrawEthDeposit(
        @Parameter(description = "APL account id of sender (RS, signed or unsigned int64/long)", required = true) @FormParam("sender") @NotBlank String accountString,
        @Parameter(description = "Passphrase for the vault account", required = true) @FormParam("passphrase") @NotBlank String passphrase,
        @Parameter(description = "Two factor authentication code, if 2fa is enabled") @FormParam("code2FA") @DefaultValue("0") @Min(0) @Max(999999) int code2FA,
        @Parameter(description = "Id of order for which money should to be withdrawn. Signed or unsigned int64/long", required = true) @FormParam("orderId") @NotBlank String orderIdString,
        @Parameter(description = "Eth address, which created deposit. Only deposit creator can get back money. If specified address pass the validation, money will be transferred to it after successful withdrawal") @FormParam(DexApiConstants.WALLET_ADDRESS) String walletAddress,
        @Context HttpServletRequest request
    ) throws ParameterException, AplException.ExecutiveProcessException {
        AccountDetails accountDetails = getAndVerifyAccount(accountString, passphrase, code2FA);
        Account account = accountDetails.getAccount();
        validator.validateEthAccount(account.getId(), accountDetails.getPassphrase(), walletAddress);
        long orderId = Convert.parseLong(orderIdString);
        if (!dexService.depositExists(orderId, walletAddress)) {
            throw new ParameterException(JSONResponses.error("Deposit can't be withdrawn for address: " + walletAddress));
        }
        String hash = dexService.refundEthPaxFrozenMoney(accountDetails.getPassphrase(), account.getId(), orderId, walletAddress);
        return Response.ok(new TransactionHash(hash)).build();
    }

    private DexOrder getSellOrder(ExchangeContract contract) {
        return getOrderBy(contract, OrderType.SELL);
    }

    private DexOrder getBuyOrder(ExchangeContract contract) {
        return getOrderBy(contract, OrderType.BUY);
    }

    private DexOrder getOrderBy(ExchangeContract contract, OrderType type) {
        DexOrder order = dexService.getOrder(contract.getOrderId());
        if (order.getType() == type) {
            return order;
        }
        return dexService.getOrder(contract.getCounterOrderId());
    }

    private AccountDetails getAndVerifyAccount(String accountString, String passphrase, int code2FA) throws ParameterException {
        Account account = HttpParameterParserUtil.getAccount(accountString, "sender");
        String decryptedPassphrase = HttpParameterParserUtil.getPassphrase(passphrase, true);
        validator.validateVaultAccount(account.getId(), passphrase);
        Helper2FA.verifyVault2FA(account.getId(), code2FA);
        return new AccountDetails(decryptedPassphrase, account);
    }

    private DexOrder getAndVerifyAccountOrder(String orderId, long accountId) throws ParameterException {
        long parsedOrderId = Convert.parseLong(orderId);
        DexOrder order = dexService.getOrder(parsedOrderId);
        if (order == null) {
            throw new ParameterException(JSONResponses.incorrect("orderId", "Order does not exist"));
        }
        if (order.getAccountId() != accountId) {
            throw new ParameterException(JSONResponses.incorrect("orderId", "Specified order is owned by another account"));
        }
        return order;
    }

    private DexOrder getAndVerifyCounterOrder(String counterOrderId, DexOrder order) throws ParameterException {
        DexOrder counterOrder = getOrder(counterOrderId, COUNTER_ORDER_ID);
        if (counterOrder.getAccountId().equals(order.getAccountId())) {
            throw new ParameterException(JSONResponses.incorrect(COUNTER_ORDER_ID, "Specified counterOrder is owned by account, which also owns matched order"));
        }
        if (counterOrder.getType() == order.getType()) {
            throw new ParameterException(JSONResponses.incorrect(COUNTER_ORDER_ID, "Counter order type have to be opposite to the specified order"));
        }
        return counterOrder;
    }

    private DexOrder getOrder(String orderId, String orderParameterName) throws ParameterException {
        long parsedOrderId = Convert.parseLong(orderId);
        DexOrder order = dexService.getOrder(parsedOrderId);
        if (order == null) {
            throw new ParameterException(JSONResponses.incorrect(orderParameterName, "Order does not exist"));
        }
        return order;
    }

    @Data
    @AllArgsConstructor
    private static class AccountDetails {
        private String passphrase;
        private Account account;
    }
}
