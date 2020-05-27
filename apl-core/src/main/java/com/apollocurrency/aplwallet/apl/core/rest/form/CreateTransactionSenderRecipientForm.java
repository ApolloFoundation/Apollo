/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.form;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.rest.parameter.SenderIdParameter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Compose all fields for transaction creation
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class CreateTransactionSenderRecipientForm {

    @Parameter/*(schema = @Schema(implementation = String.class))*/
    @Schema(description = "The sender account ID. Should be specified if sender account is Vault, optional for Standard account", implementation = String.class)
    @FormParam("sender") SenderIdParameter sender;

    @Parameter @Schema(description = "Secret phrase of standard account, when specified - passphrase param will be ignored", format = "password")
    @FormParam("secretPhrase") String secretPhrase;

    @Parameter @Schema(description = "account publicKey")
    @FormParam("publicKey") String publicKey;

    @Parameter(required = true, schema = @Schema(implementation = Long.class)) @Schema(description = "fee ATM value, minimum 100000000 APL")
    @FormParam("feeATM") @NotNull
    @DefaultValue("100000000") @Positive Long feeATM = 100000000L;

    @Parameter(required = true) @Schema(description = "deadline value, minimum 1440")
    @FormParam("deadline") @NotNull @DefaultValue("1440") String deadline = "1440";

    @Parameter @Schema(description = "referenced Transaction FullHash")
    @FormParam("referencedTransactionFullHash") String referencedTransactionFullHash;

    @Parameter @Schema(description = "broadcast")
    @FormParam("broadcast") @DefaultValue("true") Boolean broadcast;

    @Parameter @Schema(description = "message")
    @FormParam("message") String message;

    @Parameter @Schema(description = "Is message Text?")
    @FormParam("messageIsText") Boolean messageIsText;

    @Parameter @Schema(description = "Is message Prunable?")
    @FormParam("messageIsPrunable") Boolean messageIsPrunable;

    @Parameter @Schema(description = "message To Encrypt")
    @FormParam("messageToEncrypt") String messageToEncrypt;

    @Parameter @Schema(description = "Is messageToEncrypt Text?")
    @FormParam("messageToEncryptIsText") Boolean messageToEncryptIsText;

    @Parameter @Schema(description = "encrypted Message Data")
    @FormParam("encryptedMessageData") String encryptedMessageData;

    @Parameter @Schema(description = "encrypted Message Nonce")
    @FormParam("encryptedMessageNonce") String encryptedMessageNonce;

    @Parameter @Schema(description = "Is encryptedMessage Prunable?")
    @FormParam("encryptedMessageIsPrunable") Boolean encryptedMessageIsPrunable;

    @Parameter @Schema(description = "compressMessageToEncrypt")
    @FormParam("compressMessageToEncrypt") Boolean compressMessageToEncrypt;

    @Parameter @Schema(description = "messageToEncryptToSelf")
    @FormParam("messageToEncryptToSelf") String messageToEncryptToSelf;

    @Parameter @Schema(description = "messageToEncryptToSelfIsText ?")
    @FormParam("messageToEncryptToSelfIsText") Boolean messageToEncryptToSelfIsText;

    @Parameter @Schema(description = "encryptToSelfMessageData")
    @FormParam("encryptToSelfMessageData") String encryptToSelfMessageData;

    @Parameter @Schema(description = "encryptToSelfMessageNonce")
    @FormParam("encryptToSelfMessageNonce") String encryptToSelfMessageNonce;

    @Parameter @Schema(description = "compressMessageToEncryptToSelf")
    @FormParam("compressMessageToEncryptToSelf") Boolean compressMessageToEncryptToSelf;

    @Parameter @Schema(description = "phased")
    @FormParam("phased") Boolean phased;

    @Parameter @Schema(description = "phasingFinishHeight")
    @FormParam("phasingFinishHeight") @Valid @DefaultValue("-1") Integer phasingFinishHeight = -1;

    @Parameter @Schema(description = "The expected voting model of the phasing", implementation = VoteWeighting.VotingModel.class)
    @FormParam("phasingVotingModel") @Valid @DefaultValue("NONE") VoteWeighting.VotingModel phasingVotingModel = VoteWeighting.VotingModel.NONE;

    @Parameter(description = "The expected phasing quorum")
    @DefaultValue("0") @FormParam("phasingQuorum") @Valid Long phasingQuorum = 0L;

    @Parameter(description = "The minimum phasing quorum")
    @DefaultValue("0") @FormParam("phasingMinBalance") @Valid Long phasingMinBalance = 0L;

    @Parameter(description = "Phasing holding id")
    @DefaultValue("0") @FormParam("phasingHolding") @Valid Long phasingHolding = 0L;

    @Parameter @Schema(required = true, description = "The expected minimum balance model. Possible values: NONE(0), ATM(1), ASSET(2), CURRENCY(3)",
        implementation = VoteWeighting.MinBalanceModel.class)
    @FormParam("phasingMinBalanceModel") @DefaultValue("NONE") @Valid VoteWeighting.VotingModel phasingMinBalanceModel = VoteWeighting.VotingModel.NONE;

    @Parameter @Schema(description = "multiple values - the expected phasing whitelisted account", implementation = String.class)
    @FormParam("phasingWhitelisted")
    List<String> phasingWhitelisted;

    @Parameter @Schema(description = "multiple values - the expected 'phasing Full Hash' as string", implementation = String.class)
    @FormParam("phasingLinkedFullHash") List<String> phasingLinkedFullHash;

    @Parameter @Schema(description = "phasing 'Hashed Secret' as HEX string", implementation = String.class)
    @FormParam("phasingHashedSecret") String phasingHashedSecret;

    @Parameter(description = "phasing Hashed 'Secret Algorithm' as byte")
    @DefaultValue("0") @FormParam("phasingHashedSecretAlgorithm") @Valid Byte phasingHashedSecretAlgorithm = 0;

    @Parameter @Schema(description = "recipient Public Key as HEX string", implementation = String.class)
    @FormParam("recipientPublicKey") String recipientPublicKey;

    @Parameter(description = "ec Block Id")
    @DefaultValue("0") @FormParam("ecBlockId") @Valid Long ecBlockId = 0L;

    @Parameter(description = "ec Block Height")
    @DefaultValue("0") @FormParam("ecBlockHeight") @Valid Integer ecBlockHeight = 0;

    @Parameter @Schema(description = "Passphrase to vault account, should be specified if sender account is vault", format = "password")
    @FormParam("passphrase") String passphrase;

    @Parameter(description = "Two-factor auth code, if 2fa enabled")
    @FormParam("code2FA") @DefaultValue("0") @Valid Integer code2FA = 0;

}
