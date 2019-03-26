package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.model.CurrencyBalance;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

@Path("/wallet")
@Singleton
public class WalletController {


    private final EthereumWalletService ethereumWalletService = CDI.current().select(EthereumWalletService.class).get();


    @GET
    @Path("/eth")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get eth address amount.",
            tags = {"keyStore"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)))
            }
    )
    public Response getAccountInfo(@QueryParam("address") String address){

        if(StringUtils.isBlank(address)){
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parameter address is null.")
                    .build();
        }

        BigDecimal balance = ethereumWalletService.getBalanceEther(address);

        Response.ResponseBuilder response = Response.ok(new CurrencyBalance(balance));
        return response.build();
    }

}
