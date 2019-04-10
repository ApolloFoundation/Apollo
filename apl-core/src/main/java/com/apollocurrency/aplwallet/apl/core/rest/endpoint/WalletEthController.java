package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.response.EthTransferResponse;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

@Path("/wallet/eth")
@Singleton
public class WalletEthController {
    private final EthereumWalletService ethereumWalletService = CDI.current().select(EthereumWalletService.class).get();
    private final KeyStoreService keyStoreService = CDI.current().select(KeyStoreService.class).get();

    @POST
    @Path("/transfer")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Transfer eth from user wallet to another.",
            tags = {"keyStore"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)))
            }
    )
    public Response transferEth(@FormParam("account") String account,
                                   @FormParam("passPhrase") String passphraseReq,
                                   @FormParam("toAddress") String toAddress,
                                   @FormParam("amount") String amountEth) throws ParameterException {
        String passphraseStr = ParameterParser.getPassphrase(passphraseReq, true);
        long accountId = ParameterParser.getAccountId(account, "account", true);
        BigDecimal amount;

        if(StringUtils.isBlank(toAddress)){
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parameter 'toAddress' is null.")
                    .build();
        }

        try {
            amount = new BigDecimal(amountEth);
        } catch (NumberFormatException ex){
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parameter 'amount' is not valid.")
                    .build();
        }
        if(!keyStoreService.isKeyStoreForAccountExist(accountId)){
            return Response.status(Response.Status.BAD_REQUEST).entity("Key for this account is not exist.").build();
        }

        String transferHash = ethereumWalletService.transfer(passphraseStr, accountId, toAddress, amount);

        Response.ResponseBuilder response = Response.ok(new EthTransferResponse(transferHash));
        return response.build();
    }

}
