package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverterCreator;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.AccountIdParameter;
import com.apollocurrency.aplwallet.apl.core.rest.utils.AccountParametersParser;
import com.apollocurrency.aplwallet.apl.core.rest.validation.ValidPlatformSpecs;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Attachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.api.converter.PlatformSpecs;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
import java.util.HashSet;

@Path("/updates")
@OpenAPIDefinition(info = @Info(description = "Update v2 operations"))
@Singleton
@Setter
public class UpdateController {
    private AccountParametersParser parser;
    private TransactionCreator txCreator;
    private UnconfirmedTransactionConverter converter;
    private BlockchainConfig blockchainConfig;

    public UpdateController() {
    }

    @Inject
    public UpdateController(AccountParametersParser parser, TransactionCreator txCreator, UnconfirmedTransactionConverterCreator converterCreator, BlockchainConfig blockchainConfig) {
        this.parser = parser;
        this.txCreator = txCreator;
        this.converter = converterCreator.create(false);
        this.blockchainConfig = blockchainConfig;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Operation(tags = {"updates"}, summary = "Send update v2 transaction ", description = "Will send update v2 transaction with specified manifest url, level, platforms signed by private key holder to which certificate specified by cn + sn belongs.")
    @ApiResponse(description = "Transaction in json format", content = @Content(schema = @Schema(implementation = TransactionDTO.class)))
    @PermitAll
    @Secured2FA
    public Response sendUpdateV2(
        @Parameter(required = true) @URL(protocol = "https") @NotNull @Schema(example = "https://aws.s3/manifest-pack.zip", description = "Manifest url where release manifest is located. Https only") @FormParam("manifestUrl") String manifestUrl,
        @Parameter(required = true) @NotNull @Schema(description = "Level of update") @FormParam("level") Level level,
        @Parameter(required = true) @ValidPlatformSpecs @NotNull @Schema(example = "MAC_OS-AMD64,WINDOWS-ARM32,NoOS-NoArch,NoOS-ARM64,WINDOWS-AMD64,ALL-X86", description = "Target update platform specs, represent a set of coma separated platforms to update (each platform spec is represented by two hyphen separated params: first is a Platform(OS) such as MAC_OS, WINDOWS, LINUX; second is an Architecture, such as AMD64") @FormParam("platformSpec") PlatformSpecs platformSpec,
        @Parameter(required = true) @Schema(example = "1.42.111", description = "Version of update: 1.2.3, 1.25.10, etc") @FormParam("version") Version version,
        @Parameter(required = true) @NotBlank @URL @Schema(example = "https://example.ca.com", description = " CA identifier represented by domain name") @FormParam("cn") String cn,
        @Parameter(required = true) @Schema(example = "1", description = "Serial number of issued certificate by CA, which is used to sign update") @FormParam("serialNumber") BigInteger serialNumber,
        @Parameter(required = true) @NotNull @Schema(implementation = String.class, example = "aff3892310", description = "Signature (hexadecimal format) made on update data by utilizing private key for certificate identified by cn+serialNumber") @FormParam("signature") byte[] signature,
        @Parameter @Schema(description = "Transaction timestamp, which should be used for its creation. Must be specified in seconds since blockchain network epoch beginning") @FormParam("timestamp") @DefaultValue("0") Integer timestamp,
        @Parameter(required = true, schema = @Schema(implementation = String.class, description = "Id of account  int64 singed/unsigned or RS")) @FormParam("account") AccountIdParameter account,
        @Parameter @Schema(description = "Passphrase to vault account, should be specified if sender account is vault") @FormParam("passphrase") String passphrase,
        @Parameter @Schema(description = "Secret phrase of standard account, when specified - passphrase param will be ignored") @FormParam("secretPhrase") String secretPhrase,
        @Parameter @Schema(description = "Two-factor auth code, if 2fa enabled") @FormParam("code2FA") @DefaultValue("0") Integer code2FA,
        @Context HttpServletRequest servletRequest) {
        Account senderAccount = parser.getSenderAccount(servletRequest, "account");
        UpdateV2Attachment attachment = new UpdateV2Attachment(manifestUrl, level, version, cn, serialNumber, signature, new HashSet<>(platformSpec.getSpecList()));
        byte[] keySeed = parser.getKeySeed(servletRequest, senderAccount.getId(), false);
        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .attachment(attachment)
            .timestamp(timestamp)
            .senderAccount(senderAccount)
            .broadcast(true)
            .feeATM(blockchainConfig.getOneAPL())
            .deadlineValue("1440")
            .keySeed(keySeed)
            .publicKeyValue(Convert.toHexString(senderAccount.getPublicKey().getPublicKey()))
            .publicKey(senderAccount.getPublicKey().getPublicKey())
            .secretPhrase(secretPhrase)
            .passphrase(passphrase)
            .build();
        Transaction transaction = txCreator.createTransactionThrowingException(txRequest);
        UnconfirmedTransactionDTO txDto = converter.convert(transaction);
        return Response.ok(txDto).build();
    }
}


