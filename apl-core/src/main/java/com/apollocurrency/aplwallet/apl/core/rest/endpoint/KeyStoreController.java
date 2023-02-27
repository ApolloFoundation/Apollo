/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FA;
import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;
import com.apollocurrency.aplwallet.apl.core.service.appdata.SecureStorageService;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.model.UserKeyStore;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.rest.converter.UserKeyStoreConverter;
import com.apollocurrency.aplwallet.vault.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.vault.service.KMSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;

@Slf4j
@Path("/keyStore")
@Singleton
public class KeyStoreController {
    private static MultipartConfigElement MULTI_PART_CONFIG;
    private UserKeyStoreConverter userKeyStoreConverter = new UserKeyStoreConverter();

    private KMSService KMSService;
    private SecureStorageService secureStorageService;
    private Integer maxKeyStoreSize;

    @Inject
    public KeyStoreController(KMSService KMSService, SecureStorageService secureStorageService,
                              @Property(name = "apl.maxKeyStoreFileSize") Integer maxKeyStoreSize) {
        this.KMSService = KMSService;
        this.secureStorageService = secureStorageService;
        this.maxKeyStoreSize = maxKeyStoreSize;
        ensureTempUploadDirOnInit();
    }

    private static void ensureTempUploadDirOnInit() {
        if (MULTI_PART_CONFIG == null) {
            String tempDir = System.getProperty("java.io.tmpdir");
            if (tempDir.isEmpty()) {
                String error = "No TMP dir is assigned!";
                log.error(error);
                throw new RuntimeException(error);
            }
            java.nio.file.Path multipartTmpDir = Paths.get(tempDir);
            try {
                ensureDirExists(multipartTmpDir);
            } catch (IOException e) {
                String error = "Can't ensure TMP dir!";
                log.error(error);
                throw new RuntimeException(e);
            }
            MULTI_PART_CONFIG = new MultipartConfigElement(tempDir);
        }
    }

    // Don't delete. For RESTEASY.
    public KeyStoreController() {
    }

    private static  java.nio.file.Path ensureDirExists( java.nio.file.Path path) throws IOException {
        java.nio.file.Path dir = path.toAbsolutePath();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    @POST
    @Path("/accountInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get user's all eth/apl wallet's public keys and addresses. " +
        "The passphrase will not be added to the response",
        tags = {"keyStore"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Response.class)))
        }
    )
    @PermitAll
    public Response getAccountInfo(@FormParam("account") String account,
                                   @FormParam("passphrase") String passphraseReq) throws ParameterException {
        String passphraseStr = HttpParameterParserUtil.getPassphrase(passphraseReq, true);
        long accountId = RestParametersParser.parseAccountId(account);

        if (!KMSService.isWalletExist(accountId)) {
            return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "Key for this account is not exist.").build();
        }

        WalletKeysInfo keyStoreInfo = KMSService.getWalletInfo(accountId, passphraseStr);

        if (keyStoreInfo == null) {
            return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "KeyStore or passPhrase is not valid.").build();
        }
        secureStorageService.addUserPassPhrase(accountId, passphraseStr);

        WalletKeysConverter walletKeysConverter = new WalletKeysConverter();
        return Response.ok(walletKeysConverter.apply(keyStoreInfo)).build();
    }


    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Import keystore container. (file)",
        tags = {"keyStore"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Response.class)))
        }
    )
    @PermitAll
    public Response importKeyStore(@Context HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/")) {
            // we must do assign that attribute to prevent 'IllegalStateException: No multipart config for servlet' error
            request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
            // Check that we have a file upload request
            Collection<Part> parts = null;
            try {
                parts = request.getParts();
            } catch (IOException | ServletException | IllegalStateException e) {
                log.error("Getting upload parts error...", e);
                return ResponseBuilder.apiError(ApiErrors.CUSTOM_ERROR_MESSAGE, "Multipart content/form was not sent ?").build();
            }
            if (parts == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            byte[] keyStore = null;
            String passPhrase = null;
            try {
                ServletFileUpload upload = new ServletFileUpload();
                // Set overall request size constraint
                upload.setSizeMax(maxKeyStoreSize);
                Iterator<Part> fileIterator = parts.iterator();

                while (fileIterator.hasNext()) {
                    Part uploadedContentPart = fileIterator.next();
                    String foundContentPart = extractFieldValueFromContent(uploadedContentPart, "passphrase", "keyStore", "filename");
                    if ("keyStore".equalsIgnoreCase(foundContentPart)) {
                        keyStore = IOUtils.toByteArray(uploadedContentPart.getInputStream());
                    } else if ("passphrase".equalsIgnoreCase(foundContentPart)) {
                        String passPhraseEncrypted = IOUtils.toString(uploadedContentPart.getInputStream());
                        // decrypt from el-gamal
                        passPhrase = HttpParameterParserUtil.getPassphrase(passPhraseEncrypted, true);
                    } else if ("filename".equalsIgnoreCase(foundContentPart)) {
                        log.debug("Vault Wallet named '{}' is being imported...", foundContentPart);
                    } else {
                        return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR,
                            "Failed to upload file. Unknown parameter:" + foundContentPart).build();
                    }
                }
            } catch (IOException | ParameterException ex) {
                log.error("Import keyStore error, upload failed: {}", ex.getMessage());
                return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "Failed to upload file." + ex).build();
            }

            if (passPhrase == null || keyStore == null || keyStore.length == 0) {
                return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "Parameter 'passPhrase' or 'keyStore' is null").build();
            }

            KMSResponseStatus status = KMSService.storeWallet(keyStore, passPhrase);

            if (status.isOK()) {
                VaultWalletResponse response = new VaultWalletResponse();
                return Response.status(Response.Status.OK).entity(response).build();
            } else {
                return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, status.message).build();
            }
        }
        // incorrect upload API usage
        return ResponseBuilder.apiError(ApiErrors.CUSTOM_ERROR_MESSAGE, "Error, No Multipart content/form was sent in request?").build();
    }

    private String extractFieldValueFromContent(Part partSource, String... fieldCheckArray) {
        String partHeader = partSource.getHeader("Content-Disposition"); // contains 'name=value' data
        String[] splitForm = partHeader.split(";");
        log.trace("getFile / partSource :\n{}\n{}", partHeader, splitForm);
        for (String content : splitForm) {
            for (String contentToFind : fieldCheckArray) {
                log.trace("getFile / partSource-content = {}, contentToFound = {}", content, contentToFind);
                // try to extract using 'content value'
                if (content.trim().toLowerCase().contains(contentToFind.toLowerCase()))
                    return content.substring(content.indexOf("=") + 2, content.length() - 1);
                // try to extract using 'content key'
                if (content.trim().toLowerCase().startsWith(contentToFind.toLowerCase()))
                    return content.substring(content.indexOf("=") + 2, content.length() - 1);
            }
        }
        return null;
    }


    @POST
    @Path("/download")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Export keystore container. (file)",
        tags = {"keyStore"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "multipart/form-data",
                    schema = @Schema(implementation = Response.class)))
        }
    )
    @Secured2FA(passphraseParamNames = {"passphrase", "passPhrase"})
    @PermitAll
    public Response downloadKeyStore(@FormParam("account") String account,
                                     @FormParam("passphrase") String passphraseReq, @Context HttpServletRequest request) throws ParameterException, IOException {
        String passphraseStr = HttpParameterParserUtil.getPassphrase(passphraseReq, false);
        String passphraseReqAlternative = request.getParameter("passPhrase");

        // backward compatibility with a previous 'passPhrase' parameter name
        String passphraseStrAlternative = HttpParameterParserUtil.getPassphrase(passphraseReqAlternative, false);
        String passphrase = passphraseStr == null ? passphraseStrAlternative : passphraseStr;
        if (StringUtils.isBlank(passphrase)) {
            return ResponseBuilder.apiError(ApiErrors.MISSING_PARAM, "passphrase").build();
        }

        long accountId = RestParametersParser.parseAccountId(account);


        if (!KMSService.isWalletExist(accountId)) {
            return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "Key for this account is not exist.").build();
        }

        UserKeyStore keyStore = KMSService.exportUserKeyStore(accountId, passphrase);
        if (keyStore == null) {
            return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "Incorrect account id or passphrase").build();
        }

        Response.ResponseBuilder response = Response.ok(userKeyStoreConverter.apply(keyStore));
        return response.build();
    }

    @POST
    @Path("/eth")
    @Secured2FA
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"keyStore"}, summary = "Export eth keystore",
        description = "Generate eth keystore for specified account in json format fully compatible with original geth keystore. Required 2fa code for accounts with enabled 2fa.",
        responses = @ApiResponse(description = "Eth wallet keystore for account in json format", responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletFile.class))))
    public Response downloadEthKeyStore(@Parameter(description = "Apl account id or rs", required = true) @FormParam("account") String account,
                                        @Parameter(description = "Eth account address", required = true) @FormParam("ethAddress") String ethAccountAddress,
                                        @Parameter(description = "Passphrase for apl vault account", required = true) @FormParam("passphrase") String passphrase,
                                        @Parameter(description = "New password to encrypt eth key, if omitted apl passphrase will be used instead (not recommended)") @FormParam("ethKeystorePassword") String ethKeystorePassword,
                                        @Parameter(description = "2fa code for account if enabled") @FormParam("code2FA") @DefaultValue("0") int code) throws ParameterException {
        String aplVaultPassphrase = HttpParameterParserUtil.getPassphrase(passphrase, true);
        long accountId = RestParametersParser.parseAccountId(account);
        String passwordToEncryptEthKeystore = StringUtils.isBlank(ethKeystorePassword) ? aplVaultPassphrase : ethKeystorePassword;

        if (!KMSService.isWalletExist(accountId)) {
            return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "Key for this account is not exist.").build();
        }

        EthWalletKey ethWalletKey = KMSService.getEthWallet(accountId, aplVaultPassphrase, ethAccountAddress);

        try {
            WalletFile walletFile = Wallet.createStandard(passwordToEncryptEthKeystore, ethWalletKey.getCredentials().getEcKeyPair());
            return Response.ok(walletFile).build();
        } catch (CipherException e) {
            return ResponseBuilder.apiError(ApiErrors.WEB3J_CRYPTO_ERROR, ThreadUtils.getStackTraceSilently(e), e.getMessage()).build();
        }
    }
}
