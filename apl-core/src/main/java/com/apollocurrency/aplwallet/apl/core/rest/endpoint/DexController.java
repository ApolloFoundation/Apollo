/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;


import com.apollocurrency.aplwallet.api.dto.ExchangeContractDTO;
import com.apollocurrency.aplwallet.api.request.GetEthBalancesRequest;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.converter.ExchangeContractToDTOConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.CustomRequestWrapper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderCancelAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.AddressEthDepositsInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.AddressEthExpiredSwaps;
import com.apollocurrency.aplwallet.apl.exchange.model.DBSortOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderSortBy;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderWithFreezing;
import com.apollocurrency.aplwallet.apl.exchange.model.EthDepositsWithOffset;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.WalletsBalance;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOrderTransactionCreator;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexSmartContractService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;
import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ORDER_DURATION_SEC;
import static org.slf4j.LoggerFactory.getLogger;

@Path("/dex")
@OpenAPIDefinition(info = @Info(description = "Operations with exchange."))
@Singleton
public class DexController {
    private static final Logger log = getLogger(DexController.class);

    private DexService service;
    private DexOrderTransactionCreator dexOrderTransactionCreator;
    private TimeService timeService;
    private DexEthService dexEthService;
    private EthereumWalletService ethereumWalletService;
    private Integer DEFAULT_DEADLINE_MIN = 60*2;
    private String TX_DEADLINE = "1440";
    private ObjectMapper mapper = new ObjectMapper();
    private DexSmartContractService dexSmartContractService;
    private ExchangeContractToDTOConverter contractConverter = new ExchangeContractToDTOConverter();


    @Inject
    public DexController(DexService service, DexOrderTransactionCreator dexOrderTransactionCreator, TimeService timeService, DexEthService dexEthService,
                         EthereumWalletService ethereumWalletService, DexSmartContractService dexSmartContractService) {
        this.service = Objects.requireNonNull(service, "DexService is null");
        this.dexOrderTransactionCreator = Objects.requireNonNull(dexOrderTransactionCreator, "DexOfferTransactionCreator is null");
        this.timeService = Objects.requireNonNull(timeService, "EpochTime is null");
        this.dexEthService = Objects.requireNonNull(dexEthService, "DexEthService is null");
        this.ethereumWalletService = Objects.requireNonNull(ethereumWalletService, "Ethereum Wallet Service");
    }

    //For DI
    public DexController() {
    }

    @GET
    @Path("/balance")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Balances of cryptocurrency wallets",
            description = "dexGetBalances endpoint returns cryptocurrency wallets' (ETH/PAX) balances")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets balances"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getBalances(@Parameter(description = "Addresses to get balance", required = true) @QueryParam("eth") List<String> ethAddresses
        ) throws NotFoundException {

        log.debug("getBalances: ");

        for (String ethAddress : ethAddresses) {
            log.debug("address: {}",ethAddress);
            if(!EthUtil.isAddressValid(ethAddress)){
                log.debug("Valid!");
                return Response.status(Response.Status.OK).entity(incorrect("ethAddress", "Address length is not correct.")).build();
            }
        }

        WalletsBalance customerWalletsBalance = service.getBalances(new GetEthBalancesRequest(ethAddresses));

        try {
            return Response.ok(mapper.writeValueAsString(customerWalletsBalance)).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.OK).entity(JSON.toString(JSONResponses.incorrect("Response processing exception."))).build();
        }
    }


    @POST
    @Path("/offer")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Create order", description = "get trading history for certain account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response createOffer(@Parameter(description = "Type of the APL offer. (BUY APL / SELL APL) 0/1", required = true) @FormParam("offerType") Byte offerType,
                                @Parameter(description = "From address", required = true) @FormParam("walletAddress") String walletAddress,
                                @Parameter(description = "APL amount (buy or sell) for order in Gwei (1 Gwei = 0.000000001), " +
                                        "in other words  - amount of apollo atoms multiplied by 10. 1 Gwei = 10^-9, 1 ATM = 10^-8", required = true) @FormParam("offerAmount") Long orderAmount,
                                @Parameter(description = "Paired currency. (ETH=1, PAX=2)", required = true) @FormParam("pairCurrency") Byte pairCurrency,
                                @Parameter(description = "Pair rate in Gwei. (1 Gwei = 0.000000001). Represent real pair rate, multiplied by 10^9", required = true) @FormParam("pairRate") Long pairRate,
                                @Parameter(description = "Amount of time for this offer. (seconds)", required = true) @FormParam("amountOfTime") Integer amountOfTime,
                                @Context HttpServletRequest req) throws NotFoundException {


        log.debug("createOffer: offerType: {}, walletAddress: {}, orderAmount: {}, pairCurrency: {}, pairRate: {}, amountOfTime: {}", offerType, walletAddress, orderAmount, pairCurrency, pairRate, amountOfTime);
        if (pairRate <= 0 ) {
            return Response.ok(JSON.toString(incorrect("pairRate", "Should be more than zero."))).build();
        }
        if (orderAmount <= 0) {
            return Response.ok(JSON.toString(incorrect("orderAmount", "Should be more than zero."))).build();
        }

        if (amountOfTime <= 0 || amountOfTime > MAX_ORDER_DURATION_SEC) {
            return Response.ok(
                    JSON.toString(incorrect("amountOfTime",  String.format("value %d not in range [%d-%d]", amountOfTime, 0, MAX_ORDER_DURATION_SEC)))
            ).build();
        }

        Integer currentTime = timeService.getEpochTime();
        try {
            Account account = ParameterParser.getSenderAccount(req);
            OrderType type = OrderType.getType(offerType);
            DexOrder order;
            try {
                DexCurrency pairedCurrency = DexCurrency.getType(pairCurrency);
                if (pairedCurrency == DexCurrency.APL) {
                    return Response.ok(incorrect("pairedCurrency", "APL is not allowed to be a paired currency")).build();
                }
                order = DexOrder.builder()
                        .accountId(account.getId())
                        .type(type)
                        .orderAmount(EthUtil.gweiToAtm(orderAmount))
                        .fromAddress(type.isSell() ? Convert2.defaultRsAccount(account.getId()) : walletAddress)
                        .toAddress(type.isSell() ? walletAddress : Convert2.defaultRsAccount(account.getId()))
                        .orderCurrency(DexCurrency.APL)
                        .pairCurrency(pairedCurrency)
                        .pairRate(EthUtil.gweiToEth(pairRate))
                        .status(OrderStatus.OPEN)
                        .finishTime(currentTime + amountOfTime)
                        .build();
            } catch (Exception ex) {
                return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
            }

            if (order.getOrderCurrency().equals(order.getPairCurrency())) {
                return Response.ok(JSON.toString(JSONResponses.incorrect("OfferCurrency and PairCurrency are equal."))).build();
            }

            if (order.getPairCurrency().isEthOrPax() && order.getType().isBuy()) {
                if (!EthUtil.isAddressValid(order.getFromAddress())) {
                    return Response.ok(JSON.toString(incorrect("fromAddress", " is not valid."))).build();
                }
                try {
                    Convert.parseAccountId(order.getToAddress());
                } catch (Exception ex){
                    return Response.ok(JSON.toString(incorrect("toAddress", " is not valid."))).build();
                }
            } else if (order.getPairCurrency().isEthOrPax() && order.getType().isSell()) {
                try {
                    if (!Convert2.rsAccount(account.getId()).equals(order.getFromAddress())) {
                        return Response.ok(JSON.toString(incorrect("fromAddress", "You can use only your address."))).build();
                    }
                } catch (Exception ex){
                    return Response.ok(JSON.toString(incorrect("fromAddress", " is not valid."))).build();
                }

                if (!EthUtil.isAddressValid(order.getToAddress())) {
                    return Response.ok(JSON.toString(incorrect("toAddress", " is not valid."))).build();
                }
            }

            //If we should freeze APL
            if (order.getType().isSell()) {
                if (account.getUnconfirmedBalanceATM() < order.getOrderAmount()) {
                    return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_APL)).build();
                }
            } else if (order.getPairCurrency().isEthOrPax() && order.getType().isBuy()) {
                BigInteger amount = ethereumWalletService.getEthOrPaxBalanceWei(order.getFromAddress(), order.getPairCurrency());
                BigDecimal haveToPay = EthUtil.atmToEth(order.getOrderAmount()).multiply(order.getPairRate());

                if(amount==null || amount.compareTo(EthUtil.etherToWei(haveToPay)) < 0){
                    return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_APL)).build();
                }
            }

            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(req);
            requestWrapper.addParameter("deadline", TX_DEADLINE);

            try {
                JSONStreamAware response = service.createOffer(requestWrapper, account, order);

                return Response.ok(JSON.toString(response)).build();
            } catch (AplException.ValidationException e) {
                log.error(e.getMessage(), e);
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_APL)).build();
            } catch (AplException.ThirdServiceIsNotAvailable e) {
                log.error(e.getMessage(), e);
                return Response.ok(JSON.toString(JSONResponses.error("Third service is not available, try later."))).build();
            } catch (ExecutionException e) {
                log.error(e.getMessage(), e);
                return Response.ok(JSON.toString(JSONResponses.error("Exception during work with third service."))).build();
            } catch (Exception e) { // should catch NotSufficientFundsException and NotValidTransactionException, etc
                log.error(e.getMessage(), e);
                return Response.ok(JSON.toString(JSONResponses.error(e.getMessage()))).build();
            }

        } catch (ParameterException e) {
            log.error(e.getMessage(), e);
            return Response.ok(JSON.toString(e.getErrorResponse())).build();
        }
    }

    @GET
    @Path("/offers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get exchange offers", description = "dexGetOffers endpoint list of opened pending exchange orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange offers"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getOffers(@Parameter(description = "Type of the offer. (BUY = 0 /SELL = 1)") @QueryParam("orderType") Byte orderType,
                              @Parameter(description = "Criteria by Paired currency. (APL=0, ETH=1, PAX=2)") @QueryParam("pairCurrency") Byte pairCurrency,
                              @Parameter(description = "Offer status. (Open = 0, Close = 2)") @QueryParam("status") Byte status,
                              @Parameter(description = "User account id.") @QueryParam("accountId") String accountIdStr,
                              @Parameter(description = "Return offers available for now. By default = false") @DefaultValue(value = "false") @QueryParam("isAvailableForNow") boolean isAvailableForNow,
                              @Parameter(description = "Criteria by min prise.") @QueryParam("minAskPrice") BigDecimal minAskPrice,
                              @Parameter(description = "Criteria by max prise.") @QueryParam("maxBidPrice") BigDecimal maxBidPrice,
                              @Parameter(description = "Required order freezing status") @QueryParam("hasFrozenMoney") Boolean hasFrozenMoney,
                              @Parameter(description = "Sorted by (PAIR_RATE , DB_ID)") @DefaultValue(value = "PAIR_RATE") @QueryParam("sortBy") DexOrderSortBy sortBy,
                              @Parameter(description = "Sorted order (ASC, DESC)") @DefaultValue(value = "ASC") @QueryParam("sortOrder") DBSortOrder sortOrder,
                              @Context HttpServletRequest req) throws NotFoundException {

        log.debug("getOrders:  orderType: {}, pairCurrency: {}, status: {}, accountIdStr: {}, isAvailableForNow: {}, minAskPrice: {}, maxBidPrice: {}", orderType, pairCurrency, status, accountIdStr, isAvailableForNow, minAskPrice, maxBidPrice);

        OrderType type = null;
        OrderStatus orderStatus = null;
        DexCurrency pairCur = null;
        Integer currentTime = null;
        Long accountId = null;

        //Validate
        try {
            if (orderType != null) {
                type = OrderType.getType(orderType);
            }
            if (pairCurrency != null) {
                pairCur = DexCurrency.getType(pairCurrency);
            }
            if (isAvailableForNow) {
                currentTime = timeService.getEpochTime();
            }
            if(!StringUtils.isBlank(accountIdStr)){
                accountId = Long.parseUnsignedLong(accountIdStr);
            }
            if(status != null){
                orderStatus = OrderStatus.getType(status);
            }
        } catch (Exception ex){
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        int limit = DbUtils.calculateLimit(firstIndex, lastIndex);

        log.debug("args dump, type: {}, currentTime: {}, pairCur: {}, accountId: {}, offerStatus: {}, minAskPrice: {}, maxBidPrice: {}, offset: {}, limit: {}", type, currentTime, pairCur, accountId, orderStatus, minAskPrice, maxBidPrice, firstIndex, limit);

        DexOrderDBRequest dexOrderDBRequest = DexOrderDBRequest.builder()
                .type(type != null ? type.ordinal() : null)
                .currentTime(currentTime)
                .offerCur(DexCurrency.APL.ordinal())
                .pairCur(pairCur != null ? pairCur.ordinal() : null)
                .accountId(accountId)
                .status(orderStatus)
                .minAskPrice(minAskPrice)
                .maxBidPrice(maxBidPrice)
                .offset(firstIndex)
                .limit(limit)
                .hasFrozenMoney(hasFrozenMoney)
            .sortBy(sortBy)
            .sortOrder(sortOrder)
                .build();

        List<DexOrderWithFreezing> orders = service.getOrdersWithFreezing(dexOrderDBRequest);
        return Response.ok(orders.stream()
                .map(order -> order.getDexOrder().toDto(order.isHasFrozenMoney()))
                .collect(Collectors.toList())
        ).build();
    }

    @GET
    @Path("/orders/{orderId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get exchange offers", description = "dexGetOffers endpoint list of opened pending exchange orders")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order"),
        @ApiResponse(responseCode = "200", description = "Unexpected error")})
    public Response getOrder(@PathParam("orderId") String orderIdStr) throws NotFoundException {
        long orderId = Convert.parseLong(orderIdStr);
        DexOrderWithFreezing dexOrderWithFreezing = service.getOrderWithFreezing(orderId);

        return Response.ok(dexOrderWithFreezing.getDexOrder().toDto(dexOrderWithFreezing.isHasFrozenMoney())).build();
    }

    @POST
    @Path("/offer/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Cancel Order By Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response cancelOrderByOrderID(@Parameter(description = "Order id") @FormParam("orderId") String orderId,
                                         @Context HttpServletRequest req) throws NotFoundException {

        log.debug("cancelOrderByOrderID: {}", orderId);

        try{
            Long transactionId;
            Account account = ParameterParser.getSenderAccount(req);

            if (StringUtils.isBlank(orderId)) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("id", "Can't be null."))).build();
            }
            try{
                transactionId = Long.parseUnsignedLong(orderId);
            } catch (Exception ex){
                return Response.ok(JSON.toString(incorrect("id", "Transaction ID is not correct."))).build();
            }
            DexOrder order = service.getOrder(transactionId);
            if (order == null) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("orderId", "Order was not found."))).build();
            }
            if (!order.getAccountId().equals(account.getId())) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("accountId", "Can cancel only your orders."))).build();
            }
            if (!OrderStatus.OPEN.equals(order.getStatus())) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("orderId", "Can cancel only Open orders."))).build();
            }

            String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, true));
            if(passphrase == null) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("passphrase", "Can't be null."))).build();
            }

            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(req);
            requestWrapper.addParameter("deadline", DEFAULT_DEADLINE_MIN.toString());
            DexOrderCancelAttachment dexOrderCancelAttachment = new DexOrderCancelAttachment(transactionId);

            try {

                JSONStreamAware response = dexOrderTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, dexOrderCancelAttachment, true).getJson();
                return Response.ok(JSON.toString(response)).build();
            } catch (AplException.ValidationException e) {
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_APL)).build();
            }
        } catch (ParameterException ex){
            return Response.ok(JSON.toString(ex.getErrorResponse())).build();
        }
    }

    @POST
    @Path("/withdraw")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Widthraw cryptocurrency", description = "dexWidthraw endpoint provides transferEth of Ethereum/Pax")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction hash"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response dexWithdrawPost(
                                    @NotNull @Parameter(description = "amount eth for withdraw") @FormParam("amount") BigDecimal amount,
                                    @NotNull @Parameter(description = "Send from address") @FormParam("fromAddress") String fromAddress,
                                    @NotNull @Parameter(description = "Send to address") @FormParam("toAddress") String toAddress,
                                    @NotNull @Parameter(description = "Transfer fee in GWei") @FormParam("transferFee") Long transferFee,
                                    @NotNull @Parameter(description = "crypto currency for withdraw:ETH=1/PAX=2") @FormParam("cryptocurrency") Byte cryptocurrency,
                                    @Context HttpServletRequest req) {

        log.debug("dexWithdrawPost, amount: {}, fromAddress: {}, toAddr: {}, transferFee: {}, currency: ", amount, fromAddress, toAddress, transferFee, cryptocurrency);

        DexCurrency currencies = null;
        String passphrase;
        long sender;
        try{
            passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, true));
            sender = ParameterParser.getAccountId(req, "sender", true);

            if (cryptocurrency != null) {
                currencies = DexCurrency.getType(cryptocurrency);
            }
        } catch (ParameterException ex){
            log.error(ex.getMessage(), ex);
            return Response.ok(JSON.toString(ex.getErrorResponse())).build();
        } catch (Exception ex){
            log.error(ex.getMessage(), ex);
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }

        if (currencies == null || DexCurrency.APL.equals(currencies)) {
            return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("cryptocurrency", "Withdraw can work only with Eth or Pax."))).build();
        }

        if(!EthUtil.isAddressValid(toAddress)){
            return Response.status(Response.Status.OK).entity(incorrect("toAddress", "Address length is not correct.")).build();
        }
        if(!EthUtil.isAddressValid(fromAddress)){
            return Response.status(Response.Status.OK).entity(incorrect("fromAddress", "Address length is not correct.")).build();
        }
        if(transferFee < 1 || transferFee > Integer.MAX_VALUE){
            return Response.status(Response.Status.OK).entity(incorrect("transferFee", String.format("value %d not in range [%d-%d]", transferFee, 1, Integer.MAX_VALUE))).build();
        }

        if(currencies.isEth()){
            BigDecimal eth = EthUtil.weiToEther(ethereumWalletService.getEthBalanceWei(fromAddress));
            //we cant send every thing because we should pay fee.
            if(eth==null || eth.compareTo(amount) < 1){
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_APL)).build();
            }
        } else if(currencies.isPax()){
            BigDecimal pax = EthUtil.weiToEther(ethereumWalletService.getPaxBalanceWei(fromAddress));
            if(pax==null || pax.compareTo(amount) < 0){
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_APL)).build();
            }
        }

        String transaction;
        try {
            transaction = service.withdraw(sender, passphrase, fromAddress, toAddress, amount, currencies, transferFee);
        } catch (Exception e){
            return Response.ok(JSON.toString(JSONResponses.error(e.getMessage()))).build();
        }

        if(StringUtils.isBlank(transaction)){
            return Response.ok(JSON.toString(JSONResponses.error("Transfer didn't send."))).build();
        } else {
            return Response.ok(new WithdrawResponse(transaction)).build();
        }
    }

    @GET
    @Path("/ethInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Eth gas info", description = "get gas prices for different tx speed.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Eth gas info")})
    public Response dexEthInfo(@Context SecurityContext securityContext) throws NotFoundException {
        try {
            EthGasInfo ethGasInfo = dexEthService.getEthPriceInfo();
            return Response.ok(ethGasInfo.toDto()).build();
        } catch (Exception ex){
            return Response.ok(incorrect("Gas service is not available now.")).build();
        }
    }



    @GET
    @Path("/flush")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Flush temporary keys", description = "cleanup after the exchange routine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange offers"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response flush(      @Parameter(description = "User account id.") @QueryParam("accountid") String accountIdStr,
                                @Context HttpServletRequest req) throws NotFoundException {

        log.debug("flush: accountIdStr: {}", accountIdStr);
        Long accountId = null;
        try {
            if(!StringUtils.isBlank(accountIdStr)){
                accountId = Long.parseUnsignedLong(accountIdStr);
            }
            String xpassphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, true));
            if(xpassphrase == null) {
                log.error("null passphrase is unacceptable");
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("passphrase", "Can't be null."))).build();
            }
            if ( service.flushSecureStorage(accountId, xpassphrase) ) {
                return ResponseBuilder.done().build();
            } else {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("keyData", "Key does not exist or has already been wiped"))).build();
            }

        } catch (Exception ex){
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }
    }

    @GET
    @Path("/contracts")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Retrieve dex contracts for order", description = "Lookup the database to get dex contracts associated with specified account and order with status >= STEP1",
            responses = @ApiResponse(description = "List of contracts, by default should contain 1 entry, in some cases may contain more than 1 entry (i.e. order was reopened due to expired contract; few users sent matching contract to one order) ", responseCode = "200",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExchangeContractDTO.class))))
    public Response getContractForOrder(@Parameter(description = "APL account id (RS, singed or unsigned int64/long) ") @QueryParam("accountId") String account,
                                        @Parameter(description = "Order id (signed/unsigned int64/long) ") @QueryParam("orderId") String order) {
        long accountId = Convert.parseAccountId(account);
        long orderId = Convert.parseLong(order);
        List<ExchangeContract> contracts = service.getContractsByAccountOrderFromStatus(accountId, orderId, (byte) ExchangeContractStatus.STEP_1.ordinal());
        return Response.ok(contractConverter.convert(contracts)).build();
    }

    @GET
    @Path("/eth-deposits")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Retrieve eth/pax deposits for eth address", description = "Query eth node for deposits for specified eth address",
        responses = @ApiResponse(description = "List of deposits with offset ", responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EthDepositsWithOffset.class))))
    public Response getUserActiveDeposits(@Parameter(description = "Number of first N deposits, which should be skipped during fetching (useful for pagination)") @QueryParam("offset") @PositiveOrZero long offset,
                                        @Parameter(description = "Number of deposits to extract") @QueryParam("limit") @Min(1) @Max(100) long limit,
                                        @Parameter(description = "Eth address, for which active deposits should be extracted") @QueryParam(DexApiConstants.WALLET_ADDRESS) @NotBlank String walletAddress) {

        try {
            return Response.ok(service.getUserActiveDeposits(walletAddress,offset, limit )).build();
        } catch (AplException.ExecutiveProcessException e) {
            return ResponseBuilder.apiError(ApiErrors.ETH_NODE_ERROR, e.getMessage()).build();
        }
    }

    @GET
    @Path("/eth-swaps")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Retrieve eth/pax order swaps for eth address", description = "Query eth node for orders, which participate in atomic swaps for specified eth address",
        responses = @ApiResponse(description = "List of swap deposits with offset ", responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EthDepositsWithOffset.class))))
    public Response getUsersFilledOrders(@Parameter(description = "Number of first N deposits, which should be skipped during fetching (useful for pagination)") @QueryParam("offset") @PositiveOrZero long offset,
                                         @Parameter(description = "Number of deposits to extract") @QueryParam("limit") @Min(1) @Max(100) long limit,
                                         @Parameter(description = "Eth address, for which deposits involved into atomic swap should be extracted") @QueryParam(DexApiConstants.WALLET_ADDRESS) @NotBlank String walletAddress) {

        try {
            return Response.ok(service.getUserFilledOrders(walletAddress, offset, limit)).build();
        } catch (AplException.ExecutiveProcessException e) {
            return ResponseBuilder.apiError(ApiErrors.ETH_NODE_ERROR, e.getMessage()).build();
        }
    }


    @GET
    @Path("/all-contracts")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Retrieve all versioned dex contracts for order", description = "Get all versions of dex contracts related to the specified order (including all contracts with STEP1 status and previous versions of processable contract) ",
            responses = @ApiResponse(description = "List of versioned contracts", responseCode = "200",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExchangeContractDTO.class))))
    public Response getAllVersionedContractsForOrder(@Parameter(description = "APL account id (RS, singed or unsigned int64/long) ") @QueryParam("accountId") String account,
                                                     @Parameter(description = "Order id (signed/unsigned int64/long) ") @QueryParam("orderId") String order) {
        long accountId = Convert.parseAccountId(account);
        long orderId = Convert.parseLong(order);
        List<ExchangeContract> contracts = service.getVersionedContractsByAccountOrder(accountId, orderId);
        return Response.ok(contractConverter.convert(contracts)).build();
    }

    @GET
    @Path("/eth/addresses")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, description = "Get all user addresses on the smart contract. ",
        responses = @ApiResponse(description = "List of user addresses", responseCode = "200",
            content = @Content(mediaType = "application/json")))
    public Response getAllUsers() {
        List<String> addresses;
        try {
            addresses = service.getAllUsers();
        } catch (AplException.ExecutiveProcessException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(addresses).build();
    }

    @GET
    @Path("/eth/filled-orders")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, description = "Get all users filled orders on the smart contract. ",
        responses = @ApiResponse(description = "List of user filled orders", responseCode = "200",
            content = @Content(mediaType = "application/json")))
    public Response getAllUsersFilledOrders() {
        List<AddressEthDepositsInfo> addressEthDepositsInfos;
        try {
            addressEthDepositsInfos = service.getAllFilledOrders();
        } catch (AplException.ExecutiveProcessException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(addressEthDepositsInfos).build();
    }

    @GET
    @Path("/eth/expired-swaps")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, description = "Get all users expired swaps on the smart contract. ",
        responses = @ApiResponse(description = "List of user expired swaps", responseCode = "200",
            content = @Content(mediaType = "application/json")))
    public Response getAllUsersExpiredSwaps() {
        List<AddressEthExpiredSwaps> addressEthExpiredSwaps;
        try {
            addressEthExpiredSwaps = service.getAllExpiredSwaps();
        } catch (AplException.ExecutiveProcessException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(addressEthExpiredSwaps).build();
    }


    @GET
    @Path("/eth/active-deposits")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, description = "Get all users active deposits on the smart contract. ",
        responses = @ApiResponse(description = "List of user active deposits", responseCode = "200",
            content = @Content(mediaType = "application/json")))
    public Response getAllUsersActiveDeposits() {
        List<AddressEthDepositsInfo> addressEthDepositsInfos;
        try {
            addressEthDepositsInfos = service.getAllActiveDeposits();
        } catch (AplException.ExecutiveProcessException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(addressEthDepositsInfos).build();
    }

}