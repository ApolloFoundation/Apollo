/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.annotation.security.PermitAll;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.apollocurrency.aplwallet.api.dto.utils.DecodeQRCodeRequestDto;
import com.apollocurrency.aplwallet.api.dto.utils.DetectMimeTypeDto;
import com.apollocurrency.aplwallet.api.dto.utils.FullHashToIdDto;
import com.apollocurrency.aplwallet.api.dto.utils.HashDto;
import com.apollocurrency.aplwallet.api.dto.utils.HexConvertDto;
import com.apollocurrency.aplwallet.api.dto.utils.QrDecodeDto;
import com.apollocurrency.aplwallet.api.dto.utils.QrEncodeDto;
import com.apollocurrency.aplwallet.api.dto.utils.RsConvertDto;
import com.apollocurrency.aplwallet.api.request.DetectMimeTypeUploadForm;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.RestParameters;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import com.apollocurrency.aplwallet.apl.util.Search;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

/**
 * Expose several utility methods
 */
@NoArgsConstructor
@Slf4j
@Path("/utils")
@OpenAPIDefinition(info = @Info(description = "Provide several utility methods"))
@Singleton
public class UtilsController {

    private BlockchainConfig blockchainConfig;

    @Inject
    public UtilsController(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
    }

    @Path("/qrcode/encode")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "The API converts a UTF-8 string to a base64-encoded jpeg image of a 2-D QR (Quick Response) code",
        description = "The output qrCodeBase64 string can be incorporated into an in-line HTML image like this: &lt;img src=\"data:image/jpeg;base64,qrCodeBase64\"&gt; ",
        tags = {"utility"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QrEncodeDto.class)))
        })
    @PermitAll
    public Response encodeQRCode(
        @Parameter(name = "qrCodeData", description = "QR code data", required = true) // works on UI without good description
            @FormParam("qrCodeData") String qrCodeData,
        @Parameter(name = "width", description = "QR code image width, optional") // works on UI without good description
            @FormParam("width") @DefaultValue("0") String widthStr,
        @Parameter(name = "height", description = "QR code image height, optional") // works on UI without good description
            @FormParam("height") @DefaultValue("0") String heightStr
    ) {
        log.debug("Started encodeQRCode: \n\t\twidthStr={}, heightStr={}, qrCodeData={}", widthStr, heightStr, qrCodeData);
        ResponseBuilder response = ResponseBuilder.startTiming();
        if (StringUtils.isEmpty(qrCodeData)) {
            return response.error( ApiErrors.MISSING_PARAM, "qrCodeData").build();
        }
        int width;
        try {
            width = RestParameters.parseInt(widthStr, "width", 0, 5000, false);
        } catch (RestParameterException e) {
            return response.error(ApiErrors.INCORRECT_PARAM, "width", widthStr).build();
        }
        int height;
        try {
            height = RestParameters.parseInt(heightStr, "height", 0, 5000, false);
        } catch (RestParameterException e) {
            return response.error(ApiErrors.INCORRECT_PARAM, "height", heightStr).build();
        }

        QrEncodeDto dto = new QrEncodeDto();
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            // Error correction level: L (7%), M (15%), Q (25%), H (30%) -- Default L.
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0); // Default 4
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new MultiFormatWriter().encode(qrCodeData,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            );
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpeg", os);
            byte[] bytes = os.toByteArray();
            os.close();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            log.debug("base64 = {}", base64);
            dto.qrCodeBase64 = base64;
        } catch(WriterException | IOException | NullPointerException ex) {
            String errorMessage = String.format("Error creating QR from qrCodeData: %s, ex = %s", qrCodeData, ex.getMessage());
            log.warn(errorMessage, ex);
            return response.error(ApiErrors.INTERNAL_SERVER_EXCEPTION, errorMessage).build();
        }
        log.debug("encodeQRCode result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/qrcode/decode")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "The API converts a base64-encoded image of a 2-D QR (Quick Response) code to a UTF-8 string",
        description = "The API converts a base64-encoded image of a 2-D QR (Quick Response) code to a UTF-8 string. The input qrCodeBase64 can be the output of the decodeQRCode API",
        tags = {"utility"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QrDecodeDto.class)))
        })
    @PermitAll
    public Response decodeQRCode(
        @Parameter(name = "qrCodeBase64", description = "A base64 string encoded from an image of a QR code", required = true,
            schema = @Schema(implementation = DecodeQRCodeRequestDto.class)
        )
        @Form DecodeQRCodeRequestDto requestDto
    ) {
        log.debug("Started decodeQRCode: qrCodeBase64: \t{}", requestDto);
        ResponseBuilder response = ResponseBuilder.startTiming();
        if (StringUtils.isEmpty(requestDto.qrCodeBase64)) {
            return response.error(ApiErrors.MISSING_PARAM, "qrCodeBase64").build();
        }
        QrDecodeDto dto = new QrDecodeDto();
        try {
            BinaryBitmap binaryBitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(
                    ImageIO.read(new ByteArrayInputStream(
                        Base64.getDecoder().decode(requestDto.qrCodeBase64)
                    ))
                ))
            );
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

            Result qrCodeData = new MultiFormatReader().decode(binaryBitmap, hints);
            dto.qrCodeData = qrCodeData.getText();
            log.debug("qrCodeData = {}", qrCodeData);
        } catch(IOException | NullPointerException | IllegalArgumentException e) {
            String errorMessage = String.format("Error reading base64 byte stream, incorrect base64 encoding or else: e = %s", e.getMessage());
            log.warn(errorMessage, e);
            return response.error(ApiErrors.INTERNAL_SERVER_EXCEPTION, errorMessage).build();
        } catch(NotFoundException e) {
            String errorMessage = String.format("Error creating QR from qrCodeData: e = %s", e.getMessage());
            log.warn(errorMessage, e); // return DTO for backward compatibility
        }
        log.debug("decodeQRCode result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/detect/mime-type")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
        summary = "The API makes possible to detect Mime-Type of file or data field",
        description = "Try to upload file OR specify data field for detecting supplied mime-type",
        tags = {"utility"},
        requestBody = @RequestBody(
            content = @Content(
                mediaType = "multipart/form-data;charset=utf-8",
                schema = @Schema(implementation = DetectMimeTypeUploadForm.class), // class is used by swagger UI
                encoding = @Encoding(name = "UTF-8", contentType = "multipart/form-data;charset=utf-8") // doesn't affect actually
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DetectMimeTypeDto.class)))
        })
    @PermitAll
    public Response detectMimeType(
        @Parameter(hidden = true) @MultipartForm MultipartFormDataInput formDataInput
    ) {
        log.debug("Started detectMimeType : \n\t 'formDataInput' size = [{}]",
            formDataInput.getFormDataMap() != null ? formDataInput.getFormDataMap().size() : -1);
        ResponseBuilder response = ResponseBuilder.startTiming();
        if (formDataInput.getFormDataMap() == null) {
            // missing one of params to detect
            return response.error(ApiErrors.MISSING_PARAM, "file or data fields").build();
        }
        DetectMimeTypeDto dto = new DetectMimeTypeDto();
        String uploadedFileName = "unknown-uploaded-file";
        byte[] data; // bytes data to check mime
        Map<String, List<InputPart>> uploadForm = formDataInput.getFormDataMap();
        if (uploadForm.containsKey("file")) {
            List<InputPart> inputParts = uploadForm.get("file");
            for (InputPart inputPart : inputParts) {
                    MultivaluedMap<String, String> header = inputPart.getHeaders();
                    uploadedFileName = extractFileNameFromUploadHeader(header);
                    //convert the uploaded file to inputstream
                try (InputStream inputStream = inputPart.getBody(InputStream.class,null)) {
                    int nRead;
                    byte[] bytes = new byte[1024];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((nRead = inputStream.read(bytes, 0, bytes.length)) != -1) {
                        baos.write(bytes, 0, nRead);
                    }
                    data = baos.toByteArray();
                    log.debug("Read [{}] bytes content from uploaded file '{}'", nRead, uploadedFileName);
                    String detectedMimeType = Search.detectMimeType(data, uploadedFileName);
                    log.debug("Detected Mime-Type '{}'", detectedMimeType);
                    dto.type = detectedMimeType;
                } catch (Exception e) {
                    log.warn("Error reading bytes from uploaded file: " + uploadedFileName, e);
                }
            }
        } else if (formDataInput.getFormDataMap().containsKey("data")) {
            // suppose 'data' is filled with some content
            List<InputPart> inputParts = uploadForm.get("data");
            for (InputPart inputPart : inputParts) {
                try {
                    String inputString = inputPart.getBody(String.class,null);
                    if (inputString != null && !inputString.isEmpty()) {
                        data = Convert.toBytes(inputString);
                        log.debug("Read 'data' content [{}]", data.length);
                        String detectedMimeType = Search.detectMimeType(data);
                        log.debug("Detect Mime-Type '{}'", detectedMimeType);
                        dto.type = detectedMimeType;
                    }
                } catch (Exception e) {
                    log.warn("Error reading bytes from uploaded file: " + uploadedFileName, e);
                }
            }
        }
        log.debug("detectMimeType result: {}", dto);
        return response.bind(dto).build();
    }

    private String extractFileNameFromUploadHeader(MultivaluedMap<String, String> header) {
        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        log.debug("extractFileNameFromUploadHeader() - contentDisposition split = [{}]",
            Arrays.toString(contentDisposition));
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                String finalFileName = name[1].trim().replaceAll("\"", "");
                log.debug("extracted file name = '{}'", finalFileName);
                return finalFileName;
            }
        }
        log.warn("Content-Disposition file name was NOT extracted...");
        return "unknown file name";
    }

    @Path("/fullhash/toid")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API returns Long Id and Unsigned Long values by supplied UTF-8 HASH string value",
        description = "Long Id and Unsigned Long ID values are returned by supplied UTF-8 HASH string value",
        tags = {"utility"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FullHashToIdDto.class)))
        })
    public Response fullHashToId(
        @Parameter(name = "fullHash", description = "full hash data as string", required = true)
            @QueryParam("fullHash") @NotBlank String fullHash) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.debug("Started getFullHashToId : \t 'fullHash' = {}", fullHash);
        long longId = 0;
        FullHashToIdDto dto = new FullHashToIdDto();
        try {
            longId = Convert.fullHashToId(Convert.parseHexString(fullHash));
            dto.longId = String.valueOf(longId);
            dto.stringId = Long.toUnsignedString(longId);
        } catch (NumberFormatException e) {
            String errorMessage = String.format("Error converting hashed string to ID: %s, e = %s",
                fullHash, e.getMessage());
            log.warn(errorMessage, e);
            return response.error(ApiErrors.INCORRECT_PARAM, "fullHash", fullHash).build();
        }
        log.debug("getFullHashToId result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/convert/hex")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API converts HEX string into text or binary representation",
        description = "The API makes attempt to parse HEX string into text representation, if attempt fails it tries take bytes and convert them into string",
        tags = {"utility"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = HexConvertDto.class)))
        })
    public Response getHexConvert(
        @Parameter(name = "string", description = "correct HEX data as string representation", required = true)
            @QueryParam("string") @NotBlank String stringHash) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.debug("Started getHexConvert : \t 'stringHash' = {}", stringHash);
        HexConvertDto dto = new HexConvertDto();
        try {
            byte[] asHex = Convert.parseHexString(stringHash);
            if (asHex.length > 0) {
                String parsedHex = Convert.toString(asHex);
                log.debug("parsedHex = '{}'", parsedHex);
                dto.text = parsedHex;
            }
        } catch (RuntimeException ignore) {}
        try {
            byte[] asText = Convert.toBytes(stringHash);
            String bytesAsString = Convert.toHexString(asText);
            log.debug("bytesAsString = '{}'", bytesAsString);
            dto.binary = bytesAsString;
        } catch (RuntimeException ignore) {}
        log.debug("getHexConvert result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/convert/long")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API converts supplied Id string into text or long representation",
        description = "The API converts supplied Id string into Long Id text or BigInteger Id representation",
        tags = {"utility"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FullHashToIdDto.class)))
        })
    public Response longConvert(
        @Parameter(name = "id", description = "valid Id data as string representation", required = true)
            @QueryParam("id") @NotBlank String stringId) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.debug("Started getLongConvert : \t 'stringId' = {}", stringId);
        FullHashToIdDto dto = new FullHashToIdDto();
        BigInteger bigInteger = null;
        try {
            bigInteger = new BigInteger(stringId);
        } catch (NumberFormatException e) {
            String errorMessage = String.format("1. Error converting Id to Long ID: value = %s",
                stringId);
            log.warn(errorMessage);
            return response.error(ApiErrors.INCORRECT_VALUE, "id", stringId).build();
        }
        if (bigInteger.signum() < 0) {
            if (bigInteger.negate().compareTo(Convert.two64) > 0) {
                String errorMessage = String.format("1. Error converting Id to Long ID: value = %s",
                    stringId);
                log.warn(errorMessage);
                return response.error(ApiErrors.OVERFLOW_PARAM, "id", stringId).build();
            } else {
                dto.stringId = bigInteger.add(Convert.two64).toString();
                dto.longId = String.valueOf(bigInteger.longValue());
            }
        } else {
            if (bigInteger.compareTo(Convert.two64) >= 0) {
                String errorMessage = String.format("2. Error converting Id to Long ID: value = %s",
                    stringId);
                log.warn(errorMessage);
                return response.error(ApiErrors.OVERFLOW_PARAM, "id", stringId).build();
            } else {
                dto.stringId = bigInteger.toString();
                dto.longId = String.valueOf(bigInteger.longValue());
            }
        }
        log.debug("getLongConvert result: {}", dto);
        return response.bind(dto).build();
    }

    @Path("/convert/rs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API receive valid account Id string and return Account info",
        description = "The API receive valid account Id string and return Account info like account Long Id, Reed-Solomon name, account Id",
        tags = {"utility"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RsConvertDto.class)))
        })
    public Response rcConvert(
        @Parameter(name = "account", description = "existing, valid account Id", required = true)
            @QueryParam("account") @NotBlank String accountIdString) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.debug("Started getLongConvert : \t 'accountIdString' = {}", accountIdString);
        RsConvertDto dto = new RsConvertDto();
        try {
            long accountId = Convert.parseAccountId(accountIdString);
            if (accountId == 0) {
                String errorMessage = String.format("Error, invalid account ID: value = %s",
                    accountIdString);
                log.warn(errorMessage);
                return response.error(ApiErrors.INCORRECT_VALUE, "account", accountIdString).build();
            }
            dto.account = Long.toUnsignedString(accountId);
            dto.accountRS = Convert2.rsAccount(blockchainConfig.getAccountPrefix(), accountId);
            dto.accountLongId = String.valueOf(accountId);
            log.debug("getRcConvert result: {}", dto);
        } catch (RuntimeException e) {
            String errorMessage = String.format("Incorrect account ID: value = %s",
                accountIdString);
            log.warn(errorMessage, e);
            return response.error(ApiErrors.INCORRECT_PARAM_VALUE, "account", accountIdString).build();
        }
        return response.bind(dto).build();
    }

    @Path("/hash")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API hashes of passed data by using specified Hash algorithm",
        description = "The API hashes of passed data by using specified Hash algorithm, data and if it's text of binary is passed with algorithm number",
        tags = {"utility"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = HashDto.class)))
        })
    public Response hashByAlgorithm(
        @Parameter(name = "hashAlgorithm", description = "Valid Algorithm from available list", required = true,
            schema = @Schema(implementation = HashFunction.class))
            @QueryParam("hashAlgorithm") HashFunction hashAlgorithm,
        @Parameter(name = "secretIsText", description = "false (default) is HEX string, true if data is plain Text")
            @QueryParam("secretIsText") @DefaultValue("false") Boolean secretIsText,
        @Parameter(name = "secret", description = "text or data to be hashed by selected algorithm", required = true)
            @QueryParam("secret") @NotBlank String secret
    ) {
        ResponseBuilder response = ResponseBuilder.startTiming();
        log.debug("Started hashByAlgorithm : \t 'hashAlgorithm' = {}", hashAlgorithm);
        HashDto dto = new HashDto();
        try {
            byte[] secretAsByteArray = secretIsText != null && secretIsText ?
                Convert.toBytes(secret) : Convert.parseHexString(secret);
            dto.hash = Convert.toHexString(hashAlgorithm.hash(secretAsByteArray));
            log.debug("hashByAlgorithm result: {}", dto);
        } catch (RuntimeException e) {
            String errorMessage = String.format("Error hashing by an algorithm = '%s', value = '%s'",
                hashAlgorithm, secret);
            log.warn(errorMessage, e);
            return response.error(ApiErrors.INTERNAL_SERVER_EXCEPTION, errorMessage).build();
        }
        return response.bind(dto).build();
    }

}
