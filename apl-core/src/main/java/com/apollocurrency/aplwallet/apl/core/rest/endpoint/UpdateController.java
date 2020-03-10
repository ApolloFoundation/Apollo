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
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Platform;
import com.apollocurrency.aplwallet.apl.util.Version;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
import java.util.Set;

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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(tags = {"updates"}, summary = "Send update v2 transaction ", description = "Will send update v2 transaction with specified manifest url and level")
    @ApiResponse(description = "Transaction in json format", content = @Content(schema = @Schema(implementation = TransactionDTO.class)))
    @PermitAll
    @Secured2FA
    public Response sendUpdateV2(
        @Parameter(required = true) @URL(protocol = "https") @NotNull @Schema(implementation = String.class, description = "Manifest url where release manifest is located. Https only") @FormParam("manifestUrl") String manifestUrl,
        @Parameter(required = true) @NotNull @Schema(description = "Level of update") @FormParam("level") Level level,
        @Parameter(required = true) @NotNull @Schema(description = "Target update architecture") @FormParam("level") Architecture architecture,
        @Parameter(required = true) @NotNull @Schema(description = "OS for which update is intended, specify ALL to update all existing platforms") @FormParam("level") Platform platform,
        @Parameter(required = true) @NotNull @Schema(description = "Version of update: 1.2.3, 1.25.10, etc") @FormParam("version") @Pattern(regexp = "\\d+\\.\\d+\\.\\d+") Version version,
        @Parameter(required = true, schema = @Schema(implementation = String.class, description = "Id of account  int64 singed/unsigned or RS")) @FormParam("account") AccountIdParameter account,
        @Parameter @Schema(description = "Passphrase to vault account, should be specified if sender account is vault") @FormParam("passphrase") String passphrase,
        @Parameter @Schema(description = "Secret phrase of standard account, when specified - passphrase param will be ignored") @FormParam("secretPhrase") String secretPhrase,
        @Parameter @Schema(description = "Two-factor auth code, if 2fa enabled") @FormParam("code2FA") @DefaultValue("0") Integer code2FA,
        @Context HttpServletRequest servletRequest) throws ParameterException, AplException.ValidationException {
        Account senderAccount = accountService.getAccount(account.get());
        CreateTransactionRequest txRequest = HttpRequestToCreateTransactionRequestConverter.convert(servletRequest, senderAccount, 0, 0, new UpdateV2Attachment(manifestUrl, level, version, "", BigInteger.ONE, new byte[32], Set.of(new UpdateV2Attachment.PlatformPair(platform, architecture)) ), true, accountService);
        txRequest.setDeadlineValue("1440");
        txRequest.setFeeATM(Constants.ONE_APL);
        Transaction transaction = txCreator.createTransaction(txRequest);
        UnconfirmedTransactionDTO txDto = converter.convert(transaction);
        return Response.ok(txDto).build();
    }
}
