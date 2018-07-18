package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.updater.RSAUtil;
import com.apollocurrency.aplwallet.apl.util.Convert;

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

            System.out.println("Enter encrypted message in hexadecimal format");
            encryptedMessageString = sc.nextLine();

            System.out.println("Enter certificate path");
            certificatePathString = sc.nextLine();

            System.out.println("Do you want to convert bytes to UTF-8 string ('true' or 'false')");
            convertToString = sc.nextLine();

            sc.close();

        } else if (args.length == 3) {
            certificatePathString = args[0];
            encryptedMessageString = args[1];
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

        boolean isConvert;
        try {
            isConvert = Boolean.parseBoolean(convertToString);
        }
        catch (Exception e) {
            isConvert = false;
        }
        byte[] encryptedMessageBytes = Convert.parseHexString(encryptedMessageString);

        byte[] decryptedBytes = RSAUtil.decrypt(certificatePathString, encryptedMessageBytes);

        System.out.println("Your decrypted message in hexadecimal format: ");
        System.out.println(Convert.toHexString(decryptedBytes));

        if (isConvert) {
            System.out.println("Result message is:");
            System.out.println(new String(decryptedBytes, "UTF-8"));
        }
    }
}
