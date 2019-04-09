/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;


import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.ApiError;
import com.apollocurrency.aplwallet.apl.core.model.Balances;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeBalances;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOfferTransactionCreator;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
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

@Path("/dex")
@Singleton
public class DexController {

    private DexService service = CDI.current().select(DexService.class).get();
    private DexOfferTransactionCreator dexOfferTransactionCreator;

    @Inject
    public DexController(DexService service, DexOfferTransactionCreator dexOfferTransactionCreator) {
        this.service = service;
        this.dexOfferTransactionCreator = dexOfferTransactionCreator;
    }

    public DexController() {
    }

    @GET
    @Path("/balance")
    @Produces(MediaType.APPLICATION_JSON)
    @io.swagger.annotations.ApiOperation(value = "Balances of cryptocurrency wallets", notes = "dexGetBalances endpoint returns cryptocurrency wallets' (ETH/BTC/PAX) balances", response = Balances.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Wallets balances", response = ExchangeBalances.class),

            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response getBalances(@QueryParam("eth") String ethAddress, @QueryParam("pax") String paxAddress)
            throws NotFoundException {

        //Apl info don't use right now.

//        long accountId = ParameterParser.getAccountId(account, "account", true);
//        Account userAccount = Account.getAccount(accountId);
//        if (userAccount == null) {
//            return Response.ok(JSONResponses.unknownAccount(accountId)).build();
//        }

        return Response.ok(service.getBalances(ethAddress, paxAddress).balanceToJson()).build();
    }


    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    @io.swagger.annotations.ApiOperation(value = "get trading history for certain account", notes = "get trading history for certain account", response = ExchangeOrder.class, responseContainer = "List", tags={  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Wallets balances", response = ExchangeOrder.class, responseContainer = "List"),

            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response getHistory( @NotNull  @QueryParam("account") String account,  @QueryParam("pair") String pair,  @QueryParam("type") String type,@Context SecurityContext securityContext)
            throws NotFoundException {
        return Response.ok(service.getHistory(account,pair,type)).build();
    }

    @POST
    @Path("/offer")
    @Produces(MediaType.APPLICATION_JSON)
    @io.swagger.annotations.ApiOperation(value = "create offer", response = ExchangeOrder.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "OK", response = Response.class),

            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response createOffer(@QueryParam("orderID") Long orderID, @Context HttpServletRequest req)
            throws NotFoundException {

        DexOffer offer = new DexOffer();
        offer.setTransactionId(1L);
        offer.setType(OfferType.BUY);
        offer.setAccountId(1L);
        offer.setOfferAmount(100L);
        offer.setOfferCurrency(DexCurrencies.APL);
        offer.setPairCurrency(DexCurrencies.ETH);
        offer.setPairRate(new BigDecimal("0.12"));
        offer.setFinishTime(12);
//        service.saveOffer(offer);

        Account account = ParameterParser.getSenderAccount(req);
        DexOfferAttachment dexOfferAttachment = new DexOfferAttachment(offer);

        try {
            JSONStreamAware response = dexOfferTransactionCreator.createTransaction(req, account, dexOfferAttachment);
            return Response.ok(JSON.toString(response)).build();
        } catch (AplException.InsufficientBalanceException e) {
            return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
        }
    }

    @GET
    @Path("/offers")
    @Produces(MediaType.APPLICATION_JSON)
    @io.swagger.annotations.ApiOperation(value = "Get exchange offers", notes = "dexGetOffers endpoint list of opened pending exchange orders", response = ExchangeOrder.class, responseContainer = "List", tags={  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Exchange offers", response = ExchangeOrder.class, responseContainer = "List"),

            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response getOffers(  @QueryParam("account") String account,  @QueryParam("pair") String pair,  @QueryParam("type") String type,  @QueryParam("minAskPrice") BigDecimal minAskPrice,  @QueryParam("maxBidPrice") BigDecimal maxBidPrice,@Context SecurityContext securityContext)
            throws NotFoundException {
        return Response.ok(service.getOffers(account,pair,type,minAskPrice,maxBidPrice)).build();
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
