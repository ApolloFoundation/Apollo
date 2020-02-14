/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.annotation.security.PermitAll;
import javax.imageio.ImageIO;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apollocurrency.aplwallet.api.dto.utils.DetectMimeTypeDto;
import com.apollocurrency.aplwallet.api.dto.utils.FullHashToIdDto;
import com.apollocurrency.aplwallet.api.dto.utils.HexConvertDto;
import com.apollocurrency.aplwallet.api.dto.utils.QrDecodeDto;
import com.apollocurrency.aplwallet.api.dto.utils.QrEncodeDto;
import com.apollocurrency.aplwallet.api.request.DetectMimeTypeUploadForm;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.RestParameters;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
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

    @Path("/qrcode/encode")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
        summary = "The API converts a UTF-8 string to a base64-encoded jpeg image of a 2-D QR (Quick Response) code",
        description = "The output qrCodeBase64 string can be incorporated into an in-line HTML image like this: &lt;img src=\"data:image/jpeg;base64,qrCodeBase64\"&gt; ",
        tags = {"utility"},
        parameters = {
            @Parameter(name = "qrCodeData", description = "QR code data", required = true),
            @Parameter(name = "width", description = "QR code image width, optional", allowEmptyValue = true,
                schema = @Schema(
                    type = "integer",
                    format = "int64",
                    description = "QR code image width positive value (0 - 5000), optional",
                    allowableValues = {"0","100","5000"}
                ),
                example = "value from 0 to 5000 maximum"
            ),
            @Parameter(name = "height", description = "QR code image height, optional", allowEmptyValue = true,
                schema = @Schema(
                    type = "integer",
                    format = "int64",
                    description = "QR code image height positive value (0 - 5000), optional",
                    allowableValues = {"0","100","5000"}
                ),
                example = "value from 0 to 5000 maximum"
            )
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QrEncodeDto.class)))
        })
    @PermitAll
    public Response encodeQRCode(
        @FormParam("qrCodeData") String qrCodeData,
        @FormParam("width") @DefaultValue("0") String widthStr,
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
        @Parameter(description = "A base64 string encoded from an image of a QR code", required = true)
        @FormParam("qrCodeBase64") String qrCodeBase64
    ) {
        log.debug("Started decodeQRCode: qrCodeBase64: \t{}", qrCodeBase64);
        ResponseBuilder response = ResponseBuilder.startTiming();
        if (StringUtils.isEmpty(qrCodeBase64)) {
            return response.error(ApiErrors.MISSING_PARAM, "qrCodeBase64").build();
        }
        QrDecodeDto dto = new QrDecodeDto();
        try {
            BinaryBitmap binaryBitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(
                    ImageIO.read(new ByteArrayInputStream(
                        Base64.getDecoder().decode(qrCodeBase64)
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
                    log.error("Error reading bytes from uploaded file: " + uploadedFileName, e);
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
                    log.error("Error reading bytes from uploaded file: " + uploadedFileName, e);
                }
            }
        }
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
        parameters = {
            @Parameter(name = "fullHash", description = "full hash data as string", required = true)
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FullHashToIdDto.class)))
        })
    public Response getFullHashToId(@QueryParam("fullHash") @NotBlank String fullHash) {
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
        return response.bind(dto).build();
    }

    @Path("/hexconvert")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "The API converts HEX string into text or binary representation",
        description = "The API makes attempt to parse HEX string into text representation, if attempt fails it tries take bytes and convert them into string",
        tags = {"utility"},
        parameters = {
            @Parameter(name = "string", description = "correct HEX data as string representation", required = true)
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = HexConvertDto.class)))
        })
    public Response getHexConvert(@QueryParam("string") @NotBlank String stringHash) {
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

        return response.bind(dto).build();
    }

}
