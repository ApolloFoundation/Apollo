/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;


import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;

import com.apollocurrency.aplwallet.api.request.GetEthBalancesRequest;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.CustomRequestWrapper;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachmentV2;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferCancelAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.model.WalletsBalance;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexMatcherServiceImpl;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOfferTransactionCreator;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import com.apollocurrency.aplwallet.apl.core.rest.converter.DexTradeEntryMinToDtoConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.DexTradeEntryToDtoConverter;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntryMin;
import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ORDER_DURATION_SEC;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Random;
import org.checkerframework.checker.units.qual.A;
import static org.slf4j.LoggerFactory.getLogger;

@Path("/dex")
@OpenAPIDefinition(tags = {@Tag(name = "/dex")}, info = @Info(description = "Operations with exchange."))
@Singleton
public class DexController {
    private static final Logger log = getLogger(DexController.class);

    private DexService service;
    private DexOfferTransactionCreator dexOfferTransactionCreator;
    private TimeService timeService;
    private DexEthService dexEthService;
    private EthereumWalletService ethereumWalletService;
    private DexMatcherServiceImpl dexMatcherService;
    private Integer DEFAULT_DEADLINE_MIN = 60*2;
    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    public DexController(DexService service, DexOfferTransactionCreator dexOfferTransactionCreator, TimeService timeService, DexEthService dexEthService,
                         EthereumWalletService ethereumWalletService, DexMatcherServiceImpl dexMatcherService) {
        this.service = Objects.requireNonNull(service,"DexService is null");
        this.dexOfferTransactionCreator = Objects.requireNonNull(dexOfferTransactionCreator,"DexOfferTransactionCreator is null");
        this.timeService = Objects.requireNonNull(timeService,"EpochTime is null");
        this.dexEthService = Objects.requireNonNull(dexEthService,"DexEthService is null");
        this.ethereumWalletService = Objects.requireNonNull(ethereumWalletService, "Ethereum Wallet Service");
        this.dexMatcherService = Objects.requireNonNull( dexMatcherService,"dexMatcherService is null");
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
    public Response createOffer(@Parameter(description = "Type of the offer. (BUY/SELL) 0/1", required = true) @FormParam("offerType") Byte offerType,
                                @Parameter(description = "From address", required = true) @FormParam("walletAddress") String walletAddress,
                                @Parameter(description = "Offer amount in Gwei (1 Gwei = 0.000000001)", required = true) @FormParam("offerAmount") Long offerAmount,
                                @Parameter(description = "Paired currency. (APL=0, ETH=1, PAX=2)", required = true) @FormParam("pairCurrency") Byte pairCurrency,
                                @Parameter(description = "Pair rate in Gwei. (1 Gwei = 0.000000001)", required = true) @FormParam("pairRate") Long pairRate,
                                @Parameter(description = "Amount of time for this offer. (seconds)", required = true) @FormParam("amountOfTime") Integer amountOfTime,
                                @Context HttpServletRequest req) throws NotFoundException {


        log.debug("createOffer: offerType: {}, walletAddress: {}, offerAmount: {}, pairCurrency: {}, pairRate: {}, amountOfTime: {}", offerType, walletAddress, offerAmount, pairCurrency, pairRate, amountOfTime );



        if (pairRate <= 0 ) {
            return Response.ok(JSON.toString(incorrect("pairRate", "Should be more than zero."))).build();
        }
        if (offerAmount <= 0 ) {
            return Response.ok(JSON.toString(incorrect("offerAmount", "Should be more than zero."))).build();
        }

        try {
            Math.multiplyExact(pairRate, offerAmount);
        } catch (ArithmeticException ex){
            return Response.ok(JSON.toString(incorrect("pairRate or offerAmount", "Are too big."))).build();
        }

        if (amountOfTime <= 0 || amountOfTime > MAX_ORDER_DURATION_SEC) {
            return Response.ok(
                    JSON.toString(incorrect("amountOfTime",  String.format("value %d not in range [%d-%d]", amountOfTime, 0, MAX_ORDER_DURATION_SEC)))
            ).build();
        }

        Integer currentTime = timeService.getEpochTime();
        JSONStreamAware response = null;
        try {
            Account account = ParameterParser.getSenderAccount(req);
            DexOffer offer = new DexOffer();
            OfferType type = OfferType.getType(offerType);

            try {
                offer.setAccountId(account.getId());
                offer.setType(type);
                //TODO refactoring it. Use Long (apollo unit of measurement)
                offer.setOfferAmount(EthUtil.gweiToApl(offerAmount));
                offer.setFromAddress(type.isSell() ? Convert2.defaultRsAccount(account.getId()) : walletAddress);
                offer.setToAddress(type.isSell() ? walletAddress : Convert2.defaultRsAccount(account.getId()));
                offer.setOfferCurrency(DexCurrencies.APL);
                offer.setPairCurrency(DexCurrencies.getType(pairCurrency));
                //TODO refactoring it. Use Long (apollo unit of measurement)
                offer.setPairRate(EthUtil.gweiToEth(pairRate));
                offer.setStatus(OfferStatus.OPEN);
                offer.setFinishTime(currentTime + amountOfTime);
            } catch (Exception ex) {
                return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
            }

            if (offer.getOfferCurrency().equals(offer.getPairCurrency())) {
                return Response.ok(JSON.toString(JSONResponses.incorrect("OfferCurrency and PairCurrency are equal."))).build();
            }

            if(offer.getPairCurrency().isEthOrPax() && offer.getType().isBuy()){
                if(!EthUtil.isAddressValid(offer.getFromAddress())) {
                    return Response.ok(JSON.toString(incorrect("fromAddress", " is not valid."))).build();
                }
                try {
                    Convert.parseAccountId(offer.getToAddress());
                } catch (Exception ex){
                    return Response.ok(JSON.toString(incorrect("toAddress", " is not valid."))).build();
                }
            } else if(offer.getPairCurrency().isEthOrPax() && offer.getType().isSell()){
                try {
                    if (!Convert2.rsAccount(account.getId()).equals(offer.getFromAddress())) {
                        return Response.ok(JSON.toString(incorrect("fromAddress", "You can use only your address."))).build();
                    }
                } catch (Exception ex){
                    return Response.ok(JSON.toString(incorrect("fromAddress", " is not valid."))).build();
                }

                if(!EthUtil.isAddressValid(offer.getToAddress())) {
                    return Response.ok(JSON.toString(incorrect("toAddress", " is not valid."))).build();
                }
            }

            //If we should freeze APL
            if (offer.getType().isSell()) {
                if (account.getUnconfirmedBalanceATM() < offer.getOfferAmount()) {
                    return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
                }
            } else if(offer.getPairCurrency().isEthOrPax() && offer.getType().isBuy()){
                BigInteger amount = ethereumWalletService.getBalanceWei(offer.getFromAddress(), offer.getPairCurrency());
                BigDecimal haveToPay = EthUtil.aplToEth(offer.getOfferAmount()).multiply(offer.getPairRate());

                if(amount==null || amount.compareTo(EthUtil.etherToWei(haveToPay)) < 0){
                    return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
                }
            }

            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(req);
            requestWrapper.addParameter("deadline", "1440");
            DexOfferAttachmentV2 dexOfferAttachment = new DexOfferAttachmentV2(offer);
            String freezeTx=null;

            // Looks like this is the correct position for event handling
            // for matcher.

            DexOffer counterOffer = dexMatcherService.findCounterOffer(offer);

            try {
                //TODO move it in to service.
                if (counterOffer != null) {
                    byte[] secretX = new byte[32];
                    Crypto.getSecureRandom().nextBytes(secretX);
                    byte[] secretHash = Crypto.sha256().digest(secretX);
                    String passphrase = ParameterParser.getPassphrase(req, true);

                    byte[] encryptedSecretX = Crypto.aesGCMEncrypt(secretX, Crypto.sha256().digest(Convert.toBytes(passphrase)));

                    //SELL APL
                    if (offer.getType().isSell()) {
                        long recipientId = Convert.parseAccountId(counterOffer.getToAddress());

                        CreateTransactionRequest transferMoneyWithApprovalRequest = HttpRequestToCreateTransactionRequestConverter
                                .convert(req, account, recipientId, offer.getOfferAmount(), null);
                        String transactionId = service.transferMoneyWithApproval(transferMoneyWithApprovalRequest, offer, secretHash);

                        DexContractAttachment contractAttachment = new DexContractAttachment(offer.getTransactionId(), counterOffer.getTransactionId(), secretHash, transactionId, encryptedSecretX);
                        response = dexOfferTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, contractAttachment);
                    } else if (offer.getType().isBuy() && offer.getPairCurrency().isEthOrPax()) {
                        //TODO Implement it for ETH/PAX
                    }

                } else {
                    if (offer.getPairCurrency().isEthOrPax() && offer.getType().isBuy()) {
                        String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, true));
                        freezeTx = service.freezeEthPax(passphrase, offer);
                    }

                    response = dexOfferTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, dexOfferAttachment);
                    if (freezeTx != null) {
                        ((JSONObject) response).put("frozenTx", freezeTx);
                    }
                }
                return Response.ok(JSON.toString(response)).build();
            } catch (AplException.ValidationException e) {
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
            } catch (AplException.ExecutiveProcessException e) {
                return Response.ok(JSON.toString(JSONResponses.error(e.getMessage()))).build();
            } catch (AplException.ThirdServiceIsNotAvailable e){
                Response.ok(JSON.toString(JSONResponses.error("Third service is not available, try later."))).build();
            } catch (ExecutionException e){
                Response.ok(JSON.toString(JSONResponses.error("Exception during work with third service."))).build();
            }

        } catch (ParameterException ex){
            return Response.ok(JSON.toString(ex.getErrorResponse())).build();
        }

        return Response.ok(JSON.toString(response)).build();
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

        log.debug("getOffers:  orderType: {}, pairCurrency: {}, status: {}, accountIdStr: {}, isAvailableForNow: {}, minAskPrice: {}, maxBidPrice: {}",orderType, pairCurrency,status, accountIdStr,isAvailableForNow, minAskPrice, maxBidPrice);

        OfferType type = null;
        OfferStatus offerStatus = null;
        DexCurrencies pairCur = null;
        Integer currentTime = null;
        Long accountId = null;

        //Validate
        try {
            if (orderType != null) {
                type = OfferType.getType(orderType);
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
                offerStatus = OfferStatus.getType(status);
            }
        } catch (Exception ex){
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        int offset = firstIndex > 0 ? firstIndex : 0;
        int limit = DbUtils.calculateLimit(firstIndex, lastIndex);

        log.debug("args dump, type: {}, currentTime: {}, pairCur: {}, accountId: {}, offerStatus: {}, minAskPrice: {}, maxBidPrice: {}, offset: {}, limit: {}", type, currentTime, pairCur, accountId, offerStatus, minAskPrice, maxBidPrice, offset, limit );

        DexOfferDBRequest dexOfferDBRequest = new DexOfferDBRequest(type, currentTime, DexCurrencies.APL, pairCur, accountId, offerStatus, minAskPrice, maxBidPrice, offset, limit);
        List<DexOffer> offers = service.getOffers(dexOfferDBRequest);

        return Response.ok(offers.stream()
                .map(o -> o.toDto())
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
    public Response cancelOrderByOrderID(@Parameter(description = "Order id") @FormParam("orderId") String transactionIdStr,
                                         @Context HttpServletRequest req) throws NotFoundException {

        log.debug("cancelOrderByOrderID: {}", transactionIdStr);

        try{
            Long transactionId;
            Account account = ParameterParser.getSenderAccount(req);

            if (StringUtils.isBlank(transactionIdStr)) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("transactionId", "Can't be null."))).build();
            }
            try{
                transactionId = Long.parseUnsignedLong(transactionIdStr);
            } catch (Exception ex){
                return Response.ok(JSON.toString(incorrect("transactionId", "Transaction ID is not correct."))).build();
            }
            DexOffer offer = service.getOfferByTransactionId(transactionId);
            if(offer == null) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("orderId", "Order was not found."))).build();
            }
            if(!Long.valueOf(offer.getAccountId()).equals(account.getId())){
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("accountId", "Can cancel only your orders."))).build();
            }
            if(!OfferStatus.OPEN.equals(offer.getStatus())) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("orderId", "Can cancel only Open orders."))).build();
            }

            if(service.isThereAnotherCancelUnconfirmedTx(transactionId, null)){
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("orderId", "There is another cancel transaction for this order in the unconfirmed tx pool already."))).build();
            }

            String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, true));
            if(passphrase == null) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("passphrase", "Can't be null."))).build();
            }

            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(req);
            requestWrapper.addParameter("deadline", DEFAULT_DEADLINE_MIN.toString());
            DexOfferCancelAttachment dexOfferCancelAttachment = new DexOfferCancelAttachment(transactionId);
            String freezeTx=null;

            try {
                if(offer.getPairCurrency().isEthOrPax() && offer.getType().isBuy()) {
                    freezeTx = service.refundEthPaxFrozenMoney(passphrase, offer);
                }

                JSONStreamAware response = dexOfferTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, dexOfferCancelAttachment);
                if(freezeTx != null){
                    ((JSONObject)response).put("frozenTx", freezeTx);
                }
                return Response.ok(JSON.toString(response)).build();
            } catch (AplException.ValidationException e) {
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
            } catch (AplException.ExecutiveProcessException e) {
                return Response.ok(JSON.toString(JSONResponses.error(e.getMessage()))).build();
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
    @Path("/tradeInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get trade data", description = "obtaining trading information for the given period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange offers"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getTradeInfoForPeriod(  
                                @Parameter(description = "period start time", required = true) @QueryParam("start") Integer start,
                                @Parameter(description = "period finish time", required = true) @QueryParam("finish") Integer finish,
                                @Parameter(description = "Paired currency. (APL=0, ETH=1, PAX=2)", required = true) @QueryParam("pairCurrency") Byte pairCurrency,
                                @Context HttpServletRequest req) throws NotFoundException {

        log.debug("getTradeInfoForPeriod:  start: {}, finish: {} ", start, finish );
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        int offset = firstIndex > 0 ? firstIndex : 0;
        int limit = DbUtils.calculateLimit(firstIndex, lastIndex);
        
        List<DexTradeEntry> tradeEntries = service.getTradeInfoForPeriod(start, finish, pairCurrency, offset, limit);
        
        return Response.ok(tradeEntries.stream()
            .map(o -> {
            return new DexTradeEntryToDtoConverter().apply(o); 
        })
            .collect(Collectors.toList())
        ).build();
    }

    @GET
    @Path("/tradeInfoMin")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get trade data", description = "obtaining trading information for the given period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange offers"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getTradeInfoForPeriodMin(  
                                @Parameter(description = "period start time", required = true) @QueryParam("start") Integer start,
                                @Parameter(description = "period finish time", required = true) @QueryParam("finish") Integer finish,
                                @Parameter(description = "Paired currency. (APL=0, ETH=1, PAX=2)", required = true) @QueryParam("pairCurrency") Byte pairCurrency,
                                @Context HttpServletRequest req) throws NotFoundException {

        log.debug("getTradeInfoForPeriod:  start: {}, finish: {} ", start, finish );
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        int offset = firstIndex > 0 ? firstIndex : 0;
        int limit = DbUtils.calculateLimit(firstIndex, lastIndex);
        
        List<DexTradeEntry> tradeEntries = service.getTradeInfoForPeriod(start, finish, pairCurrency, offset, limit);
                
        DexTradeEntryMin dexTradeEntryMin = new DexTradeEntryMin(); // dexTradeInfoMinDto = new DexTradeInfoMinDto();
        
        DexTradeEntryToDtoConverter cnv = new DexTradeEntryToDtoConverter();
        
        BigDecimal hi = cnv.apply( tradeEntries.get(0)).pairRate;//,low=t,open,close; 
        BigDecimal low = cnv.apply( tradeEntries.get(0)).pairRate;
        BigDecimal open = cnv.apply( tradeEntries.get(0) ).pairRate;
        BigDecimal close = cnv.apply( tradeEntries.get( tradeEntries.size()-1 )).pairRate;
        
        // iterate list to find the highest or the lowest values
        for (DexTradeEntry currEl : tradeEntries) {    
            DexTradeInfoDto currElDto = cnv.apply(currEl);
            if ( currElDto.pairRate.compareTo( hi ) == 1 ) hi = currElDto.pairRate;
            if ( currElDto.pairRate.compareTo( low ) == -1 ) low = currElDto.pairRate;
        }
        
        dexTradeEntryMin.setHi(hi);
        dexTradeEntryMin.setLow(low);
        dexTradeEntryMin.setOpen(open);
        dexTradeEntryMin.setClose(close);
        
        return Response.ok( ( new DexTradeEntryMinToDtoConverter().apply(dexTradeEntryMin))).build();
                
    }

    
    @GET
    @Path("/ethInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Eth gas info", description = "get gas prices for different tx speed.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Eth gas info")})
    public Response dexEthInfo(@Context SecurityContext securityContext) throws NotFoundException, ExecutionException {

        log.debug("dexEthInfo: ");

        try {
            EthGasInfo ethGasInfo = dexEthService.getEthPriceInfo();
            return Response.ok(ethGasInfo.toDto()).build();
        } catch (Exception ex){
            return Response.ok().build();
        }
    }

}