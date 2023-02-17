/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static com.apollocurrency.aplwallet.apl.core.http.BlockEventSource.LOG;

@Path("/keyStore")
@Singleton
public class KeyStoreController {

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
    }

    // Dont't delete. For RESTEASY.
    public KeyStoreController() {
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
    public Response importKeyStore(@Context HttpServletRequest request) throws IOException, ServletException {
        // Check that we have a file upload request
//        if (!ServletFileUpload.isMultipartContent(request)) {
        if (request.getParts() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        byte[] keyStore = null;
        String passPhrase = null;

        try {
            ServletFileUpload upload = new ServletFileUpload();
            // Set overall request size constraint
            upload.setSizeMax(Long.valueOf(maxKeyStoreSize));
//            FileItemIterator fileIterator = upload.getItemIterator(request);
            Iterator<Part> fileIterator = request.getParts().iterator();

            while (fileIterator.hasNext()) {
//                FileItemStream item = fileIterator.next();
                Part item = fileIterator.next();
                String fileName = getFileName(item);
//                item.write(uploadPath + File.separator + fileName);
                if ("keyStore".equals(fileName)) {
                    keyStore = IOUtils.toByteArray(item.getInputStream());
                } else if ("passPhrase".equals(fileName)) {
                    passPhrase = IOUtils.toString(item.getInputStream());
                } else {
                    return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR,
                        "Failed to upload file. Unknown parameter:" + fileName).build();
                }
            }
        } catch (/*FileUploadException | */IOException ex) {
            LOG.error(ex.getMessage(), ex);
            return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "Failed to upload file.").build();
        }

        if (passPhrase == null || keyStore == null || keyStore.length == 0) {
            return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, "Parameter 'passPhrase' or 'keyStore' is null").build();
        }

        KMSResponseStatus status = KMSService.storeWallet(keyStore, passPhrase);

        if (status.isOK()) {
            return Response.status(200).build();
        } else {
            return ResponseBuilder.apiError(ApiErrors.ACCOUNT_2FA_ERROR, status.message).build();
        }
    }

    private String getFileName(Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename"))
                return content.substring(content.indexOf("=") + 2, content.length() - 1);
        }
        throw new IllegalArgumentException("MultiPart doesn't have a fileName");
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
