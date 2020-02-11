/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import javax.imageio.ImageIO;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.apollocurrency.aplwallet.api.dto.QrDecodeDto;
import com.apollocurrency.aplwallet.api.dto.QrEncodeDto;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.RestParameters;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@Slf4j
@Path("/utils")
@OpenAPIDefinition(info = @Info(description = "Provide several utility methods used by UI"))
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
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QrEncodeDto.class)))
        })
    public Response encodeQRCode(
        @Parameter(description = "QR code data", required = true) @FormParam("qrCodeData") String qrCodeData,
        @Parameter(description = "QR code image width", allowEmptyValue = true) @FormParam("width") @DefaultValue("0") String widthStr,
        @Parameter(description = "QR code image height", allowEmptyValue = true) @FormParam("height") @DefaultValue("0") String heightStr
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
            log.error(errorMessage, ex);
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
            log.error(errorMessage, e);
            return response.error(ApiErrors.INTERNAL_SERVER_EXCEPTION, errorMessage).build();
        } catch(NotFoundException e) {
            String errorMessage = String.format("Error creating QR from qrCodeData: e = %s", e.getMessage());
            log.error(errorMessage, e); // return DTO for backward compatibility
        }
        return response.bind(dto).build();
    }

}
