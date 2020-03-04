package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.AccountIdParameter;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Attachment;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOrderTransactionCreator;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Data;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/updates")
@OpenAPIDefinition(info = @Info(description = "Update v2 operations"))
@Singleton
@Setter
public class UpdateController {
    private AccountService accountService;
    private DexOrderTransactionCreator txCreator;
    private UnconfirmedTransactionConverter converter = new UnconfirmedTransactionConverter();
    public UpdateController() {
    }

    @Inject
    public UpdateController(AccountService accountService, DexOrderTransactionCreator txCreator) {
        this.accountService = accountService;
        this.txCreator = txCreator;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(tags = {"updates"}, summary = "Send update v2 transaction ", description = "Will send update v2 transaction with specified manifest url and level")
    @ApiResponse(description = "Transaction in json format", content = @Content(schema = @Schema(implementation = TransactionDTO.class)))
    @PermitAll
    @Secured2FA
    public Response sendUpdateV2(@Valid SendUpdateRequest request, @Context HttpServletRequest servletRequest) throws ParameterException, AplException.ValidationException {
        Account account = accountService.getAccount(request.getAccount().get());
        CreateTransactionRequest txRequest = HttpRequestToCreateTransactionRequestConverter.convert(servletRequest, account, 0, 0, new UpdateV2Attachment(request.getManifestUrl(), request.getLevel()), true, accountService);
        Transaction transaction = txCreator.createTransaction(txRequest);
        UnconfirmedTransactionDTO txDto = converter.convert(transaction);
        return Response.ok(txDto).build();
    }

    @Data
    private static class SendUpdateRequest {
        @Parameter(description = "Manifest url where release manifest is located. Https only", required = true)
        @org.hibernate.validator.constraints.URL(protocol = "https")
        @NotNull
        private String manifestUrl;

        @Parameter(description = "Level of update", required = true)
        @NotNull
        private Level level;

        @Parameter(description = "Id of account  int64 singed/unsigned or RS", required = true)
        @NotNull
        private AccountIdParameter account;

        @Parameter(description = "Id of account  int64 singed/unsigned or RS", required = true)
        @NotNull
        private String passphrase;

        @Parameter(description = "Fee for transaction, by default is 1apl")
        @Range(min = Constants.ONE_APL)
        @DefaultValue("" + Constants.ONE_APL)
        private long feeATM;

        @Parameter(description = "Deadline for transaction to expire in unconfirmed tx pool")
        @Positive
        @DefaultValue("1440")
        private short deadline;

    }

}
