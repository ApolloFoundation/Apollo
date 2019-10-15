/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.inject.Vetoed;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>The EncodeQRCode API converts a UTF-8 string to a base64-encoded
 * jpeg image of a 2-D QR (Quick Response) code, using the ZXing library.</p>
 * 
 * <p>The output qrCodeBase64 string can be incorporated into an in-line HTML
 * image like this: &lt;img src="data:image/jpeg;base64,qrCodeBase64"&gt;
 * </p>
 * 
 * <p>The output qrCodeBase64 can be input to the DecodeQRCode API to
 * recover the original qrCodeData.</p>
 * 
 * <p>Request parameters:</p>
 * 
 * <ul>
 * <li>qrCodeData - A UTF-8 text string.</li>
 * <li>width - The width of the output image in pixels, optional.</li>
 * <li>height - The height of the output image in pixels, optional.</li>
 * </ul>
 * 
 * <p>Notes:</p>
 * <ul>
 * <li>The output image consists of a centrally positioned square QR code
 * with a size which is an integer multiple of the minimum size, surrounded by 
 * sufficient white padding to achieve the requested width/height.</li>
 * <li>The default width/height of 0 results in the minimum sized output
 * image, with one pixel per black/white region of the QR code and no
 * extra padding.</li>
 * <li>To eliminate padding, the requested width/height must be an integer
 * multiple of the minimum.</li>
 * </ul>
 * 
 * <p>Response fields:</p>
 * 
 * <ul>
 * <li>qrCodeBase64 - A base64 string encoding a jpeg image of
 * the QR code.</li>
 * </ul>
 */
@Vetoed
public final class EncodeQRCode extends AbstractAPIRequestHandler {
    private static final Logger LOG = getLogger(EncodeQRCode.class);

    public EncodeQRCode() {
        super(new APITag[] {APITag.UTILS}, "qrCodeData", "width", "height");
    }
    
    @Override
    public JSONStreamAware processRequest(HttpServletRequest request)
            throws AplException {
        
        JSONObject response = new JSONObject();

        String qrCodeData = Convert.nullToEmpty(request.getParameter("qrCodeData"));

        int width = ParameterParser.getInt(request, "width", 0, 5000, false);
        int height = ParameterParser.getInt(request, "height", 0, 5000, false);
        
        try {
            Map hints = new HashMap();
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
            response.put("qrCodeBase64", base64);
        } catch(WriterException|IOException ex) {
            String errorMessage = "Error creating image from qrCodeData";
            LOG.error(errorMessage, ex);
            JSONData.putException(response, ex, errorMessage);
        }
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
