package com.apollocurrency.aplwallet.apl.core.rest.endpoint;


import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.exchange.model.ApiError;
import com.apollocurrency.aplwallet.apl.exchange.model.Balances;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeBalances;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
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

    @Inject
    private DexService service = CDI.current().select(DexService.class).get();

    @GET
    @Path("/balance")
    @Produces(MediaType.APPLICATION_JSON)
    @io.swagger.annotations.ApiOperation(value = "Balances of cryptocurrency wallets", notes = "dexGetBalances endpoint returns cryptocurrency wallets' (ETH/BTC/PAX) balances", response = Balances.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Wallets balances", response = ExchangeBalances.class),

            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response getBalances(@QueryParam("eth") String ethAddress, @QueryParam("pax") String paxAddress)
            throws NotFoundException {

//        long accountId = ParameterParser.getAccountId(account, "account", true);
//        Account userAccount = Account.getAccount(accountId);
//
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
    public Response dexGetOrderOrderIDGet(@QueryParam("orderID") Long orderID, @Context SecurityContext securityContext)
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
