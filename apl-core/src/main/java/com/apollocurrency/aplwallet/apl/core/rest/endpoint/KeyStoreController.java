package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStore;
import com.apollocurrency.aplwallet.apl.eth.utils.FbWalletUtil;
import io.firstbridge.cryptolib.container.FbWallet;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.apollocurrency.aplwallet.apl.core.http.BlockEventSource.LOG;

@Path("/keyStore")
public class KeyStoreController {

    private final VaultKeyStore vaultKeyStore = CDI.current().select(VaultKeyStore.class).get();

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@Context HttpServletRequest request) {

        // Check that we have a file upload request
        if(!ServletFileUpload.isMultipartContent(request)){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        byte[] keyStore = null;
        String passPhrase = null;
        FbWallet fbWallet;

        try {
            ServletFileUpload upload = new ServletFileUpload();
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

            VaultKeyStore.Status status = vaultKeyStore.saveSecretKeyStore(passPhrase, fbWallet);

            if(status.isOK()){
                return Response.status(200).build();
            } else {
                Response.status(Response.Status.BAD_REQUEST).entity("Failed to upload file. " + status.message).build();
            }
        } catch (Exception ex){
            LOG.error(ex.getMessage(), ex);
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to upload file.").build();
        }

        return Response.status(200).entity("Uploaded file name : ").build();
    }

}
