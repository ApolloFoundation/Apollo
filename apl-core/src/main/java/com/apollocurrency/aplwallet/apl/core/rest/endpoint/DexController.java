/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;


import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import com.apollocurrency.aplwallet.api.request.GetEthBalancesRequest;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrency.aplwallet.api.trading.ConversionType;
import com.apollocurrency.aplwallet.api.trading.RateLimit;
import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.cache.DexOrderFreezingCacheConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.rest.converter.DexTradeEntryMinToDtoConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.DexTradeEntryToDtoConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.CustomRequestWrapper;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderCancelAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.*;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOrderTransactionCreator;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexSmartContractService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.apollocurrency.aplwallet.apl.util.cache.CacheType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.checkerframework.checker.units.qual.C;
import static org.slf4j.LoggerFactory.getLogger;

@Path("/dex")
@OpenAPIDefinition(tags = {@Tag(name = "/dex")}, info = @Info(description = "Operations with exchange."))
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
    private LoadingCache<Long, OrderFreezing> cache;


    @Inject
    public DexController(DexService service, DexOrderTransactionCreator dexOrderTransactionCreator, TimeService timeService, DexEthService dexEthService,
                         EthereumWalletService ethereumWalletService, DexSmartContractService dexSmartContractService,
                         @CacheProducer
                         @CacheType(DexOrderFreezingCacheConfig.CACHE_NAME) Cache<Long, OrderFreezing> cache) {
        this.service = Objects.requireNonNull(service, "DexService is null");
        this.dexOrderTransactionCreator = Objects.requireNonNull(dexOrderTransactionCreator, "DexOfferTransactionCreator is null");
        this.timeService = Objects.requireNonNull(timeService, "EpochTime is null");
        this.dexEthService = Objects.requireNonNull(dexEthService, "DexEthService is null");
        this.ethereumWalletService = Objects.requireNonNull(ethereumWalletService, "Ethereum Wallet Service");
        this.dexSmartContractService = dexSmartContractService;
        this.cache = (LoadingCache<Long, OrderFreezing>) Objects.requireNonNull(cache);
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

    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get trading history for certain account", description = "get trading history for certain account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets balances"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getHistory( @NotNull  @QueryParam("account") String account,  @QueryParam("pair") String pair,  @QueryParam("type") String type,@Context SecurityContext securityContext)
            throws NotFoundException {

        log.debug("getHistory: account: {}, pair: {}, type: {}", account, pair, type );

        return Response.ok(service.getHistory(account,pair,type)).build();
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
                DexCurrencies pairedCurrency = DexCurrencies.getType(pairCurrency);
                if (pairedCurrency == DexCurrencies.APL) {
                    return Response.ok(incorrect("pairedCurrency", "APL is not allowed to be a paired currency")).build();
                }
                order = DexOrder.builder()
                        .accountId(account.getId())
                        .type(type)
                        .orderAmount(EthUtil.gweiToAtm(orderAmount))
                        .fromAddress(type.isSell() ? Convert2.defaultRsAccount(account.getId()) : walletAddress)
                        .toAddress(type.isSell() ? walletAddress : Convert2.defaultRsAccount(account.getId()))
                        .orderCurrency(DexCurrencies.APL)
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
                    return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
                }
            } else if (order.getPairCurrency().isEthOrPax() && order.getType().isBuy()) {
                BigInteger amount = ethereumWalletService.getEthOrPaxBalanceWei(order.getFromAddress(), order.getPairCurrency());
                BigDecimal haveToPay = EthUtil.atmToEth(order.getOrderAmount()).multiply(order.getPairRate());

                if(amount==null || amount.compareTo(EthUtil.etherToWei(haveToPay)) < 0){
                    return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
                }
            }

            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(req);
            requestWrapper.addParameter("deadline", TX_DEADLINE);

            try {
                JSONStreamAware response = service.createOffer(requestWrapper, account, order);

                return Response.ok(JSON.toString(response)).build();
            } catch (AplException.ValidationException e) {
                log.error(e.getMessage(), e);
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
            } catch (AplException.ExecutiveProcessException e) {
                log.error(e.getMessage(), e);
                return Response.ok(JSON.toString(JSONResponses.error(e.getMessage()))).build();
            } catch (AplException.ThirdServiceIsNotAvailable e){
                log.error(e.getMessage(), e);
                Response.ok(JSON.toString(JSONResponses.error("Third service is not available, try later."))).build();
            } catch (ExecutionException e){
                log.error(e.getMessage(), e);
                Response.ok(JSON.toString(JSONResponses.error("Exception during work with third service."))).build();
            }

        } catch (ParameterException e) {
            log.error(e.getMessage(), e);
            return Response.ok(JSON.toString(e.getErrorResponse())).build();
        }

        return Response.ok().build();
    }

    @GET
    @Path("/offers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get exchange offers", description = "dexGetOffers endpoint list of opened pending exchange orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange offers"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getOffers(  @Parameter(description = "Type of the offer. (BUY = 0 /SELL = 1)") @QueryParam("orderType") Byte orderType,
                                @Parameter(description = "Criteria by Paired currency. (APL=0, ETH=1, PAX=2)") @QueryParam("pairCurrency") Byte pairCurrency,
                                @Parameter(description = "Offer status. (Open = 0, Close = 2)") @QueryParam("status") Byte status,
                                @Parameter(description = "User account id.") @QueryParam("accountId") String accountIdStr,
                                @Parameter(description = "Return offers available for now. By default = false") @DefaultValue(value = "false") @QueryParam("isAvailableForNow") boolean isAvailableForNow,
                                @Parameter(description = "Criteria by min prise.") @QueryParam("minAskPrice") BigDecimal minAskPrice,
                                @Parameter(description = "Criteria by max prise.") @QueryParam("maxBidPrice") BigDecimal maxBidPrice,
                                @Context HttpServletRequest req) throws NotFoundException {

        log.debug("getOrders:  orderType: {}, pairCurrency: {}, status: {}, accountIdStr: {}, isAvailableForNow: {}, minAskPrice: {}, maxBidPrice: {}", orderType, pairCurrency, status, accountIdStr, isAvailableForNow, minAskPrice, maxBidPrice);

        OrderType type = null;
        OrderStatus orderStatus = null;
        DexCurrencies pairCur = null;
        Integer currentTime = null;
        Long accountId = null;

        //Validate
        try {
            if (orderType != null) {
                type = OrderType.getType(orderType);
            }
            if (pairCurrency != null) {
                pairCur = DexCurrencies.getType(pairCurrency);
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
                .offerCur(DexCurrencies.APL.ordinal())
                .pairCur(pairCur != null ? pairCur.ordinal() : null)
                .accountId(accountId)
                .status(orderStatus)
                .minAskPrice(minAskPrice)
                .maxBidPrice(maxBidPrice)
                .offset(firstIndex)
                .limit(limit)
                .build();

        List<DexOrder> orders = service.getOrders(dexOrderDBRequest);
        return Response.ok(orders.stream()
                .map(order -> order.toDto(order.getType() == OrderType.SELL || cache.getUnchecked(order.getId()).isHasFrozenMoney()))
                .collect(Collectors.toList())
        ).build();
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
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
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

        DexCurrencies currencies = null;
        String passphrase;
        Account sender;
        try{
            passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, true));
            sender = ParameterParser.getSenderAccount(req);

            if (cryptocurrency != null) {
                currencies = DexCurrencies.getType(cryptocurrency);
            }
        } catch (ParameterException ex){
            log.error(ex.getMessage(), ex);
            return Response.ok(JSON.toString(ex.getErrorResponse())).build();
        } catch (Exception ex){
            log.error(ex.getMessage(), ex);
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }

        if (currencies == null || DexCurrencies.APL.equals(currencies)) {
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
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
            }
        } else if(currencies.isPax()){
            BigDecimal pax = EthUtil.weiToEther(ethereumWalletService.getPaxBalanceWei(fromAddress));
            if(pax==null || pax.compareTo(amount) < 0){
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
            }
        }

        String transaction;
        try {
            transaction = service.withdraw(sender.getId(), passphrase, fromAddress, toAddress, amount, currencies, transferFee);
        } catch (AplException.ExecutiveProcessException e){
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
    public Response dexEthInfo(@Context SecurityContext securityContext) throws NotFoundException, AplException.ExecutiveProcessException {
        try {
            EthGasInfo ethGasInfo = dexEthService.getEthPriceInfo();
            return Response.ok(ethGasInfo.toDto()).build();
        } catch (Exception ex){
            return Response.ok().build();
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
               return Response.ok().build(); 
            } else {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("keyData", "Key does not exist or has already been wiped"))).build();
            }
                
        } catch (Exception ex){
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }
        
    }
    
     @GET
    @Path("/histominute")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get histominute", description = "getting histominute")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange offers"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getHistominute(  @Parameter(description = "Type of exchange (coinbase)") @QueryParam("e") String e,
                                @Parameter(description = "fsym") @QueryParam("fsym") String fsym,
                                @Parameter(description = "tsym") @QueryParam("tsym") String tsym,                                
                                @Parameter(description = "toTs") @QueryParam("toTs") Integer toTs,
                                @Parameter(description = "limit") @QueryParam("limit") Integer limit,
                                @Context HttpServletRequest req) throws NotFoundException {

        log.debug("getHistominute:  fsym: {}, tsym: {}, toTs: {}, limit: {}", fsym, tsym, toTs, limit);

            int initialTime = toTs - (60*2000);
            int startGraph = initialTime;


            TradingDataOutput tradingDataOutput = new TradingDataOutput();
            
            tradingDataOutput.setResponse("Success");
            tradingDataOutput.setType(100);
            tradingDataOutput.setAggregated(false);
            
            
            List<SimpleTradingEntry> data = new ArrayList<>();
            
            int acc = 0;
            
            int width=200;
            Random r = new Random();
            
            double prevClose=50;
            
            for (int i=0; i< 2000; i++) {
                                                                    
                SimpleTradingEntry randomEntry = new SimpleTradingEntry();
                randomEntry.time = initialTime;
                
                boolean sign = (r.nextInt(2) == 1);
                
                double rWidth = r.nextInt(50) + r.nextDouble();
                
                if (sign) rWidth = -rWidth;
                                
                randomEntry.open =  prevClose;
                randomEntry.close =  randomEntry.open + rWidth;
                
                int rHigh = 50;// 15+ r.nextInt(25);
                int rLow = 50;// 15+r.nextInt(25);
                
                if (rWidth>0) {                    
                    randomEntry.high = randomEntry.close + rHigh;
                    randomEntry.low = randomEntry.open - rLow;
                } else {
                    randomEntry.high = randomEntry.open + rHigh;
                    randomEntry.low = randomEntry.close - rLow;
                }
                
                prevClose = randomEntry.close;
                                
                randomEntry.volumefrom = r.nextInt(10);
                randomEntry.volumeto = r.nextInt(50);

                initialTime += 60;
                acc += 10;
                data.add(randomEntry);
                }
            
            // Collections.reverse(data);
            tradingDataOutput.setData(data);
            
            tradingDataOutput.setTimeTo(toTs);
            
            tradingDataOutput.setTimeFrom(startGraph);
            
            
            tradingDataOutput.setFirstValueInArray(true);
           
            ConversionType conversionType = new ConversionType();
            conversionType.type = "force_direct";
            conversionType.conversionSymbol = "";
            
            tradingDataOutput.setConversionType(conversionType);
           
            // Object rateLimit = new Object();
            // tradingDataOutput.setRateLimit(rateLimit);
            tradingDataOutput.setHasWarning(false);
            
            
            return Response.ok( tradingDataOutput.toDTO() ) .build();


    }


}