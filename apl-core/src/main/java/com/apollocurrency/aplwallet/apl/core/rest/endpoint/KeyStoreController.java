package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.eth.utils.FbWalletUtil;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.firstbridge.cryptolib.container.FbWallet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;

import static com.apollocurrency.aplwallet.apl.core.http.BlockEventSource.LOG;

@Path("/keyStore")
@Singleton
public class KeyStoreController {

    private final KeyStoreService keyStoreService = CDI.current().select(KeyStoreService.class).get();
    private PropertiesHolder propertiesLoader = CDI.current().select(PropertiesHolder.class).get();
    private Integer maxKeyStoreSize = propertiesLoader.getIntProperty("apl.maxKeyStoreFileSize");



    @POST
    @Path("/accountInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get user eth key.",
            tags = {"keyStore"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)))
            }
    )
    public Response getAccountInfo(@FormParam("account") String account,
                                     @FormParam("passphrase") String passphraseReq) throws ParameterException {
        String passphraseStr = ParameterParser.getPassphrase(passphraseReq, true);
        long accountId = ParameterParser.getAccountId(account, "account", true);

        if(!keyStoreService.isAccountExist(accountId)){
            return Response.status(Response.Status.BAD_REQUEST).entity("Key for this account is not exist.").build();
        }

        WalletKeysInfo keyStore = keyStoreService.getWalletKeysInfo(passphraseStr, accountId);

        Response.ResponseBuilder response = Response.ok(keyStore.toJSON());
        return response.build();
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
    public Response importKeyStore(@Context HttpServletRequest request) {
        // Check that we have a file upload request
        if(!ServletFileUpload.isMultipartContent(request)){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        byte[] keyStore = null;
        String passPhrase = null;
        ApolloFbWallet fbWallet;

        try {
            ServletFileUpload upload = new ServletFileUpload();
            // Set overall request size constraint
            upload.setSizeMax(Long.valueOf(maxKeyStoreSize));
            FileItemIterator fileIterator = upload.getItemIterator(request);

            while (fileIterator.hasNext()) {
                FileItemStream item = fileIterator.next();
                if ("keyStore".equals(item.getFieldName())) {
                    keyStore = IOUtils.toByteArray(item.openStream());
                } else if ("passPhrase".equals(item.getFieldName())){
                    passPhrase = IOUtils.toString(item.openStream());
                } else {
                   return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Failed to upload file. Unknown parameter: " + item.getFieldName()).build();
                }
            }

            if(passPhrase == null || keyStore == null || keyStore.length==0){
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Parameter 'passPhrase' or 'keyStore' is null").build();
            }

            fbWallet = FbWalletUtil.buildWallet(keyStore, passPhrase);

            if(fbWallet == null){
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("KeyStore or passPhrase is not valid.").build();
            }

            KeyStoreService.Status status = keyStoreService.saveSecretKeyStore(passPhrase, fbWallet);

            if(status.isOK()){
                return Response.status(200).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Failed to upload file. " + status.message).build();
            }
        } catch (Exception ex){
            LOG.error(ex.getMessage(), ex);
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to upload file.").build();
        }

    }


    @POST
    @Path("/download")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Export keystore container. (file)",
            tags = {"keyStore"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful execution",
                            content = @Content(mediaType = "multipart/form-data",
                                    schema = @Schema(implementation = Response.class)))
            }
    )
    public Response downloadKeyStore(@FormParam("account") String account,
                                     @FormParam("passphrase") String passphraseReq) throws ParameterException {
        String passphraseStr = ParameterParser.getPassphrase(passphraseReq, true);
        long accountId = ParameterParser.getAccountId(account, "account", true);

        if(!keyStoreService.isAccountExist(accountId)){
            return Response.status(Response.Status.BAD_REQUEST).entity("Key for this account is not exist.").build();
        }

        File keyStore = keyStoreService.getSecretStoreFile(accountId, passphraseStr);

        Response.ResponseBuilder response = Response.ok(keyStore);
        response.header("Content-disposition", "attachment; filename="+ keyStore.getName());
        return response.build();
    }

}
