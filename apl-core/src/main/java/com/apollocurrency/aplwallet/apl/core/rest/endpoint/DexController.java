/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;


import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.model.Balances;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.ApiError;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeBalances;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOfferTransactionCreator;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.json.simple.JSONStreamAware;

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
import java.util.List;
import java.util.stream.Collectors;

@Path("/dex")
@Api(value = "/dex", description = "Operations with exchange.")
@Singleton
public class DexController {

    private DexService service;
    private DexOfferTransactionCreator dexOfferTransactionCreator;
    private EpochTime epochTime;

    @Inject
    public DexController(DexService service, DexOfferTransactionCreator dexOfferTransactionCreator, EpochTime epochTime) {
        this.service = service;
        this.dexOfferTransactionCreator = dexOfferTransactionCreator;
        this.epochTime = epochTime;
    }

    //For DI
    public DexController() {
    }

    @GET
    @Path("/balance")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(tags = {"dex"}, value = "Balances of cryptocurrency wallets", notes = "dexGetBalances endpoint returns cryptocurrency wallets' (ETH/BTC/PAX) balances", response = Balances.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Wallets balances", response = ExchangeBalances.class),
            @ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response getBalances(@QueryParam("eth") String ethAddress, @QueryParam("pax") String paxAddress)
            throws NotFoundException {
        return Response.ok(service.getBalances(ethAddress, paxAddress).balanceToJson()).build();
    }


    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(tags = {"dex"}, value = "get trading history for certain account", notes = "get trading history for certain account", response = ExchangeOrder.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Wallets balances", response = ExchangeOrder.class, responseContainer = "List"),

            @ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response getHistory( @NotNull  @QueryParam("account") String account,  @QueryParam("pair") String pair,  @QueryParam("type") String type,@Context SecurityContext securityContext)
            throws NotFoundException {
        return Response.ok(service.getHistory(account,pair,type)).build();
    }

    @POST
    @Path("/offer")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(tags = {"dex"}, value = "create offer", response = ExchangeOrder.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Response.class),
            @ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response createOffer(@ApiParam(value = "Type of the offer. (BUY/SELL) 0/1", required = true) @FormParam("offerType") Byte offerType,
                                @ApiParam(value = "Offer amount in Gwei (1 Gwei = 0.000000001)", required = true) @FormParam("offerAmount") Long offerAmount,
                                @ApiParam(value = "Offered currency. (APL=0, ETH=1, PAX=2)", required = true) @FormParam("offerCurrency") Byte offerCurrency,
                                @ApiParam(value = "Paired currency. (APL=0, ETH=1, PAX=2)", required = true) @FormParam("pairCurrency") Byte pairCurrency,
                                @ApiParam(value = "Pair rate in Gwei. (1 Gwei = 0.000000001)", required = true) @FormParam("pairRate") Long pairRate,
                                @ApiParam(value = "Amount of time for this offer. (seconds)", required = true) @FormParam("amountOfTime") Integer amountOfTime,
                                @Context HttpServletRequest req) throws NotFoundException {
        Integer currentTime = epochTime.getEpochTime();
        //TODO add amountOfTime validation.

        DexOffer offer = new DexOffer();
        offer.setType(OfferType.getType(offerType));
        offer.setOfferAmount(offerAmount);
        offer.setOfferCurrency(DexCurrencies.getType(offerCurrency));
        offer.setPairCurrency(DexCurrencies.getType(pairCurrency));
        offer.setPairRate(pairRate);
        offer.setStatus(OfferStatus.OPEN);
        offer.setFinishTime(currentTime + amountOfTime);

        if(offer.getOfferCurrency().equals(offer.getPairCurrency())){
            return Response.status(Response.Status.OK).entity(JSON.toString(JSONResponses.incorrect("OfferCurrency and PairCurrency are equal."))).build();
        }

        Account account = ParameterParser.getSenderAccount(req);
        DexOfferAttachment dexOfferAttachment = new DexOfferAttachment(offer);


        try {
            JSONStreamAware response = dexOfferTransactionCreator.createTransaction(req, account, account.getId(), offerAmount, dexOfferAttachment);
//            JSONStreamAware response = dexOfferTransactionCreator.createTransaction(req, account, dexOfferAttachment);
            return Response.ok(JSON.toString(response)).build();
        } catch (AplException.InsufficientBalanceException e) {
            return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
        }
    }

    @GET
    @Path("/offers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(tags = {"dex"}, value = "Get exchange offers", notes = "dexGetOffers endpoint list of opened pending exchange orders", response = ExchangeOrder.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Exchange offers", response = ExchangeOrder.class, responseContainer = "List"),
            @ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response getOffers(  @ApiParam(value = "Type of the offer. (BUY = 0 /SELL = 1)") @QueryParam("orderType") Byte orderType,
                                @ApiParam(value = "Criteria by Offered currency. (APL=0, ETH=1, PAX=2)") @QueryParam("offerCurrency") Byte offerCurrency,
                                @ApiParam(value = "Criteria by Paired currency. (APL=0, ETH=1, PAX=2)") @QueryParam("pairCurrency") Byte pairCurrency,
                                @ApiParam(value = "User account id.") @QueryParam("accountId") String accountIdStr,
                                @ApiParam(value = "Return offers available for now.", defaultValue = "false") @DefaultValue(value = "false") @QueryParam("isAvailableForNow") boolean isAvailableForNow,
                                @ApiParam(value = "Criteria by min prise.") @QueryParam("minAskPrice") BigDecimal minAskPrice,
                                @ApiParam(value = "Criteria by max prise.") @QueryParam("maxBidPrice") BigDecimal maxBidPrice,
                                @Context SecurityContext securityContext) throws NotFoundException {
        OfferType type = null;
        DexCurrencies offerCur = null;
        DexCurrencies pairCur = null;
        Integer currentTime = null;
        Long accountId = null;

        //Validate
        try {
            if (orderType != null) {
                type = OfferType.getType(orderType);
            }
            if (offerCurrency != null) {
                offerCur = DexCurrencies.getType(offerCurrency);
            }
            if (pairCurrency != null) {
                pairCur = DexCurrencies.getType(pairCurrency);
            }
            if (isAvailableForNow) {
                currentTime = epochTime.getEpochTime();
            }
            if(!StringUtils.isBlank(accountIdStr)){
                accountId = Long.parseUnsignedLong(accountIdStr);
            }
        } catch (Exception ex){
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }

        DexOfferDBRequest dexOfferDBRequest = new DexOfferDBRequest(type, currentTime, offerCur, pairCur, accountId, minAskPrice, maxBidPrice);
        List<DexOffer> offers = service.getOffers(dexOfferDBRequest);

        return Response.ok(offers.stream()
                .map(o -> o.toDto())
                .collect(Collectors.toList())
        ).build();
    }

    @GET
    @Path("/order")
    @Produces(MediaType.APPLICATION_JSON)
    @io.swagger.annotations.ApiOperation(value = "get Order By Id", notes = "extract one order by OrderID", response = ExchangeOrder.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Order", response = ExchangeOrder.class),

            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response dexGetOrderByOrderID(@QueryParam("orderID") Long orderID, @Context SecurityContext securityContext)
            throws NotFoundException {

        return Response.ok(service.getOrderByID(orderID)).build();
    }

    @POST
    @Path("/widthraw")
    @Produces(MediaType.APPLICATION_JSON)
    @io.swagger.annotations.ApiOperation(value = "Widthraw cryptocurrency", notes = "dexWidthraw endpoint provides transfer of Ethereum", response = Response.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Wallets balances", response = Response.class),

            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = ApiError.class) })
    public Response dexWidthrawPost( @NotNull  @QueryParam("account") String account, @NotNull  @QueryParam("secretPhrase") String secretPhrase, @NotNull  @QueryParam("amount") BigDecimal amount, @NotNull  @QueryParam("address") String address, @NotNull  @QueryParam("cryptocurrency") String cryptocurrency,@Context SecurityContext securityContext)
            throws NotFoundException {
        boolean status = service.widthraw(account,secretPhrase,amount,address,cryptocurrency);

        if(!status){
            return Response.ok(new ApiError("Not Found", 404)).build();
        } else {
            return Response.ok().build();
        }
    }

}
