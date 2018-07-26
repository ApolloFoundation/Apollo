/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.RSAUtil;
import com.apollocurrency.aplwallet.apl.util.Convert;

import java.security.PublicKey;
import java.util.Objects;
import java.util.Scanner;

/**
 * Usage:
 * You can decrypt your message only if you have x509 certificate with public key and hexadecimal string that represents bytes of encrypted message
 * There are two different ways to decrypt message:
 * 1. Interactive - run this class without input parameters and follow the instructions
 * 2. With parameters - run class with parameters
 * For running with parameters you should pass 3 parameters in following order:
 * a) certificate path (absolute path is better)
 * b) hexadecimal string of encrypted message bytes
 * c) boolean flag that indicates that you want to make from decrypted bytes real message string
 * Example: java com.apollocurrency.aplwallet.apl.tools.RSADecryption C:/user/admin/certificate.pem f4ac4942(another 128, 256, 512 hexadecimal signs) true
 */

public class RSADecryption {
    public static void main(String [] args) throws Exception {
        String certificatePathString = "";
        String encryptedMessageString = "";
        String convertToString = "";
        if (args == null || args.length == 0) {
            Scanner sc = new Scanner(System.in);

            System.out.println("Enter encrypted message in hexadecimal format. If you want to decrypt double encrypted message separate 512 byte messages with coma or semicolon");
            encryptedMessageString = sc.nextLine();

            System.out.println("Enter certificate path. If you want to decrypt double encrypted message completely - separate certificate paths with coma or semicolon");
            certificatePathString = sc.nextLine();

            System.out.println("Do you want to convert bytes to UTF-8 string ('true' or 'false')");
            convertToString = sc.nextLine();

            sc.close();

        } else if (args.length == 3) {
            encryptedMessageString = args[0];
            certificatePathString = args[1];
            convertToString = args[2];
            Objects.requireNonNull(certificatePathString);
            Objects.requireNonNull(encryptedMessageString);
            Objects.requireNonNull(convertToString);
        } else {
            System.out.println("Invalid number of parameters: " + args.length + ". Required 3 or no parameters at all");
            System.exit(1);
        }
        System.out.println("Got public key path " + certificatePathString);
        System.out.println("Got encrypted message: \'" + encryptedMessageString + "\'");
        System.out.println("Convert to string: " + convertToString);
        String[] split = encryptedMessageString.split("([;,]+)|(\\s+)");
        boolean isSplittedMessage = split.length == 2;
        if (split.length > 2 || split.length == 0) {
            System.out.println("Invalid message string.");
            return;
        }
        String[] split1 = certificatePathString.split("([;,]+)|(\\s+)");
        if (split1.length > 2 || split1.length == 0) {
            System.out.println("Invalid certificate string");
        }

        boolean isDoubleDecryptionRequired = split1.length == 2;
        boolean isConvert;
        try {
            isConvert = Boolean.parseBoolean(convertToString);
        }
        catch (Exception e) {
            isConvert = false;
        }
        byte[] result;
        PublicKey publicKey1 = RSAUtil.getPublicKeyFromCertificate(split1[0]);
        if (isSplittedMessage) {
            byte[] firstMessagePart = Convert.parseHexString(split[0]);
            byte[] secondMessagePart = Convert.parseHexString(split[1]);
            DoubleByteArrayTuple encryptedBytes = new DoubleByteArrayTuple(firstMessagePart, secondMessagePart);
            if (isDoubleDecryptionRequired) {
                PublicKey publicKey2 = RSAUtil.getPublicKeyFromCertificate(split1[1]);
                result = RSAUtil.doubleDecrypt(publicKey1, publicKey2, encryptedBytes);
            } else {
                result = RSAUtil.firstDecrypt(publicKey1, encryptedBytes);
            }
        } else {
            result = RSAUtil.decrypt(publicKey1, Convert.parseHexString(split[0]));
        }
        System.out.println("Your decrypted message in hexadecimal format:");
        System.out.println(Convert.toHexString(result));

        if (isConvert) {
            System.out.println("Result message is:");
            System.out.println(new String(result, "UTF-8"));
        }
    }
}
